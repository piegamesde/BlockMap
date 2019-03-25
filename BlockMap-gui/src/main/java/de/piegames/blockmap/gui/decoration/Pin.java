package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import de.piegames.blockmap.world.ChunkMetadata.ChunkGenerationStatus;
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
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
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
//   - ChunkPin
//   - ChunkStatusPin?
// - BarrierPin

//##############

public abstract class Pin {

	public static class PinType {
		public static final PinType		ANY_PIN		= new PinType("Show pins", null, false, true);
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
		@Deprecated
		public static final CompressiblePinType	BUTTON_PIN					= new CompressiblePinType("Buttons", null, false, true, "/tmp.png");

		public static final CompressiblePinType	CHUNK_PIN					= new CompressiblePinType("Chunk pins", PinType.ANY_PIN, false, false, "/tmp.png");
		public static final CompressiblePinType	CHUNK_UNFINISHED			= new CompressiblePinType("Unfinished chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_unfinished.png");
		public static final CompressiblePinType	CHUNK_FAILED				= new CompressiblePinType("Corrupt chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_corrupted.png");
		public static final CompressiblePinType	CHUNK_OLD					= new CompressiblePinType("Old chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_outdated.png");

		public static final CompressiblePinType	PLAYER						= new CompressiblePinType("Player", PinType.ANY_PIN, true, false, "/tmp.png");
		public static final CompressiblePinType	PLAYER_POSITION				= new CompressiblePinType("Position", PLAYER, true, false,
				"textures/pins/player.png");
		public static final CompressiblePinType	PLAYER_SPAWN				= new CompressiblePinType("Spawnpoint", PLAYER, true, false,
				"textures/pins/spawn_player.png");

		public static final CompressiblePinType	MAP							= new CompressiblePinType("Map", PinType.ANY_PIN, true, false, "/tmp.png");
		public static final CompressiblePinType	MAP_POSITION				= new CompressiblePinType("Position", MAP, true, false,
				"textures/pins/map.png");
		public static final CompressiblePinType	MAP_BANNER					= new CompressiblePinType("Banner", MAP, true, false,
				"textures/pins/banner.png");																													// TODO
																																								// color

		public static final CompressiblePinType	VILLAGE						= new CompressiblePinType("Village", PinType.ANY_PIN, false, false, "/tmp.png");
		public static final CompressiblePinType	VILLAGE_CENTER				= new CompressiblePinType("Center", VILLAGE, false, false,
				"textures/structures/village.png");
		public static final CompressiblePinType	VILLAGE_DOOR				= new CompressiblePinType("House", VILLAGE, false, false,
				"textures/structures/house.png");

		public static final CompressiblePinType	WORLD_SPAWN					= new CompressiblePinType("Spawnpoint", PinType.ANY_PIN, true, false,
				"textures/pins/spawn_map.png");

		public static final CompressiblePinType	STRUCTURE					= new CompressiblePinType("Structures", PinType.ANY_PIN, false, false, "/tmp.png");
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

		public CompressiblePin(Vector2dc position, CompressiblePinType type, Vector2ic regionPosition, DisplayViewport viewport) {
			super(regionPosition, type, viewport);
			this.position = Objects.requireNonNull(position);
			this.type = type;
		}
	}

	public static class ButtonPin extends CompressiblePin {
		protected Button	button;
		protected PopOver	info;

		protected boolean	isDynamic;

		public ButtonPin(boolean isDynamic, Vector2dc position, CompressiblePinType type, DisplayViewport viewport) {
			super(position, type, new Vector2i((int) position.x() >> 9, (int) position.y() >> 9), viewport);
			this.isDynamic = isDynamic;
		}

		@Override
		protected Node initTopGui() {
			ImageView img = new ImageView(type.image);
			img.setSmooth(false);
			button = new Button(null, img);
			img.setPreserveRatio(true);
			button.setTooltip(new Tooltip(type.toString()));
			button.setStyle("-fx-background-radius: 6em;");
			img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> button.getFont().getSize() * 2, button.fontProperty()));

			info = new PopOver();
			info.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
			info.setAutoHide(true);
			button.setOnAction(mouseEvent -> info.show(button));
			return wrapGui(button, position, viewport);
		}
	}

	public static class ChunkPin extends ButtonPin {
		public final List<Vector2ic>	chunkPositions;
		public final Image				image;

		public ChunkPin(CompressiblePinType type, Vector2dc centerPos, List<Vector2ic> chunkPositions, Image image, DisplayViewport viewport) {
			super(true, centerPos, type, viewport);
			this.chunkPositions = Objects.requireNonNull(chunkPositions);
			this.image = image;
		}

		@Override
		protected Node initBottomGui() {
			Polygon shape = new Polygon();
			shape.getPoints().setAll(chunkPositions.stream().flatMap(v -> Stream.of(v.x(), v.y())).map(d -> (d + 1) * 16.0).collect(Collectors.toList()));
			shape.setFill(new ImagePattern(image, 0, 0, 16, 16, false));
			shape.setStrokeWidth(3);
			shape.setStroke(Color.YELLOW);
			shape.setOpacity(0.2);
			shape.setMouseTransparent(true);
			return shape;
		}
	}

	public static class UnfinishedChunkPin extends ChunkPin {

		protected int[] chunkCount;

		public UnfinishedChunkPin(Vector2dc centerPos, List<Vector2ic> chunkPositions, int[] chunkCount, Image image, DisplayViewport viewport) {
			super(CompressiblePinType.CHUNK_UNFINISHED, centerPos, chunkPositions, image, viewport);
			this.chunkCount = Objects.requireNonNull(chunkCount);
		}

		@Override
		public Node initTopGui() {
			Node n = super.initTopGui();

			GridPane popContent = new GridPane();
			info.setContentNode(popContent);

			for (int i = 0; i < chunkCount.length; i++) {
				popContent.add(new Label(ChunkGenerationStatus.values()[i].name().toLowerCase() + ":"), 0, i);
				popContent.add(new Label(chunkCount[i] + " chunks"), 1, i);
			}
			return n;
		}
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
		pin.getWorldSpawn().map(spawn -> new ButtonPin(false, new Vector2d(spawn.getSpawnpoint().x(), spawn.getSpawnpoint().z()),
				CompressiblePinType.WORLD_SPAWN, viewport))
				.ifPresent(pins::add);

		return pins;
	}

	public static Set<Pin> convert(Map<Vector2ic, ChunkMetadata> metadataMap, DisplayViewport viewport) {
		Set<Pin> pins = new HashSet<>();
		Set<Vector2ic> oldChunks = new HashSet<>(), failedChunks = new HashSet<>(), unfinishedChunks = new HashSet<>();
		/* Map each generation status to the amount of chunks with this state */
		int[] unfinishedCount = new int[ChunkGenerationStatus.values().length];
		for (ChunkMetadata metadata : metadataMap.values()) {
			switch (metadata.renderState) {
			case RENDERED:
				if (metadata.generationStatus != null && metadata.generationStatus != ChunkGenerationStatus.POSTPROCESSED)
					unfinishedChunks.add(metadata.position);
				unfinishedCount[metadata.generationStatus.ordinal()]++;
				break;
			case FAILED:
				failedChunks.add(metadata.position);
				break;
			case TOO_OLD:
				oldChunks.add(metadata.position);
				break;
			default:
				break;
			}
		}

		for (Set<Vector2ic> chunks : splitChunks(unfinishedChunks)) {
			Vector2dc center = chunks.stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new UnfinishedChunkPin(center, outlineSet(chunks), unfinishedCount,
					new Image(Pin.class.getResource("textures/overlays/chunk_unfinished.png").toString(), 64, 64, true, false),
					viewport));
		}
		for (Set<Vector2ic> chunks : splitChunks(failedChunks)) {
			Vector2dc center = chunks.stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new ChunkPin(CompressiblePinType.CHUNK_UNFINISHED, center, outlineSet(chunks),
					new Image(Pin.class.getResource("textures/overlays/chunk_corrupted.png").toString(), 64, 64, true, false),
					viewport));
		}
		for (Set<Vector2ic> chunks : splitChunks(oldChunks)) {
			Vector2dc center = chunks.stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new ChunkPin(CompressiblePinType.CHUNK_UNFINISHED, center, outlineSet(chunks),
					new Image(Pin.class.getResource("textures/overlays/chunk_outdated.png").toString(), 64, 64, true, false),
					viewport));
		}

		metadataMap.values().stream().flatMap(m -> m.structures.entrySet().stream()).forEach(e -> {
			CompressiblePinType type = null;
			// TODO refactor this into the pin type; this is ugly AF
			switch (e.getKey()) {
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
				log.warn("Could not find a pin type named " + e.getKey());
			}
			if (type != null)
				pins.add(new ButtonPin(true, new Vector2d(e.getValue().x(), e.getValue().z()), type, viewport));
		});
		return pins;
	}

	public static StackPane wrapGui(Node node, Vector2dc position, DisplayViewport viewport) {
		return wrapGui(node, position, Bindings.createDoubleBinding(
				() -> 2 * Math.min(1 / viewport.scaleProperty.get(), 1),
				viewport.scaleProperty), viewport);
	}

	public static StackPane wrapGui(Node node, Vector2dc position, DoubleBinding scale, DisplayViewport viewport) {
		if (scale != null) {
			node.scaleXProperty().bind(scale);
			node.scaleYProperty().bind(scale);
		}

		StackPane stack = new StackPane(node);
		Translate t = new Translate();
		t.xProperty().bind(stack.widthProperty().multiply(-0.5));
		t.yProperty().bind(stack.heightProperty().multiply(-0.5));
		stack.getTransforms().addAll(t, new Translate(position.x(), position.y()));
		return stack;
	}

	/**
	 * Takes in a set of chunk positions, identifies all connected subsets and calculates the outline of each one.
	 * 
	 * @param chunks
	 *            A set of chunk positions. This will be emptied during the calculation.
	 */
	private static List<Set<Vector2ic>> splitChunks(Set<Vector2ic> chunks) {
		List<Set<Vector2ic>> islands = new ArrayList<>();
		while (!chunks.isEmpty()) {
			Set<Vector2ic> done = new HashSet<>();
			Queue<Vector2ic> todo = new LinkedList<>();
			todo.add(chunks.iterator().next());
			chunks.remove(todo.element());
			while (!todo.isEmpty()) {
				Vector2ic current = todo.remove();
				for (Vector2i neighbor : new Vector2i[] { new Vector2i(-1, 0), new Vector2i(1, 0), new Vector2i(0, -1), new Vector2i(0, 1) }) {
					neighbor.add(current);
					if (chunks.remove(neighbor))
						todo.add(neighbor);
				}
				done.add(current);
			}
			islands.add(done);
		}
		return islands;
	}

	private static List<Vector2ic> outlineSet(Set<Vector2ic> chunks) {
		if (chunks.isEmpty())
			return Collections.emptyList();
		Vector2i pos = new Vector2i(chunks.iterator().next());
		/* O O <- bit 4, 3 */
		/* O X <- bit 2, 1 | X: Current position */
		int sample = 0;
		/* top-left */
		if (chunks.contains(new Vector2i(0, 0).add(pos)))
			sample |= 0b1000;
		/* top-right */
		if (chunks.contains(new Vector2i(1, 0).add(pos)))
			sample |= 0b0100;
		/* bottom-left */
		if (chunks.contains(new Vector2i(0, 1).add(pos)))
			sample |= 0b0010;
		/* bottom-right */
		if (chunks.contains(new Vector2i(1, 1).add(pos)))
			sample |= 0b0001;
		int direction = 0;

		ArrayList<Vector2ic> outline = new ArrayList<>();
		while (outline.isEmpty() || !pos.equals(outline.get(0))) {

			/* To which pixel to move next? */
			switch (sample) {
			/* Move right */
			case 0b0001:
			case 0b1011:
				outline.add(new Vector2i(pos));
			case 0b0011:
			case 0b1111:
				direction = 0;
				break;
			/* Move down */
			case 0b0010:
			case 0b1110:
				outline.add(new Vector2i(pos));
			case 0b1010:
				direction = 1;
				break;
			/* Move left */
			case 0b1000:
			case 0b1101:
				outline.add(new Vector2i(pos));
			case 0b1100:
				direction = 2;
				break;
			/* Move up */
			case 0b0100:
			case 0b0111:
				outline.add(new Vector2i(pos));
			case 0b0101:
				direction = 3;
				break;
			/* Ambiguous: Turn 90° CW from previous */
			case 0b1001:
			case 0b0110:
				direction = (direction + 1) & 3;
				break;

			default:
				throw new InternalError("Invalid state while finding the outline. Either the input is not valid or something is seriously wrong.");
			}

			/* Move to the next pixel and sample at the new position */
			switch (direction) {
			/* Move right */
			case 0:
				pos.add(1, 0);
				sample = (sample << 1) & 0b1010;

				/* top-right */
				if (chunks.contains(new Vector2i(1, 0).add(pos)))
					sample |= 0b0100;
				/* bottom-right */
				if (chunks.contains(new Vector2i(1, 1).add(pos)))
					sample |= 0b0001;
				break;

			/* Move down */
			case 1:
				pos.add(0, 1);
				sample = (sample << 2) & 0b1100;

				/* bottom-left */
				if (chunks.contains(new Vector2i(0, 1).add(pos)))
					sample |= 0b0010;
				/* bottom-right */
				if (chunks.contains(new Vector2i(1, 1).add(pos)))
					sample |= 0b0001;
				break;

			/* Move left */
			case 2:
				pos.add(-1, 0);
				sample = (sample >>> 1) & 0b0101;

				/* top-left */
				if (chunks.contains(new Vector2i(0, 0).add(pos)))
					sample |= 0b1000;
				/* bottom-left */
				if (chunks.contains(new Vector2i(0, 1).add(pos)))
					sample |= 0b0010;
				break;

			/* Move up */
			case 3:
				pos.add(0, -1);
				sample = (sample >>> 2) & 0b0011;

				/* top-left */
				if (chunks.contains(new Vector2i(0, 0).add(pos)))
					sample |= 0b1000;
				/* top-right */
				if (chunks.contains(new Vector2i(1, 0).add(pos)))
					sample |= 0b0100;
				break;
			}
		}

		return outline;
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