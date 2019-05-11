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

	private static Log log = null;

	/** Lazily initialize the logger to avoid loading Log4j too early (startup performance). */
	private static void checkLogger() {
		if (log == null)
			log = LogFactory.getLog(GuiMain.class);
	}

	/** Internal API, public due to technical reasons */
	public static GuiMain	instance;
	/** Internal API, public due to technical reasons */
	public GuiController	controller;
	/** Internal API, public due to technical reasons */
	public Stage			stage;

	private Parent			root;

	public GuiMain() {
	}

	@Override
	public void init() throws IOException {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
			root = (Parent) loader.load();
			controller = (GuiController) loader.getController();
		} catch (Throwable t) {
			checkLogger();
			log.fatal("Cannot start BlockMap", t);
			System.exit(-1);
		}
	}

	@Override
	public void start(Stage stage) throws IOException {
		try {
			this.stage = stage;
			stage.setTitle("BlockMap map viewer");

			Scene scene = new Scene(root, 700, 450);
			scene.getStylesheets().add("/de/piegames/blockmap/guistandalone/style.css");
			stage.setScene(scene);
			stage.show();

			GuiMainPreloader.splashScreen.hide();

			/* Put this last to guarantee that the application is fully initialized once instance!=null. */
			instance = this;
		} catch (Throwable t) {
			checkLogger();
			log.fatal("Cannot start BlockMap", t);
			System.exit(-1);
		}
	}

	@Override
	public void stop() {
		controller.shutDown();
	}

	public static void main(String... args) {
		System.setProperty("javafx.preloader", GuiMainPreloader.class.getCanonicalName());
		Application.launch(args);
	}
}