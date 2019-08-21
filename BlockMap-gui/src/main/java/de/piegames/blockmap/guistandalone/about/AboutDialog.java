package de.piegames.blockmap.guistandalone.about;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class AboutDialog extends Alert {
	private static final String	LICENSE_TEXT	= "MIT License\n" +
			"\n" +
			"Copyright (c) 2018 piegames\n" +
			"\n" +
			"Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
			"of this software and associated documentation files (the \"Software\"), to deal\n" +
			"in the Software without restriction, including without limitation the rights\n" +
			"to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
			"copies of the Software, and to permit persons to whom the Software is\n" +
			"furnished to do so, subject to the following conditions:\n" +
			"\n" +
			"The above copyright notice and this permission notice shall be included in all\n" +
			"copies or substantial portions of the Software.\n" +
			"\n" +
			"THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
			"IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
			"FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
			"AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
			"LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
			"OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
			"SOFTWARE.\n" +
			"";

	@FXML
	VBox						dependencies;
	@FXML
	ScrollPane					dependencyContainer;
	@FXML
	TextArea					license;

	public AboutDialog() throws IOException {
		super(AlertType.NONE, null, ButtonType.CLOSE);
		setTitle("About BlockMap");
		setResizable(true);
		initModality(Modality.APPLICATION_MODAL);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutpane.fxml"));
		loader.setController(this);
		getDialogPane().setContent(loader.load());
		getDialogPane().getStylesheets().add("/de/piegames/blockmap/guistandalone/about/style.css");

		@SuppressWarnings("serial")
		List<Dependency> dependencies = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).create().fromJson(
				// TODO automate copying that file on dependency change
				new InputStreamReader(getClass().getResourceAsStream("licenseReport.json")),
				new TypeToken<List<Dependency>>() {
				}.getType());

		for (Dependency dependency : dependencies) {
			this.dependencies.getChildren().add(new DependencyPane(dependency));
		}

		license.setText(LICENSE_TEXT);
	}

	static class Dependency {
		@SerializedName("project")
		String				name;
		Optional<String>	version;
		Optional<String>	description;
		License[]			licenses;
		Optional<String>	year;
		String[]			developers;
		Optional<String>	url;
		String				dependency;
	}

	static class License {
		@SerializedName("license")
		String	name;
		@SerializedName("license_url")
		String	url;
	}
}
