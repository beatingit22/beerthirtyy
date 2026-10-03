package com.beer30.automine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Pure logic for the Auto Miner (no Minecraft classes, so it can be unit tested):
 * boundary detection from the bedrock shell, flight-path planning and steering.
 *
 * Layout the detector expects, going outward:  [mineable blocks] [1 air gap] [bedrock shell]
 */
public final class MinerLogic {
	private MinerLogic() {}

	/** Read-only view of the world. Chunk coordinates for loaded(), block coordinates for the rest. */
	public interface Terrain {
		boolean loaded(int chunkX, int chunkZ);
		boolean bedrock(int x, int y, int z);
		boolean air(int x, int y, int z);
	}

	public static final class Bounds {
		public Integer wallXNeg, wallXPos, wallZNeg, wallZPos, wallYNeg, wallYPos; // bedrock coordinates, null = not found
		public int mineMinX, mineMaxX, mineMinZ, mineMaxZ;                        // blocks that may be mined (inside the air gap)
		public int mineMinY = Integer.MIN_VALUE, mineMaxY = Integer.MAX_VALUE;
		public double minX, maxX, minZ, maxZ;                                     // where the player may fly
		public double minY = Double.NEGATIVE_INFINITY, maxY = Double.POSITIVE_INFINITY;
		public int wallsFound;                                                    // horizontal walls found (0-4)

		public boolean valid() { return maxX - minX >= 12 && maxZ - minZ >= 12; }
		public boolean estimated() { return wallsFound < 4; }
		public String describe() {
			return String.format("X %d..%d  Z %d..%d%s%s", mineMinX, mineMaxX, mineMinZ, mineMaxZ,
				wallYNeg != null || wallYPos != null ? String.format("  Y %s..%s", mineMinY == Integer.MIN_VALUE ? "-" : mineMinY, mineMaxY == Integer.MAX_VALUE ? "-" : mineMaxY) : "",
				estimated() ? "  (" + wallsFound + "/4 walls seen, rest estimated)" : "  (bedrock shell verified)");
		}
	}

	private static final int[] Y_OFFSETS = {0, 1, -1, 3, -3, 6, -6, 12, -12};
	private static final int[][] COLUMNS = {{0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}, {6, 6}, {-6, -6}};

	/** Distance to the wall in direction (dx,dz), or -1. A wall only counts when the block in front of it (toward us) is air, seen at 2+ heights. */
	static int scanHorizontal(Terrain w, int px, int py, int pz, int dx, int dz, int range) {
		Map<Integer, Integer> votes = new HashMap<>();
		for (int off : Y_OFFSETS) {
			int y = py + off;
			for (int i = 1; i <= range; i++) {
				int x = px + dx * i, z = pz + dz * i;
				if (!w.loaded(x >> 4, z >> 4)) break;
				if (w.bedrock(x, y, z)) {
					if (w.air(x - dx, y, z - dz)) votes.merge(i, 1, Integer::sum);
					break;
				}
			}
		}
		return best(votes);
	}

	static int scanVertical(Terrain w, int px, int py, int pz, int dy, int range) {
		Map<Integer, Integer> votes = new HashMap<>();
		for (int[] c : COLUMNS) {
			int x = px + c[0], z = pz + c[1];
			for (int i = 1; i <= range; i++) {
				int y = py + dy * i;
				if (!w.loaded(x >> 4, z >> 4)) break;
				if (w.bedrock(x, y, z)) {
					if (w.air(x, y - dy, z)) votes.merge(i, 1, Integer::sum);
					break;
				}
			}
		}
		return best(votes);
	}

	private static int best(Map<Integer, Integer> votes) {
		int bestDist = -1, bestCount = 1; // need at least 2 agreeing samples
		for (Map.Entry<Integer, Integer> e : votes.entrySet()) {
			if (e.getValue() > bestCount || (e.getValue() == bestCount && bestDist != -1 && e.getKey() < bestDist)) { bestCount = e.getValue(); bestDist = e.getKey(); }
		}
		return bestCount >= 2 ? bestDist : -1;
	}

	public static Bounds detect(Terrain w, int px, int py, int pz, int margin, int fallbackRadius) {
		Bounds b = new Bounds();
		int xp = scanHorizontal(w, px, py, pz, 1, 0, 256), xn = scanHorizontal(w, px, py, pz, -1, 0, 256);
		int zp = scanHorizontal(w, px, py, pz, 0, 1, 256), zn = scanHorizontal(w, px, py, pz, 0, -1, 256);
		if (xp > 0) { b.wallXPos = px + xp; b.wallsFound++; }
		if (xn > 0) { b.wallXNeg = px - xn; b.wallsFound++; }
		if (zp > 0) { b.wallZPos = pz + zp; b.wallsFound++; }
		if (zn > 0) { b.wallZNeg = pz - zn; b.wallsFound++; }
		// mineable area: wall, then 1 air gap, then blocks => first mineable block is 2 in from the bedrock
		b.mineMaxX = b.wallXPos != null ? b.wallXPos - 2 : px + fallbackRadius;
		b.mineMinX = b.wallXNeg != null ? b.wallXNeg + 2 : px - fallbackRadius;
		b.mineMaxZ = b.wallZPos != null ? b.wallZPos - 2 : pz + fallbackRadius;
		b.mineMinZ = b.wallZNeg != null ? b.wallZNeg + 2 : pz - fallbackRadius;
		b.minX = b.mineMinX + margin; b.maxX = b.mineMaxX + 1 - margin;
		b.minZ = b.mineMinZ + margin; b.maxZ = b.mineMaxZ + 1 - margin;
		int yp = scanVertical(w, px, py, pz, 1, 320), yn = scanVertical(w, px, py, pz, -1, 320);
		if (yp > 0) { b.wallYPos = py + yp; b.mineMaxY = b.wallYPos - 2; b.maxY = b.wallYPos - 4; }
		if (yn > 0) { b.wallYNeg = py - yn; b.mineMinY = b.wallYNeg + 2; b.minY = b.wallYNeg + 3; }
		return b;
	}

	/** May this block be mined? Never anything outside the air gap, whatever the crosshair says. */
	public static boolean canMine(Bounds b, int x, int y, int z) {
		return x >= b.mineMinX && x <= b.mineMaxX && z >= b.mineMinZ && z <= b.mineMaxZ && y >= b.mineMinY && y <= b.mineMaxY;
	}

	// ---------- flight paths ----------
	public static final class Plan {
		public final double cx, cz, rx, rz;
		public final int dir; // +1 / -1
		Plan(double cx, double cz, double rx, double rz, int dir) { this.cx = cx; this.cz = cz; this.rx = rx; this.rz = rz; this.dir = dir; }
		public String describe() { return String.format("%s ring r=%.0f x %.0f", dir > 0 ? "clockwise" : "counter-clockwise", rx, rz); }
	}

	/** A fresh loop that fits inside the flight limits. radiusCap 0 = as big as the area allows. */
	public static Plan newPlan(Random rnd, Bounds b, int radiusCap) {
		double halfX = (b.maxX - b.minX) / 2, halfZ = (b.maxZ - b.minZ) / 2, cap = radiusCap > 0 ? radiusCap : 1e9;
		double maxRx = Math.max(1, Math.min(halfX, cap)), maxRz = Math.max(1, Math.min(halfZ, cap));
		double rx = Math.min(8, maxRx) + rnd.nextDouble() * (maxRx - Math.min(8, maxRx)); // never tighter than the turning circle when avoidable
		double rz = rnd.nextDouble() < 0.5 ? Math.min(rx, maxRz) : Math.min(8, maxRz) + rnd.nextDouble() * (maxRz - Math.min(8, maxRz));
		double cx = b.minX + rx + rnd.nextDouble() * Math.max(0, (b.maxX - b.minX) - 2 * rx);
		double cz = b.minZ + rz + rnd.nextDouble() * Math.max(0, (b.maxZ - b.minZ) - 2 * rz);
		return new Plan(cx, cz, rx, rz, rnd.nextBoolean() ? 1 : -1);
	}

	/** Yaw (Minecraft convention: 0 = +Z, 90 = -X) to steer toward the next point on the loop; heads for the middle if outside the limits. */
	public static double targetYaw(Plan p, double px, double pz, Bounds b) {
		double tx, tz;
		if (px < b.minX - 0.5 || px > b.maxX + 0.5 || pz < b.minZ - 0.5 || pz > b.maxZ + 0.5) {
			tx = (b.minX + b.maxX) / 2; tz = (b.minZ + b.maxZ) / 2;
		} else {
			double ang = Math.atan2((pz - p.cz) / p.rz, (px - p.cx) / p.rx) + p.dir * 0.30;
			tx = p.cx + p.rx * Math.cos(ang); tz = p.cz + p.rz * Math.sin(ang);
		}
		return Math.toDegrees(Math.atan2(-(tx - px), tz - pz));
	}

	/** Smooth turn: how many degrees to rotate this tick for a given error. */
	public static float turn(float delta) { return Math.max(-8f, Math.min(8f, delta * 0.4f)); }
}
