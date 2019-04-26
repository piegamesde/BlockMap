package de.piegames.blockmap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionRenderer_1_13;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.standalone.PostProcessing;
import de.piegames.blockmap.world.Region.BufferedRegion;
import de.piegames.blockmap.world.RegionFolder.CachedRegionFolder;
import de.piegames.blockmap.world.RegionFolder.LocalRegionFolder;
import de.piegames.blockmap.world.RegionFolder.RemoteRegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;

public class RegionFolderTest {
	/* The region coordinates that exist in the test world */
	final Vector2ic[]		REGIONS	= new Vector2ic[] { new Vector2i(-1, 1), new Vector2i(-1, 2), new Vector2i(0, 1), new Vector2i(0, 2) };

	@Rule
	public TemporaryFolder	folder	= new TemporaryFolder();

	@Test
	public void testCacheSave() throws IOException {
		Queue<Vector2ic> rendered = new LinkedList<>();
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		RegionRenderer renderer = new RegionRenderer_1_13(settings) {
			@Override
			public BufferedRegion render(Vector2ic regionPos, RegionFile file) {
				rendered.add(regionPos);
				return super.render(regionPos, file);
			}
		};

		WorldRegionFolder localWorld = WorldRegionFolder.load(
				Paths.get(URI.create(getClass().getResource("/BlockMapWorld/region").toString())),
				renderer);

		File out1 = folder.newFolder();
		CachedRegionFolder cachedWorld = new CachedRegionFolder(localWorld, false, out1.toPath());
		for (Vector2ic v : REGIONS) {
			assertNotNull(cachedWorld.render(v));
			assertFalse(rendered.isEmpty());
			assertEquals(v, rendered.remove());
		}
		for (Vector2ic v : REGIONS) {
			assertNotNull(cachedWorld.render(v));
			assertFalse(rendered.isEmpty());
			assertEquals(v, rendered.remove());
		}

		File out2 = folder.newFolder();
		CachedRegionFolder cachedWorldLazy = new CachedRegionFolder(localWorld, true, out2.toPath());
		for (Vector2ic v : REGIONS) {
			assertNotNull(cachedWorldLazy.render(v));
			assertFalse(rendered.isEmpty());
			assertEquals(v, rendered.remove());
		}
		for (Vector2ic v : REGIONS) {
			assertNotNull(cachedWorldLazy.render(v));
			assertTrue(rendered.isEmpty());
		}

		LocalRegionFolder savedWorld = cachedWorldLazy.save();
		for (Vector2ic v : REGIONS) {
			assertNotNull(savedWorld.render(v));
			assertTrue(rendered.isEmpty());
		}

		cachedWorldLazy.save(out2.toPath().resolve("saved.json"), "testworld", true);

		LocalRegionFolder savedWorld2 = new LocalRegionFolder(out2.toPath().resolve("saved.json"), "testworld");
		RemoteRegionFolder savedWorld3 = new RemoteRegionFolder(out2.toPath().resolve("saved.json").toUri(), "testworld");
		for (Vector2ic v : REGIONS) {
			assertNotNull(savedWorld2.render(v));
			assertNotNull(savedWorld3.render(v));
		}
		assertTrue(rendered.isEmpty());
	}

	/**
	 * Test for #15 ({@link https://github.com/Minecraft-Technik-Wiki/BlockMap/issues/15})
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateBigImage() throws IOException {
		Queue<Vector2ic> rendered = new LinkedList<>();
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		RegionRenderer_1_13 renderer = new RegionRenderer_1_13(settings) {
			@Override
			public BufferedRegion render(Vector2ic regionPos, RegionFile file) {
				rendered.add(regionPos);
				return super.render(regionPos, file);
			}
		};

		WorldRegionFolder localWorld = WorldRegionFolder.load(
				Paths.get(URI.create(getClass().getResource("/BlockMapWorld/region").toString())),
				renderer);

		File out1 = folder.newFolder();
		CachedRegionFolder cachedWorld = new CachedRegionFolder(localWorld, false, out1.toPath());
		for (Vector2ic v : REGIONS) {
			assertNotNull(cachedWorld.render(v));
			assertFalse(rendered.isEmpty());
			assertEquals(v, rendered.remove());
		}

		PostProcessing.createBigImage(cachedWorld.save(), out1.toPath(), settings);
		assertTrue(rendered.isEmpty());
	}
}
