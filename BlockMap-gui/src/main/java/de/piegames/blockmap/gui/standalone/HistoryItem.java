package de.piegames.blockmap.gui.standalone;

import java.util.Objects;

public class HistoryItem {
	protected boolean			server;
	protected long				timestamp;
	protected String			iconURL;
	protected String			name;
	protected String			path;

	public HistoryItem() {
	}

	public HistoryItem(boolean server, String name, String path, String iconURL, long timestamp) {
		this.server = server;
		this.name = Objects.requireNonNull(name);
		this.path = Objects.requireNonNull(path);
		this.iconURL = iconURL;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return name + "\t–\t" + path;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	/** For history items, the last time the item was loaded in BlockMap. Otherwise, the last time it was loaded in Minecraft */
	public long lastAccessed() {
		return timestamp;
	}
}
