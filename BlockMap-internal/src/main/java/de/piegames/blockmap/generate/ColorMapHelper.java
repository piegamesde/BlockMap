package de.piegames.blockmap.generate;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.renderer.Block;

public class ColorMapHelper {

	private static Log log = LogFactory.getLog(ColorMapHelper.class);

	public Map<String, List<String>> placeholders = new HashMap<>();
	public Map<Block, List<String>> blocks = new HashMap<>();

	public ColorMapHelper() {

	}

	public void inherit(ColorMapHelper inherit) {
		placeholders.putAll(inherit.placeholders);
		blocks.putAll(inherit.blocks);
	}

	public void override(ColorMapHelper override, List<String> overrideWith) {
		placeholders.putAll(override.placeholders);
		/* Take all keys, map them to overrideWith and add them to the map. */
		blocks.putAll(override.blocks.keySet().stream().collect(Collectors.toMap(b -> b, b -> overrideWith)));
	}

	public void addBlock(ColorMapEntry entry) {
		for (String placeholder : placeholders.keySet())
			if (entry.containsPlaceholder(placeholder)) {
				for (String replace : placeholders.get(placeholder))
					/* Recursively replace the placeholder and add the block */
					addBlock(entry.replace(placeholder, replace));
				/* Return, since the block already got added after the recursion */
				return;
			}
		/*
		 * Apparently our entry does not contain any placeholders anymore, so we can safely add it. But before we do this, we need to unfold all
		 * possible block state variations used by wildcards, like "facing=*". We do not convert the instruction list to a color yet.
		 */
		for (Block block : Block.byCompactForm("minecraft:" + entry.key)) {
			log.debug("Added block " + block + " " + entry.value);
			blocks.put(block, entry.value);
		}
	}

	public BlockColorMap compileColorMap(FileSystem jarFile) throws IOException {
		Map<Block, Color> blockColors = new HashMap<>();
		Set<Block> grassBlocks = new HashSet<>();
		Set<Block> foliageBlocks = new HashSet<>();
		Set<Block> waterBlocks = new HashSet<>();

		for (Entry<Block, List<String>> e : blocks.entrySet()) {
			Block block = e.getKey();
			Queue<String> colorInfo = new LinkedList<>(e.getValue());

			Color color = ColorCompiler.compileTexture(block.toString(), colorInfo, jarFile);
			String tint = "none";
			if (!colorInfo.isEmpty())
				tint = colorInfo.remove();
			switch (tint) {
			case "none":
				break;
			case "grass":
				grassBlocks.add(block);
				break;
			case "foliage":
				foliageBlocks.add(block);
				break;
			case "water":
				waterBlocks.add(block);
				break;
			default:
				throw new IOException("Block " + block + ": " + e.getValue() + " is malformed.");
			}
			log.debug("Compiled texture " + e.getKey() + " to " + color);
			blockColors.put(block, color);
		}

		log.debug("Grass blocks " + grassBlocks);
		log.debug("Foliage blocks " + foliageBlocks);
		log.debug("Water blocks " + waterBlocks);

		return new BlockColorMap(blockColors, grassBlocks, foliageBlocks, waterBlocks);
	}

	static class ColorMapEntry {
		String key;
		List<String> value;

		ColorMapEntry(String key, List<String> value) {
			this.key = key;
			this.value = value;
		}

		boolean containsPlaceholder(String placeholder) {
			return key.contains("${" + placeholder + "}")
					|| value.stream().anyMatch(s -> s.contains("${" + placeholder + "}"));
		}

		ColorMapEntry replace(String placeholder, String replace) {
			return new ColorMapEntry(key.replace("${" + placeholder + "}", replace),
					value.stream().map(s -> s.replace("${" + placeholder + "}", replace)).collect(Collectors.toList()));
		}
	}
}