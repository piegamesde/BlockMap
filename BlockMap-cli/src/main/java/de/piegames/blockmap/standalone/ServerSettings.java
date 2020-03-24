package de.piegames.blockmap.standalone;

import java.nio.file.Path;

import com.google.gson.annotations.SerializedName;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.standalone.DeserializeNullChecker.DeserializeNonNull;

public class ServerSettings {

	public RegionFolderSettings[] worlds = new RegionFolderSettings[0];
	@SerializedName("output dir")
	@DeserializeNonNull
	public Path outputDir;
	@SerializedName("hide offline players")
	public boolean hideOfflinePlayers;

	public static class RegionFolderSettings {

		@DeserializeNonNull
		public String name;
		@SerializedName("input dir")
		@DeserializeNonNull
		public Path inputDir;
		public MinecraftDimension dimension = MinecraftDimension.OVERWORLD;
		public boolean force = false;
		public boolean pins = true;
		@SerializedName("render settings")
		public RenderSettings renderSettings = new RenderSettings();
	}
}
