package de.piegames.blockmap.generate;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
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
		Map<Block, BlockColor> blockColors = new HashMap<>();

		for (Entry<Block, List<String>> e : blocks.entrySet()) {
			Block block = e.getKey();
			Queue<String> colorInfo = new LinkedList<>(e.getValue());

			BlockColor color = new BlockColor();
			color.color = ColorCompiler.compileTexture(block.toString(), colorInfo, jarFile);
			for (String remainingProperty : colorInfo) {
				if (remainingProperty.startsWith("tint="))
					switch (remainingProperty.substring("tint=".length())) {
					case "none":
						break;
					case "grass":
						color.isGrass = true;
						break;
					case "foliage":
						color.isFoliage = true;
						break;
					case "water":
						color.isWater = true;
						break;
					default:
						throw new IOException("Block " + block + ": " + e.getValue() + " is malformed.");
					}
				if (remainingProperty.startsWith("translucent=")
						&& Boolean.parseBoolean(remainingProperty.substring("translucent=".length())))
					color.isTranslucent = true;
			}

			log.debug("Compiled texture " + e.getKey() + " to " + color);
			blockColors.put(block, color);
		}

		return new BlockColorMap(blockColors);
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