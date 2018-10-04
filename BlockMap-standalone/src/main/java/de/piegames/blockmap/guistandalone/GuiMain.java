package de.piegames.blockmap.guistandalone;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiMain extends Application {

	private GuiController controller;

	public GuiMain() {
	}

	@Override
	public void start(Stage stage) throws IOException {
		stage.setTitle("BlockMap map viewer");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
		Parent root = (Parent) loader.load();
		controller = (GuiController) loader.getController();

		stage.setScene(new Scene(root, 700, 450));
		stage.show();
	}

	@Override
	public void stop() {
		controller.renderer.shutDown();
	}

	public static void main(String... args) {
		Application.launch(args);
	}

	/*
	 * Apparently, I can't get it to launch the default main method using generics, but this works. To fix it, remove this method and adapt
	 * CommandLineMain#run.
	 */
	public static void main2() {
		main(new String[0]);
	}
}