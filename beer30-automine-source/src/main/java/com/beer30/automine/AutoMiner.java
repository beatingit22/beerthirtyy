package com.beer30.automine;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Flies loops inside a bedrock-walled area and holds the attack button while the crosshair is on a minable block.
 * Pitch is never touched - aim where you want to mine, the bot only turns left/right along its path.
 * Safety: never attacks bedrock or anything outside the air gap, pauses with any menu open, stops on low health,
 * a nearly broken tool, leaving the world, or when you press the toggle key again.
 */
public final class AutoMiner {
	public static final class Settings {
		public int maxRadius = 0;       // 0 = as large as the area allows
		public int switchSeconds = 60;  // average time between path changes
		public int margin = 2;          // blocks kept between the player and the air gap
		public boolean mining = true;
	}

	public static Settings cfg = new Settings();
	public static boolean running;
	public static MinerLogic.Bounds bounds;
	public static String status = "Idle", pathInfo = "";

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("beer30-miner.json");
	private static final Random RNG = new Random();
	private static MinerLogic.Plan plan;
	private static ClientWorld startWorld;
	private static long tick, nextSwitch;
	private static double baseY, lastX, lastZ;

	private AutoMiner() {}

	public static void load() {
		try { if (Files.exists(FILE)) { Settings s = new Gson().fromJson(Files.readString(FILE), Settings.class); if (s != null) cfg = s; } }
		catch (Exception e) { System.err.println("[Beer30] miner settings unreadable, using defaults: " + e); }
		clamp();
	}

	public static void save() {
		clamp();
		try { Files.writeString(FILE, new Gson().toJson(cfg)); } catch (Exception e) { System.err.println("[Beer30] could not save miner settings: " + e); }
	}

	private static void clamp() {
		cfg.maxRadius = MathHelper.clamp(cfg.maxRadius, 0, 200);
		cfg.switchSeconds = MathHelper.clamp(cfg.switchSeconds, 10, 300);
		cfg.margin = MathHelper.clamp(cfg.margin, 1, 8);
	}

	private static MinerLogic.Terrain terrain(ClientWorld w) {
		return new MinerLogic.Terrain() {
			public boolean loaded(int cx, int cz) { return w.isChunkLoaded(cx, cz); }
			public boolean bedrock(int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)).isOf(Blocks.BEDROCK); }
			public boolean air(int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)).isAir(); }
		};
	}

	/** Scan from where the player stands. Returns the bounds (also stored for the GUI). */
	public static MinerLogic.Bounds detect(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) { status = "Join a world first"; return null; }
		bounds = MinerLogic.detect(terrain(mc.world), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), cfg.margin, 24);
		status = bounds.valid() ? "Boundary ready" : "Area too small (need 12+ blocks of room)";
		return bounds;
	}

	public static void toggle(MinecraftClient mc) { if (running) stop(mc, "stopped"); else start(mc); }

	public static void start(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) return;
		detect(mc);
		if (bounds == null || !bounds.valid()) { say(mc, "Auto Miner: " + status); return; }
		startWorld = mc.world; baseY = mc.player.getY(); tick = 0;
		if (bounds.minY != Double.NEGATIVE_INFINITY) baseY = Math.max(baseY, bounds.minY);
		if (bounds.maxY != Double.POSITIVE_INFINITY) baseY = Math.min(baseY, bounds.maxY);
		lastX = mc.player.getX(); lastZ = mc.player.getZ();
		newPlan();
		running = true; status = "Running";
		say(mc, "Auto Miner ON - " + (bounds.estimated() ? "estimated area" : "bedrock shell found"));
	}

	public static void stop(MinecraftClient mc, String why) {
		boolean was = running;
		running = false; status = "Stopped (" + why + ")";
		release(mc);
		if (was) say(mc, "Auto Miner OFF - " + why);
	}

	private static void say(MinecraftClient mc, String s) { if (mc.player != null) mc.player.sendMessage(Text.literal(s), true); }

	private static void release(MinecraftClient mc) {
		mc.options.forwardKey.setPressed(false);
		mc.options.jumpKey.setPressed(false);
		mc.options.sneakKey.setPressed(false);
		mc.options.attackKey.setPressed(false);
	}

	private static void newPlan() {
		plan = MinerLogic.newPlan(RNG, bounds, cfg.maxRadius);
		pathInfo = plan.describe();
		nextSwitch = tick + (long) (20.0 * cfg.switchSeconds * (0.6 + 0.8 * RNG.nextDouble()));
	}

	public static void tick(MinecraftClient mc) {
		if (!running) return;
		var p = mc.player;
		if (p == null || mc.world == null || mc.world != startWorld) { stop(mc, "left the world"); return; }
		if (mc.currentScreen != null) { release(mc); status = "Paused (menu open)"; return; }
		if (p.getHealth() <= 6.0f) { stop(mc, "health low"); return; }
		ItemStack tool = p.getMainHandStack();
		if (cfg.mining && tool.isDamageable() && tool.getMaxDamage() - tool.getDamage() <= 8) { stop(mc, "tool almost broken"); return; }
		tick++;
		status = "Running";

		boolean flying = p.getAbilities().flying;
		if (p.getAbilities().allowFlying && !flying) { p.getAbilities().flying = true; p.sendAbilitiesUpdate(); flying = true; }

		if (tick >= nextSwitch) newPlan();
		if (tick % 30 == 0) { // stuck? pick a new loop
			double moved = Math.hypot(p.getX() - lastX, p.getZ() - lastZ);
			lastX = p.getX(); lastZ = p.getZ();
			if (moved < 0.5) { newPlan(); status = "Re-routing (blocked)"; }
		}

		float delta = MathHelper.wrapDegrees((float) MinerLogic.targetYaw(plan, p.getX(), p.getZ(), bounds) - p.getYaw());
		p.setYaw(p.getYaw() + MinerLogic.turn(delta));
		mc.options.forwardKey.setPressed(true);

		if (flying) {
			double dy = baseY - p.getY();
			mc.options.jumpKey.setPressed(dy > 0.3);
			mc.options.sneakKey.setPressed(dy < -0.3);
		}

		boolean attack = false;
		if (cfg.mining && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos bp = hit.getBlockPos();
			attack = MinerLogic.canMine(bounds, bp.getX(), bp.getY(), bp.getZ()) && !mc.world.getBlockState(bp).isOf(Blocks.BEDROCK);
		}
		mc.options.attackKey.setPressed(attack);
	}
}
