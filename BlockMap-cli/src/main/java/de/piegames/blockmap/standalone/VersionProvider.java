package de.piegames.blockmap.standalone;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

	/* This part of the code gets generated automatically through `gradle generateSources`. Do not modify! */
	// $REPLACE_START
	public static final String VERSION = "2.3.0";
	// $REPLACE_END

	@Override
	public String[] getVersion() throws Exception {
		return new String[] { VERSION };
	}
}
