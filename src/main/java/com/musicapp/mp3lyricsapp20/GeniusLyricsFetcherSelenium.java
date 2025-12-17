package com.musicapp.mp3lyricsapp20;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeniusLyricsFetcherSelenium {

    private final boolean headless;

    public GeniusLyricsFetcherSelenium(boolean headless) {
        this.headless = headless;
    }

    public String fetchLyrics(String geniusSongUrl) {
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();

            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.managed_default_content_settings.images", 2);
            options.setExperimentalOption("prefs", prefs);

            options.setExperimentalOption("detach", false);


            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1400,900");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--lang=ru-RU");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            if (headless) {
                options.addArguments("--headless=new");
            }

            driver = new ChromeDriver(options);
            driver.get(geniusSongUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            By lyricsSelector = By.cssSelector("div[data-lyrics-container='true']");

            By fallbackSelector = By.cssSelector("div[class*='Lyrics__Container']");

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(lyricsSelector),
                    ExpectedConditions.presenceOfAllElementsLocatedBy(fallbackSelector)
            ));

            List<WebElement> blocks = driver.findElements(lyricsSelector);
            if (blocks.isEmpty()) blocks = driver.findElements(fallbackSelector);
            if (blocks.isEmpty()) return null;

            JavascriptExecutor js = (JavascriptExecutor) driver;
            StringBuilder sb = new StringBuilder();

            for (WebElement b : blocks) {
                String t = (String) js.executeScript("return arguments[0].innerText;", b);


                if (t == null || t.trim().isEmpty()) {
                    t = (String) js.executeScript("return arguments[0].textContent;", b);
                }

                if (t != null) {
                    t = t.trim();
                    if (!t.isEmpty()) {
                        if (!sb.isEmpty()) sb.append("\n\n");
                        sb.append(t);
                    }
                }
            }

            String lyrics = normalizeLyrics(sb.toString());
            return (lyrics == null || lyrics.isBlank()) ? null : lyrics;

        } catch (Exception e) {
            return null;
        } finally {
            if (driver != null) {
                try { driver.close(); } catch (Exception ignored) {}
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    private String normalizeLyrics(String raw) {
        if (raw == null) return null;

        String t = raw;


        t = t.replace("\r\n", "\n").replace("\r", "\n");


        t = t.replaceAll("(?s)<[^>]*>", "");

        t = t.replaceAll("(?m)^\\s*\\d+\\s+Contributors.*$", "");
        t = t.replaceAll("(?m)^\\s*Contributors.*$", "");
        t = t.replaceAll("(?m)^\\s*Lyrics\\s*$", "");
        t = t.replaceAll("(?m)^\\s*Embed\\s*$", "");


        t = t.replaceAll("(?m)^\\s*\\[Текст песни.*\\]\\s*$", "");


        int idx = t.indexOf("\n[");
        if (idx > 0) {
            t = t.substring(idx + 1);
        } else if (!t.startsWith("[") && t.contains("[")) {
            int i2 = t.indexOf("[");
            if (i2 > 0) t = t.substring(i2);
        }


        t = t.replaceAll("(?m)^(\\[[^\\]]+\\])\\s*", "$1\n");


        t = t.replaceAll("\n{3,}", "\n\n");

        return t.trim();
    }
}
