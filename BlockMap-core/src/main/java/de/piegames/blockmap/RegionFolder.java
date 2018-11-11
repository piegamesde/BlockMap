package de.piegames.blockmap;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Vector2i;

/**
 * A collection of region files with a position.
 * 
 * @author piegames
 */
public class RegionFolder {

	public Map<Vector2i, Region> regions = new HashMap<>();

	/** Add a region file to this folder. */
	public void addRegion(Region r) {
		regions.put(new Vector2i(r.position.x(), r.position.y()), r);
	}

	static final Pattern rfpat = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

	/** Recursively add all region files that match the default naming pattern within Minecraft worlds */
	protected void add(Path dir) {
		Matcher m;
		if (Files.isDirectory(dir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
				for (Path p : stream) {
					m = rfpat.matcher(p.getFileName().toString());
					if (m.matches())
						add(p);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if ((m = rfpat.matcher(dir.getFileName().toString())).matches()) {
			if (!Files.exists(dir)) {
				System.err.println("Warning: region file '" + dir.toAbsolutePath() + "' doesn't exist!");
				return;
			}
			addRegion(new Region(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), dir));
		} else {
			throw new RuntimeException(dir.toAbsolutePath() + " does not seem to be a directory or a region file");
		}
	}

	public static RegionFolder load(Path file) {
		RegionFolder rm = new RegionFolder();
		rm.add(file);
		return rm;
	}
}