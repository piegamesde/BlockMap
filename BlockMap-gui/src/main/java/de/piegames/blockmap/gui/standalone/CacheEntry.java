package de.piegames.blockmap.gui.standalone;

import de.piegames.blockmap.world.RegionFolder;

public class CacheEntry {
	public String		hash;
	public RegionFolder	folder;
	public boolean		force;

	public CacheEntry(String hash, RegionFolder folder, boolean force) {
		this.hash = hash;
		this.folder = folder;
		this.force = force;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result + (force ? 1231 : 1237);
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheEntry other = (CacheEntry) obj;
		if (folder == null) {
			if (other.folder != null)
				return false;
		} else if (!folder.equals(other.folder))
			return false;
		if (force != other.force)
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		return true;
	}
}
