package de.piegames.blockmap.generate;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.piegames.blockmap.MinecraftBlocks;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.renderer.Block;
import de.piegames.blockmap.renderer.BlockState;

public class ColorMapBuilder {

	private static Log					log				= LogFactory.getLog(ColorMapBuilder.class);

	public Map<String, List<String>>	placeholders	= new HashMap<>();
	/* (block name, block state) -> color generation info */
	public Map<Block, List<String>>		blocks			= new HashMap<>();

	MinecraftBlocks						minecraftBlocks;
	BlockState							states;

	public ColorMapBuilder(MinecraftBlocks minecraftBlocks, BlockState states) {
		this.minecraftBlocks = minecraftBlocks;
		this.states = states;
	}

	public void inherit(ColorMapBuilder inherit) {
		placeholders.putAll(inherit.placeholders);
		blocks.putAll(inherit.blocks);
	}

	public void override(ColorMapBuilder override, List<String> overrideWith) {
		placeholders.putAll(override.placeholders);
		/* Take all keys, map them to overrideWith and add them to the map. */
		blocks.putAll(override.blocks.keySet().stream().collect(Collectors.toMap(b -> b, b -> overrideWith)));
	}

	public void addBlock(ColorInstruction entry) {
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
		for (Block block : entry.decode(minecraftBlocks, states)) {
			// log.debug("Added block " + block + " " + entry.value);
			blocks.put(block, entry.value);
		}
	}

	public BlockColorMap compileColorMap(FileSystem jarFile) throws IOException {
		Map<String, Map<BitSet, BlockColor>> blockColors = new HashMap<>();

		for (Entry<Block, List<String>> e : blocks.entrySet()) {
			Block block = e.getKey();
			Queue<String> colorInfo = new LinkedList<>(e.getValue());

			// log.debug("Compiling block " + block + " with texture " + colorInfo);
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

			// log.debug("Compiled texture " + e.getKey() + " to " + color);
			blockColors.computeIfAbsent(block.name, k -> new HashMap<>());
			blockColors.get(block.name).put(block.state, color);
		}
		Map<String, BlockColorMap.StateColors> map = blockColors.entrySet().stream()
				.filter(e -> e.getValue().values().stream().distinct().count() <= 1)
				.filter(e -> isAllStates(e.getKey(), e.getValue().keySet()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> new BlockColorMap.SingleStateColors(e.getValue().values().iterator().next())));
		map.putAll(blockColors.entrySet().stream()
				.filter(e -> !map.containsKey(e.getKey()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> new BlockColorMap.NormalStateColors(e.getValue()))));
		return new BlockColorMap(map);
	}

	private boolean isAllStates(String blockName, Collection<BitSet> states) {
		MinecraftBlocks.Block block = minecraftBlocks.states.get(blockName);
		for (MinecraftBlocks.Block.State state : block.states) {
			BitSet compiledState = new BitSet(this.states.getSize());
			state.getProperties().entrySet().forEach(e -> compiledState.set(this.states.getProperty(e.getKey(), e.getValue())));
			if (!states.contains(compiledState))
				return false;
		}
		return true;
	}

	/**
	 * An instruction in the {@code block-color-instructions.json} file. It consists of a key specifying the block and a (list of) value(s). The
	 * value tells how to compile this key down to an actual color.
	 * 
	 * @see ColorCompiler#compileTexture(String, Queue, FileSystem)
	 */
	static class ColorInstruction {
		String			key;
		List<String>	value;

		ColorInstruction(String key, List<String> value) {
			this.key = key;
			this.value = value;
		}

		boolean containsPlaceholder(String placeholder) {
			return key.contains("${" + placeholder + "}")
					|| value.stream().anyMatch(s -> s.contains("${" + placeholder + "}"));
		}

		ColorInstruction replace(String placeholder, String replace) {
			return new ColorInstruction(key.replace("${" + placeholder + "}", replace),
					value.stream().map(s -> s.replace("${" + placeholder + "}", replace)).collect(Collectors.toList()));
		}

		/**
		 * <p>
		 * Turns a compact notation of a block like {@code door,half=bottom,open=false} into a {@code Block} object. Property wildcards like
		 * {@code age=*} will be expanded to all possible allowed values, hence a list is returned. Multiple wildcards will result in the cartesian
		 * product of all possible values. The format is {@code block_name[,property=value[,property=value[...]]]}, where "*" is a valid value.
		 * </p>
		 * <p>
		 * When providing invalid data, this method may throw a {@link RuntimeException} or silently ignore it and create a Block object
		 * representing an invalid block. Invalid blocks are those that couldn't exist in Minecraft. Specifying multiple values to one property,
		 * using invalid properties or values for a block or inventing block names are all invalid, as well omitting properties a Block is allowed
		 * to have.
		 * </p>
		 * 
		 * @return A list of blocks where each block matches the given compact form when expanding all wildcards correctly. Will contain exactly one
		 *         element if no wildcards are used.
		 */
		public List<Block> decode(MinecraftBlocks minecraftBlocks, BlockState states) {
			String[] subs = key.split(",");
			String blockName = "minecraft:" + subs[0];
			List<BitSet> blocks = new LinkedList<>();
			/* Start with at least one block */
			blocks.add(new BitSet(states.getSize()));
			/* Ignore the first entry, since it only contains the name */
			for (int i = 1; i < subs.length; i++) {
				String key = subs[i].split("=")[0];
				String val = subs[i].substring(subs[i].indexOf("=") + 1);
				/*
				 * If the values is a placeholder, duplicate all states in their current form while adding one variant of the new key to each. (Think of it
				 * as some cartesian product)
				 * 
				 * If the value is a normal value, simply add it to all found states.
				 */
				if (val.equals("*")) {
					List<BitSet> oldBlocks = blocks;
					blocks = new LinkedList<>();
					try {
						for (String allowedValue : minecraftBlocks.states.get(blockName).getProperties().get(key)) {
							for (BitSet oldState : oldBlocks) {
								BitSet toAdd = (BitSet) oldState.clone();
								toAdd.set(states.getProperty(key, allowedValue));
								blocks.add(toAdd);
							}
						}
					} catch (NullPointerException e) {
						if (minecraftBlocks.states.containsKey(blockName))
							throw new IllegalArgumentException("Block " + blockName + " does not have a state named " + key
									+ ", please remove it from color map", e);
						else
							throw new IllegalArgumentException("Block " + blockName + " does not exist, please remove it from color map", e);
					}
				} else
					blocks.stream().forEach(set -> set.set(states.getProperty(key, val)));
			}
			return blocks.stream().map(state -> new Block(blockName, state)).collect(Collectors.toList());
		}
	}
}