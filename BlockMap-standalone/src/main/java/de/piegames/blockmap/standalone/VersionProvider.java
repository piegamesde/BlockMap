package de.piegames.blockmap.standalone;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

	@Override
	public String[] getVersion() throws Exception {
		/* This part of the code gets generated automatically through `gradle generateSources`. Do not modify! */
		// $REPLACE_START
		return new String[] { "1.2.0" };
		// $REPLACE_END
	}
}
