package de.piegames.blockmap.gui.standalone;

import org.controlsfx.control.StatusBar;

import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A simple mutation of {@link impl.org.controlsfx.skin.StatusBarSkin} because I want the label to be on the left */
public class StatusBarSkin2 extends SkinBase<StatusBar> {

	private HBox		leftBox;
	private HBox		rightBox;
	private Label		label;
	private ProgressBar	progressBar;

	public StatusBarSkin2(StatusBar statusBar) {
		super(statusBar);

		leftBox = new HBox();
		leftBox.getStyleClass().add("left-items"); //$NON-NLS-1$

		rightBox = new HBox();
		rightBox.getStyleClass().add("right-items"); //$NON-NLS-1$

		progressBar = new ProgressBar();
		progressBar.progressProperty().bind(statusBar.progressProperty());
		// progressBar.visibleProperty().bind(
		// Bindings.notEqual(0, statusBar.progressProperty()));

		label = new Label();
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		label.textProperty().bind(statusBar.textProperty());
		label.graphicProperty().bind(statusBar.graphicProperty());
		label.getStyleClass().add("status-label"); //$NON-NLS-1$

		leftBox.getChildren().setAll(getSkinnable().getLeftItems());

		rightBox.getChildren().setAll(getSkinnable().getRightItems());

		statusBar.getLeftItems().addListener(
				(Observable evt) -> leftBox.getChildren().setAll(
						getSkinnable().getLeftItems()));

		statusBar.getRightItems().addListener(
				(Observable evt) -> rightBox.getChildren().setAll(
						getSkinnable().getRightItems()));

		GridPane gridPane = new GridPane();

		GridPane.setFillHeight(leftBox, true);
		GridPane.setFillHeight(rightBox, true);
		GridPane.setFillHeight(label, true);
		GridPane.setFillHeight(progressBar, true);

		GridPane.setVgrow(leftBox, Priority.ALWAYS);
		GridPane.setVgrow(rightBox, Priority.ALWAYS);
		GridPane.setVgrow(label, Priority.ALWAYS);
		GridPane.setVgrow(progressBar, Priority.ALWAYS);

		GridPane.setHgrow(leftBox, Priority.ALWAYS);

		GridPane.setMargin(leftBox, new Insets(0, 10, 0, 10));
		GridPane.setMargin(rightBox, new Insets(0, 10, 0, 10));

		gridPane.add(label, 0, 0);
		gridPane.add(leftBox, 1, 0);
		gridPane.add(rightBox, 2, 0);
		gridPane.add(progressBar, 4, 0);

		getChildren().add(gridPane);
	}
}
