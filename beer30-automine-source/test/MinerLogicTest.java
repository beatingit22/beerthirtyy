import com.beer30.automine.MinerLogic;
import java.util.Random;

public class MinerLogicTest {
	// A bedrock box: walls at x=-60/+80, z=-40/+100, floor y=-60 (just air-gapped at -59), ceiling y=100. Everything else solid stone except the air gap.
	static final int XN = -60, XP = 80, ZN = -40, ZP = 100, YN = -64, YP = 100;
	static boolean wallBlock(int x, int y, int z) { return x == XN || x == XP || z == ZN || z == ZP || y == YN || y == YP; }
	static boolean gap(int x, int y, int z) { return !wallBlock(x, y, z) && (x == XN + 1 || x == XP - 1 || z == ZN + 1 || z == ZP - 1 || y == YN + 1 || y == YP - 1); }
	static void check(boolean c, String m) { if (!c) { System.err.println("FAIL: " + m); System.exit(1); } }

	public static void main(String[] a) {
		MinerLogic.Terrain box = new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return true; }
			public boolean bedrock(int x, int y, int z) { return wallBlock(x, y, z) && x >= XN && x <= XP && z >= ZN && z <= ZP && y >= YN && y <= YP; }
			public boolean air(int x, int y, int z) { return gap(x, y, z); }
		};
		MinerLogic.Bounds b = MinerLogic.detect(box, 10, 0, 20, 2, 24);
		check(b.wallXNeg == XN && b.wallXPos == XP && b.wallZNeg == ZN && b.wallZPos == ZP, "all four walls found: " + b.describe());
		check(b.wallsFound == 4 && !b.estimated(), "verified shell");
		check(b.mineMinX == XN + 2 && b.mineMaxX == XP - 2 && b.mineMinZ == ZN + 2 && b.mineMaxZ == ZP - 2, "mineable area starts after the air gap");
		check(b.wallYNeg == YN && b.wallYPos == YP, "floor/ceiling found");
		check(MinerLogic.canMine(b, 0, 0, 0) && !MinerLogic.canMine(b, XN + 1, 0, 0) && !MinerLogic.canMine(b, XN, 0, 0) && !MinerLogic.canMine(b, 0, 0, ZP) && !MinerLogic.canMine(b, 0, YP - 1, 0), "never mines the gap or bedrock");
		check(b.minX == XN + 2 + 2 && b.maxX == XP - 2 + 1 - 2, "fly limits keep the margin");

		// natural bedrock floor (stone above it, no air gap) must NOT count as a shell
		MinerLogic.Terrain natural = new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return true; }
			public boolean bedrock(int x, int y, int z) { return y == -64; }
			public boolean air(int x, int y, int z) { return false; }
		};
		MinerLogic.Bounds nb = MinerLogic.detect(natural, 0, -40, 0, 2, 24);
		check(nb.wallYNeg == null && nb.wallsFound == 0 && nb.estimated(), "natural bedrock ignored");
		check(nb.mineMinX == -24 && nb.mineMaxX == 24, "fallback radius used when no walls");

		// a stray single bedrock block with air beside it must not be mistaken for a wall
		MinerLogic.Terrain stray = new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return true; }
			public boolean bedrock(int x, int y, int z) { return x == 15 && y == 0 && z == 0; }
			public boolean air(int x, int y, int z) { return true; }
		};
		check(MinerLogic.detect(stray, 0, 0, 0, 2, 24).wallXPos == null, "single stray block ignored");

		// holes in the wall: a few heights are open, the rest still agree
		MinerLogic.Terrain holey = new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return true; }
			public boolean bedrock(int x, int y, int z) { return box.bedrock(x, y, z) && !(x == XP && (y == 0 || y == 1)); }
			public boolean air(int x, int y, int z) { return box.air(x, y, z); }
		};
		check(MinerLogic.detect(holey, 10, 0, 20, 2, 24).wallXPos == XP, "wall with holes still found");

		// unloaded chunks: ray stops, wall reported missing, falls back
		MinerLogic.Terrain far = new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return Math.abs(cx) < 4 && Math.abs(cz) < 4; }
			public boolean bedrock(int x, int y, int z) { return box.bedrock(x, y, z); }
			public boolean air(int x, int y, int z) { return box.air(x, y, z); }
		};
		check(MinerLogic.detect(far, 10, 0, 20, 2, 24).wallXPos == null, "no wall beyond loaded chunks");

		// ---- simulate flying for 10 minutes with path switches: never leaves the fly limits ----
		Random rnd = new Random(7);
		double x = 10, z = 20, yaw = 0, speed = 0.55, maxOut = 0; int switches = 0, stuckRoutes = 0;
		MinerLogic.Plan plan = MinerLogic.newPlan(rnd, b, 0);
		double[] edges = {0, 0};
		for (int tick = 0; tick < 20 * 60 * 10; tick++) {
			if (tick % (20 * 45) == 0) { plan = MinerLogic.newPlan(rnd, b, tick % 3 == 0 ? 20 : 0); switches++; }
			double want = MinerLogic.targetYaw(plan, x, z, b);
			float d = (float) (((want - yaw) % 360 + 540) % 360 - 180);
			yaw += MinerLogic.turn(d);
			x += -Math.sin(Math.toRadians(yaw)) * speed; z += Math.cos(Math.toRadians(yaw)) * speed;
			maxOut = Math.max(maxOut, Math.max(Math.max(b.minX - x, x - b.maxX), Math.max(b.minZ - z, z - b.maxZ)));
			check(x > XN + 2 && x < XP - 1 && z > ZN + 2 && z < ZP - 1, "player stayed inside the air gap at tick " + tick);
		}
		System.out.printf("flew 10 min, %d path switches, max overshoot past fly limit: %.2f blocks%n", switches, Math.max(0, maxOut));
		check(maxOut < 3.0, "overshoot small");

		// small area is rejected; radius cap respected
		MinerLogic.Bounds tiny = new MinerLogic.Bounds(); tiny.minX = 0; tiny.maxX = 8; tiny.minZ = 0; tiny.maxZ = 30;
		check(!tiny.valid(), "tiny area invalid");
		for (int i = 0; i < 50; i++) { MinerLogic.Plan p = MinerLogic.newPlan(rnd, b, 12); check(p.rx <= 12.0001 && p.rz <= 12.0001, "radius cap"); }
		System.out.println("MinerLogic tests passed");
	}
}
