package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.AABBd;
import org.joml.Vector2d;

import de.piegames.blockmap.pin.WorldPins;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Pin {
	public Vector2d	position;
	public AABBd	bounds;
	public Image	image	= new Image(getClass().getResource("pin-steve.png").toString());
	public Node		gui;
	public String	tag;

	public void draw(GraphicsContext gc) {

	}

	public static class MapPin extends Pin {
		public int scale;

		@Override
		public void draw(GraphicsContext gc) {
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
							return p;
						})
						.collect(Collectors.toList()));
		return pins;
	}
}