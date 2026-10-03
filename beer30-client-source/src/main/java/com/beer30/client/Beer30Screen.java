package com.beer30.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** The RSHIFT menu: toggle every module on or off, plus Spotify controls. */
public final class Beer30Screen extends Screen {
	private static final int COL_W = 124, ROW_H = 22, GAP = 6;

	public Beer30Screen() { super(Text.literal("Beer30 Client")); }

	private static Text label(Module m) { return Text.literal(m.name + ": " + (m.on() ? "ON" : "OFF")); }

	@Override
	protected void init() {
		int left = this.width / 2 - COL_W - GAP / 2;
		int top = this.height / 2 - 78;
		for (int i = 0; i < Module.ALL.size(); i++) {
			Module m = Module.ALL.get(i);
			int x = left + (i % 2) * (COL_W + GAP), y = top + (i / 2) * ROW_H;
			this.addDrawableChild(ButtonWidget.builder(label(m), b -> { Config.set(m, !m.on()); b.setMessage(label(m)); })
				.dimensions(x, y, COL_W, 20).build());
		}
		int rows = (Module.ALL.size() + 1) / 2, y = top + rows * ROW_H + 8;
		if (SpotifyBridge.available()) {
			int bw = 40, cx = this.width / 2;
			this.addDrawableChild(ButtonWidget.builder(Text.literal("|<"), b -> SpotifyBridge.send("previous")).dimensions(cx - bw - 24, y, bw, 20).build());
			this.addDrawableChild(ButtonWidget.builder(Text.literal("Play/Pause"), b -> SpotifyBridge.send("toggle")).dimensions(cx - 20 - 4, y, 68, 20).build());
			this.addDrawableChild(ButtonWidget.builder(Text.literal(">|"), b -> SpotifyBridge.send("next")).dimensions(cx + 48, y, bw, 20).build());
			y += 26;
		}
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close()).dimensions(this.width / 2 - 50, y, 100, 20).build());
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		int rows = (Module.ALL.size() + 1) / 2;
		int pw = COL_W * 2 + GAP + 24, ph = rows * ROW_H + 130;
		int x = (this.width - pw) / 2, y = this.height / 2 - 78 - 34;
		ctx.fill(x, y, x + pw, y + ph, 0xE0071C3A);
		ctx.drawBorder(x, y, pw, ph, Hud.ACCENT);
		ctx.fill(x, y, x + pw, y + 3, Hud.ACCENT);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, "BEER30 CLIENT", this.width / 2, this.height / 2 - 78 - 24, 0xFFE0F2FE);
		ctx.drawCenteredTextWithShadow(this.textRenderer, "Right Shift to close", this.width / 2, this.height / 2 - 78 - 13, 0xFF7DD3FC);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { this.close(); return true; }
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() { return false; }
}
