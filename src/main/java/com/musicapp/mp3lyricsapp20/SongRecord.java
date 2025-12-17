package com.musicapp.mp3lyricsapp20;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SongRecord {
    private final int id;

    private final StringProperty title;
    private final StringProperty artist;
    private final StringProperty album;
    private final StringProperty year;
    private final StringProperty genre;

    private final StringProperty lyrics;
    private final String filePath;

    public SongRecord(int id, String title, String artist, String album,
                      String year, String genre, String lyrics, String filePath) {
        this.id = id;
        this.title = new SimpleStringProperty(title);
        this.artist = new SimpleStringProperty(artist);
        this.album = new SimpleStringProperty(album);
        this.year = new SimpleStringProperty(year);
        this.genre = new SimpleStringProperty(genre);
        this.lyrics = new SimpleStringProperty(lyrics);
        this.filePath = filePath;
    }

    public int getId() { return id; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }
    public void setTitle(String v) { title.set(v); }

    public String getArtist() { return artist.get(); }
    public StringProperty artistProperty() { return artist; }
    public void setArtist(String v) { artist.set(v); }

    public String getAlbum() { return album.get(); }
    public StringProperty albumProperty() { return album; }
    public void setAlbum(String v) { album.set(v); }

    public String getYear() { return year.get(); }
    public StringProperty yearProperty() { return year; }
    public void setYear(String v) { year.set(v); }

    public String getGenre() { return genre.get(); }
    public StringProperty genreProperty() { return genre; }
    public void setGenre(String v) { genre.set(v); }

    public String getLyrics() { return lyrics.get(); }
    public StringProperty lyricsProperty() { return lyrics; }
    public void setLyrics(String v) { lyrics.set(v); }

    public String getFilePath() { return filePath; }
}
