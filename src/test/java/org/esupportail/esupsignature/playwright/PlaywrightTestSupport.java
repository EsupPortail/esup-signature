package org.esupportail.esupsignature.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PlaywrightTestSupport {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightTestSupport.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Playwright playwright;
    private final Browser browser;
    private final Path testArtifactsDirectory;
    private final Duration timeout;
    private final List<UiSession> sessions = new ArrayList<>();

    public PlaywrightTestSupport(String testDisplayName, int viewportWidth, int viewportHeight, Duration timeout) {
        this.timeout = timeout;
        this.testArtifactsDirectory = buildArtifactsDirectory(testDisplayName);
        try {
            Files.createDirectories(testArtifactsDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire des artefacts Playwright.", e);
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(resolveHeadlessMode())
                .setArgs(List.of(
                        "--window-size=" + viewportWidth + "," + viewportHeight,
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-software-rasterizer"
                )));
    }

    public UiSession newSession(String sessionName) {
        Path sessionDirectory = testArtifactsDirectory.resolve(slugify(sessionName));
        Path videoDirectory = sessionDirectory.resolve("videos");
        try {
            Files.createDirectories(videoDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire vidéo Playwright.", e);
        }

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true)
                .setRecordVideoDir(videoDirectory));
        context.setDefaultTimeout(timeout.toMillis());
        context.setDefaultNavigationTimeout(timeout.toMillis());
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        Page page = context.newPage();
        page.setDefaultTimeout(timeout.toMillis());
        page.setDefaultNavigationTimeout(timeout.toMillis());

        UiSession session = new UiSession(slugify(sessionName), sessionDirectory, context, page);
        sessions.add(session);
        return session;
    }

    public void close(Throwable failure) {
        boolean keepArtifacts = failure != null;
        RuntimeException closeFailure = null;

        for (int i = sessions.size() - 1; i >= 0; i--) {
            try {
                sessions.get(i).close(keepArtifacts);
            } catch (RuntimeException e) {
                if (closeFailure == null) {
                    closeFailure = e;
                } else {
                    closeFailure.addSuppressed(e);
                }
            }
        }

        try {
            browser.close();
        } catch (PlaywrightException e) {
            if (closeFailure == null) {
                closeFailure = e;
            } else {
                closeFailure.addSuppressed(e);
            }
        }

        try {
            playwright.close();
        } catch (PlaywrightException e) {
            if (closeFailure == null) {
                closeFailure = e;
            } else {
                closeFailure.addSuppressed(e);
            }
        }

        if (!keepArtifacts) {
            deleteRecursively(testArtifactsDirectory);
        } else {
            writeTestIndex(testArtifactsDirectory);
        }

        Path artifactsRoot = testArtifactsDirectory.getParent();
        if (artifactsRoot != null) {
            writeRootIndex(artifactsRoot);
        }

        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    public static final class UiSession {
        private final String name;
        private final Path artifactsDirectory;
        private final BrowserContext context;
        private final Page page;

        private UiSession(String name, Path artifactsDirectory, BrowserContext context, Page page) {
            this.name = name;
            this.artifactsDirectory = artifactsDirectory;
            this.context = context;
            this.page = page;
        }

        public String name() {
            return name;
        }

        public BrowserContext context() {
            return context;
        }

        public Page page() {
            return page;
        }

        private void close(boolean keepArtifacts) {
            try {
                if (keepArtifacts && !page.isClosed()) {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(artifactsDirectory.resolve("failure.png"))
                            .setFullPage(true));
                }
            } catch (PlaywrightException e) {
                logger.warn("Impossible de capturer la capture d'écran Playwright pour " + name, e);
            }

            try {
                if (keepArtifacts) {
                    context.tracing().stop(new Tracing.StopOptions()
                            .setPath(artifactsDirectory.resolve("trace.zip")));
                } else {
                    context.tracing().stop();
                }
            } catch (PlaywrightException e) {
                logger.warn("Impossible d'arrêter le tracing Playwright pour " + name, e);
            }

            try {
                context.close();
            } catch (PlaywrightException e) {
                logger.warn("Impossible de fermer le contexte Playwright pour " + name, e);
            }

            if (!keepArtifacts) {
                deleteRecursively(artifactsDirectory);
            }
        }
    }

    private static Path buildArtifactsDirectory(String testDisplayName) {
        String root = System.getProperty("playwright.artifacts.dir", "target/playwright-artifacts");
        String suffix = TIMESTAMP_FORMAT.format(java.time.LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        return Path.of(root, slugify(testDisplayName) + "-" + suffix);
    }

    private static boolean resolveHeadlessMode() {
        String configuredValue = firstNonBlank(
                System.getProperty("playwright.headless"),
                System.getenv("PLAYWRIGHT_HEADLESS"),
                System.getProperty("selenium.headless"),
                System.getenv("SELENIUM_HEADLESS")
        );
        if (configuredValue != null) {
            return Boolean.parseBoolean(configuredValue);
        }
        String display = System.getenv("DISPLAY");
        boolean headless = display == null || display.isBlank();
        if (headless) {
            logger.warn("Headless mode activated for Playwright");
        }
        return headless;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String slugify(String input) {
        String normalized = input == null ? "playwright-test" : input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "playwright-test" : normalized;
    }

    private static void writeRootIndex(Path artifactsRoot) {
        try {
            Files.createDirectories(artifactsRoot);
            List<Path> testDirectories;
            try (var stream = Files.list(artifactsRoot)) {
                testDirectories = stream
                        .filter(Files::isDirectory)
                        .sorted()
                        .toList();
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">")
                    .append("<title>Playwright artifacts</title>")
                    .append("<style>body{font-family:Arial,sans-serif;margin:2rem;}ul{line-height:1.8;}code{background:#f3f3f3;padding:.2rem .4rem;border-radius:4px;}</style>")
                    .append("</head><body><h1>Artefacts Playwright</h1>");

            if (testDirectories.isEmpty()) {
                html.append("<p>Aucun artefact conservé. Les tests ont probablement réussi sans erreur UI.</p>");
            } else {
                html.append("<p>Tests en échec avec artefacts conservés :</p><ul>");
                for (Path testDirectory : testDirectories) {
                    String directoryName = testDirectory.getFileName().toString();
                    html.append("<li><a href=\"")
                            .append(escapeHtml(directoryName))
                            .append("/index.html\">")
                            .append(escapeHtml(directoryName))
                            .append("</a></li>");
                }
                html.append("</ul>");
            }

            html.append("</body></html>");
            Files.writeString(artifactsRoot.resolve("index.html"), html.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de générer l'index HTML des artefacts Playwright.", e);
        }
    }

    private static void writeTestIndex(Path testArtifactsDirectory) {
        try {
            Files.createDirectories(testArtifactsDirectory);
            List<Path> files;
            try (var stream = Files.walk(testArtifactsDirectory)) {
                files = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.getFileName().toString().equals("index.html"))
                        .sorted()
                        .toList();
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">")
                    .append("<title>")
                    .append(escapeHtml(testArtifactsDirectory.getFileName().toString()))
                    .append("</title>")
                    .append("<style>body{font-family:Arial,sans-serif;margin:2rem;}li{margin:.5rem 0;}code{background:#f3f3f3;padding:.2rem .4rem;border-radius:4px;}</style>")
                    .append("</head><body><h1>")
                    .append(escapeHtml(testArtifactsDirectory.getFileName().toString()))
                    .append("</h1><p><a href=\"../index.html\">← Retour à la liste</a></p>");

            if (files.isEmpty()) {
                html.append("<p>Aucun fichier d'artefact trouvé.</p>");
            } else {
                html.append("<ul>");
                for (Path file : files) {
                    String relativePath = testArtifactsDirectory.relativize(file).toString().replace('\\', '/');
                    html.append("<li><a href=\"")
                            .append(escapeHtml(relativePath))
                            .append("\"><code>")
                            .append(escapeHtml(relativePath))
                            .append("</code></a></li>");
                }
                html.append("</ul>");
            }

            html.append("</body></html>");
            Files.writeString(testArtifactsDirectory.resolve("index.html"), html.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de générer l'index HTML détaillé des artefacts Playwright.", e);
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void deleteRecursively(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(currentPath -> {
                try {
                    Files.deleteIfExists(currentPath);
                } catch (IOException e) {
                    throw new UncheckedIOException("Impossible de supprimer le répertoire d'artefacts Playwright : " + currentPath, e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de parcourir le répertoire d'artefacts Playwright.", e);
        }
    }
}



