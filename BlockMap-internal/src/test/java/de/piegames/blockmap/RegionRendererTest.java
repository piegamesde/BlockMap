package de.piegames.blockmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.joml.Vector2i;
import org.junit.Test;

import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RenderSettings;

public class RegionRendererTest {

	@Test
	public void simpleTest1() throws IOException, URISyntaxException, InterruptedException {
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		Files.createDirectories(Paths.get("./output"));
		RegionRenderer renderer = new RegionRenderer(settings);
		BufferedImage image = renderer.render(new Vector2i(0, 0), new RegionFile(Paths.get(URI.create(getClass().getResource("/r.0.0.mca").toString()))));
		ImageIO.write(image, "png", Files.newOutputStream(Paths.get("./output").resolve("out.png")));
	}
}