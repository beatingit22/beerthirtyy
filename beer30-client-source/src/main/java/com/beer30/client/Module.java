package com.beer30.client;

import java.util.List;

/** A toggleable client feature. */
public final class Module {
	public final String id, name, description;
	public final boolean defaultOn;

	private Module(String id, String name, String description, boolean defaultOn) {
		this.id = id; this.name = name; this.description = description; this.defaultOn = defaultOn;
	}

	public boolean on() { return Config.isEnabled(this); }

	public static final Module FPS = new Module("fps", "FPS", "Frames per second", true);
	public static final Module CPS = new Module("cps", "CPS", "Clicks per second (left | right)", true);
	public static final Module PING = new Module("ping", "Ping", "Server latency", true);
	public static final Module COORDS = new Module("coords", "Coordinates", "XYZ position", false);
	public static final Module FACING = new Module("facing", "Direction", "Compass direction", false);
	public static final Module CLOCK = new Module("clock", "Clock", "Real-world time", false);
	public static final Module KEYSTROKES = new Module("keystrokes", "Keystrokes", "WASD and mouse buttons", true);
	public static final Module ARMOR = new Module("armor", "Armor Status", "Armor and durability", true);
	public static final Module POTIONS = new Module("potions", "Potion Status", "Active effects", true);
	public static final Module SPRINT = new Module("sprint", "Toggle Sprint", "Sprint automatically while moving forward", false);
	public static final Module ZOOM = new Module("zoom", "Zoom", "Hold the zoom key to zoom in", true);
	public static final Module SPOTIFY = new Module("spotify", "Spotify", "Now playing bar + media keys", true);

	public static final List<Module> ALL = List.of(FPS, CPS, PING, COORDS, FACING, CLOCK, KEYSTROKES, ARMOR, POTIONS, SPRINT, ZOOM, SPOTIFY);
}
