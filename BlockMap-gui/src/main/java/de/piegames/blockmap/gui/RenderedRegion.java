package de.piegames.blockmap.gui;

import org.joml.AABBd;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class RenderedRegion {

	protected final RenderedMap	map;
	protected WritableImage		image;
	public final int			level;
	public final Vector2ic		position;

	public RenderedRegion(RenderedMap map, Vector2ic pos) {
		this(map, 0, pos);
	}

	public RenderedRegion(RenderedMap map, int level, Vector2ic position) {
		this.map = map;
		this.level = level;
		this.position = new Vector2i(position);
		setImage(null);
	}

	public void setImage(WritableImage image) {
		this.image = image;
	}

	public void setSubImage(WritableImage sub, int dstX, int dstY) {
		if (image == null)
			image = new WritableImage(512, 512);
		PixelWriter writer = image.getPixelWriter();
		PixelReader reader = sub.getPixelReader();
		for (int y = 0; y < 256; y++)
			for (int x = 0; x < 256; x++) {
				int c1 = reader.getArgb(x << 1, y << 1),
						c2 = reader.getArgb((x == 256 ? 255 : x << 1) + 1, y << 1),
						c3 = reader.getArgb(x << 1, (y == 256 ? 255 : y << 1) + 1),
						c4 = reader.getArgb((x == 256 ? 255 : x << 1) + 1, (y == 256 ? 255 : y << 1) + 1);
				// TODO premultiply alpha to avoid dark edges
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

				writer.setArgb(dstX + x, dstY + y, (int) argb);
			}
	}

	public WritableImage getImage() {
		return image;
	}

	public boolean isVisible(AABBd frustum) {
		int size = 512 << level;
		return frustum.testAABB(new AABBd(position.x() * size, position.y() * size, 0, (position.x() + 1) * size, (position.y() + 1) * size, 0));
	}

	public void draw(GraphicsContext gc, AABBd frustum, double scale) {
		if (image != null) {
			int size = 512 << this.level;
			gc.drawImage(image, position.x() * size, position.y() * size, size, size);
		}
	}

	/** This method assumes the appropriate fill is already set */
	public void drawBackground(GraphicsContext gc, double scale) {
		if (this.level != 0)
			throw new IllegalStateException("Only the base level can draw the background");
		int size = 512;
		gc.fillRect(position.x() * size - 1 / scale, position.y() * size - 1 / scale, size + 2 / scale, size + 2 / scale);
	}

	public void drawForeground(GraphicsContext gc, AABBd frustum, double scale) {
		if (this.level != 0)
			throw new IllegalStateException("Only the base level can draw the foreground");

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