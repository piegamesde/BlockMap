package togos.minecraft.maprend;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Vector2i;

public class World {

	public static class Region {
		public int rx, rz;
		public Path regionFile;
		public Path imageFile;
	}

	public Map<Vector2i, Region> regions = new HashMap<>();

	public int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

	public void addRegion(Region r) {
		regions.put(new Vector2i(r.rx, r.rz), r);
		if (r.rx < minX)
			minX = r.rx;
		if (r.rz < minZ)
			minZ = r.rz;
		if (r.rx >= maxX)
			maxX = r.rx + 1;
		if (r.rz >= maxZ)
			maxZ = r.rz + 1;
	}

	static final Pattern rfpat = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

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
				System.err.println("Warning: region file '" + dir + "' doesn't exist!");
				return;
			}
			Region r = new Region();
			r.rx = Integer.parseInt(m.group(1));
			r.rz = Integer.parseInt(m.group(2));
			r.regionFile = dir;
			addRegion(r);
		} else {
			throw new RuntimeException(dir + " does not seem to be a directory or a region file");
		}
	}

	public static World load(Path file) {
		World rm = new World();
		rm.add(file);
		return rm;
	}
}
