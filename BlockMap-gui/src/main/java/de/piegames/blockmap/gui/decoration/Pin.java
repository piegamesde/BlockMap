package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.joml.AABBd;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.shanerx.mojang.Mojang;
import org.shanerx.mojang.Mojang.ServiceStatus;
import org.shanerx.mojang.Mojang.ServiceType;
import org.shanerx.mojang.PlayerProfile;

import com.codepoetics.protonpack.StreamUtils;

import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkGenerationStatus;
import de.piegames.blockmap.world.WorldPins;
import de.piegames.blockmap.world.WorldPins.VillagePin;
import javafx.animation.Interpolator;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Translate;

public class Pin {

	public static class PinType {
		public static final PinType		MERGED_PIN					= new PinType("Merged pin", null, false, false, "/tmp.png");
		public static final PinType		ANY_PIN						= new PinType("Show pins", null, false, true, "/tmp.png");

		public static final PinType		CHUNK_PIN					= new PinType("Chunk pins", ANY_PIN, false, false, "/tmp.png");
		public static final PinType		CHUNK_UNFINISHED			= new PinType("Unfinished chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_unfinished.png");
		public static final PinType		CHUNK_FAILED				= new PinType("Corrupt chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_corrupted.png");
		public static final PinType		CHUNK_OLD					= new PinType("Old chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_outdated.png");

		public static final PinType		PLAYER						= new PinType("Player", ANY_PIN, true, false, "/tmp.png");
		public static final PinType		PLAYER_POSITION				= new PinType("Position", PLAYER, true, false,
				"textures/pins/player.png");
		public static final PinType		PLAYER_SPAWN				= new PinType("Spawnpoint", PLAYER, true, false,
				"textures/pins/spawn_player.png");

		public static final PinType		MAP							= new PinType("Map", ANY_PIN, true, false, "/tmp.png");
		public static final PinType		MAP_POSITION				= new PinType("Position", MAP, true, false,
				"textures/pins/map.png");
		public static final PinType		MAP_BANNER					= new PinType("Banner", MAP, true, false,
				"textures/pins/banner.png");

		public static final PinType		VILLAGE						= new PinType("Village", ANY_PIN, false, false, "/tmp.png");
		public static final PinType		VILLAGE_CENTER				= new PinType("Center", VILLAGE, false, false,
				"textures/structures/village.png");
		public static final PinType		VILLAGE_DOOR				= new PinType("House", VILLAGE, false, false,
				"textures/structures/house.png");

		public static final PinType		WORLD_SPAWN					= new PinType("Spawnpoint", ANY_PIN, true, false,
				"textures/pins/spawn_map.png");

		public static final PinType		STRUCTURE					= new PinType("Structures", ANY_PIN, false, true, "/tmp.png");
		public static final PinType		STRUCTURE_TREASURE			= new PinType("Treasure", STRUCTURE, false, false,
				"textures/structures/buried_treasure.png");
		public static final PinType		STRUCTURE_PYRAMID			= new PinType("Pyramid", STRUCTURE, false, false,
				"textures/structures/desert_pyramid.png");
		public static final PinType		STRUCTURE_END_CITY			= new PinType("End city", STRUCTURE, false, false,
				"textures/structures/end_city.png");
		public static final PinType		STRUCTURE_FORTRESS			= new PinType("Fortress", STRUCTURE, false, false,
				"textures/structures/fortress.png");
		public static final PinType		STRUCTURE_IGLOO				= new PinType("Igloo", STRUCTURE, false, false,
				"textures/structures/igloo.png");
		public static final PinType		STRUCTURE_JUNGLE_TEMPLE		= new PinType("Jungle temple", STRUCTURE, false, false,
				"textures/structures/jungle_pyramid.png");
		public static final PinType		STRUCTURE_MANSION			= new PinType("Mansion", STRUCTURE, false, false,
				"textures/structures/mansion.png");
		public static final PinType		STRUCTURE_MINESHAFT			= new PinType("Mineshaft", STRUCTURE, false, false,
				"textures/structures/mineshaft.png");
		public static final PinType		STRUCTURE_OCEAN_MONUMENT	= new PinType("Ocean monument", STRUCTURE, false, false,
				"textures/structures/monument.png");
		public static final PinType		STRUCTURE_OCEAN_RUIN		= new PinType("Ocean ruin", STRUCTURE, false, false,
				"textures/structures/ocean_ruin.png");
		public static final PinType		STRUCTURE_SHIPWRECK			= new PinType("Shipwreck", STRUCTURE, false, false,
				"textures/structures/shipwreck.png");
		public static final PinType		STRUCTURE_STRONGHOLD		= new PinType("Stronghold", STRUCTURE, false, false,
				"textures/structures/stronghold.png");
		public static final PinType		STRUCTURE_WITCH_HUT			= new PinType("Witch hut", STRUCTURE, false, false,
				"textures/structures/swamp_hut.png");

		protected final List<PinType>	children					= new ArrayList<>();
		private final String			name;
		public final boolean			selectedByDefault, expandedByDefault;
		public final Image				image;

		PinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, String path) {
			this(name, parent, selectedByDefault, expandedByDefault, new Image(Pin.class.getResource(path).toString(), 128, 128, true, false));
		}

		PinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, Image image) {
			this.name = name;
			this.selectedByDefault = selectedByDefault;
			this.expandedByDefault = expandedByDefault;
			this.image = image;
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
	}

	private static Log				log	= LogFactory.getLog(Pin.class);

	public final PinType			type;
	protected final Vector2dc		position;
	protected final DisplayViewport	viewport;
	private Node					topGui, bottomGui;
	private List<KeyValue>			animShow, animHide;

	protected Button				button;
	protected PopOver				info;

	int								level, parentLevel, zoomLevel = -1;

	public Pin(Vector2dc position, PinType type, DisplayViewport viewport) {
		this.type = Objects.requireNonNull(type);
		this.viewport = viewport;
		this.position = Objects.requireNonNull(position);
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

	protected Node initBottomGui() {
		return null;
	}

	public List<KeyValue> getAnimShow() {
		if (animShow == null)
			return animShow = animationKeys(true);
		else
			return animShow;
	}

	public List<KeyValue> getAnimHide() {
		if (animHide == null)
			return animHide = animationKeys(false);
		else
			return animHide;
	}

	protected List<KeyValue> animationKeys(boolean visible) {
		return Collections.unmodifiableList(Arrays.asList(
				new KeyValue(getTopGui().opacityProperty(), visible ? 1.0 : 0.0, Interpolator.EASE_BOTH),
				new KeyValue(getTopGui().visibleProperty(), visible, Interpolator.DISCRETE)));
	}

	public boolean isVisible(int level) {
		return level >= this.level && level < parentLevel;
	}

	public List<KeyValue> setZoomLevel(int level) {
		boolean visible = isVisible(level);
		if (visible != isVisible(zoomLevel) || zoomLevel == -1) {
			zoomLevel = -1;
			return visible ? getAnimShow() : getAnimHide();
		} else
			return Collections.emptyList();
	}

	public static class ChunkPin extends Pin {
		public final List<Vector2ic>	chunkPositions;
		public final Image				image;

		public ChunkPin(PinType type, Vector2dc centerPos, List<Vector2ic> chunkPositions, Image image, DisplayViewport viewport) {
			super(centerPos, type, viewport);
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
			super(PinType.CHUNK_UNFINISHED, centerPos, chunkPositions, image, viewport);
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

	public static class MapPin extends Pin {

		protected List<de.piegames.blockmap.world.WorldPins.MapPin> maps;

		public MapPin(Vector2d position, List<de.piegames.blockmap.world.WorldPins.MapPin> maps, DisplayViewport viewport) {
			super(position, PinType.MAP_POSITION, viewport);
			this.maps = Objects.requireNonNull(maps);
		}

		@Override
		protected Node initTopGui() {
			Node n = super.initTopGui();
			GridPane content = new GridPane();

			content.add(new Label("Map"), 0, 0, 2, 1);
			content.add(new Separator(), 0, 1, 2, 1);

			if (maps.size() > 1) {
				content.add(new Label("Map count:"), 0, 2);
				content.add(new Label(Integer.toString(maps.size())), 1, 2);

				content.add(new Label("Scales:"), 0, 3);
				content.add(new Label(maps.stream().map(m -> m.getScale()).map(scale -> "1:" + (1 << scale)).collect(Collectors.toSet()).toString()), 1, 3);
			} else {
				content.add(new Label("Scale:"), 0, 2);
				content.add(new Label(maps.stream().map(m -> m.getScale()).map(scale -> "1:" + (1 << scale)).findAny().get().toString()), 1, 2);
			}

			// TODO maybe add the map's image?

			info.setContentNode(content);
			return n;
		}

		@Override
		protected Node initBottomGui() {
			StackPane stack = new StackPane();
			stack.getChildren().setAll(maps.stream().map(map -> map.getScale()).distinct().map(scale -> {
				int size = 128 * (1 << scale);
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
				return rect;
			}).collect(Collectors.toList()));
			Translate t = new Translate();
			t.xProperty().bind(stack.widthProperty().multiply(-0.5));
			t.yProperty().bind(stack.heightProperty().multiply(-0.5));
			stack.getTransforms().addAll(t, new Translate(position.x(), position.y()));
			stack.setPickOnBounds(false);
			return stack;
		}
	}

	public static class PlayerPin extends Pin {

		protected de.piegames.blockmap.world.WorldPins.PlayerPin	player;
		protected Label												playerName;

		public PlayerPin(de.piegames.blockmap.world.WorldPins.PlayerPin player, DisplayViewport viewport) {
			super(new Vector2d(player.getPosition().x(), player.getPosition().z()), PinType.PLAYER_POSITION,
					viewport);
			this.player = player;
		}

		@Override
		protected Node initTopGui() {
			Node node = super.initTopGui();
			PopOver info = this.info;
			GridPane content = new GridPane();

			content.add(new Label("Player"), 0, 0, 2, 1);
			content.add(new Separator(), 0, 1, 2, 1);

			content.add(new Label("Name:"), 0, 2);
			content.add(playerName = new Label("loading..."), 1, 2);

			new Thread(() -> {
				Optional<PlayerProfile> playerInfo = player.getUUID().flatMap(uuid -> getPlayerInfo(uuid));
				if (playerInfo.isPresent()) {
					Platform.runLater(() -> playerName.setText(playerInfo.get().getUsername()));
					playerInfo.get().getTextures().flatMap(textures -> textures.getSkin()).ifPresent(url -> {
						Platform.runLater(() -> button.setGraphic(getSkin(url.toString())));
					});
				} else {
					Platform.runLater(() -> playerName.setText("(failed loading)"));
				}
			}).start();

			player.getSpawnpoint().ifPresent(spawn -> {
				content.add(new Label("Spawnpoint: "), 0, 3);
				Button jumpButton = new Button(spawn.toString());
				jumpButton.setTooltip(new Tooltip("Click to go there"));
				content.add(jumpButton, 1, 3);
				jumpButton.setOnAction(e -> {
					Vector2d spawnpoint = new Vector2d(spawn.x(), spawn.z());
					AABBd frustum = viewport.frustumProperty.get();
					viewport.translationProperty.set(spawnpoint.negate().add((frustum.maxX - frustum.minX) / 2, (frustum.maxY - frustum.minY) / 2));
					info.hide();
				});
			});

			info.setContentNode(content);

			return node;
		}

		private ImageView getSkin(String url) {
			log.debug("Loading player skin from: " + url);
			Image image = new Image(url);
			PixelReader reader = image.getPixelReader();
			image = new WritableImage(reader, 8, 8, 8, 8);

			ImageView graphic = new ImageView(image);
			graphic.setSmooth(false);
			graphic.setPreserveRatio(true);
			graphic.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> button.getFont().getSize() * 2, button.fontProperty()));
			return graphic;
		}

		private static Mojang mojang = new Mojang().connect();

		private static Optional<PlayerProfile> getPlayerInfo(String uuid) {
			if (mojang.getStatus(ServiceType.API_MOJANG_COM) == ServiceStatus.GREEN) {
				return Optional.of(mojang.getPlayerProfile(uuid));
			} else {
				return Optional.empty();
			}
		}
	}

	public static class PlayerSpawnpointPin extends Pin {
		protected de.piegames.blockmap.world.WorldPins.PlayerPin player;

		public PlayerSpawnpointPin(de.piegames.blockmap.world.WorldPins.PlayerPin player, DisplayViewport viewport) {
			super(new Vector2d(player.getSpawnpoint().get().x(), player.getSpawnpoint().get().z()), PinType.PLAYER_SPAWN, viewport);
			this.player = Objects.requireNonNull(player);
		}

		@Override
		protected Node initTopGui() {
			Node node = super.initTopGui();
			PopOver info = this.info;
			GridPane content = new GridPane();

			content.add(new Label("Player Spawnpoint"), 0, 0, 2, 1);
			content.add(new Separator(), 0, 1, 2, 1);

			content.add(new Label("Player position:"), 0, 2);

			Vector3dc position = player.getPosition();
			Button jumpButton = new Button(position.toString());
			jumpButton.setTooltip(new Tooltip("Click to go there"));
			content.add(jumpButton, 1, 2);
			jumpButton.setOnAction(e -> {
				Vector2d spawnpoint = new Vector2d(position.x(), position.z());
				AABBd frustum = viewport.frustumProperty.get();
				viewport.translationProperty.set(spawnpoint.negate().add((frustum.maxX - frustum.minX) / 2, (frustum.maxY - frustum.minY) / 2));
				info.hide();
			});

			info.setContentNode(content);

			return node;
		}
	}

	public static class _VillagePin extends Pin {

		protected VillagePin village;

		public _VillagePin(VillagePin village, DisplayViewport viewport) {
			super(new Vector2d(village.getPosition().x(), village.getPosition().z()), PinType.VILLAGE_CENTER, viewport);
			this.village = Objects.requireNonNull(village);
		}

		@Override
		protected Node initTopGui() {
			Node node = super.initTopGui();
			PopOver info = this.info;
			GridPane content = new GridPane();

			content.add(new Label("Village"), 0, 0, 1, 2);
			content.add(new Separator(), 0, 1, 1, 2);

			if (village.getRadius().isPresent()) {
				content.add(new Label("Radius: "), 0, 2);
				content.add(new Label(village.getRadius().get().toString()), 1, 2);
			}
			if (village.getGolems().isPresent()) {
				content.add(new Label("Golem count: "), 0, 3);
				content.add(new Label(village.getGolems().get().toString()), 1, 3);
			}
			if (village.getDoors().isPresent()) {
				content.add(new Label("Door count: "), 0, 4);
				content.add(new Label(String.valueOf(village.getDoors().get().size())), 1, 4);
			}

			info.setContentNode(content);

			return node;
		}
	}

	public static final class MergedPin extends Pin {

		final int					subCount;
		final Map<PinType, Long>	pinCount;

		private GridPane			popContent;

		public MergedPin(Pin subLeft, Pin subRight, int subCount, Vector2dc position, Map<PinType, Long> pinCount, DisplayViewport viewport) {
			super(position, PinType.MERGED_PIN, viewport);
			this.subCount = subCount;
			this.pinCount = Collections.unmodifiableMap(pinCount);
		}

		@Override
		protected Node initTopGui() {
			popContent = new GridPane();

			int columns = (int) Math.floor(Math.sqrt(pinCount.size()));
			GridPane box = new GridPane();
			box.setPadding(new Insets(5));
			box.setStyle("-fx-background-color: transparent;");
			StreamUtils.zipWithIndex(pinCount.entrySet().stream()).forEach(e -> {
				{/* Image for the pin's button */
					ImageView img = new ImageView(e.getValue().getKey().image);
					img.setSmooth(false);
					img.setPreserveRatio(true);
					Label label = new Label(String.format("%dx", e.getValue().getValue()), img);
					label.setPadding(new Insets(5));
					img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> label.getFont().getSize() * 2, label.fontProperty()));

					GridPane.setMargin(label, new Insets(5));
					box.add(label, (int) e.getIndex() % columns, (int) e.getIndex() / columns);
				}

				{/* Image+Text for the popover */
					ImageView img = new ImageView(e.getValue().getKey().image);
					img.setSmooth(false);
					img.setPreserveRatio(true);
					Label label1 = new Label(e.getValue().getKey().toString(), img);
					img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> label1.getFont().getSize() * 1.3, label1.fontProperty()));
					popContent.add(label1, 0, (int) e.getIndex());
					Label label2 = new Label(String.format("%dx", e.getValue().getValue()));
					popContent.add(label2, 1, (int) e.getIndex());
					GridPane.setMargin(label1, new Insets(5));
					GridPane.setMargin(label2, new Insets(5));
				}
			});

			button = new Button(null, box);
			button.setStyle("-fx-background-radius: 6em;");

			info = new PopOver();
			info.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
			info.setAutoHide(true);
			button.setOnAction(mouseEvent -> info.show(button));

			DoubleBinding scale = Bindings.createDoubleBinding(
					() -> 1 * Math.min(1 / viewport.scaleProperty.get(), 2),
					viewport.scaleProperty);

			info.setContentNode(popContent);

			return wrapGui(button, position, null, scale, viewport);
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
			pins.add(new _VillagePin(
					village,
					viewport));
			for (Vector3ic door : village.getDoors().orElse(Collections.emptyList()))
				pins.add(new Pin(
						new Vector2d(door.x(), door.z()),
						PinType.VILLAGE_DOOR, viewport) {
					// TODO cleanup
					@Override
					protected Node initTopGui() {
						Node n = super.initTopGui();
						info.setContentNode(new Label("Door"));
						return n;
					}
				});
		}

		/* Cluster maps at identical position to merge their pins. */
		pins.addAll(pin.getMaps().map(List::stream).orElse(Stream.empty())
				.collect(Collectors.groupingBy(map -> map.getPosition()))
				.entrySet()
				.stream()
				.map(e -> new MapPin(new Vector2d(e.getKey().x(), e.getKey().y()), e.getValue(), viewport))
				.collect(Collectors.toList()));
		/* All banner pins of the maps */
		pins.addAll(pin.getMaps().map(List::stream).orElse(Stream.empty())
				.flatMap(map -> map.getBanners().map(List::stream).orElse(Stream.empty()))
				.map(banner -> new Pin(new Vector2d(banner.getPosition().x(), banner.getPosition().y()), PinType.MAP_BANNER, viewport))
				.collect(Collectors.toList()));

		pin.getWorldSpawn().map(spawn -> new Pin(new Vector2d(spawn.getSpawnpoint().x(), spawn.getSpawnpoint().z()),
				PinType.WORLD_SPAWN, viewport) {

			@Override
			public Node initTopGui() {
				// TODO cleanup
				Node n = super.initTopGui();
				GridPane content = new GridPane();

				content.add(new Label("Spawnpoint"), 0, 0, 2, 1);
				content.add(new Separator(), 0, 1, 2, 1);

				content.add(new Label("Position:"), 0, 2);
				content.add(new Label(spawn.getSpawnpoint().toString()), 1, 2);

				info.setContentNode(content);
				return n;
			}

		}).ifPresent(pins::add);

		return pins;
	}

	public static List<Pin> convert(Map<Vector2ic, ChunkMetadata> metadataMap, DisplayViewport viewport) {
		List<Pin> pins = new ArrayList<>();
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
			pins.add(new ChunkPin(PinType.CHUNK_UNFINISHED, center, outlineSet(chunks),
					new Image(Pin.class.getResource("textures/overlays/chunk_corrupted.png").toString(), 64, 64, true, false),
					viewport));
		}
		for (Set<Vector2ic> chunks : splitChunks(oldChunks)) {
			Vector2dc center = chunks.stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new ChunkPin(PinType.CHUNK_UNFINISHED, center, outlineSet(chunks),
					new Image(Pin.class.getResource("textures/overlays/chunk_outdated.png").toString(), 64, 64, true, false),
					viewport));
		}

		metadataMap.values().stream().flatMap(m -> m.structures.entrySet().stream()).forEach(e -> {
			PinType type = null;
			// TODO refactor this into the pin type; this is ugly AF
			switch (e.getKey()) {
			case "Igloo":
				type = PinType.STRUCTURE_IGLOO;
				break;
			case "Monument":
				type = PinType.STRUCTURE_OCEAN_MONUMENT;
				break;
			case "Ocean_Ruin":
				type = PinType.STRUCTURE_OCEAN_RUIN;
				break;
			case "Swamp_Hut":
				type = PinType.STRUCTURE_WITCH_HUT;
				break;
			case "Desert_Pyramid":
				type = PinType.STRUCTURE_PYRAMID;
				break;
			case "Jungle_Pyramid":
				type = PinType.STRUCTURE_JUNGLE_TEMPLE;
				break;
			case "Mineshaft":
				type = PinType.STRUCTURE_MINESHAFT;
				break;
			case "Buried_Treasure":
				type = PinType.STRUCTURE_TREASURE;
				break;
			case "Shipwreck":
				type = PinType.STRUCTURE_SHIPWRECK;
				break;
			case "Stronghold":
				type = PinType.STRUCTURE_STRONGHOLD;
				break;
			case "Mansion":
				type = PinType.STRUCTURE_MANSION;
			case "Village":
				/* Villages are handled separately, so ignore them */
				break;
			default:
				log.warn("Could not find a pin type named " + e.getKey());
			}
			if (type != null) {
				pins.add(new Pin(new Vector2d(e.getValue().x(), e.getValue().z()), type, viewport) {
					@Override
					public Node initTopGui() {
						// TODO cleanup
						Node n = super.initTopGui();
						info.setContentNode(new Label(type.name));
						return n;
					}
				});
			}
		});
		return pins;
	}

	public static StackPane wrapGui(Node node, Vector2dc position, DisplayViewport viewport) {
		return wrapGui(node, position, null, Bindings.createDoubleBinding(
				() -> 2 * Math.min(1 / viewport.scaleProperty.get(), 1),
				viewport.scaleProperty), viewport);
	}

	// TODO maybe remove variablePosition if not needed
	public static StackPane wrapGui(Node node, Vector2dc basePosition, ObjectProperty<Vector2dc> variablePosition, DoubleBinding scale,
			DisplayViewport viewport) {
		if (scale != null) {
			node.scaleXProperty().bind(scale);
			node.scaleYProperty().bind(scale);
		}

		StackPane stack = new StackPane(node);
		Translate center = new Translate();
		center.xProperty().bind(stack.widthProperty().multiply(-0.5));
		center.yProperty().bind(stack.heightProperty().multiply(-0.5));
		stack.getTransforms().add(center);
		if (basePosition != null)
			stack.getTransforms().add(new Translate(basePosition.x(), basePosition.y()));
		if (variablePosition != null) {
			Translate pos = new Translate();
			pos.xProperty().bind(Bindings.createDoubleBinding(() -> variablePosition.get().x(), variablePosition));
			pos.yProperty().bind(Bindings.createDoubleBinding(() -> variablePosition.get().y(), variablePosition));
			stack.getTransforms().add(pos);
		}
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
}