package de.piegames.blockmap.guistandalone;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiMain extends Application {

	private static Log		log	= LogFactory.getLog(GuiMain.class);

	/** Internal API, public due to technical reasons */
	public static GuiMain	instance;
	/** Internal API, public due to technical reasons */
	public GuiController	controller;
	/** Internal API, public due to technical reasons */
	public Stage			stage;

	public GuiMain() {
	}

	@Override
	public void start(Stage stage) throws IOException {
		try {
			this.stage = stage;
			stage.setTitle("BlockMap map viewer");

			FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
			Parent root = (Parent) loader.load();
			controller = (GuiController) loader.getController();

			Scene scene = new Scene(root, 700, 450);
			scene.getStylesheets().add("/de/piegames/blockmap/guistandalone/style.css");
			stage.setScene(scene);
			stage.show();

			/* Put this last to guarantee that the application is fully initialized once instance!=null. */
			instance = this;
		} catch (Throwable t) {
			log.fatal("Cannot start BlockMap", t);
			System.exit(-1);
		}
	}

	@Override
	public void stop() {
		controller.shutDown();
	}

	public static void main(String... args) {
		Application.launch(args);
	}
}