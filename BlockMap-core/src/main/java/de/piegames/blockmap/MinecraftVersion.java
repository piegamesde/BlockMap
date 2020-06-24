package de.piegames.blockmap;

import java.io.InputStreamReader;

import com.google.gson.Gson;

import de.piegames.blockmap.renderer.BlockState;

public enum MinecraftVersion {
	/*
	 * See https://minecraft.gamepedia.com/Data_version#List_of_data_versions and
	 * https://minecraft-de.gamepedia.com/Versionen#Version-IDs
	 * 
	 * Snapshots and pre-releases usually are not supported.
	 */
	MC_1_13("1_13", 1519, 1631, "1.13.2"),
	MC_1_14("1_14", 1901, 1976, "1.14.4"),
	MC_1_15("1_15", 2200, 2230, "1.15.2"),
	MC_1_16("1_16", 2566, Integer.MAX_VALUE, "1.16.1");

	public static final MinecraftVersion LATEST = MC_1_16;

	public final int						minVersion, maxVersion;
	public final String						fileSuffix, versionName;
	private BlockState						states;

	MinecraftVersion(String fileName, int minVersion, int maxVersion, String versionName) {
		this.fileSuffix = fileName;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
		this.versionName = versionName;
	}

	public BlockState getBlockStates() {
		if (states == null)
			states = new Gson().fromJson(new InputStreamReader(BlockState.class.getResourceAsStream("/block-states-" + fileSuffix + ".json")),
					BlockState.class);
		return states;
	}
}
