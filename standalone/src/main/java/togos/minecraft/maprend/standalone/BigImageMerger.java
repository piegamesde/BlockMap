package togos.minecraft.maprend.standalone;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import togos.minecraft.maprend.RegionMap;
import togos.minecraft.maprend.RegionMap.Region;

public class BigImageMerger
{
	public void createBigImage( RegionMap rm, File outputDir, boolean debug ) {
		int width = (rm.maxX-rm.minX)*512;
		int height = (rm.maxZ-rm.minZ)*512;
		BufferedImage bigImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
		if( debug ) System.err.println( "Dimension: "+width+", "+height );
		
		for( Region r : rm.regions ) {
			BufferedImage region = null;
			try {
				region = ImageIO.read( r.imageFile );
			} catch ( IOException e ) {
				System.err.println( "Could not load image "+r.imageFile.getName() );
				continue;
			}
			bigImage.createGraphics().drawImage( region, (r.rx-rm.minX)*512, (r.rz-rm.minZ)*512, null );
			if( debug ) System.err.println( "Region "+r.rx+", "+r.rz+" drawn to "+(r.rx-rm.minX)*512+", "+(r.rz-rm.minZ)*512 );
		}
		try {
			ImageIO.write( bigImage, "png", new File( outputDir+"/big.png" ) );
		} catch ( IOException e ) {
			System.err.println( "Could not write big image to "+outputDir+"/big.png" );
		}
	}
}
