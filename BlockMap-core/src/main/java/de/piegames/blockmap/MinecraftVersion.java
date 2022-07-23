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
	MC_1_13("1_13", 1519, 1631, "1.13.2", "https://launchermeta.mojang.com/v1/packages/57f0285f5e6800233e3269a93ad11bfb631f2412/1.13.2.json"),
	MC_1_14("1_14", 1901, 1976, "1.14.4", "https://launchermeta.mojang.com/v1/packages/d73c3f908365863ebe6b01cf454990182f2652f4/1.14.4.json"),
	MC_1_15("1_15", 2200, 2230, "1.15.2", "https://launchermeta.mojang.com/v1/packages/52396828d64eae5bdd01a5ac787234a5f4c85e59/1.15.2.json"),
	MC_1_16("1_16", 2566, 2586, "1.16.5", "https://launchermeta.mojang.com/v1/packages/934dfe455cf13141a1ab8a64c57114becba83c6d/1.16.5.json"),
	MC_1_17("1_17", 2724, 2730, "1.17.1", "https://launchermeta.mojang.com/v1/packages/1c11f595196065f7311b5f4ffe74a4fde433ff27/1.17.1.json"),
	MC_1_18("1_18", 2860, 2975, "1.18.2", "https://launchermeta.mojang.com/v1/packages/f1cf44b0fb6fe11910bac139617b72bf3ef330b9/1.18.2.json"),
	MC_1_19("1_19", 3105, Integer.MAX_VALUE, "1.19.0", "https://launchermeta.mojang.com/v1/packages/638ed447a978baf93a3e64fe6d406f5f20fbb5a9/1.19.json");

	public static final MinecraftVersion LATEST = MC_1_19;

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
