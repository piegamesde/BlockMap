package togos.minecraft.maprend.gui;

import java.util.Objects;
import org.joml.Vector3ic;
import org.mapdb.HTreeMap;
import javafx.scene.image.WritableImage;

public class RenderedImage {

	// private WritableImage image;
	private final RenderedMap							map;
	private final Vector3ic								key;
	private final HTreeMap<Vector3ic, WritableImage>	cache;
	private volatile boolean							hasValue	= false;

	public RenderedImage(RenderedMap map, HTreeMap<Vector3ic, WritableImage> cache, Vector3ic key) {
		this.map = Objects.requireNonNull(map);
		this.cache = Objects.requireNonNull(cache);
		this.key = Objects.requireNonNull(key);
		// System.out.println(cache + " " + key);
	}

	public void setImage(WritableImage image) {
		// this.image = image;
		// System.out.println(cache + " " + key + " " + image);
		if (image == null)
			cache.remove(key);
		else
			cache.put(key, image);
		hasValue = image != null;
	}

	/** Returns if the last {@link #setImage(WritableImage)} call was with a non-null value. */
	public boolean isImageSet() {
		return hasValue;
	}

	/**
	 * Returns true if the image is loaded in RAM, causing {@link #getImage(boolean)} to return immediately. Returns false if the image got written to disk to
	 * save memory.
	 */
	public boolean isImageLoaded() {
		return map.isImageLoaded(key);// cache.containsKey(key);
	}

	public WritableImage getImage(boolean force) {
		return (force || isImageLoaded()) ? cache.get(key) : null;
	}
}