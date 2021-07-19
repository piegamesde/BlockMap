package de.piegames.blockmap.gui;

import org.joml.AABBd;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class RenderedRegion {

	/* Set to null if the region has no pixels (all transparent) */
	protected WritableImage[]	images;
	public final Vector2ic		position;
	protected boolean			isRendered	= false;

	public RenderedRegion(Vector2ic position) {
		this.position = new Vector2i(position);
		this.images = new WritableImage[6];
	}

	/** Set this region's image and also calculate down-scaled versions */
	public void setImage(WritableImage image) {
		check: { /* Check if the image is empty to save RAM */
			var reader = image.getPixelReader();
			for (int y = 0; y < 512; y++)
				for (int x = 0; x < 512; x++) {
					int argb = reader.getArgb(x, y);
					if (argb != 0)
						break check;
				}
			/* The image is transparent */
			this.images = null;
			this.isRendered = true;
			return;
		}

		/* We do 5 MIP map levels, giving 1:32 zoom at most. This corresponds to 1 pixel per chunk. */
		this.images[0] = image;
		for (int i = 1; i <= 5; i++) {
			var size = 512 >>> i;
			var nextImage = new WritableImage(size, size);

			PixelWriter writer = nextImage.getPixelWriter();
			PixelReader reader = image.getPixelReader();
			for (int y = 0; y < size; y++)
				for (int x = 0; x < size; x++) {
					int c1 = reader.getArgb(x << 1, y << 1),
							c2 = reader.getArgb((x << 1) + 1, y << 1),
							c3 = reader.getArgb(x << 1, (y << 1) + 1),
							c4 = reader.getArgb((x << 1) + 1, (y << 1) + 1);
					// TODO pre-multiply alpha to avoid dark edges
					long argb = 0;// use long against overflow
					long a1 = c1 >>> 24, a2 = c2 >>> 24, a3 = c3 >>> 24, a4 = c4 >>> 24;
					// alpha
					argb |= ((a1 + a2 + a3 + a4) << 22) & 0xFF000000;
					// red
					argb |= ((((c1 & 0x00FF0000) * a1 + (c2 & 0x00FF0000) * a2 + (c3 & 0x00FF0000) * a3 + (c4 & 0x00FF0000) * a4) / 255) >> 2) & 0x00FF0000;
					// green
					argb |= ((((c1 & 0x0000FF00) * a1 + (c2 & 0x0000FF00) * a2 + (c3 & 0x0000FF00) * a3 + (c4 & 0x0000FF00) * a4) / 255) >> 2) & 0x0000FF00;
					// blue
					argb |= ((((c1 & 0x000000FF) * a1 + (c2 & 0x000000FF) * a2 + (c3 & 0x000000FF) * a3 + (c4 & 0x000000FF) * a4) / 255) >> 2) & 0x000000FF;

					writer.setArgb(x, y, (int) argb);
				}

			image = nextImage;
			this.images[i] = image;
		}
		this.isRendered = true;
	}

	public boolean isVisible(AABBd frustum) {
		int size = 512;
		return frustum.intersectsAABB(new AABBd(position.x() * size, position.y() * size, 0, (position.x() + 1) * size, (position.y() + 1) * size, 0));
	}

	public void draw(GraphicsContext gc, AABBd frustum, double scale, int level) {
		if (this.isRendered) {
			int size = 512;
			if (images != null)
				gc.drawImage(images[level], position.x() * size, position.y() * size, size, size);
		} else {
			drawBackground(gc, scale);
		}
	}

	/** This method assumes the appropriate fill is already set */
	public void drawBackground(GraphicsContext gc, double scale) {
		int size = 512;
		gc.fillRect(position.x() * size - 1 / scale, position.y() * size - 1 / scale, size + 2 / scale, size + 2 / scale);
	}

	public void drawForeground(GraphicsContext gc, AABBd frustum, double scale) {
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