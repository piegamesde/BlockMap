package de.piegames.blockmap.generate;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.generate.Downloader.VersionManifest.Version;

public class Downloader {

	private static Log log = LogFactory.getLog(Downloader.class);
	private static final String	DOWNLOAD_URL	= "https://launchermeta.mojang.com/mc/game/version_manifest.json";

	public static VersionManifest downloadManifest() throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(DOWNLOAD_URL).openStream()))) {
			return new Gson().fromJson(in, VersionManifest.class);
		}
	}

	public static void checkMinecraftVersions() throws IOException {
		VersionManifest manifest = Downloader.downloadManifest();

		List<Version> latestReleases = Arrays.stream(manifest.versions)
				.filter(Version::isRelease)
				.collect(Collectors.groupingBy(v -> new Semver(v.id, SemverType.LOOSE).getMinor()))
				.values()
				.stream()
				.map(versions -> versions.stream().max(Comparator.comparing(v -> new Semver(v.id, SemverType.LOOSE))))
				.map(Optional::get)
				.collect(Collectors.toList());
		log.debug("Latest known Minecraft versions: " + latestReleases.stream().map(v -> v.id).collect(Collectors.toList()));
		for (MinecraftVersion version : MinecraftVersion.values()) {
			log.info("Checking " + version);
			Semver minecraftVersion = new Semver(version.versionName, SemverType.LOOSE);
			Version latestMatching = latestReleases.stream()
					.filter(v -> new Semver(v.id, SemverType.LOOSE).getMinor() == minecraftVersion.getMinor())
					.findAny()
					.get();
			if (new Semver(latestMatching.id).isGreaterThan(minecraftVersion)) {
				log.error("Outdated: newest available version is " + latestMatching.id);
			} else {
				log.info("Up to date (" + version.versionName + ")");
			}
			if (!latestMatching.url.equals(version.manifestURL)) {
				log.error("Specified URL does not match, should be " + latestMatching.url);
			}
		}
		Semver latestRelease = latestReleases.stream()
				.map(v -> new Semver(v.id, SemverType.LOOSE))
				.max(Comparator.naturalOrder())
				.get();
		if (latestRelease.isGreaterThan(new Semver(MinecraftVersion.LATEST.versionName, SemverType.LOOSE))) {
			log.error("There is a new Minecraft release out there: " + latestRelease + ". Please update BlockMap!");
		}
	}

	public static void downloadMinecraft(MinecraftVersion version) throws IOException {
		log.info("Downlading files for Minecraft " + version.versionName);

		Path versionInformation = Generator.OUTPUT_INTERNAL_CACHE.resolve("version-" + version.versionName + ".json");
		if (!Files.exists(versionInformation)) {
			log.info("Downloading version information");
			downloadFile(version.manifestURL, versionInformation);
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
			if (!matchesHashSHA1(path, size, sha1)) {
				log.info("Downloading client.jar");
				downloadFile(url, path);
			} else
				log.info("client.jar is up to date");
		}
		{ // Server
			Path path = Generator.OUTPUT_INTERNAL_CACHE.resolve("server-" + version.fileSuffix + ".jar");
			JsonObject client = downloads.getAsJsonObject("server");
			String sha1 = client.get("sha1").getAsString();
			int size = client.get("size").getAsInt();
			String url = client.get("url").getAsString();
			if (!matchesHashSHA1(path, size, sha1)) {
				log.info("Downloading server.jar");
				downloadFile(url, path);
			} else
				log.info("server.jar is up to date");
		}
	}

	static String getHashSHA256(Path file) throws IOException {
		return Hashing.sha256().hashBytes(Files.readAllBytes(file)).toString();
	}

	static boolean matchesHashSHA1(Path file, int size, String sha1) {
		try {
			if (!Files.exists(file))
				return false;
			if (Files.size(file) != size)
				return false;
			@SuppressWarnings("deprecation")
			String hash = Hashing.sha1().hashBytes(Files.readAllBytes(file)).toString();
			log.debug("File hash: " + hash + ", comparing to: " + sha1);
			return hash.equals(sha1);
		} catch (IOException e) {
			log.error("Could not check file integrity: " + file, e);
			return false;
		}
	}

	static boolean matchesHashSHA256(Path file, String sha256) {
		try {
			if (!Files.exists(file))
				return false;
			String hash = getHashSHA256(file);
			log.debug("File hash: " + hash + ", comparing to: " + sha256);
			return hash.equals(sha256);
		} catch (IOException e) {
			log.error("Could not check file integrity: " + file, e);
			return false;
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

			boolean isRelease() {
				return type.equals("release");
			}
		}
	}
}
