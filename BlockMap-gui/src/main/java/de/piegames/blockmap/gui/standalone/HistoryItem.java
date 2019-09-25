package de.piegames.blockmap.gui.standalone;

import java.util.Optional;

public class HistoryItem {
	public boolean			server;
	public Optional<String>	iconURL;
	public Optional<String>	name;
	public String			path;

	public HistoryItem() {
	}

	public HistoryItem(boolean server, String name, String path, String iconURL) {
		this.server = server;
		this.name = Optional.ofNullable(name);
		this.path = path;
		this.iconURL = Optional.ofNullable(iconURL);
	}

	@Override
	public String toString() {
		if (name.isPresent())
			return name.get() + "\t–\t" + path;
		else
			return path;
	}
}
