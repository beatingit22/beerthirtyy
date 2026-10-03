package com.beer30.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Talks to the Beer30 launcher on localhost (it holds the Spotify login; this mod never sees tokens).
 * The launcher passes -Dbeer30.bridge=127.0.0.1:PORT and -Dbeer30.token=SECRET when it starts the game.
 */
public final class SpotifyBridge {
	public static final class State {
		public boolean active, playing;
		public String title = "", artist = "", device = "";
		public long progressMs, durationMs, receivedAt;
		public long progressNow() {
			long p = progressMs + (playing ? System.currentTimeMillis() - receivedAt : 0);
			return durationMs > 0 ? Math.min(p, durationMs) : p;
		}
	}

	private static final Gson GSON = new Gson();
	private static final String BASE = System.getProperty("beer30.bridge") == null ? null : "http://" + System.getProperty("beer30.bridge");
	private static final String TOKEN = System.getProperty("beer30.token", "");
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

	private static volatile State state = new State();
	private static volatile String error = "";
	private static volatile boolean polling = false;

	private SpotifyBridge() {}

	public static boolean available() { return BASE != null; }
	public static State state() { return state; }
	public static String error() { return error; }

	/** Start background polling (every 2 s while the Spotify module is on). */
	public static void start() {
		if (!available()) return;
		ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "beer30-spotify"); t.setDaemon(true); return t; });
		ex.scheduleWithFixedDelay(() -> { if (Module.SPOTIFY.on() && !polling) poll(); }, 1, 2, TimeUnit.SECONDS);
	}

	private static void poll() {
		polling = true;
		try {
			HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/spotify/state")).timeout(Duration.ofSeconds(4)).header("X-Beer30-Token", TOKEN).GET().build();
			HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
			JsonObject o = GSON.fromJson(res.body(), JsonObject.class);
			if (res.statusCode() != 200 || o == null || o.has("error")) {
				error = o != null && o.has("error") ? o.get("error").getAsString() : "HTTP " + res.statusCode();
				state = new State();
				return;
			}
			State s = new State();
			s.active = o.has("active") && o.get("active").getAsBoolean();
			if (s.active) {
				s.playing = o.get("playing").getAsBoolean();
				s.title = o.get("title").getAsString();
				s.artist = o.get("artist").getAsString();
				s.progressMs = o.get("progressMs").getAsLong();
				s.durationMs = o.get("durationMs").getAsLong();
				s.device = o.has("device") && !o.get("device").isJsonNull() ? o.get("device").getAsString() : "";
			}
			s.receivedAt = System.currentTimeMillis();
			error = "";
			state = s;
		} catch (Exception e) {
			error = "launcher not reachable";
		} finally {
			polling = false;
		}
	}

	/** cmd: play | pause | next | previous */
	public static void send(String cmd) {
		if (!available()) return;
		State s = state;
		if (cmd.equals("toggle")) cmd = s.playing ? "pause" : "play";
		final String c = cmd;
		HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/spotify/cmd")).timeout(Duration.ofSeconds(4))
			.header("X-Beer30-Token", TOKEN).header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"cmd\":\"" + c + "\"}")).build();
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
			if (r.statusCode() != 200) {
				try { JsonObject o = GSON.fromJson(r.body(), JsonObject.class); error = o.get("error").getAsString(); } catch (Exception ignored) { error = "HTTP " + r.statusCode(); }
			} else {
				// optimistic update so the HUD reacts instantly; the next poll corrects it
				State n = state; if (c.equals("play")) n.playing = true; if (c.equals("pause")) n.playing = false;
				if (!poll_busy()) java.util.concurrent.CompletableFuture.runAsync(SpotifyBridge::pollSoon);
			}
		});
	}

	private static boolean poll_busy() { return polling; }
	private static void pollSoon() { try { Thread.sleep(500); } catch (InterruptedException ignored) {} if (!polling) poll(); }
}
