package com.beer30.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public final class Beer30Client implements ClientModInitializer {
	private static final String CAT = "key.categories.beer30client";
	private static KeyBinding menuKey, zoomKey, spToggle, spNext, spPrev;
	private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
	private static boolean wasLeft, wasRight, zooming;
	private static int savedFov;

	private static KeyBinding bind(String id, int key) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.beer30client." + id, InputUtil.Type.KEYSYM, key, CAT));
	}

	/** Clicks in the last second. */
	public static int cps(boolean right) {
		ArrayDeque<Long> q = right ? rightClicks : leftClicks;
		long cut = System.currentTimeMillis() - 1000;
		while (!q.isEmpty() && q.peekFirst() < cut) q.pollFirst();
		return q.size();
	}

	@Override
	public void onInitializeClient() {
		Config.load();
		menuKey = bind("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
		zoomKey = bind("zoom", GLFW.GLFW_KEY_C);
		spToggle = bind("sp_toggle", GLFW.GLFW_KEY_BACKSLASH);
		spNext = bind("sp_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
		spPrev = bind("sp_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
		SpotifyBridge.start();
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
		ClientTickEvents.END_CLIENT_TICK.register(Beer30Client::tick);
	}

	private static void tick(MinecraftClient mc) {
		if (mc.player == null) return;
		boolean inGame = mc.currentScreen == null;

		while (menuKey.wasPressed()) if (inGame) mc.setScreen(new Beer30Screen());

		// CPS: count presses (rising edges) of attack / use
		boolean l = mc.options.attackKey.isPressed(), r = mc.options.useKey.isPressed();
		if (l && !wasLeft) leftClicks.addLast(System.currentTimeMillis());
		if (r && !wasRight) rightClicks.addLast(System.currentTimeMillis());
		wasLeft = l; wasRight = r;

		// Toggle sprint: keep the sprint key down while walking forward
		if (Module.SPRINT.on() && inGame && mc.options.forwardKey.isPressed() && !mc.options.sneakKey.isPressed())
			mc.options.sprintKey.setPressed(true);

		// Zoom: temporarily narrow the FOV while the key is held
		boolean wantZoom = Module.ZOOM.on() && inGame && zoomKey.isPressed();
		if (wantZoom && !zooming) { savedFov = mc.options.getFov().getValue(); mc.options.getFov().setValue(30); zooming = true; }
		else if (!wantZoom && zooming) { mc.options.getFov().setValue(savedFov); zooming = false; }

		// Spotify media keys
		if (Module.SPOTIFY.on()) {
			while (spToggle.wasPressed()) SpotifyBridge.send("toggle");
			while (spNext.wasPressed()) SpotifyBridge.send("next");
			while (spPrev.wasPressed()) SpotifyBridge.send("previous");
		} else {
			while (spToggle.wasPressed() || spNext.wasPressed() || spPrev.wasPressed()) { /* module off: ignore */ }
		}
	}
}
