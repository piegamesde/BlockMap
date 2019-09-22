package de.piegames.blockmap.standalone;

import java.nio.file.Path;

import com.google.gson.annotations.SerializedName;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RenderSettings;

public class ServerSettings {

	public RegionFolderSettings[]	worlds;
	@SerializedName("output dir")
	public Path						outputDir;
	@SerializedName("hide offline players")
	public boolean					hideOfflinePlayers;

	public static class RegionFolderSettings {

		public String				name;
		@SerializedName("input dir")
		public Path					inputDir;
		public MinecraftDimension	dimension		= MinecraftDimension.OVERWORLD;
		public boolean				force			= false;
		public boolean				pins			= true;
		@SerializedName("render settings")
		public RenderSettings		renderSettings	= new RenderSettings();
	}
}
