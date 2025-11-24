package com.musicapp;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.time.Duration;
import java.util.Scanner;

public class MP3LyricsApp {
    private static AudioFile currentAudioFile;
    private static String currentFilePath;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  MP3 Metadata Editor & Lyrics Finder   ║");
        System.out.println("║         (Simple Console Version)       ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        boolean running = true;
        while (running) {
            displayMenu();
            System.out.print("\n📌 Выбери опцию (1-6): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    loadMP3File(scanner);
                    break;
                case "2":
                    viewMetadata();
                    break;
                case "3":
                    editMetadata(scanner);
                    break;
                case "4":
                    searchLyricsOnGenius(scanner);
                    break;
                case "5":
                    saveMetadata();
                    break;
                case "6":
                    System.out.println("\n👋 До свидания!");
                    running = false;
                    break;
                default:
                    System.out.println("❌ Неверная опция. Попробуй ещё раз.");
            }
        }
        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("\n┌─ МЕНЮ ───────────────────────────────────┐");
        System.out.println("│ 1. 📂 Загрузить MP3 файл                 │");
        System.out.println("│ 2. 📖 Посмотреть метаданные              │");
        System.out.println("│ 3. ✏️  Редактировать метаданные          │");
        System.out.println("│ 4. 🔍 Поиск текстов на Genius            │");
        System.out.println("│ 5. 💾 Сохранить изменения                │");
        System.out.println("│ 6. ❌ Выход                              │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    private static void loadMP3File(Scanner scanner) {
        System.out.print("\n📂 Введи путь к MP3 файлу: ");
        String path = scanner.nextLine().trim();

        try {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("❌ Файл не найден: " + path);
                return;
            }

            if (!path.toLowerCase().endsWith(".mp3")) {
                System.out.println("❌ Это не MP3 файл!");
                return;
            }

            currentAudioFile = AudioFileIO.read(file);
            currentFilePath = path;
            System.out.println("✅ Файл загружен успешно: " + file.getName());
        } catch (Exception e) {
            System.out.println("❌ Ошибка при загрузке файла: " + e.getMessage());
        }
    }

    private static void viewMetadata() {
        if (currentAudioFile == null) {
            System.out.println("❌ Сначала загрузи MP3 файл!");
            return;
        }

        Tag tag = currentAudioFile.getTag();
        System.out.println("\n📋 ТЕКУЩИЕ МЕТАДАННЫЕ:");
        System.out.println("───────────────────────────────────────");
        System.out.println("🎵 Название: " + getTagValue(tag, FieldKey.TITLE));
        System.out.println("🎤 Артист: " + getTagValue(tag, FieldKey.ARTIST));
        System.out.println("💿 Альбом: " + getTagValue(tag, FieldKey.ALBUM));
        System.out.println("📅 Год: " + getTagValue(tag, FieldKey.YEAR));
        System.out.println("🎼 Жанр: " + getTagValue(tag, FieldKey.GENRE));
        System.out.println("👥 Артист альбома: " + getTagValue(tag, FieldKey.ALBUM_ARTIST));
        System.out.println("───────────────────────────────────────");
    }

    private static void editMetadata(Scanner scanner) {
        if (currentAudioFile == null) {
            System.out.println("❌ Сначала загрузи MP3 файл!");
            return;
        }

        Tag tag = currentAudioFile.getTag();

        System.out.print("\n🎵 Новое название (пусто - пропустить): ");
        String title = scanner.nextLine().trim();
        if (!title.isEmpty()) {
            setTagField(tag, FieldKey.TITLE, title);
        }

        System.out.print("🎤 Новый артист (пусто - пропустить): ");
        String artist = scanner.nextLine().trim();
        if (!artist.isEmpty()) {
            setTagField(tag, FieldKey.ARTIST, artist);
        }

        System.out.print("💿 Новый альбом (пусто - пропустить): ");
        String album = scanner.nextLine().trim();
        if (!album.isEmpty()) {
            setTagField(tag, FieldKey.ALBUM, album);
        }

        System.out.print("📅 Новый год (пусто - пропустить): ");
        String year = scanner.nextLine().trim();
        if (!year.isEmpty()) {
            setTagField(tag, FieldKey.YEAR, year);
        }

        System.out.print("🎼 Новый жанр (пусто - пропустить): ");
        String genre = scanner.nextLine().trim();
        if (!genre.isEmpty()) {
            setTagField(tag, FieldKey.GENRE, genre);
        }

        System.out.println("✅ Метаданные обновлены. Нажми '5' для сохранения!");
    }

    private static void searchLyricsOnGenius(Scanner scanner) {
        System.out.print("\n🎤 Введи имя артиста: ");
        String artist = scanner.nextLine().trim();

        System.out.print("🎵 Введи название песни: ");
        String track = scanner.nextLine().trim();

        if (artist.isEmpty() || track.isEmpty()) {
            System.out.println("❌ Поля не должны быть пустыми!");
            return;
        }

        System.out.println("🔍 Ищу текст на Genius...");

        try {
            String lyrics = fetchLyricsUsingChrome(artist, track);

            if (lyrics != null && !lyrics.isEmpty()) {
                System.out.println("\n✅ НАЙДЕНО!");
                System.out.println("───────────────────────────────────────");
                System.out.println("🎵 Песня: " + track);
                System.out.println("🎤 Артист: " + artist);
                System.out.println("───────────────────────────────────────");


                System.out.println("\n📝 ПОЛНЫЙ ТЕКСТ ПЕСНИ:\n");
                System.out.println(lyrics);

            } else {
                System.out.println("❌ Текст не найден на Genius.");
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка при поиске: " + e.getMessage());
        }
    }

    private static String fetchLyricsUsingChrome(String artist, String track) {
        WebDriver driver = null;
        try {
            System.out.println("   📡 Запускаю Chrome...");

            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            options.addArguments("--disable-extensions");

            driver = new ChromeDriver(options);

            String url = buildGeniusUrl(artist, track);
            System.out.println("   🔗 Открываю: " + url);

            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[data-lyrics-container=true]")));

            System.out.println("   📝 Извлекаю текст...");
            StringBuilder lyrics = new StringBuilder();

            java.util.List<WebElement> lyricsElements = driver.findElements(By.cssSelector("div[data-lyrics-container=true]"));

            if (lyricsElements.isEmpty()) {
                lyricsElements = driver.findElements(By.cssSelector("div[class*=Lyrics]"));
            }

            for (WebElement element : lyricsElements) {
                String text = element.getText().trim();
                if (text.length() > 10) {
                    lyrics.append(text).append("\n");
                }
            }

            String result = lyrics.toString().trim();

            if (result.isEmpty()) {
                System.out.println("   ❌ Текст пуст - песня не найдена");
                return null;
            }

            System.out.println("   ✅ Текст загружен (" + result.length() + " символов)");
            return result;

        } catch (Exception e) {
            System.out.println("   ⚠️  Ошибка: " + e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                System.out.println("   🔒 Закрываю Chrome...");
                try {
                    driver.quit();
                } catch (Exception ignored) {

                }
            }
        }
    }

    private static String buildGeniusUrl(String artist, String track) {
        String artistPart = artist.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "");

        String trackPart = track.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "");

        return "https://genius.com/" + artistPart + "-" + trackPart + "-lyrics";
    }

    private static void saveMetadata() {
        if (currentAudioFile == null) {
            System.out.println("❌ Нет загруженного файла!");
            return;
        }

        try {
            AudioFileIO.write(currentAudioFile);
            System.out.println("✅ Метаданные сохранены успешно!");
            System.out.println("📁 Файл: " + currentFilePath);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при сохранении: " + e.getMessage());
        }
    }

    private static String getTagValue(Tag tag, FieldKey field) {
        try {
            String value = tag.getFirst(field);
            return value != null && !value.isEmpty() ? value : "[не установлено]";
        } catch (Exception e) {
            return "[ошибка чтения]";
        }
    }

    private static void setTagField(Tag tag, FieldKey field, String value) {
        try {
            tag.setField(field, value);
        } catch (FieldDataInvalidException e) {
            System.out.println("⚠️  Предупреждение: некорректное значение");
        } catch (Exception e) {
            System.out.println("⚠️  Ошибка: " + e.getMessage());
        }
    }
}
