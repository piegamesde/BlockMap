package togos.minecraft.maprend.standalone;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import togos.minecraft.maprend.World;
import togos.minecraft.maprend.World.Region;
import togos.minecraft.maprend.renderer.RegionRenderer;
import togos.minecraft.maprend.renderer.RenderSettings;

/** This class contains a collection of methods that transform the rendered map into another, more accessible representation */
public class PostProcessing {

	private static Log log = LogFactory.getLog(RegionRenderer.class);

	private PostProcessing() {
	}

	public static void createTileHtml(World world, Path outputDir, RenderSettings settings) throws IOException {
		log.info("Writing HTML tiles...");
		for (int scale : settings.mapScales) {
			// TODO catch exception here
			int regionSize = 512 / scale;

			Path cssFile = outputDir.resolve("tiles.css");
			// Copy css file from local resources to destination
			if (!Files.exists(cssFile)) {
				try (InputStream cssInputStream = PostProcessing.class.getResourceAsStream("tiles.css");) {
					byte[] buffer = new byte[1024 * 4];
					try (OutputStream cssOutputStream = Files.newOutputStream(cssFile);) {
						int r;
						while ((r = cssInputStream.read(buffer)) > 0) {
							cssOutputStream.write(buffer, 0, r);
						}
					}
				}
			}

			try (Writer w = Files.newBufferedWriter(
					outputDir.resolve(scale == 1 ? "tiles.html" : "tiles.1-" + scale + ".html"));) {
				w.write("<html><head>\n");
				w.write("<title>" + settings.mapTitle + " - 1:" + scale + "</title>\n");
				w.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"tiles.css\"/>\n");
				w.write("</head><body>\n");
				w.write("<div style=\"height: " + (world.maxZ - world.minZ + 1) * regionSize + "px\">");

				for (int z = world.minZ; z <= world.maxZ; ++z) {
					for (int x = world.minX; x <= world.maxX; ++x) {
						String fullSizeImageFilename = "tile." + x + "." + z + ".png";
						File imageFile = new File(outputDir + "/" + fullSizeImageFilename);
						String scaledImageFilename = scale == 1 ? fullSizeImageFilename : "tile." + x + "." + z + ".1-" + scale + ".png";
						if (imageFile.exists()) {
							int top = (z - world.minZ) * regionSize, left = (x - world.minX) * regionSize;
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
			}
		}
	}

	public static void createBigImage(World rm, Path outputDir, RenderSettings settings) {
		log.info("Creating big image...");

		int width = (rm.maxX - rm.minX) * 512;
		int height = (rm.maxZ - rm.minZ) * 512;
		BufferedImage bigImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		log.debug("Dimension: " + width + ", " + height);

		for (Region r : rm.regions.values()) {
			BufferedImage region = null;
			try {
				region = ImageIO.read(Files.newInputStream(r.imageFile));
			} catch (IOException e) {
				System.err.println("Could not load image " + r.imageFile.getFileName());
				continue;
			}
			bigImage.createGraphics().drawImage(region, (r.rx - rm.minX) * 512, (r.rz - rm.minZ) * 512, null);
			log.debug("Region " + r.rx + ", " + r.rz + " drawn to " + (r.rx - rm.minX) * 512 + ", " + (r.rz - rm.minZ) * 512);
		}
		try {
			ImageIO.write(bigImage, "png", new File(outputDir + "/big.png"));
		} catch (IOException e) {
			log.error("Could not write big image to " + outputDir + "/big.png", e);
		}
	}
}
