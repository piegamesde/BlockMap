package de.piegames.blockmap.gui.standalone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A simple file based cache superclass, that holds redundant actions, like flushing, evicting and creating the cache
 * directory.
 *
 * @author saibotk
 */
abstract class FileCache {
    private static final Log log = LogFactory.getLog(FileCache.class);
    protected final Path cacheDir;

    /**
     * Constructs the cache instance and creates the cache folder if needed.
     * This will also call {@link #evict()}, to clear all old items from the cache.
     */
    FileCache(Path cacheDir) {
        this.cacheDir = cacheDir;

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException exception) {
            log.warn("Could not create cache directory!", exception);
        }
    }

    /**
     * Clears the cache.
     * This will remove all elements from the cache folder, regardless of their date.
     */
    public void flush() {
        try (Stream<Path> stream = Files.list(cacheDir)) {
            stream.forEach(this::removeFile);
        } catch (IOException e) {
            log.error("Could not delete old cache files.", e);
        }

        log.info("Flushed all items from cache...");
    }

    /**
     * Removes old items from the cache.
     * This will remove all items from the cache, that are too old and would not be used anymore.
     */
    public void evict() {
        try (Stream<Path> stream = Files.list(cacheDir)) {
            stream
                    .filter(path -> !isFresh(path))
                    .forEach(this::removeFile);
        } catch (IOException e) {
            log.error("Could not delete old cache files.", e);
        }

        log.info("Evicted old items from cache...");
    }

    /**
     * Determine if a file is still considered "fresh".
     * Fresh means it is still valid and should be used, instead of fetching the resource from the origin again.
     *
     * @param path The path to the cache file.
     *
     * @return If it is still considered fresh.
     */
    protected abstract boolean isFresh(Path path);

    protected void removeFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.error("Could not delete cached file: " + path.getFileName() );
        }
    }

}
