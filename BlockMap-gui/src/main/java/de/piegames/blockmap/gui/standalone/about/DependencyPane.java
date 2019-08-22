package de.piegames.blockmap.gui.standalone.about;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import de.piegames.blockmap.gui.standalone.about.AboutDialog.Dependency;
import de.piegames.blockmap.gui.standalone.about.AboutDialog.License;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

public class DependencyPane extends VBox {

	@FXML
	Label name, version;

	public DependencyPane(Dependency dependency) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("dependency.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		loader.load();

		name.setText(dependency.name);
		dependency.version.ifPresentOrElse(v -> version.setText("v." + v), () -> version.setVisible(false));
		{
			Label l = new Label(dependency.dependency);
			l.getStyleClass().add("dependency-gradle");
			getChildren().add(l);
		}
		dependency.description.ifPresent(d -> getChildren().add(new Label(String.valueOf(d).replace("\\n", "\n").replace("\\t", "\t"))));
		/* If there is a year or a set of developers, combine them to a string */
		if (dependency.year.isPresent() || dependency.developers.length > 0)
			getChildren().add(new Label(Streams.concat(dependency.year.stream(), Stream.of(String.join(", ", dependency.developers))).collect(Collectors
					.joining(" ", "© ", ""))));
		dependency.url.ifPresent(url -> getChildren().add(new Hyperlink(url)));

		Separator separator = new Separator();
		separator.setMaxWidth(150);
		getChildren().add(separator);

		for (License license : dependency.licenses) {
			getChildren().add(new Label(license.name));
			getChildren().add(new Hyperlink(license.url));
		}
	}
}
