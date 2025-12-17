package com.musicapp.mp3lyricsapp20;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    public record SongRow(
            int id,
            String title,
            String artist,
            String album,
            String year,
            String genre,
            String lyrics,
            String filePath
    ) {}

    public record HistoryRow(
            int id,
            String artist,
            String track,
            String lyrics,
            String searchDate
    ) {}

    private static final String DB_URL = "jdbc:sqlite:mp3_lyrics.db";
    private Connection connection;

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTablesIfNotExist();
        } catch (Exception e) {
            System.out.println("DB init error: " + e.getMessage());
        }
    }

    private void createTablesIfNotExist() {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS songs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT,
                        year TEXT,
                        genre TEXT,
                        lyrics TEXT,
                        file_path TEXT UNIQUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        artist TEXT NOT NULL,
                        track TEXT NOT NULL,
                        lyrics TEXT,
                        search_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

        } catch (SQLException e) {
            System.out.println("DB table error: " + e.getMessage());
        }
    }

    public void upsertSong(String title, String artist, String album, String year,
                           String genre, String lyrics, String filePath) {

        String sql = """
                INSERT INTO songs (title, artist, album, year, genre, lyrics, file_path)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(file_path) DO UPDATE SET
                    title=excluded.title,
                    artist=excluded.artist,
                    album=excluded.album,
                    year=excluded.year,
                    genre=excluded.genre,
                    lyrics=excluded.lyrics
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, artist);
            ps.setString(3, album);
            ps.setString(4, year);
            ps.setString(5, genre);
            ps.setString(6, lyrics);
            ps.setString(7, filePath);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DB upsertSong error: " + e.getMessage());
        }
    }

    public boolean deleteSongByFilePath(String filePath) {
        String sql = "DELETE FROM songs WHERE file_path = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, filePath);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("DB deleteSong error: " + e.getMessage());
            return false;
        }
    }

    public List<SongRow> getAllSongs() {
        String sql = "SELECT * FROM songs ORDER BY created_at DESC";
        List<SongRow> res = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                res.add(new SongRow(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("album"),
                        rs.getString("year"),
                        rs.getString("genre"),
                        rs.getString("lyrics"),
                        rs.getString("file_path")
                ));
            }
        } catch (SQLException e) {
            System.out.println("DB getAllSongs error: " + e.getMessage());
        }

        return res;
    }

    public void saveSearchHistory(String artist, String track, String lyrics) {
        String sql = "INSERT INTO search_history (artist, track, lyrics) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, artist);
            ps.setString(2, track);
            ps.setString(3, lyrics);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DB saveHistory error: " + e.getMessage());
        }
    }
    public String getCachedLyrics(String artist, String track) {
        String sql = """
                SELECT lyrics
                FROM search_history
                WHERE artist = ? AND track = ? AND lyrics IS NOT NULL AND TRIM(lyrics) <> ''
                ORDER BY search_date DESC
                LIMIT 1
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, artist);
            ps.setString(2, track);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("lyrics");
            }
        } catch (SQLException e) {
            System.out.println("DB getCachedLyrics error: " + e.getMessage());
        }

        return null;
    }

    public List<HistoryRow> getSearchHistory() {
        String sql = "SELECT id, artist, track, lyrics, search_date FROM search_history ORDER BY search_date DESC LIMIT 50";
        List<HistoryRow> res = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                res.add(new HistoryRow(
                        rs.getInt("id"),
                        rs.getString("artist"),
                        rs.getString("track"),
                        rs.getString("lyrics"),
                        rs.getString("search_date")
                ));
            }
        } catch (SQLException e) {
            System.out.println("DB getHistory error: " + e.getMessage());
        }

        return res;
    }

    public void clearSearchHistory() {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM search_history")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DB clearHistory error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.out.println("DB close error: " + e.getMessage());
        }
    }
}
