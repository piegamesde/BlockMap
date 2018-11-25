package de.piegames.blockmap;

import java.nio.file.Path;

public enum MinecraftDimension {

	OVERWORLD("Overworld", 0, "region"),
	NETHER("Nether", -1, "DIM-1", "region"),
	END("End", 1, "DIM1", "region");

	int			index;
	String		name;
	String[]	path;

	MinecraftDimension(String name, int index, String... path) {
		this.name = name;
		this.index = index;
		this.path = path;
	}

	public Path resolve(Path base) {
		for (String s : path)
			base = base.resolve(s);
		return base;
	}
}
