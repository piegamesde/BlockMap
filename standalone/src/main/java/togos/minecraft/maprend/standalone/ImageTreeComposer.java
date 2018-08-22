package togos.minecraft.maprend.standalone;

import java.io.IOException;
import togos.minecraft.maprend.RegionMap;
import togos.minecraft.maprend.RegionMap.Region;
import togos.minecraft.maprend.io.ContentStore;

public class ImageTreeComposer
{
	final ContentStore store;
	
	public ImageTreeComposer( ContentStore store ) {
		this.store = store;
	}
	
	protected static boolean fitsInRadius( RegionMap rmap, int radius ) {
		if( rmap.minX < -radius ) return false;
		if( rmap.maxX >  radius ) return false;
		if( rmap.minZ < -radius ) return false;
		if( rmap.maxZ >  radius ) return false;
		return true;
	}
	
	protected static String quote( String s ) {
		return "'"+s.replace("\\","\\\\").replace("'","\\'")+"'";
	}
	
	protected static String indent( String s ) {
		return s.replace("\n","\n\t");
	}
	
	protected static String paren( String content ) {
		return "(\n\t"+indent(content)+"\n)";
	}
	
	protected String compose( String title, int rx, int rz, int size, String r0, String r1, String r2, String r3 ) {
		int subSize = size/2;
		String compoundImageSource =
			"COMPOUND-IMAGE 1024,1024\n" +
			"TITLE "+quote(title) +
			(r0 == null ? "" : "\nCOMPONENT 0,0,512,512 "    +r0+" name="+quote( rx         +","+ rz         +","+subSize+"x"+subSize)) +
			(r1 == null ? "" : "\nCOMPONENT 512,0,512,512 "  +r1+" name="+quote((rx+subSize)+","+ rz         +","+subSize+"x"+subSize)) +
			(r2 == null ? "" : "\nCOMPONENT 0,512,512,512 "  +r2+" name="+quote( rx         +","+(rz+subSize)+","+subSize+"x"+subSize)) +
			(r3 == null ? "" : "\nCOMPONENT 512,512,512,512 "+r3+" name="+quote((rx+subSize)+","+(rz+subSize)+","+subSize+"x"+subSize));
		// return paren(compoundImageSource);
		try {
			return store.store( compoundImageSource );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected String compose( final RegionMap rmap, final int rx, final int rz, final int size ) {
		if( size == 0 ) {
			throw new RuntimeException( "Bad region range size: "+size+" (should be a power of 2, >= 1)");
		}
		if( size == 1 ) {
			Region r = rmap.regionAt(rx,rz);
			try {
	            return r == null ? null : store.store(r.imageFile);
            } catch( IOException e ) {
            	throw new RuntimeException(e);
            }
		} else {
			final int subSize = size/2;
			String r0 = compose( rmap, rx        , rz        , subSize );
			String r1 = compose( rmap, rx+subSize, rz        , subSize );
			String r2 = compose( rmap, rx        , rz+subSize, subSize );
			String r3 = compose( rmap, rx+subSize, rz+subSize, subSize );
			return r0 == null && r1 == null && r2 == null && r3 == null ? null :
				compose( size+"x"+size+" regions at "+rx+","+rz, rx, rz, size, r0, r1, r2, r3 );
		}
	}
	
	public String compose( RegionMap rmap ) {
		int radius = 1; // In region widths (512 meters)
		while( !fitsInRadius(rmap,radius) ) radius *= 2;
		return compose( rmap, -radius, -radius, radius*2 );
	}
}
