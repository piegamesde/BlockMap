package de.piegames.blockmap;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum MinecraftDimension {

	OVERWORLD("Overworld", 0, Paths.get("region"), Paths.get("data", "villages.dat")),
	NETHER("Nether", -1, Paths.get("DIM-1", "region"), Paths.get("data", "villages_nether.dat")),
	END("End", 1, Paths.get("DIM1", "region"), Paths.get("data", "villages_end.dat"));

	public final int	index;
	public final String	displayName;
	public final Path	regionPath, villagePath;

	MinecraftDimension(String displayName, int index, Path regionPath, Path villagePath) {
		this.displayName = displayName;
		this.index = index;
		this.regionPath = regionPath;
		this.villagePath = villagePath;
	}

	public Path getRegionPath() {
		return regionPath;
	}

	public Path getVillagePath() {
		return villagePath;
	}

	public static MinecraftDimension byID(int id) {
		switch (id) {
		case -1:
			return NETHER;
		case 0:
			return OVERWORLD;
		case 1:
			return END;
		default:
			throw new IllegalArgumentException("Value must be either -1, 0 or 1");
		}
	}
}
