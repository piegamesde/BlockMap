package de.piegames.blockmap.guistandalone;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class GuiMainPreloader extends Preloader {

	/**
	 * Yeah this is kind of ugly, but we need GuiMain to know the preloader stage to hide it. This would be realizable in a less ugly way using
	 * event communication, but this would infer some latency while switching windows.
	 */
	static Stage splashScreen;

	private static final int	SPLASH_WIDTH	= 300;
	private static final int	SPLASH_HEIGHT	= SPLASH_WIDTH;

	@Override
	public void start(Stage stage) throws Exception {
		splashScreen = stage;
		stage.centerOnScreen();
		stage.initStyle(StageStyle.TRANSPARENT);
		stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
		ImageView icon = new ImageView(getClass().getResource("icon.png").toString());
		icon.setFitWidth(SPLASH_WIDTH);
		icon.setPreserveRatio(true);
		BorderPane parent = new BorderPane(icon);
		parent.setBackground(Background.EMPTY);
		Scene scene = new Scene(parent, SPLASH_WIDTH, SPLASH_HEIGHT);
		scene.setFill(Color.TRANSPARENT);

		splashScreen.setScene(scene);
		splashScreen.show();
	}
}
