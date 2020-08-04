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
	MC_1_13("1_13", 1519, 1631, "1.13.2", "https://launchermeta.mojang.com/v1/packages/26ec75fc9a8b990fa976100a211475d18bd97de0/1.13.2.json"),
	MC_1_14("1_14", 1901, 1976, "1.14.4", "https://launchermeta.mojang.com/v1/packages/74b5fb5fa9ec7b14abe60b468e40703cfbf8d10e/1.14.4.json"),
	MC_1_15("1_15", 2200, 2230, "1.15.2", "https://launchermeta.mojang.com/v1/packages/eb0cbf02b9e1b0c65e89697762ece03f6f3f8d5c/1.15.2.json"),
	MC_1_16("1_16", 2566, Integer.MAX_VALUE, "1.16.1", "https://launchermeta.mojang.com/v1/packages/0c127f291db55d4a09830a3891c5447e63714d92/1.16.1.json");

	public static final MinecraftVersion LATEST = MC_1_16;

	public final int minVersion, maxVersion;
	public final String fileSuffix, versionName, manifestURL;
	private BlockState states;

	MinecraftVersion(String fileName, int minVersion, int maxVersion, String versionName, String manifestURL) {
		this.fileSuffix = fileName;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
		this.versionName = versionName;
		this.manifestURL = manifestURL;
	}

	public BlockState getBlockStates() {
		if (states == null)
			states = new Gson().fromJson(
					new InputStreamReader(BlockState.class.getResourceAsStream("/block-states-" + fileSuffix + ".json")),
					BlockState.class
			);
		return states;
	}
}
