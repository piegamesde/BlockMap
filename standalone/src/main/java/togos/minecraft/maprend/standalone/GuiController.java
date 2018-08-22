package togos.minecraft.maprend.standalone;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.controlsfx.dialog.CommandLinksDialog;
import org.controlsfx.dialog.CommandLinksDialog.CommandLinksButtonType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import togos.minecraft.maprend.DotMinecraft;
import togos.minecraft.maprend.gui.MapPane;
import togos.minecraft.maprend.gui.WorldRendererCanvas;
import togos.minecraft.maprend.gui.decoration.DragScrollDecoration;
import togos.minecraft.maprend.gui.decoration.SettingsOverlay;
import togos.minecraft.maprend.renderer.RegionRendererOld;
import togos.minecraft.maprend.renderer.RenderSettings;

public class GuiController implements Initializable {

	public WorldRendererCanvas	renderer;

	@FXML
	private BorderPane			root;
	@FXML
	private TextField			pathField;
	@FXML
	private Button				browseButton;

	protected MapPane			pane;

	public GuiController() {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		try {
			renderer = new WorldRendererCanvas(new RegionRendererOld(new RenderSettings()));
			root.setCenter(pane = new MapPane(renderer));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		pane.decorationLayers.add(new DragScrollDecoration(renderer.viewport));
		pane.settingsLayers.add(new SettingsOverlay(renderer));
	}

	public void reloadWorld() {
		String world = pathField.getText();
		if (world.isEmpty())
			return;
		try {
			Path path = Paths.get(world);
			if (Files.exists(path) && Files.isDirectory(path)) {
				if (Files.exists(path.resolve("level.dat"))) { // Selected world folder
					String[] dimensions = new String[] { "region", "DIM-1\\region", "DIM1\\region" };
					String[] dimensionNames = new String[] { "Overworld", "Nether", "End" };
					final Path path2 = path;
					List<CommandLinksButtonType> availableDimensions = IntStream.range(0, 3)
							.filter(i -> Files.exists(path2.resolve(dimensions[i])))
							.mapToObj(i -> new CommandLinksButtonType(dimensionNames[i], false))
							.collect(Collectors.toList());
					if (!availableDimensions.isEmpty()) {
						// TODO change to conventional button dialog, this one is too big for our use case and doesn't look good
						CommandLinksDialog dialog = new CommandLinksDialog(availableDimensions);
						dialog.setTitle("Select dimension");
						dialog.initModality(Modality.APPLICATION_MODAL);
						Optional<ButtonType> result = availableDimensions.size() == 1 ? Optional.of(availableDimensions.get(0).getButtonType()) : dialog.showAndWait();
						if (result.isPresent()) {
							switch (result.get().getText()) {
								case "Overworld":
									path = path.resolve(dimensions[0]);
									break;
								case "Nether":
									path = path.resolve(dimensions[1]);
									break;
								case "End":
									path = path.resolve(dimensions[2]);
									break;
							}
						} else {
							throw new Error("TODO");
						}
					}
					pathField.setText(path.toAbsolutePath().toString());
				}

				// Region folder selected from here
				if (!hasFilesWithEnding(path, "mca"))
					new Alert(AlertType.WARNING, "Your selected folder seems to not contain any useful files." + (hasFilesWithEnding(path, "mcr")
							? " It does contain some region files in the old format though, please open this world in a newer version of Minecraft to automatically convert them." : "")).showAndWait();
				renderer.loadWorld(path.toFile());
			} else
				new Alert(AlertType.ERROR, "Folder does not exist", ButtonType.OK).showAndWait();
		} catch (InvalidPathException e) {
			new Alert(AlertType.ERROR, "Invalid path", ButtonType.OK).showAndWait();
		}
	}

	public void browse() {
		DirectoryChooser dialog = new DirectoryChooser();
		File f = pathField.getText().isEmpty() ? DotMinecraft.DOTMINECRAFT.resolve("saves").toFile() : new File(pathField.getText());
		// dialog.getExtensionFilters().add(new ExtensionFilter(description, extensions))
		dialog.setInitialDirectory(f);
		try {
			f = dialog.showDialog(null);
		} catch (IllegalArgumentException e) {
			// Invalid initial folder
			dialog.setInitialDirectory(DotMinecraft.DOTMINECRAFT.resolve("saves").toFile());
			f = dialog.showDialog(null);
		}
		pathField.setText(f == null ? "" : f.getAbsolutePath());
		pathField.fireEvent(new ActionEvent());
	}

	private boolean hasFilesWithEnding(Path path, String ending) {
		try {
			return Files.list(path).anyMatch(p -> p.getFileName().toString().endsWith("." + ending));
		} catch (IOException e) {
			System.err.println("Could not read content of the folder: " + path);
			e.printStackTrace();
			// No warning will be shown to the user. If there is a severe error, it will pop up again when trying to load it. If the folder does not
			// contain any usable files, the world map will be empty without warning,
			return true;
		}
	}
}