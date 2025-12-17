package com.musicapp.mp3lyricsapp20;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Separator;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.List;

public class MP3LyricsGUI extends Application {

    private static final boolean LYRICS_HEADLESS = false; // быстрее true, но может блокировать

    private DatabaseManager db;
    private GeniusApiClient geniusApi;
    private GeniusLyricsFetcherSelenium lyricsFetcher;

    private final ObservableList<SongRecord> songs = FXCollections.observableArrayList();
    private TableView<SongRecord> table;

    private Label status;
    private TextArea lyricsArea;
    private TextArea historyArea;
    private ProgressBar progress;

    @Override
    public void start(Stage stage) {
        db = new DatabaseManager();
        geniusApi = GeniusApiClient.fromEnv();
        lyricsFetcher = new GeniusLyricsFetcherSelenium(LYRICS_HEADLESS);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(createLibraryTab(), createSearchTab(), createHistoryTab());

        stage.setTitle("MP3 Lyrics Manager Pro");
        stage.setWidth(1100);
        stage.setHeight(800);
        stage.setScene(new Scene(tabs));
        stage.show();

        loadSongs();
        loadHistory();
    }

    private Tab createLibraryTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Library");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Button add = new Button("Add MP3");
        add.setOnAction(e -> addMp3());

        Button del = new Button("Delete");
        del.setOnAction(e -> deleteSelected());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadSongs());

        top.getChildren().addAll(title, add, del, refresh);

        table = new TableView<>();
        table.setEditable(true);
        table.setItems(songs);
        table.setPrefHeight(450);

        TableColumn<SongRecord, String> cTitle = new TableColumn<>("Title");
        cTitle.setPrefWidth(220);
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        cTitle.setCellFactory(TextFieldTableCell.forTableColumn());
        cTitle.setOnEditCommit(e -> { e.getRowValue().setTitle(e.getNewValue()); autoSave(e.getRowValue()); });

        TableColumn<SongRecord, String> cArtist = new TableColumn<>("Artist");
        cArtist.setPrefWidth(180);
        cArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        cArtist.setCellFactory(TextFieldTableCell.forTableColumn());
        cArtist.setOnEditCommit(e -> { e.getRowValue().setArtist(e.getNewValue()); autoSave(e.getRowValue()); });

        TableColumn<SongRecord, String> cAlbum = new TableColumn<>("Album");
        cAlbum.setPrefWidth(180);
        cAlbum.setCellValueFactory(new PropertyValueFactory<>("album"));
        cAlbum.setCellFactory(TextFieldTableCell.forTableColumn());
        cAlbum.setOnEditCommit(e -> { e.getRowValue().setAlbum(e.getNewValue()); autoSave(e.getRowValue()); });

        TableColumn<SongRecord, String> cYear = new TableColumn<>("Year");
        cYear.setPrefWidth(90);
        cYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        cYear.setCellFactory(TextFieldTableCell.forTableColumn());
        cYear.setOnEditCommit(e -> { e.getRowValue().setYear(e.getNewValue()); autoSave(e.getRowValue()); });

        TableColumn<SongRecord, String> cGenre = new TableColumn<>("Genre");
        cGenre.setPrefWidth(130);
        cGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        cGenre.setCellFactory(TextFieldTableCell.forTableColumn());
        cGenre.setOnEditCommit(e -> { e.getRowValue().setGenre(e.getNewValue()); autoSave(e.getRowValue()); });

        table.getColumns().addAll(cTitle, cArtist, cAlbum, cYear, cGenre);

        status = new Label("Ready");
        root.getChildren().addAll(top, new Separator(), table, status);

        return new Tab("Library", root);
    }

    private Tab createSearchTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Label title = new Label("Lyrics search (Genius)");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        TextField artist = new TextField();
        artist.setPromptText("Artist");
        artist.setPrefWidth(250);

        TextField track = new TextField();
        track.setPromptText("Track");
        track.setPrefWidth(250);

        Button search = new Button("Search");
        search.setOnAction(e -> searchLyrics(artist.getText(), track.getText()));

        progress = new ProgressBar(0);
        progress.setPrefWidth(220);

        box.getChildren().addAll(new Label("Artist:"), artist, new Label("Track:"), track, search, progress);

        lyricsArea = new TextArea();
        lyricsArea.setEditable(false);
        lyricsArea.setWrapText(true);
        lyricsArea.setPrefHeight(480);

        root.getChildren().addAll(title, box, new Separator(), lyricsArea);
        return new Tab("Search", root);
    }

    private Tab createHistoryTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Label title = new Label("History");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setWrapText(true);
        historyArea.setPrefHeight(520);

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadHistory());

        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            db.clearSearchHistory();
            loadHistory();
        });

        root.getChildren().addAll(title, historyArea, new HBox(10, refresh, clear));
        return new Tab("History", root);
    }

    private void addMp3() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        File f = fc.showOpenDialog(new Stage());
        if (f == null) return;

        try {
            AudioFile af = AudioFileIO.read(f);
            Tag tag = af.getTag();

            String title = getTag(tag, FieldKey.TITLE);
            String artist = getTag(tag, FieldKey.ARTIST);
            String album = getTag(tag, FieldKey.ALBUM);
            String year = getTag(tag, FieldKey.YEAR);
            String genre = getTag(tag, FieldKey.GENRE);

            db.upsertSong(title, artist, album, year, genre, "", f.getAbsolutePath());
            loadSongs();

            status.setText("Added: " + f.getName());
        } catch (Exception e) {
            status.setText("Add error: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        SongRecord s = table.getSelectionModel().getSelectedItem();
        if (s == null) {
            status.setText("Select a song to delete");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm");
        a.setHeaderText("Delete from library (DB)?");
        a.setContentText(s.getArtist() + " - " + s.getTitle());

        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        db.deleteSongByFilePath(s.getFilePath());
        loadSongs();
    }

    private void autoSave(SongRecord s) {
        status.setText("Auto-saving...");

        new Thread(() -> {
            db.upsertSong(
                    safe(s.getTitle()),
                    safe(s.getArtist()),
                    safe(s.getAlbum()),
                    safe(s.getYear()),
                    safe(s.getGenre()),
                    safe(s.getLyrics()),
                    s.getFilePath()
            );

            writeTagsToFile(s);

            Platform.runLater(() -> status.setText("Auto-saved"));
        }).start();
    }

    private void writeTagsToFile(SongRecord s) {
        try {
            AudioFile af = AudioFileIO.read(new File(s.getFilePath()));
            Tag tag = af.getTagOrCreateAndSetDefault();

            tag.setField(FieldKey.TITLE, safe(s.getTitle()));
            tag.setField(FieldKey.ARTIST, safe(s.getArtist()));
            tag.setField(FieldKey.ALBUM, safe(s.getAlbum()));
            tag.setField(FieldKey.YEAR, safe(s.getYear()));
            tag.setField(FieldKey.GENRE, safe(s.getGenre()));

            af.commit();
        } catch (Exception e) {
            Platform.runLater(() -> status.setText("Tag write error: " + e.getMessage()));
        }
    }

    private void searchLyrics(String artist, String track) {
        if (artist == null || artist.trim().isEmpty() || track == null || track.trim().isEmpty()) {
            lyricsArea.setText("Enter artist and track.");
            return;
        }

        String cached = db.getCachedLyrics(artist.trim(), track.trim());
        if (cached != null && !cached.isBlank()) {
            lyricsArea.setText(cached);
            return;
        }

        lyricsArea.setText("Searching...");
        progress.setProgress(-1);

        new Thread(() -> {
            try {
                GeniusApiClient.SearchResult r = geniusApi.searchTopHit(artist.trim(), track.trim());

                if (r == null) {
                    Platform.runLater(() -> {
                        progress.setProgress(0);
                        lyricsArea.setText("Not found on Genius.");
                    });
                    return;
                }

                String lyrics = lyricsFetcher.fetchLyrics(r.url());
                if (lyrics == null || lyrics.isBlank()) {
                    lyrics = "Lyrics not found / blocked.";
                }

                String finalLyrics = lyrics;
                Platform.runLater(() -> {
                    progress.setProgress(0);
                    lyricsArea.setText(finalLyrics);
                });

                db.saveSearchHistory(artist.trim(), track.trim(), lyrics);
                loadHistory();

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progress.setProgress(0);
                    lyricsArea.setText("Search error:\n" + ex.getMessage());
                });
            }
        }).start();
    }

    private void loadSongs() {
        new Thread(() -> {
            List<DatabaseManager.SongRow> rows = db.getAllSongs();
            ObservableList<SongRecord> fresh = FXCollections.observableArrayList();
            for (DatabaseManager.SongRow r : rows) {
                fresh.add(new SongRecord(r.id(), r.title(), r.artist(), r.album(), r.year(), r.genre(), r.lyrics(), r.filePath()));
            }
            Platform.runLater(() -> {
                songs.setAll(fresh);
                status.setText("Loaded: " + fresh.size());
            });
        }).start();
    }

    private void loadHistory() {
        new Thread(() -> {
            List<DatabaseManager.HistoryRow> rows = db.getSearchHistory();
            StringBuilder sb = new StringBuilder();
            for (DatabaseManager.HistoryRow r : rows) {
                sb.append(r.artist()).append(" - ").append(r.track())
                        .append(" @ ").append(r.searchDate()).append("\n")
                        .append(r.lyrics() == null ? "" : r.lyrics())
                        .append("\n\n");
            }
            String text = sb.length() == 0 ? "Empty." : sb.toString();
            Platform.runLater(() -> historyArea.setText(text));
        }).start();
    }

    private String getTag(Tag tag, FieldKey key) {
        try {
            if (tag == null) return "Unknown";
            String v = tag.getFirst(key);
            return (v == null || v.isBlank()) ? "Unknown" : v;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void stop() {
        db.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
