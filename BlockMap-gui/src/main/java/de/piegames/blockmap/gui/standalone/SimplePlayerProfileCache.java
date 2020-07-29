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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

/**
 * A simple {@link PlayerProfile} cache implementation, to overcome the API request limit of a minute.
 * It will save fetched profiles to disk and load them from the disk when possible.
 *
 * @author saibotk
 */
public class SimplePlayerProfileCache extends FileCache {
    private static final Log log = LogFactory.getLog(SimplePlayerProfileCache.class);

    private final MojangAPI api = new MojangAPI();
    private static final Gson GSON = Util.getGson();

    private static final long MAX_CACHE_TIME = 60000L;

    /**
     * Constructs the cache instance.
     */
    public SimplePlayerProfileCache() {
        super(Paths.get(ProjectDirectories.from("de", "piegames", "blockmap").cacheDir + "/profiles"));
    }

    /**
     * Gets a {@link PlayerProfile} from cache if available and loads it from Mojang otherwise.
     * This will also remove an existing old cache entry and save a fresh profile to disk if necessary.
     *
     * @param uuid The uuid of the player.
     *
     * @return The requested profile, wrapped as an Optional.
     *
     * @throws ApiResponseException Exception when contacting the Mojang API.
     */
    public Optional<PlayerProfile> get(String uuid) throws ApiResponseException {
        try {
            Path cachedFile = cacheDir.resolve(uuid + ".json");
            boolean exists = Files.exists(cachedFile);

            if (exists) {
                if (isFresh(cachedFile)) {
                    log.info("Loading player profile from cache with uuid: " + uuid);

                    return Optional.of(GSON.fromJson(Files.newBufferedReader(cachedFile), PlayerProfile.class));
                } else {
                    log.info("Player profile will be removed from cache for uuid: " + uuid);

                    removeFile(cachedFile);
                }
            }
        } catch (IOException exception) {
            log.info("Could not fetch player profile from cache for uuid: " + uuid, exception);
        }

        log.info("Fetching player profile from API for uuid: " + uuid);

        // retrieve the profile from Mojang
        Optional<PlayerProfile> profile = fetch(uuid);

        // Save the profile, if it is available
        profile.ifPresent(this::writeToCache);

        return profile;
    }

    /**
     * Fetches a player profile from Mojang's API and returns it.
     *
     * @param uuid The uuid of the player.
     *
     * @return The {@link PlayerProfile} wrapped in an Optional.
     *
     * @throws ApiResponseException The exception of the Mojang API call.
     */
    public Optional<PlayerProfile> fetch(String uuid) throws ApiResponseException {
        return api.getPlayerProfile(uuid);
    }

    @Override
    protected boolean isFresh(Path path) {
        try {
            return Instant.now().toEpochMilli() - Files.getLastModifiedTime(path).toMillis() <= MAX_CACHE_TIME;
        } catch (IOException e) {
            log.error("Could not access last modified date on file: " + path.getFileName(), e);

            return false;
        }
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
