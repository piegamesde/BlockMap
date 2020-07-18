package de.piegames.blockmap.gui.standalone;

import com.google.gson.Gson;
import de.saibotk.jmaw.ApiResponseException;
import de.saibotk.jmaw.MojangAPI;
import de.saibotk.jmaw.PlayerProfile;
import de.saibotk.jmaw.Util;
import io.github.soc.directories.ProjectDirectories;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class SimplePlayerProfileCache implements IPlayerProfileCache {
    private final MojangAPI api = new MojangAPI();
    private static final Gson GSON = Util.getGson();
    private static final Log log = LogFactory.getLog(SimplePlayerProfileCache.class);

    private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
    private final Path cacheDir	= Paths.get(directories.cacheDir + "/profiles");

    private static final long MAX_CACHE_TIME = 60000L;

    public SimplePlayerProfileCache() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException exception) {
            log.warn("Could not create profile cache directory!", exception);
        }

        purge();
    }

    @Override
    public Optional<PlayerProfile> get(String uuid) throws ApiResponseException {
        try {
            File cachedFile = cacheDir.resolve(uuid + ".json").toFile();

            if (Instant.now().toEpochMilli() - cachedFile.lastModified() <= MAX_CACHE_TIME) {
                log.info("Found player profile in cache for uuid: " + uuid);
                return Optional.of(GSON.fromJson(Files.newBufferedReader(cachedFile.toPath()), PlayerProfile.class));
            } else {
                log.info("Player profile will be removed from cache for uuid: " + uuid);
                cachedFile.delete();
            }
        } catch (IOException exception) {
            log.info("Could not fetch player profile from cache for uuid: " + uuid, exception);
        }

        log.info("Fetching player profile from API for uuid: " + uuid);

        return fresh(uuid);
    }

    @Override
    public Optional<PlayerProfile> fresh(String uuid) throws ApiResponseException {
        Optional<PlayerProfile> profile = api.getPlayerProfile(uuid);

        profile.ifPresent(this::writeToCache);

        return profile;
    }

    @Override
    public void flush() {
        File directory = cacheDir.toFile();
        File[] cachedFiles = directory.listFiles();
        if (cachedFiles != null)
            Arrays.stream(cachedFiles).forEach(File::delete);

        log.info("Flushed cache...");
    }

    @Override
    public void purge() {
        File directory = cacheDir.toFile();
        File[] cachedFiles = directory.listFiles();

        if (cachedFiles != null)
            Arrays.stream(cachedFiles).filter(x -> Instant.now().toEpochMilli() - x.lastModified() > MAX_CACHE_TIME)
                    .forEach(File::delete);

        log.info("Purged cache...");
    }

    protected void writeToCache(PlayerProfile profile) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(cacheDir.resolve(profile.getId() + ".json"));
            GSON.toJson(profile, writer);
            writer.flush();
            log.info("Cached profile to disk with uuid: " + profile.getId());
        } catch (IOException exception) {
            log.error("Could not write player profile with uuid: " + profile.getId());
        }
    }
}
