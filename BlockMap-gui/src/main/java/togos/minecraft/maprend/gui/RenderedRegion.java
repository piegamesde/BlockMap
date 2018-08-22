package togos.minecraft.maprend.gui;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.joml.AABBd;
import org.joml.ImmutableVector2i;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import togos.minecraft.maprend.World.Region;

public class RenderedRegion {

	public enum RenderingState {
		VALID, // Don't touch
		INVALID, // Recalculate please
		DRAWING, // Recalculating
		REDRAW; // Aborted while recalculating, re-recalculate
		// ABORT; // World changed, abort calculation

		public boolean isInvalid() {
			return this != VALID;
		}
	}

	protected final RenderedMap						map;
	// public final AtomicBoolean invalid = new AtomicBoolean(true);
	protected final RenderedImage					image;
	public final int								level;
	public final ImmutableVector2i					position;
	public final AtomicReference<RenderingState>	valid	= new AtomicReference<>(RenderingState.INVALID);

	public Region									region;

	public RenderedRegion(RenderedMap map, Region region) {
		this(map, 0, new ImmutableVector2i(region.rx, region.rz));
		this.region = region;
	}

	public RenderedRegion(RenderedMap map, int level, ImmutableVector2i position) {
		this.map = map;
		this.level = level;
		this.position = position;
		this.image = map.createImage(this);
		// setImage(null);
	}

	public void setImage(WritableImage image) {
		Objects.requireNonNull(image);
		invalidateTree(true);
		this.image.setImage(image);
		valid.set(RenderingState.VALID);
	}

	public void invalidateTree(boolean keepImage) {
		if (!valid.compareAndSet(RenderingState.DRAWING, RenderingState.REDRAW))
			valid.set(RenderingState.INVALID);
		if (level >= 0)
			Arrays.stream(getBelow(false)).filter(__ -> __ != null).forEach(r -> r.invalidateTree(keepImage));
		if (level <= 0) {
			RenderedRegion above = getAbove(false);
			if (above != null)
				above.invalidateTree(keepImage);
		}
		if (!keepImage)
			this.image.setImage(null);
	}

	public RenderedRegion[] getBelow(boolean create) {
		return map.get(level + 1, RenderedMap.belowPos(position), create);
	}

	public RenderedRegion getAbove(boolean create) {
		return map.get(level - 1, RenderedMap.abovePos(position), create);
	}

	public RenderedRegion getGround(boolean create) {
		return map.get(0, RenderedMap.groundPos(position, level), create);
	}

	public boolean updateImage() {
		boolean changed = false;

		if (!this.image.isImageLoaded())
			changed = true;
		// This will load an image back from cache if needed
		WritableImage image = this.image.getImage(true);

		if (level != 0 && (image == null || valid.get().isInvalid())) {
			if (level > 0) {
				// check above
				// get above image
				RenderedRegion above = getGround(true);
				if (above == null)
					System.out.println(getGround(true));
				WritableImage aboveImage = above.getImage(true);
				// upscale image
				if (aboveImage != null)
					image = RenderedMap.doubleSize(image, aboveImage, level, new Vector2i(position.x() & ((1 << level) - 1), position.y() & ((1 << level) - 1)));
			} else if (level < 0) {
				// check below
				RenderedRegion[] below = getBelow(true);
				for (RenderedRegion r : below)
					if (r != null)
						changed |= r.updateImage();
				// get below images
				WritableImage topLeft = below[0] == null ? null : below[0].getImage(true);
				WritableImage topRight = below[1] == null ? null : below[1].getImage(true);
				WritableImage bottomLeft = below[2] == null ? null : below[2].getImage(true);
				WritableImage bottomRight = below[3] == null ? null : below[3].getImage(true);
				// downscale images
				image = RenderedMap.halfSize(image, topLeft, topRight, bottomLeft, bottomRight);
			}
			this.image.setImage(image);
			valid.set(RenderingState.VALID);
			if (image != null)
				changed = true;
		}

		return changed;
	}

	public WritableImage getImage(boolean force) {
		return image.getImage(force);
	}

	public boolean isVisible(AABBd frustum) {
		return isVisible(position, level, frustum);
	}

	public static boolean isVisible(Vector2ic position, int level, AABBd frustum) {
		int size = WorldRendererCanvas.pow2(512, -level);
		return frustum.testAABB(new AABBd(position.x() * size, position.y() * size, 0, (position.x() + 1) * size, (position.y() + 1) * size, 0));
	}

	public void draw(GraphicsContext gc, int drawingLevel, AABBd frustum, double scale) {
		// bounds must have been checked here

		int size = WorldRendererCanvas.pow2(512, -this.level);
		final int overDraw = 3; // TODO make setting

		WritableImage image = this.image.getImage(false);

		if (image != null) {
			if (drawingLevel <= this.level && this.level > 0)
				// Fill background to prevent bleeding from lower levels of detail
				gc.fillRect(position.x() * size + 1 / scale, position.y() * size + 1 / scale, size - 2 / scale, size - 2 / scale);
			// Draw that image
			gc.drawImage(image, position.x() * size, position.y() * size, size, size);
		}

		// Draw below if needed (check bounds)
		if (drawingLevel > this.level || ((this.level < 0 || drawingLevel > (this.level - overDraw)) && image == null)) {
			Arrays.stream(RenderedMap.belowPos(position))
					.filter(v -> isVisible(v, this.level + 1, frustum))
					.map(v -> map.get(this.level + 1, v, true))
					.filter(r -> r != null)
					.forEach(r -> r.draw(gc, drawingLevel, frustum, scale));
		}
	}

	/** This method assumes the appropriate fill is already set */
	public void drawBackground(GraphicsContext gc, double scale) {
		int size = 512;// WorldRendererFX.pow2(512, -this.level);
		// gc.translate(region.coordinates.x * 512, region.coordinates.y * 512);
		gc.fillRect(position.x() * size - 1 / scale, position.y() * size - 1 / scale, size + 2 / scale, size + 2 / scale);
	}

	public void drawForeground(GraphicsContext gc, AABBd frustum, double scale) {
		int size = 512;// WorldRendererFX.pow2(512, -this.level);
		if (valid.get().isInvalid() && image.isImageSet()) {// reduce brightness
			gc.setFill(new Color(0f, 0f, 0f, 0.5f));
			gc.fillRect(position.x() * size, position.y() * size, size, size);
		}
		if (valid.get() == RenderingState.DRAWING || valid.get() == RenderingState.REDRAW) {
			gc.setFill(new Color(0.9f, 0.9f, 0.15f, 1.0f));

			double x = position.x() * 512, y = position.y() * 512, w = 512, h = 512, m = Math.min(6 / scale, 35);

			double xw = Math.min(frustum.maxX, x + w);
			double yh = Math.min(frustum.maxY, y + h);
			x = Math.max(frustum.minX, x);
			y = Math.max(frustum.minY, y);
			w = xw - x;
			h = yh - y;

			// gc.translate(x, y);
			gc.fillRect(x, y, w, m);
			gc.fillRect(x, y + h - m, w, m);
			gc.fillRect(x, y, m, h);
			gc.fillRect(x + w - m, y, m, h);
		}
	}
}