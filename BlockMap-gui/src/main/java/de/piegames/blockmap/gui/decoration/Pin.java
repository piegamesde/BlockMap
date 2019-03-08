package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3ic;

import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.WorldPins;
import de.piegames.blockmap.world.WorldPins.MapPin.BannerPin;
import de.piegames.blockmap.world.WorldPins.VillagePin;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Translate;

//##############

// Pin type hierarchy (not class hierarchy; meta pins in brackets):
//
// (Pin)
// - (Button pin)
//   - (Structure pin)
//     - ...
//     - ...
//   - Player pin
//     - Player spawnpoint pin
//   - Map pin
//     - Banner pin
//   - Village pin
//     - Door pin
//   - Spawnpoint pin
// - Barrier pin
// - (Chunk pin)
//   - Force chunk pin
//   - Slime chunk pin
//   - Chunk status pin

//##############

// Actual pin class hierarchy
// Pin
// - ButtonPin
//   - PlayerPin
//   - MapPin
// - ChunkPin
//   - ChunkStatusPin?
// - BarrierPin

//##############

public abstract class Pin {

	public static class PinType {
		public static final PinType		ANY_PIN		= new PinType("Show pins", null, false, true);
		public static final PinType		CHUNK_PIN	= new PinType("Chunk pins", ANY_PIN, false, false);
		public static final PinType		BARRIER_PIN	= new PinType("Barrier", ANY_PIN, true, false);

		protected final List<PinType>	children	= new ArrayList<>();
		private final String			name;
		private final PinType			parent;
		public final boolean			selectedByDefault, expandedByDefault;

		public PinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault) {
			this.name = name;
			this.parent = parent;
			this.selectedByDefault = selectedByDefault;
			this.expandedByDefault = expandedByDefault;
			if (parent != null)
				parent.children.add(this);
		}

		public List<? extends PinType> getChildren() {
			return Collections.unmodifiableList(children);
		}

		@Override
		public String toString() {
			return name;
		}

		static {
			/* Force initialization of the class */
			Object o = CompressiblePinType.BUTTON_PIN;
		}
	}

	public static class CompressiblePinType extends PinType {
		public static final CompressiblePinType	BUTTON_PIN					= new CompressiblePinType("Buttons", PinType.ANY_PIN, false, true, "/tmp.png");

		public static final CompressiblePinType	PLAYER						= new CompressiblePinType("Player", BUTTON_PIN, true, false, "/tmp.png");
		public static final CompressiblePinType	PLAYER_POSITION				= new CompressiblePinType("Position", PLAYER, true, false,
				"textures/pins/player.png");
		public static final CompressiblePinType	PLAYER_SPAWN				= new CompressiblePinType("Spawnpoint", PLAYER, true, false,
				"textures/pins/spawn_player.png");

		public static final CompressiblePinType	MAP							= new CompressiblePinType("Map", BUTTON_PIN, true, false, "/tmp.png");
		public static final CompressiblePinType	MAP_POSITION				= new CompressiblePinType("Position", MAP, true, false,
				"textures/pins/map.png");
		public static final CompressiblePinType	MAP_BANNER					= new CompressiblePinType("Banner", MAP, true, false,
				"textures/pins/banner.png");																												// TODO
																																							// color

		public static final CompressiblePinType	VILLAGE						= new CompressiblePinType("Village", BUTTON_PIN, false, false, "/tmp.png");
		public static final CompressiblePinType	VILLAGE_CENTER				= new CompressiblePinType("Center", VILLAGE, false, false,
				"textures/structures/village.png");
		public static final CompressiblePinType	VILLAGE_DOOR				= new CompressiblePinType("House", VILLAGE, false, false,
				"textures/structures/house.png");

		public static final CompressiblePinType	WORLD_SPAWN					= new CompressiblePinType("Spawnpoint", BUTTON_PIN, true, false,
				"textures/pins/spawn_map.png");

		public static final CompressiblePinType	STRUCTURE					= new CompressiblePinType("Structures", BUTTON_PIN, false, false, "/tmp.png");
		public static final CompressiblePinType	STRUCTURE_TREASURE			= new CompressiblePinType("Treasure", STRUCTURE, false, false,
				"textures/structures/buried_treasure.png");
		public static final CompressiblePinType	STRUCTURE_PYRAMID			= new CompressiblePinType("Pyramid", STRUCTURE, false, false,
				"textures/structures/desert_pyramid.png");
		public static final CompressiblePinType	STRUCTURE_END_CITY			= new CompressiblePinType("End city", STRUCTURE, false, false,
				"textures/structures/end_city.png");
		public static final CompressiblePinType	STRUCTURE_FORTRESS			= new CompressiblePinType("Fortress", STRUCTURE, false, false,
				"textures/structures/fortress.png");
		public static final CompressiblePinType	STRUCTURE_IGLOO				= new CompressiblePinType("Igloo", STRUCTURE, false, false,
				"textures/structures/igloo.png");
		public static final CompressiblePinType	STRUCTURE_JUNGLE_TEMPLE		= new CompressiblePinType("Jungle temple", STRUCTURE, false, false,
				"textures/structures/jungle_pyramid.png");
		public static final CompressiblePinType	STRUCTURE_MANSION			= new CompressiblePinType("Mansion", STRUCTURE, false, false,
				"textures/structures/mansion.png");
		public static final CompressiblePinType	STRUCTURE_MINESHAFT			= new CompressiblePinType("Mineshaft", STRUCTURE, false, false,
				"textures/structures/mineshaft.png");
		public static final CompressiblePinType	STRUCTURE_OCEAN_MONUMENT	= new CompressiblePinType("Ocean monument", STRUCTURE, false, false,
				"textures/structures/monument.png");
		public static final CompressiblePinType	STRUCTURE_OCEAN_RUIN		= new CompressiblePinType("Ocean ruin", STRUCTURE, false, false,
				"textures/structures/ocean_ruin.png");
		public static final CompressiblePinType	STRUCTURE_SHIPWRECK			= new CompressiblePinType("Shipwreck", STRUCTURE, false, false,
				"textures/structures/shipwreck.png");
		public static final CompressiblePinType	STRUCTURE_STRONGHOLD		= new CompressiblePinType("Stronghold", STRUCTURE, false, false,
				"textures/structures/stronghold.png");
		public static final CompressiblePinType	STRUCTURE_WITCH_HUT			= new CompressiblePinType("Witch hut", STRUCTURE, false, false,
				"textures/structures/swamp_hut.png");

		public final Image						image;

		CompressiblePinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, String path) {
			this(name, parent, selectedByDefault, expandedByDefault, new Image(Pin.class.getResource(path).toString(), 128, 128, true, false));
		}

		CompressiblePinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, Image image) {
			super(name, parent, selectedByDefault, expandedByDefault);
			this.image = image;
		}
	}

	private static Log				log	= LogFactory.getLog(Pin.class);

	public final PinType			type;
	// public final Vector2ic regionPosition;
	protected final DisplayViewport	viewport;
	protected Node					topGui, bottomGui;

	public Pin(Vector2ic regionPosition, PinType type, DisplayViewport viewport) {
		// this.regionPosition = regionPosition;
		this.type = Objects.requireNonNull(type);
		this.viewport = viewport;
	}

	public final Node getTopGui() {
		if (topGui != null)
			return topGui;
		else
			return topGui = initTopGui();
	}

	public final Node getBottomGui() {
		if (bottomGui != null)
			return bottomGui;
		else
			return bottomGui = initBottomGui();
	}

	protected Node initTopGui() {
		return null;
	}

	protected Node initBottomGui() {
		return null;
	}

	public static abstract class CompressiblePin extends Pin {

		public final CompressiblePinType	type;
		public final Vector2dc				position;

		public CompressiblePin(Vector2d position, CompressiblePinType type, Vector2ic regionPosition, DisplayViewport viewport) {
			super(regionPosition, type, viewport);
			this.position = Objects.requireNonNull(position);
			this.type = type;
		}
	}

	public static class ButtonPin extends CompressiblePin {
		protected Button	button;
		protected PopOver	info;

		protected boolean	isDynamic;

		public ButtonPin(boolean isDynamic, Vector2d position, CompressiblePinType type, DisplayViewport viewport) {
			super(position, type, new Vector2i((int) position.x >> 9, (int) position.y >> 9), viewport);
			this.isDynamic = isDynamic;
		}

		@Override
		protected Node initTopGui() {
			ImageView img = new ImageView(type.image);
			img.setSmooth(false);
			button = new Button(null, img);
			img.setPreserveRatio(true);
			button.setTooltip(new Tooltip("BLUBBA"));
			button.setStyle("-fx-background-radius: 6em;");
			img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> button.getFont().getSize() * 2, button.fontProperty()));

			info = new PopOver();
			info.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
			info.setAutoHide(true);
			button.setOnAction(mouseEvent -> info.show(button));
			return wrapGui(button, position, viewport);
		}
	}

	public static class ChunkPin extends Pin {

		public final Vector2ic	chunkPosition;
		public final Image		image;

		public ChunkPin(Vector2ic regionPosition, Vector2ic chunkPosition, Image image, DisplayViewport viewport) {
			super(regionPosition, PinType.CHUNK_PIN, viewport);
			this.chunkPosition = Objects.requireNonNull(chunkPosition);
			this.image = image;
		}
	}

	public static StackPane wrapGui(Node node, Vector2dc position, DisplayViewport viewport) {
		DoubleBinding scale = Bindings.createDoubleBinding(
				() -> 2 * Math.min(1 / viewport.scaleProperty.get(), 1),
				viewport.scaleProperty);
		node.scaleXProperty().bind(scale);
		node.scaleYProperty().bind(scale);

		StackPane stack = new StackPane(node);
		Translate t = new Translate();
		t.xProperty().bind(stack.widthProperty().multiply(-0.5));
		t.yProperty().bind(stack.heightProperty().multiply(-0.5));
		stack.getTransforms().addAll(t, new Translate(position.x(), position.y()));
		return stack;
	}

	public static class MapPin extends ButtonPin {

		public int scale;

		public MapPin(Vector2d position, int scale, DisplayViewport viewport) {
			super(false, position, CompressiblePinType.MAP_POSITION, viewport);
			this.scale = scale;
		}

		@Override
		protected Node initBottomGui() {
			int size = 128 * (1 << this.scale);
			Rectangle rect = new Rectangle(size, size, new Color(0.9f, 0.15f, 0.15f, 0.02f));
			rect.setStroke(new Color(0.9f, 0.15f, 0.15f, 0.4f));
			rect.setMouseTransparent(true);
			rect.setPickOnBounds(false);
			getTopGui().hoverProperty().addListener(e -> {
				if (getTopGui().isHover())
					rect.setFill(new Color(0.9f, 0.15f, 0.15f, 0.2f));
				else
					rect.setFill(new Color(0.9f, 0.15f, 0.15f, 0.02f));
			});
			StackPane stack = new StackPane(rect);
			Translate t = new Translate();
			t.xProperty().bind(stack.widthProperty().multiply(-0.5));
			t.yProperty().bind(stack.heightProperty().multiply(-0.5));
			stack.getTransforms().addAll(t, new Translate(position.x(), position.y()));
			stack.setPickOnBounds(false);
			return stack;
		}
	}

	public static class PlayerPin extends ButtonPin {

		protected de.piegames.blockmap.world.WorldPins.PlayerPin player;

		public PlayerPin(de.piegames.blockmap.world.WorldPins.PlayerPin player, DisplayViewport viewport) {
			super(false, new Vector2d(player.getPosition().x(), player.getPosition().z()), CompressiblePinType.PLAYER_POSITION,
					viewport);
			this.player = player;
		}

		@Override
		protected Node initTopGui() {
			Node node = super.initTopGui();
			PopOver info = this.info;
			info.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
			info.setAutoHide(true);
			button.setOnAction(mouseEvent -> info.show(button));
			GridPane playerContent = new GridPane();
			info.setContentNode(playerContent);

			if (player.getSpawnpoint().isPresent()) {
				playerContent.add(new Label("Spawnpoint: " + player.getSpawnpoint().get()), 0, 0);
				Button jumpButton = new Button("Go there");
				playerContent.add(jumpButton, 1, 0);
				Vector2d spawnpoint = new Vector2d(player.getSpawnpoint().get().x(), player.getSpawnpoint().get().z());
				jumpButton.setOnAction(e -> viewport.translationProperty.set(spawnpoint));
			}

			return node;
		}
	}

	public static class PlayerSpawnpointPin extends ButtonPin {
		public PlayerSpawnpointPin(de.piegames.blockmap.world.WorldPins.PlayerPin player, DisplayViewport viewport) {
			super(false, new Vector2d(player.getSpawnpoint().get().x(), player.getSpawnpoint().get().z()), CompressiblePinType.PLAYER_SPAWN, viewport);
		}
	}

	public static Set<Pin> convert(WorldPins pin, DisplayViewport viewport) {
		Set<Pin> pins = new HashSet<>();
		for (de.piegames.blockmap.world.WorldPins.PlayerPin player : pin.getPlayers().orElse(Collections.emptyList())) {
			pins.add(new PlayerPin(player, viewport));
			if (player.getSpawnpoint().isPresent())
				pins.add(new PlayerSpawnpointPin(player, viewport));
		}

		for (VillagePin village : pin.getVillages().orElse(Collections.emptyList())) {
			pins.add(new ButtonPin(false,
					new Vector2d(village.getPosition().x(), village.getPosition().z()),
					CompressiblePinType.VILLAGE_CENTER, viewport));
			for (Vector3ic door : village.getDoors().orElse(Collections.emptyList()))
				pins.add(new ButtonPin(false,
						new Vector2d(door.x(), door.z()),
						CompressiblePinType.VILLAGE_DOOR, viewport));
		}

		for (de.piegames.blockmap.world.WorldPins.MapPin map : pin.getMaps().orElse(Collections.emptyList())) {
			pins.add(new MapPin(new Vector2d(map.getPosition().x(), map.getPosition().y()), map.getScale(), viewport));
			for (BannerPin banner : map.getBanners().orElse(Collections.emptyList())) {
				pins.add(new ButtonPin(false, new Vector2d(banner.getPosition().x(), banner.getPosition().y()), CompressiblePinType.MAP_BANNER, viewport));
			}
		}
		return pins;
	}

	public static Set<Pin> convert(ChunkMetadata metadata, DisplayViewport viewport) {
		Set<Pin> pins = new HashSet<>();
		// {
		// Image image = null;
		// switch (metadata.renderState) {
		// case FAILED:
		// image = new Image(Pin.class.getResource("overlays/chunk_corrupt.png").toString());
		// break;
		// case NOT_GENERATED:
		// image = new Image(Pin.class.getResource("overlays/chunk_unfinished.png").toString());
		// break;
		// case TOO_OLD:
		// image = new Image(Pin.class.getResource("overlays/chunk_outdated.png").toString());
		// break;
		// default:
		// }
		// if (image != null) {
		// ChunkPin p = new ChunkPin(metadata.position, new Vector2d(metadata.position.x() * 16 + 8, metadata.position.y() * 16 + 8), image);
		// pins.add(p);
		// }
		// }
		metadata.structures.forEach((name, pos) -> {
			CompressiblePinType type = null;
			// TODO refactor this into the pin type; this is ugly AF
			switch (name) {
			case "Igloo":
				type = CompressiblePinType.STRUCTURE_IGLOO;
				break;
			case "Monument":
				type = CompressiblePinType.STRUCTURE_OCEAN_MONUMENT;
				break;
			case "Ocean_Ruin":
				type = CompressiblePinType.STRUCTURE_OCEAN_RUIN;
				break;
			case "Swamp_Hut":
				type = CompressiblePinType.STRUCTURE_WITCH_HUT;
				break;
			case "Desert_Pyramid":
				type = CompressiblePinType.STRUCTURE_PYRAMID;
				break;
			case "Jungle_Pyramid":
				type = CompressiblePinType.STRUCTURE_JUNGLE_TEMPLE;
				break;
			case "Mineshaft":
				type = CompressiblePinType.STRUCTURE_MINESHAFT;
				break;
			case "Buried_Treasure":
				type = CompressiblePinType.STRUCTURE_TREASURE;
				break;
			case "Shipwreck":
				type = CompressiblePinType.STRUCTURE_SHIPWRECK;
				break;
			case "Stronghold":
				type = CompressiblePinType.STRUCTURE_STRONGHOLD;
				break;
			case "Village":
				/* Villages are handled separately, so ignore them */
				break;
			default:
				log.warn("Could not find a pin type named " + name);
			}
			if (type != null)
				pins.add(new ButtonPin(true, new Vector2d(pos.x(), pos.z()), type, viewport));
		});
		return pins;
	}

	// protected static Mojang mojang = new Mojang();
	//
	// public static void listPlayers(Path worldPath) {
	// mojang.connect();
	// if (mojang.getStatus(ServiceType.API_MOJANG_COM) == ServiceStatus.GREEN) {
	// System.out.println("Player UUID " + p.getFileName().toString().replace("-", "").split("\\.")[0]);
	// PlayerProfile profile = mojang.getPlayerProfile(p.getFileName().toString().replace("-", "").split("\\.")[0]);
	// name = profile.getUsername();
	// profile.getProperties().forEach(prop -> System.out.println(prop.getName() + ", " + prop.getValue() + ", " + prop
	// .getSignature()));
	// } else {
	// System.out.println("Status is " + mojang.getStatus(ServiceType.API_MOJANG_COM));
	// }
	// }
}