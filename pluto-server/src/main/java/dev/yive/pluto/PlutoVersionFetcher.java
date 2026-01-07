package dev.yive.pluto;

import static io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_SIMPLE;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

import com.destroystokyo.paper.VersionHistoryManager;
import com.destroystokyo.paper.util.VersionFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import io.papermc.paper.ServerBuildInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;

// 99% of this was copied from PaperVersionFetcher.
@DefaultQualifier(NonNull.class)
public class PlutoVersionFetcher implements VersionFetcher {
    private static final Logger LOGGER = LogUtils.getClassLogger();
    private static final ComponentLogger COMPONENT_LOGGER = ComponentLogger.logger(
        LogManager.getRootLogger().getName());
    private static final int DISTANCE_ERROR = -1;
    private static final int DISTANCE_UNKNOWN = -2;
    private static final String DOWNLOAD_PAGE;
    private static final String REPOSITORY = "Yive/Pluto";
    private static final ServerBuildInfo BUILD_INFO;
    private static final String USER_AGENT;
    private static final Gson GSON = new Gson();

    static {
        BUILD_INFO = ServerBuildInfo.buildInfo();
        USER_AGENT = BUILD_INFO.brandName() + "/" + BUILD_INFO.asString(VERSION_SIMPLE);

        // TODO: Probably have to change this with the new versioning of 26.x.
        String downloadPage = "https://ci.yive.dev/job/Pluto/";
        final String versionId = BUILD_INFO.minecraftVersionId();
        if (versionId.contains("-")) {
            downloadPage += versionId.substring(0, versionId.indexOf("-"));
        } else if (versionId.contains(".")) {
            downloadPage += versionId;
        }
        DOWNLOAD_PAGE = downloadPage;
    }

    @Override
    public long getCacheTime() {
        return 720000;
    }

    @Override
    public Component getVersionMessage() {
        final Component updateMessage;
        if (BUILD_INFO.buildNumber().isEmpty() && BUILD_INFO.gitCommit().isEmpty()) {
            updateMessage = text("You are running a development version without access to version information", color(0xFF5300));
        } else {
            updateMessage = getUpdateStatusMessage();
        }
        final @Nullable Component history = this.getHistory();

        return history != null ? Component.textOfChildren(updateMessage, Component.newline(), history) : updateMessage;
    }

    public static void getUpdateStatusStartupMessage() {
        int distance = DISTANCE_ERROR;

        final OptionalInt buildNumber = BUILD_INFO.buildNumber();
        if (buildNumber.isEmpty() && BUILD_INFO.gitCommit().isEmpty()) {
            COMPONENT_LOGGER.warn(text("*** You are running a development version without access to version information ***"));
        } else {
            final Optional<String> gitBranch = BUILD_INFO.gitBranch();
            final Optional<String> gitCommit = BUILD_INFO.gitCommit();
            if (gitBranch.isPresent() && gitCommit.isPresent()) {
                distance = fetchDistanceFromGitHub(gitBranch.get(), gitCommit.get());
            }

            switch (distance) {
                case DISTANCE_ERROR -> COMPONENT_LOGGER.error(text("*** Error obtaining version information! Cannot fetch version info ***"));
                case 0 -> {}
                case DISTANCE_UNKNOWN -> COMPONENT_LOGGER.warn(text("*** You are running an unknown version! Cannot fetch version info ***"));
                default -> {
                    COMPONENT_LOGGER.info(text("*** Currently you are " + distance + " build(s) behind ***"));
                    COMPONENT_LOGGER.info(text("*** It is highly recommended to download the latest build from " + DOWNLOAD_PAGE + " ***"));
                }
            }
        }
    }

    private static Component getUpdateStatusMessage() {
        int distance = DISTANCE_ERROR;

        final Optional<String> gitBranch = BUILD_INFO.gitBranch();
        final Optional<String> gitCommit = BUILD_INFO.gitCommit();
        if (gitBranch.isPresent() && gitCommit.isPresent()) {
            distance = fetchDistanceFromGitHub(gitBranch.get(), gitCommit.get());
        }

        return switch (distance) {
            case DISTANCE_ERROR -> text("Error obtaining version information", NamedTextColor.YELLOW);
            case 0 -> text("You are running the latest version", NamedTextColor.GREEN);
            case DISTANCE_UNKNOWN -> text("Unknown version", NamedTextColor.YELLOW);
            default -> text("You are " + distance + " version(s) behind", NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(text("Download the new version at: ")
                    .append(text(DOWNLOAD_PAGE, NamedTextColor.GOLD)
                        .hoverEvent(text("Click to open", NamedTextColor.WHITE))
                        .clickEvent(ClickEvent.openUrl(DOWNLOAD_PAGE))));
        };
    }

    // Contributed by Techcable <Techcable@outlook.com> in GH-65
    private static int fetchDistanceFromGitHub(final String branch, final String hash) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) URI.create("https://api.github.com/repos/%s/compare/%s...%s".formatted(REPOSITORY, branch, hash)).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) return DISTANCE_UNKNOWN; // Unknown commit
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                final JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                final String status = obj.get("status").getAsString();
                return switch (status) {
                    case "identical" -> 0;
                    case "behind" -> obj.get("behind_by").getAsInt();
                    default -> DISTANCE_ERROR;
                };
            } catch (final JsonSyntaxException | NumberFormatException e) {
                LOGGER.error("Error parsing json from GitHub's API", e);
                return DISTANCE_ERROR;
            }
        } catch (final IOException e) {
            LOGGER.error("Error while parsing version", e);
            return DISTANCE_ERROR;
        }
    }

    private @Nullable Component getHistory() {
        final VersionHistoryManager.@Nullable VersionData data = VersionHistoryManager.INSTANCE.getVersionData();
        if (data == null) {
            return null;
        }

        final @Nullable String oldVersion = data.getOldVersion();
        if (oldVersion == null) {
            return null;
        }

        return text("Previous version: " + oldVersion, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }
}
