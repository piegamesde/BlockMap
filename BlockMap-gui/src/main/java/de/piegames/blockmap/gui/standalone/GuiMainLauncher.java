package de.piegames.blockmap.gui.standalone;

public final class GuiMainLauncher {

	/**
	 * This is weird. If I launch {@code GuiMain} directly, it will fail because the modules path is not set up. But if I launch
	 * {@code GuiMainLauncher} (which really does absolutely nothing), it will just run fine without having to do anything.
	 */
	public static void main(String[] args) {
		GuiMain.main(args);
	}
}
