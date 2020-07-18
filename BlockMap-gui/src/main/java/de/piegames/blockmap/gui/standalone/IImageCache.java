package de.piegames.blockmap.gui.standalone;

import javafx.scene.image.Image;

import java.net.MalformedURLException;

public interface IImageCache {

    public Image get(String url);

    public Image fresh(String url);

    public void flush();

    public void purge();
}
