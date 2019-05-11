package de.piegames.blockmap.guistandalone;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class GuiMainPreloader extends Preloader {

	/**
	 * Yeah this is kind of ugly, but we need GuiMain to know the preloader stage to hide it. This would be realizable in a less ugly way using
	 * event communication, but this would infer some latency while switching windows.
	 */
	static Stage splashScreen;

	private static final int	SPLASH_WIDTH	= 300;
	private static final int	SPLASH_HEIGHT	= 200;

	@Override
	public void start(Stage stage) throws Exception {
		splashScreen = stage;
		stage.initStyle(StageStyle.UNDECORATED);
		Scene scene = new Scene(new Label("TODO"), SPLASH_WIDTH, SPLASH_HEIGHT);

		/* Optionally: center it */
		// final Rectangle2D bounds = Screen.getPrimary().getBounds();
		// stage.setX(bounds.getMinX() + bounds.getWidth() / 2 - SPLASH_WIDTH / 2);
		// stage.setY(bounds.getMinY() + bounds.getHeight() / 2 - SPLASH_HEIGHT / 2);

		splashScreen.setScene(scene);
		splashScreen.show();
	}
}
