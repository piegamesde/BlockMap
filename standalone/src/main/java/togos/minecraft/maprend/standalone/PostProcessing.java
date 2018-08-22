package togos.minecraft.maprend.standalone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import togos.minecraft.maprend.RegionMap;
import togos.minecraft.maprend.io.ContentStore;
import togos.minecraft.maprend.renderer.RenderSettings;

/** This class contains a collection of methods that transform the rendered map into another, more accessible representation */
public class PostProcessing {

	private PostProcessing() {
	}

	/**
	 * Create a "tiles.html" file containing a table with all region images (tile.<x>.<z>.png) that exist in outDir within the given bounds (inclusive)
	 */
	public static void createTileHtml(int minX, int minZ, int maxX, int maxZ, File outputDir, RenderSettings settings) {
		// if (settings.debug)
			System.err.println("Writing HTML tiles...");
		for (int scale : settings.mapScales) {
			int regionSize = 512 / scale;

			try {
				File cssFile = new File(outputDir, "tiles.css");
				if (!cssFile.exists()) {
					try (InputStream cssInputStream = PostProcessing.class.getResourceAsStream("tiles.css");) {
						byte[] buffer = new byte[1024 * 4];
						try (FileOutputStream cssOutputStream = new FileOutputStream(cssFile);) {
							int r;
							while ((r = cssInputStream.read(buffer)) > 0) {
								cssOutputStream.write(buffer, 0, r);
							}
						}
					}
				}

				Writer w = new OutputStreamWriter(new FileOutputStream(new File(
						outputDir,
						scale == 1 ? "tiles.html" : "tiles.1-" + scale + ".html")));
				try {
					w.write("<html><head>\n");
					w.write("<title>" + settings.mapTitle + " - 1:" + scale + "</title>\n");
					w.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"tiles.css\"/>\n");
					w.write("</head><body>\n");
					w.write("<div style=\"height: " + (maxZ - minZ + 1) * regionSize + "px\">");

					for (int z = minZ; z <= maxZ; ++z) {
						for (int x = minX; x <= maxX; ++x) {
							String fullSizeImageFilename = "tile." + x + "." + z + ".png";
							File imageFile = new File(outputDir + "/" + fullSizeImageFilename);
							String scaledImageFilename = scale == 1 ? fullSizeImageFilename : "tile." + x + "." + z + ".1-" + scale + ".png";
							if (imageFile.exists()) {
								int top = (z - minZ) * regionSize, left = (x - minX) * regionSize;
								String title = "Region " + x + ", " + z;
								String name = "r." + x + "." + z;
								String style = "width: " + regionSize + "px; height: " + regionSize + "px; " +
										"position: absolute; top: " + top + "px; left: " + left + "px; " +
										"background-image: url(" + scaledImageFilename + ")";
								w.write("<a\n" +
										"\tclass=\"tile\"\n" +
										"\tstyle=\"" + style + "\"\n" +
										"\ttitle=\"" + title + "\"\n" +
										"\tname=\"" + name + "\"\n" +
										"\thref=\"" + fullSizeImageFilename + "\"\n" +
										">&nbsp;</a>");
							}
						}
					}

					w.write("</div>\n");
					if (settings.mapScales.length > 1) {
						w.write("<div class=\"scales-nav\">");
						w.write("<p>Scales:</p>");
						w.write("<ul>");
						for (int otherScale : settings.mapScales) {
							if (otherScale == scale) {
								w.write("<li>1:" + scale + "</li>");
							} else {
								String otherFilename = otherScale == 1 ? "tiles.html" : "tiles.1-" + otherScale + ".html";
								w.write("<li><a href=\"" + otherFilename + "\">1:" + otherScale + "</a></li>");
							}
						}
						w.write("</ul>");
						w.write("</div>");
					}
					w.write("<p class=\"notes\">");
					w.write("Page rendered at " + new Date().toString());
					w.write("</p>\n");
					w.write("</body></html>");
				} finally {
					w.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void createImageTree(RegionMap rm, RenderSettings settings) {
		// if (settings.debug)
			System.err.println("Composing image tree...");
		ImageTreeComposer itc = new ImageTreeComposer(new ContentStore());
		System.out.println(itc.compose(rm));
	}

	public static void createBigImage(RegionMap rm, File outputDir, RenderSettings settings) {
		// if (settings.debug)
			System.err.println("Creating big image...");
		BigImageMerger bic = new BigImageMerger();
		bic.createBigImage(rm, outputDir, true);
	}
}
