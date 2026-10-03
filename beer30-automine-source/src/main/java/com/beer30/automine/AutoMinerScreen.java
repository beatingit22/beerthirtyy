package com.beer30.automine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Beer30 Auto Miner panel (Right Shift). */
public final class AutoMinerScreen extends Screen {
	private ButtonWidget startBtn, miningBtn, radiusLbl, switchLbl, marginLbl;

	public AutoMinerScreen() { super(Text.literal("Beer30 Auto Miner")); }

	private static Text startText() { return Text.literal(AutoMiner.running ? "STOP" : "START"); }
	private static Text miningText() { return Text.literal("Mining: " + (AutoMiner.cfg.mining ? "ON" : "OFF")); }
	private static Text radiusText() { return Text.literal("Max loop radius: " + (AutoMiner.cfg.maxRadius == 0 ? "auto" : AutoMiner.cfg.maxRadius)); }
	private static Text switchText() { return Text.literal("Change path every ~" + AutoMiner.cfg.switchSeconds + "s"); }
	private static Text marginText() { return Text.literal("Wall margin: " + AutoMiner.cfg.margin); }

	private ButtonWidget add(int x, int y, int w, Text t, ButtonWidget.PressAction a) {
		return this.addDrawableChild(ButtonWidget.builder(t, a).dimensions(x, y, w, 20).build());
	}

	@Override
	protected void init() {
		int cx = this.width / 2, y = this.height / 2 - 70;
		startBtn = add(cx - 124, y, 124, startText(), b -> { AutoMiner.toggle(MinecraftClient.getInstance()); b.setMessage(startText()); });
		add(cx + 4, y, 120, Text.literal("Detect boundary"), b -> AutoMiner.detect(MinecraftClient.getInstance()));
		y += 24;
		miningBtn = add(cx - 124, y, 248, miningText(), b -> { AutoMiner.cfg.mining = !AutoMiner.cfg.mining; AutoMiner.save(); b.setMessage(miningText()); });
		y += 24;
		radiusLbl = add(cx - 124, y, 190, radiusText(), b -> {});
		add(cx + 70, y, 26, Text.literal("-"), b -> { AutoMiner.cfg.maxRadius = Math.max(0, AutoMiner.cfg.maxRadius <= 16 ? 0 : AutoMiner.cfg.maxRadius - 8); AutoMiner.save(); radiusLbl.setMessage(radiusText()); });
		add(cx + 98, y, 26, Text.literal("+"), b -> { AutoMiner.cfg.maxRadius = AutoMiner.cfg.maxRadius == 0 ? 16 : AutoMiner.cfg.maxRadius + 8; AutoMiner.save(); radiusLbl.setMessage(radiusText()); });
		y += 24;
		switchLbl = add(cx - 124, y, 190, switchText(), b -> {});
		add(cx + 70, y, 26, Text.literal("-"), b -> { AutoMiner.cfg.switchSeconds -= 10; AutoMiner.save(); switchLbl.setMessage(switchText()); });
		add(cx + 98, y, 26, Text.literal("+"), b -> { AutoMiner.cfg.switchSeconds += 10; AutoMiner.save(); switchLbl.setMessage(switchText()); });
		y += 24;
		marginLbl = add(cx - 124, y, 190, marginText(), b -> {});
		add(cx + 70, y, 26, Text.literal("-"), b -> { AutoMiner.cfg.margin -= 1; AutoMiner.save(); marginLbl.setMessage(marginText()); });
		add(cx + 98, y, 26, Text.literal("+"), b -> { AutoMiner.cfg.margin += 1; AutoMiner.save(); marginLbl.setMessage(marginText()); });
		y += 70;
		add(cx - 60, y, 120, Text.literal("Done"), b -> this.close());
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		int x = this.width / 2 - 136, y = this.height / 2 - 104;
		ctx.fill(x, y, x + 272, y + 232, 0xE0071C3A);
		ctx.drawBorder(x, y, 272, 232, AutoMineMod.ACCENT);
		ctx.fill(x, y, x + 272, y + 3, AutoMineMod.ACCENT);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		int cx = this.width / 2, top = this.height / 2 - 70;
		ctx.drawCenteredTextWithShadow(this.textRenderer, "BEER30 AUTO MINER", cx, top - 28, 0xFFE0F2FE);
		ctx.drawCenteredTextWithShadow(this.textRenderer, "Aim where you want to mine. Pitch is yours.", cx, top - 16, 0xFF7DD3FC);
		startBtn.setMessage(startText());
		int y = top + 100;
		String[] lines = {
			"Status: " + AutoMiner.status,
			AutoMiner.bounds == null ? "Boundary: not scanned yet" : "Area: " + AutoMiner.bounds.describe(),
			AutoMiner.running && !AutoMiner.pathInfo.isEmpty() ? "Path: " + AutoMiner.pathInfo : "Z toggles from in-game. Rebind in Controls."
		};
		for (String s : lines) {
			String t = this.textRenderer.getWidth(s) > 256 ? this.textRenderer.trimToWidth(s, 250) + "..." : s;
			ctx.drawText(this.textRenderer, t, cx - 128, y, 0xFFBAE6FD, false);
			y += 11;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == AutoMineMod.menuCode) { this.close(); return true; }
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() { return false; }
}
