package de.piegames.blockmap.generate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.piegames.blockmap.MinecraftVersion;

public class Downloader {

	private static Log			log				= LogFactory.getLog(Generator.class);
	private static final String	DOWNLOAD_URL	= "https://launchermeta.mojang.com/mc/game/version_manifest.json";
	private static final String	DOWNLOAD_SHA256	= "a4343bca3e10d257b265b8c1ad8405e0f4bfcaacf87828d52a9eef3db8d5a988";

	public static VersionManifest downloadManifest() throws IOException {
		Path path = Generator.OUTPUT_INTERNAL_CACHE.resolve("version-manifest.json");
		if (needsDownload(path, DOWNLOAD_SHA256)) {
			log.info("Downloading versions manifest");
			downloadFile(DOWNLOAD_URL, path);
		} else
			log.info("Loading cached versions manifest");
		return new Gson().fromJson(Files.newBufferedReader(path), VersionManifest.class);
	}

	public static boolean downloadMinecraft(VersionManifest manifest, MinecraftVersion version) throws IOException {
		log.info("Downlading files for Minecraft " + version.versionName);
		boolean needsUpdate = false;

		Path versionInformation = Generator.OUTPUT_INTERNAL_CACHE.resolve("version-" + version.fileSuffix + ".json");
		if (!Files.exists(versionInformation)) {
			log.info("Downloading version information for " + version.versionName);
			downloadFile(manifest.get(version.versionName).url, versionInformation);
		} else
			log.info("Found version information in cache");

		JsonParser parser = new JsonParser();
		JsonElement e = parser.parse(Files.newBufferedReader(versionInformation));
		JsonObject downloads = e.getAsJsonObject().getAsJsonObject("downloads");
		{ // Client
			Path path = Generator.OUTPUT_INTERNAL_CACHE.resolve("client-" + version.fileSuffix + ".jar");
			JsonObject client = downloads.getAsJsonObject("client");
			String sha1 = client.get("sha1").getAsString();
			int size = client.get("size").getAsInt();
			String url = client.get("url").getAsString();
			if (needsDownload(path, size, sha1)) {
				log.info("Downloading client.jar");
				downloadFile(url, path);
				needsUpdate = true;
			} else
				log.info("client.jar is up to date");
		}
		{ // Server
			Path path = Generator.OUTPUT_INTERNAL_CACHE.resolve("server-" + version.fileSuffix + ".jar");
			JsonObject client = downloads.getAsJsonObject("server");
			String sha1 = client.get("sha1").getAsString();
			int size = client.get("size").getAsInt();
			String url = client.get("url").getAsString();
			if (needsDownload(path, size, sha1)) {
				log.info("Downloading server.jar");
				downloadFile(url, path);
				needsUpdate = true;
			} else
				log.info("server.jar is up to date");
		}
		return needsUpdate;
	}

	private static boolean needsDownload(Path file, int size, String sha1) {
		try {
			if (!Files.exists(file))
				return true;
			if (Files.size(file) != size)
				return true;
			@SuppressWarnings("deprecation")
			String hash = Hashing.sha1().hashBytes(Files.readAllBytes(file)).toString();
			log.debug("File hash: " + hash + ", comparing to: " + sha1);
			return !hash.equals(sha1);
		} catch (IOException e) {
			log.error("Could not check file integrity: " + file, e);
			return true;
		}
	}

	private static boolean needsDownload(Path file, String sha256) {
		try {
			if (!Files.exists(file))
				return true;
			String hash = Hashing.sha256().hashBytes(Files.readAllBytes(file)).toString();
			log.debug("File hash: " + hash + ", comparing to: " + sha256);
			return !hash.equals(sha256);
		} catch (IOException e) {
			log.error("Could not check file integrity: " + file, e);
			return true;
		}
	}

	private static void downloadFile(String url, Path path) throws MalformedURLException, IOException {
		log.debug("Downloading " + url + " to " + path);

		try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream())) {
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** Java representation of the file located at {@link Downloader#DOWNLOAD_URL}. */
	static class VersionManifest {
		Latest		latest;
		Version[]	versions;

		Version get(String id) {
			return Arrays.stream(versions).filter(v -> v.id.equals(id)).findAny().get();
		}

		static class Latest {
			String release, snapshot;
		}

		static class Version {
			String id, type, time, releaseTime, url;
		}
	}
}
