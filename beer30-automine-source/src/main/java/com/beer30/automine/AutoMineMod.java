package com.beer30.automine;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AutoMineMod implements ClientModInitializer {
	public static final int ACCENT = 0xFF38BDF8, BG = 0x99071C3A, TEXT = 0xFFE0F2FE, DIM = 0xFF7DD3FC;
	/** Right Shift opens the menu; if the Beer30 Client mod is also installed it already uses Right Shift, so this one moves to Right Ctrl. */
	public static int menuCode;
	private static KeyBinding menuKey, toggleKey;

	private static KeyBinding bind(String id, int key) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.beer30automine." + id, InputUtil.Type.KEYSYM, key, "key.categories.beer30automine"));
	}

	@Override
	public void onInitializeClient() {
		AutoMiner.load();
		menuCode = FabricLoader.getInstance().isModLoaded("beer30client") ? GLFW.GLFW_KEY_RIGHT_CONTROL : GLFW.GLFW_KEY_RIGHT_SHIFT;
		menuKey = bind("menu", menuCode);
		toggleKey = bind("toggle", GLFW.GLFW_KEY_Z);
		ClientTickEvents.END_CLIENT_TICK.register(AutoMineMod::tick);
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> hud(ctx));
	}

	private static void tick(MinecraftClient mc) {
		if (mc.player == null) { if (AutoMiner.running) AutoMiner.stop(mc, "left the world"); return; }
		boolean inGame = mc.currentScreen == null;
		while (menuKey.wasPressed()) if (inGame) mc.setScreen(new AutoMinerScreen());
		while (toggleKey.wasPressed()) if (inGame) AutoMiner.toggle(mc);
		AutoMiner.tick(mc);
	}

	private static void hud(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!AutoMiner.running || mc.options.hudHidden) return;
		String s = "AutoMiner: " + AutoMiner.status;
		int w = mc.textRenderer.getWidth(s);
		ctx.fill(4, 4, 4 + w + 8, 16, BG);
		ctx.fill(4, 4, 6, 16, ACCENT);
		ctx.drawText(mc.textRenderer, s, 9, 6, TEXT, true);
	}
}
