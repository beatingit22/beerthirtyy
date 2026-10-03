package com.beer30.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Draws every enabled HUD module. */
public final class Hud {
	static final int BG = 0x99071C3A, ACCENT = 0xFF38BDF8, TEXT = 0xFFE0F2FE, DIM = 0xFF7DD3FC, KEY_ON = 0xCC38BDF8, KEY_OFF = 0x99071C3A;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private Hud() {}

	public static void render(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden) return;
		TextRenderer tr = mc.textRenderer;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

		// top-left info lines
		List<String> lines = new ArrayList<>();
		if (Module.FPS.on()) lines.add("FPS: " + mc.getCurrentFps());
		if (Module.CPS.on()) lines.add("CPS: " + Beer30Client.cps(false) + " | " + Beer30Client.cps(true));
		if (Module.PING.on()) {
			PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
			lines.add("Ping: " + (e == null ? "-" : e.getLatency() + " ms"));
		}
		if (Module.COORDS.on()) lines.add(String.format("XYZ: %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
		if (Module.FACING.on()) lines.add("Facing: " + mc.player.getHorizontalFacing().getName());
		if (Module.CLOCK.on()) lines.add("Time: " + LocalTime.now().format(TIME));
		if (Module.SPRINT.on()) lines.add("Sprint: Toggled");
		int y = 4;
		for (String s : lines) {
			int tw = tr.getWidth(s);
			ctx.fill(4, y, 4 + tw + 6, y + 12, BG);
			ctx.fill(4, y, 6, y + 12, ACCENT);
			ctx.drawText(tr, s, 9, y + 2, TEXT, true);
			y += 14;
		}

		if (Module.KEYSTROKES.on()) keystrokes(ctx, mc, tr, 6, h / 2 - 20);
		if (Module.ARMOR.on()) armor(ctx, mc, tr, w - 52, h / 2 - 34);
		if (Module.POTIONS.on()) potions(ctx, mc, tr, w - 4, 4);
		if (Module.SPOTIFY.on() && SpotifyBridge.available()) spotify(ctx, tr, w);
	}

	private static void key(DrawContext ctx, TextRenderer tr, int x, int y, int bw, String label, boolean down) {
		ctx.fill(x, y, x + bw, y + 18, down ? KEY_ON : KEY_OFF);
		ctx.drawBorder(x, y, bw, 18, ACCENT);
		ctx.drawText(tr, label, x + (bw - tr.getWidth(label)) / 2, y + 5, down ? 0xFF07111E : TEXT, false);
	}

	private static void keystrokes(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int x, int y) {
		var o = mc.options;
		key(ctx, tr, x + 20, y, 18, "W", o.forwardKey.isPressed());
		key(ctx, tr, x, y + 20, 18, "A", o.leftKey.isPressed());
		key(ctx, tr, x + 20, y + 20, 18, "S", o.backKey.isPressed());
		key(ctx, tr, x + 40, y + 20, 18, "D", o.rightKey.isPressed());
		key(ctx, tr, x, y + 40, 28, "LMB", o.attackKey.isPressed());
		key(ctx, tr, x + 30, y + 40, 28, "RMB", o.useKey.isPressed());
	}

	private static void armor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int x, int y) {
		for (int i = 3; i >= 0; i--) { // head -> feet
			ItemStack s = mc.player.getInventory().armor.get(i);
			if (s.isEmpty()) continue;
			ctx.fill(x - 2, y - 1, x + 50, y + 17, BG);
			ctx.drawItem(s, x, y);
			if (s.isDamageable()) {
				int left = s.getMaxDamage() - s.getDamage();
				float f = (float) left / s.getMaxDamage();
				ctx.drawText(tr, String.valueOf(left), x + 20, y + 4, f < 0.2f ? 0xFFFCA5A5 : TEXT, true);
			}
			y += 18;
		}
	}

	private static void potions(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int right, int y) {
		for (StatusEffectInstance e : mc.player.getStatusEffects()) {
			String name = e.getEffectType().value().getName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
			int secs = e.getDuration() / 20;
			String s = name + "  " + (e.getDuration() < 0 || e.getDuration() > 1_000_000 ? "**" : (secs / 60) + ":" + String.format("%02d", secs % 60));
			int tw = tr.getWidth(s);
			ctx.fill(right - tw - 8, y, right, y + 12, BG);
			ctx.fill(right - 2, y, right, y + 12, e.getEffectType().value().isBeneficial() ? ACCENT : 0xFFFCA5A5);
			ctx.drawText(tr, s, right - tw - 5, y + 2, TEXT, true);
			y += 14;
		}
	}

	private static String fit(TextRenderer tr, String s, int max) {
		return tr.getWidth(s) <= max ? s : tr.trimToWidth(s, max - tr.getWidth("...")) + "...";
	}

	private static void spotify(DrawContext ctx, TextRenderer tr, int w) {
		SpotifyBridge.State s = SpotifyBridge.state();
		int bw = 190, x = (w - bw) / 2, y = 4;
		ctx.fill(x, y, x + bw, y + 30, BG);
		ctx.drawBorder(x, y, bw, 30, 0xFF1DB954);
		if (!s.active) {
			String msg = SpotifyBridge.error().isEmpty() ? "Spotify: nothing playing" : "Spotify: " + SpotifyBridge.error();
			ctx.drawText(tr, fit(tr, msg, bw - 10), x + 5, y + 11, DIM, false);
			return;
		}
		ctx.drawText(tr, fit(tr, (s.playing ? "\u25B6 " : "\u275A\u275A ") + s.title, bw - 10), x + 5, y + 4, TEXT, true);
		ctx.drawText(tr, fit(tr, s.artist, bw - 10), x + 5, y + 14, DIM, false);
		int barW = bw - 10, filled = s.durationMs > 0 ? (int) (barW * (double) s.progressNow() / s.durationMs) : 0;
		ctx.fill(x + 5, y + 25, x + 5 + barW, y + 27, 0xFF1E3A5F);
		ctx.fill(x + 5, y + 25, x + 5 + filled, y + 27, 0xFF1DB954);
	}
}
