package com.musicapp.mp3lyricsapp20;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeniusApiClient {

    public record SearchResult(String title, String artist, String url) {}

    private static final String API = "https://api.genius.com";
    private final String token;
    private final OkHttpClient http = new OkHttpClient();

    public GeniusApiClient(String token) {
        this.token = token;
    }

    public static GeniusApiClient fromEnv() {
        return new GeniusApiClient(System.getenv("GENIUS_API_TOKEN"));
    }

    public SearchResult searchTopHit(String artist, String track) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GENIUS_API_TOKEN is missing. Set it in Run Configuration / OS env.");
        }

        String q = java.net.URLEncoder.encode(artist + " " + track, "UTF-8");
        String url = API + "/search?q=" + q;

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (resp.code() == 401) {
                throw new IllegalStateException("Genius API 401 Unauthorized. Check GENIUS_API_TOKEN.");
            }
            if (!resp.isSuccessful()) {
                throw new IllegalStateException("Genius API error: HTTP " + resp.code());
            }

            String body = resp.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray hits = root.getAsJsonObject("response").getAsJsonArray("hits");
            if (hits == null || hits.isEmpty()) return null;

            JsonObject result = hits.get(0).getAsJsonObject().getAsJsonObject("result");
            String title = getString(result, "title");
            String urlSong = getString(result, "url");

            String artistName = "";
            if (result.has("primary_artist") && result.get("primary_artist").isJsonObject()) {
                artistName = getString(result.getAsJsonObject("primary_artist"), "name");
            }

            return new SearchResult(title, artistName, urlSong);
        }
    }

    private String getString(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : "";
    }
}
