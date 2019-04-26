package de.piegames.blockmap;

public enum MinecraftVersion {
	MC_1_13(1519, 1631), MC_1_14(1901, Integer.MAX_VALUE);

	public final int minVersion, maxVersion;

	MinecraftVersion(int minVersion, int maxVersion) {
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}
}
