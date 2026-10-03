package com.beer30.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stores which modules are on/off in config/beer30-client.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("beer30-client.json");
	private static Map<String, Boolean> enabled = new LinkedHashMap<>();

	private Config() {}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				Map<String, Boolean> m = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<String, Boolean>>() {}.getType());
				if (m != null) enabled = m;
			}
		} catch (Exception e) {
			System.err.println("[Beer30] could not read config, using defaults: " + e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(enabled));
		} catch (Exception e) {
			System.err.println("[Beer30] could not save config: " + e);
		}
	}

	public static boolean isEnabled(Module m) {
		return enabled.getOrDefault(m.id, m.defaultOn);
	}

	public static void set(Module m, boolean on) {
		enabled.put(m.id, on);
		save();
	}
}
