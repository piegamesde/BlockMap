package de.piegames.blockmap;

import static org.junit.Assert.assertArrayEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.joml.Vector2i;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.nbt.regionfile.RegionFile;

public class RegionRendererTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void simpleTest1() throws IOException, URISyntaxException, InterruptedException {
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		RegionRenderer renderer = new RegionRenderer(settings);
		BufferedImage image = renderer.render(new Vector2i(0, 0), new RegionFile(Paths.get(URI.create(getClass().getResource("/r.0.0.mca").toString()))))
				.getImage();
		ImageIO.write(image, "png", Files.newOutputStream(folder.newFile().toPath()));
	}

	/** Test if bounds in the render settings are respected */
	@Test
	public void testBounds() throws IOException, URISyntaxException, InterruptedException {
		System.setProperty("joml.format", "false");
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		settings.minZ = -500;
		settings.maxZ = -50;
		settings.minX = 30;
		settings.maxX = 420;
		RegionRenderer renderer = new RegionRenderer(settings);
		BufferedImage image = renderer.render(new Vector2i(0, -1), new RegionFile(Paths.get(URI.create(getClass().getResource("/r.1.3.mca").toString()))))
				.getImage();
		boolean[][] isCulled = new boolean[512][];
		boolean[][] shouldBeCulled = new boolean[512][];
		for (int x = 0; x < 512; x++) {
			isCulled[x] = new boolean[512];
			shouldBeCulled[x] = new boolean[512];
			for (int z = 0; z < 512; z++) {
				isCulled[x][z] = (image.getRGB(x, z) >> 24) == 0;
				shouldBeCulled[x][z] = z < 12 || z > 462 || x < 30 || x > 420;
			}
		}
		assertArrayEquals(shouldBeCulled, isCulled);
	}
}