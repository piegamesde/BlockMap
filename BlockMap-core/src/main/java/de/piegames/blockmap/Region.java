package de.piegames.blockmap;

import java.nio.file.Path;
import java.util.Objects;

import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * A Minecraft region file, with a position within its world and a path pointing to the file.
 * 
 * @author piegames
 */
public class Region {

	public final Vector2ic	position;
	public final Path		path;
	public Path				renderedPath;

	public Region(int x, int z, Path path) {
		this(new Vector2i(x, z), path);
	}

	public Region(Vector2ic position, Path path) {
		this.position = Objects.requireNonNull(position);
		this.path = Objects.requireNonNull(path);
	}
}
