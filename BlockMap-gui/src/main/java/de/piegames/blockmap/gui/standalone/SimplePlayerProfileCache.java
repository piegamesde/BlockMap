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
import java.util.Optional;

public class SimplePlayerProfileCache implements IPlayerProfileCache {
    private final MojangAPI api = new MojangAPI();
    private static final Gson GSON = Util.getGson();
    private static Log log = LogFactory.getLog(SimplePlayerProfileCache.class);

    private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
    private final Path cacheDir	= Paths.get(directories.cacheDir + "/profiles");

    public SimplePlayerProfileCache() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException exception) {
            log.warn("Could not create profile cache directory!", exception);
        }
    }

    @Override
    public Optional<PlayerProfile> get(String uuid) throws ApiResponseException {
        try {
            return Optional.of(GSON.fromJson(Files.newBufferedReader(cacheDir.resolve(uuid + ".json")), PlayerProfile.class));
        } catch (IOException exception) {
            log.info("Could not fetch player profile from cache for uuid: " + uuid, exception);
        }
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

    private String getFileName(String url) {
        Path location = Paths.get(url);

        return location.getFileName().toString() + ".png";
    }
}
