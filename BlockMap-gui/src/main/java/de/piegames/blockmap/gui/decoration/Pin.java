package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.joml.AABBd;
import org.joml.Vector2d;
import org.joml.Vector2ic;

import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.WorldPins;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Pin {

	public Vector2d	position;
	public AABBd	bounds;
	public Image	image;
	public Node		gui;
	public String	tag;

	public void drawBackground(GraphicsContext gc) {

	}

	public void drawForeground(GraphicsContext gc) {

	}

	public static class ChunkPin {

		public final Vector2ic chunkPosition;
		public Image			image;

		public ChunkPin(Vector2ic chunkPosition) {
			this.chunkPosition = Objects.requireNonNull(chunkPosition);
		}

		public void draw(GraphicsContext gc) {
			gc.drawImage(image, chunkPosition.x() << 4, chunkPosition.y() << 4, 16, 16);
		}
	}

	public static class MapPin extends Pin {
		public int scale;

		@Override
		public void drawBackground(GraphicsContext gc) {
			int size = 128 * (1 << scale);
			gc.setFill(new Color(0.9f, 0.15f, 0.15f, 0.2f));
			gc.setStroke(new Color(0.9f, 0.15f, 0.15f, 0.4f));
			gc.fillRect(position.x - size / 2, position.y - size / 2, size, size);
			gc.strokeRect(position.x - size / 2, position.y - size / 2, size, size);
		}
	}

	public static List<Pin> convert(WorldPins pin) {
		List<Pin> pins = new ArrayList<>();
		pins.addAll(
				pin.getPlayers().orElse(Collections.emptyList())
						.stream()
						.map(player -> {
							Pin p = new Pin();
							p.position = new Vector2d();
							p.position.x = player.getPosition().x();
							p.position.y = player.getPosition().z();
							p.image = new Image(Pin.class.getResource("pins/player.png").toString());
							return p;
						})
						.collect(Collectors.toList()));
		pins.addAll(
				pin.getVillages().orElse(Collections.emptyList())
						.stream()
						.map(village -> {
							Pin p = new Pin();
							p.position = new Vector2d();
							p.position.x = village.getPosition().x();
							p.position.y = village.getPosition().z();
							p.image = new Image(Pin.class.getResource("pins/village.png").toString());
							return p;
						})
						.collect(Collectors.toList()));
		pins.addAll(
				pin.getMaps().orElse(Collections.emptyList())
						.stream()
						.map(map -> {
							MapPin p = new MapPin();
							p.position = new Vector2d();
							p.position.x = map.getPosition().x();
							p.position.y = map.getPosition().y();
							p.scale = map.getScale();
							p.image = new Image(Pin.class.getResource("pins/map.png").toString());
							return p;
						})
						.collect(Collectors.toList()));
		return pins;
	}

	public static List<ChunkPin> convert(ChunkMetadata metadata) {
		List<ChunkPin> pins = new ArrayList<>();
		Image image = null;
		switch (metadata.renderState) {
		case FAILED:
			image = new Image(Pin.class.getResource("overlays/chunk_corrupt.png").toString());
			break;
		case NOT_GENERATED:
			image = new Image(Pin.class.getResource("overlays/chunk_unfinished.png").toString());
			break;
		case TOO_OLD:
			image = new Image(Pin.class.getResource("overlays/chunk_outdated.png").toString());
			break;
		default:
		}
		if (image != null) {
			ChunkPin p = new ChunkPin(metadata.position);
			p.image = image;
			pins.add(p);
		}
		return pins;
	}
}