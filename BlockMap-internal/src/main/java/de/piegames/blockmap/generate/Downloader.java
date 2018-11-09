package de.piegames.blockmap.generate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class Downloader {

	private static Log log = LogFactory.getLog(Generator.class);

	/**
	 * 1.13.2.json is downloaded from {@link https://launchermeta.mojang.com/v1/packages/df66b5a5f451d026c356ead8caf118c71919850b/1.13.2.json}.
	 * To update the application, download a newer version (links available at
	 * {@link https://minecraft-de.gamepedia.com/Minecraft_Wiki:Versionsliste}) and update the file, the code and this comment. Do not forget to
	 * update the input file in build.gradle!
	 */
	public static void downloadMinecraft() throws JsonSyntaxException, IOException {
		log.info("Downloading Minecraft files");
		JsonParser parser = new JsonParser();
		JsonElement e = parser.parse(new String(Files.readAllBytes(Paths.get(URI.create(Downloader.class.getResource("/1.13.2.json").toString())))));
		JsonObject downloads = e.getAsJsonObject().getAsJsonObject("downloads");
		{ // Client
			Path path = Generator.OUTPUT_INTERNAL_MAIN.resolve("client.jar");
			JsonObject client = downloads.getAsJsonObject("client");
			String sha1 = client.get("sha1").getAsString();
			int size = client.get("size").getAsInt();
			String url = client.get("url").getAsString();
			if (needsDownload(path, size, sha1)) {
				downloadFile(url, path);
			} else {
				log.debug(path + " is up to date");
			}
		}
		{ // Server
			Path path = Generator.OUTPUT_INTERNAL_MAIN.resolve("server.jar");
			JsonObject client = downloads.getAsJsonObject("server");
			String sha1 = client.get("sha1").getAsString();
			int size = client.get("size").getAsInt();
			String url = client.get("url").getAsString();
			if (needsDownload(path, size, sha1)) {
				downloadFile(url, path);
			} else {
				log.debug(path + " is up to date");
			}
		}
	}

	/**
	 * TODO Currently, this does nothing because the Land Generator is already downloaded into the resources folder, but this will change very
	 * soon
	 */
	public static void downloadLandGenerator() {
		// log.info("Downloading Minecraft Land Generator");
	}

	private static boolean needsDownload(Path file, int size, String sha1) {
		try {
			if (!Files.exists(file))
				return true;
			if (Files.size(file) != size)
				return true;
			@SuppressWarnings("deprecation")
			String hash = Hashing.sha1().hashBytes(Files.readAllBytes(file)).toString();
			return !hash.equals(sha1);
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
}
