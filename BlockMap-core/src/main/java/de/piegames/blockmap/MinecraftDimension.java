package de.piegames.blockmap;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum MinecraftDimension {

	OVERWORLD("Overworld", 0, Paths.get(""), Paths.get("data", "villages.dat")),
	NETHER("Nether", -1, Paths.get("DIM-1"), Paths.get("data", "villages_nether.dat")),
	END("End", 1, Paths.get("DIM1"), Paths.get("data", "villages_end.dat"));

	public final int	index;
	public final String	name;
	public final Path	regionPath, villagePath;

	MinecraftDimension(String name, int index, Path regionPath, Path villagePath) {
		this.name = name;
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

	public static MinecraftDimension forID(int id) {
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
