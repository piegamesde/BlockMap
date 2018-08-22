package togos.minecraft.maprend.texture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import togos.minecraft.maprend.Color;

/**
 * Interprets forth-like scripts to extract color data from texture maps.
 */
public class TextureScriptInterpreter
{
	protected static int getInt( Object o ) {
		if( o instanceof Number ) {
			return ((Number)o).intValue();
		}
		throw new RuntimeException("Can't convert "+o+" to a number"); 
	}
	
	public static int averageColor( BufferedImage image ) {
		int a=0, r=0, g=0, b=0, nPix=image.getWidth()*image.getHeight();
		for( int y=image.getHeight()-1; y>=0; --y ) {
			for( int x=image.getWidth()-1; x>=0; --x ) {
				int color = image.getRGB(x,y);
				a += Color.component(color, 24);
				r += Color.component(color, 16);
				g += Color.component(color,  8);
				b += Color.component(color,  0);
			}
		}
		return Color.color( a/nPix, r/nPix, g/nPix, b/nPix );
	}
	
	public static void subImage( List<Object> stack ) {
		int height = getInt(stack.remove(stack.size()-1));
		int width = getInt(stack.remove(stack.size()-1));
		int y = getInt(stack.remove(stack.size()-1));
		int x = getInt(stack.remove(stack.size()-1));
		BufferedImage image = (BufferedImage)stack.remove(stack.size()-1);
		stack.add( image.getSubimage(x, y, width, height) );
	}
	
	public static void multiplyColor( List<Object> stack ) {
		// Could be extended to support images, too.
		stack.add(new Integer(Color.multiply( getInt(stack.remove(stack.size()-1)), getInt(stack.remove(stack.size()-1)) )));
	}
	
	public static void averageColor( List<Object> stack ) {
		stack.add(new Integer(averageColor((BufferedImage)stack.remove(stack.size()-1))));
	}
	
	protected static boolean isWhitespace( int c ) {
		switch( c ) {
		case 0: case ' ': case '\t': case '\r': case '\n':
			return true;
		default:
			return false;
		}
	}
	
	protected static String readToken( Reader r ) throws IOException {
		int c = r.read();
		while( c >= 0 && isWhitespace(c) || c == '#' ) {
			if( c == '#' ) {
				while( (c = r.read()) != -1 && c != '\n' );
			} else {
				c = r.read();
			}
		}
		if( c == -1 ) return null;
		String t = "";
		while( c >= 0 && !isWhitespace(c) ) {
			t += (char)c;
			c = r.read();
		}
		return t;
	}
	
	static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d+");
	public static Number parseNumber( String t ) {
		if( t.startsWith("0x") ) {
			return new Integer( (int)Long.parseLong(t.substring(2),16) );
		} else if( DECIMAL_PATTERN.matcher(t).matches() ) {
			return new Integer( (int)Long.parseLong( t ) );
		} else {
			return null;
		}
	}
	
	public static void printBlockColor( List<Object> stack ) {
		Object color   = stack.remove(stack.size()-1);
		Object blockId = stack.remove(stack.size()-1);
		if( blockId instanceof Number ) {
			System.out.println( String.format("0x%04X\t0x%08X", new Object[]{blockId,color} ) );
		} else {
			System.out.println( String.format("%s\t0x%08X", new Object[]{blockId,color} ) );
		}
	}
	
	public static void interpret( Reader r ) throws IOException {
		ArrayList<Object> stack = new ArrayList<Object>();
		
		String t;
		Object o;
		while( (t = readToken(r)) != null ) {
			if( t.startsWith("'") ) {
				stack.add(t.substring(1));
			} else if( "dup".equals(t) ) {
				stack.add(stack.get(stack.size()-1));
			} else if( "swap".equals(t) ) {
				Object top = stack.get(stack.size()-1);
				stack.set(stack.size()-1, stack.get(stack.size()-2));
				stack.set(stack.size()-2, top);
			} else if( "load-image".equals(t) ) {
				String filename = (String)stack.remove(stack.size()-1);
				stack.add( ImageIO.read(new File(filename)) );
			} else if( "sub-image".equals(t) ) {
				subImage(stack);
			} else if( "multiply-color".equals(t) ) {
				multiplyColor(stack);
			} else if( "average-color".equals(t) ) {
				averageColor(stack);
			} else if( "print-block-color".equals(t) ) {
				printBlockColor(stack);
			} else if( (o = parseNumber(t)) != null ) {
				stack.add( o );
			} else {
				System.err.println("Unrecognized token: '"+t+"'");
			}
		}
	}
	
	public static void main( String[] args ) throws IOException {
		interpret( new InputStreamReader(System.in) );
	}
}
