package togos.minecraft.maprend;

import static togos.minecraft.maprend.IDUtil.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class BlockMap
{
	/**
	 * Holds the default color for a block and, optionally,
	 * a color for specific 'block data' values.
	 */
	public static class Block {
		protected static final int[] EMPTY_INT_ARRAY = new int[0];
		protected static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
		public static final int SUB_COLOR_COUNT = 0x10;
		
		public int baseColor;
		public int baseInfluence;
		public boolean isDefault;
		
		public int[] subColors = EMPTY_INT_ARRAY;
		public int[] subColorInfluences = EMPTY_INT_ARRAY;
		public boolean[] hasSubColors = EMPTY_BOOLEAN_ARRAY;
		
		private Block(int baseColor, int baseInfluence, boolean isDefault) {
			this.baseColor = baseColor;
			this.baseInfluence = baseInfluence;
			this.isDefault = isDefault;
		}
		
		private void setSubColor( int blockData, int color, int influence ) {
			if( blockData < 0 || blockData >= SUB_COLOR_COUNT ) {
				throw new RuntimeException("Block data value out of bounds: "+blockData);
			}
			if( subColors.length == 0 ) {
				hasSubColors = new boolean[SUB_COLOR_COUNT];
				subColors = new int[SUB_COLOR_COUNT];
				subColorInfluences = new int[SUB_COLOR_COUNT];
			}
			hasSubColors[blockData] = true;
			subColors[blockData] = color;
			subColorInfluences[blockData] = influence;
		}
		
		private void setBaseColor( int color, int influence, boolean isDefault ) {
			this.baseColor = color;
			this.baseInfluence = influence;
			this.isDefault = isDefault;
		}
	}
	
	public static final int INDEX_MASK = 0xFFFF;
	public static final int SIZE = INDEX_MASK+1;
	
	public static final int INF_NONE = 0;
	public static final int INF_GRASS = 1;
	public static final int INF_FOLIAGE = 2;
	public static final int INF_WATER = 3;
	
	public final Block[] blocks;
	public BlockMap( Block[] blocks ) {
		assert blocks != null;
		assert blocks.length == SIZE;
		
		this.blocks = blocks;
	}
	
	public static BlockMap load( BufferedReader s, String filename ) throws IOException {
		Block[] blocks = new Block[SIZE];
		for( int i=0; i<SIZE; ++i ) {
			blocks[i] = new Block(0, 0, true);
		}
		int lineNum = 0;
		String line;
		while( (line = s.readLine()) != null ) {
			++lineNum;
			if( line.trim().isEmpty() ) continue;
			if( line.trim().startsWith("#") ) continue;
			
			String[] v = line.split("\t", 4);
			if( v.length < 2 ) {
				System.err.println("Invalid color map line at "+filename+":"+lineNum+": "+line);
				continue;
			}
			int color = parseInt(v[1]);
			if( "default".equals(v[0]) ) {
				for( int i=0; i<blocks.length; ++i ) blocks[i].setBaseColor( color, 0, true );
			} else {
				String[] v2 = v[0].split( ":", 2 );
				int blockId = parseInt( v2[0] );
				int blockData = v2.length == 2 ? parseInt( v2[1] ) : -1;
				int influence = INF_NONE;
				if (v.length > 2){
					if (v[2].equals( "biome_grass" )) {
						influence = INF_GRASS;
					} else if (v[2].equals( "biome_foliage" )) {
						influence = INF_FOLIAGE;
					} else if (v[2].equals( "biome_water" )) {
						influence = INF_WATER;
					}
				}
				
				if( blockData < 0 ) {
					blocks[blockId&INDEX_MASK].setBaseColor( color, influence, false );
				} else {
					blocks[blockId&INDEX_MASK].setSubColor( blockData, color, influence );
					blocks[blockId&INDEX_MASK].isDefault = false;
				}
			}
		}
		return new BlockMap(blocks);
	}
	
	public static BlockMap load( File f ) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			return load(br, f.getPath());
		} finally {
			br.close();
		}
	}
	
	public static BlockMap loadDefault() {
		try {
			InputStream inputStream = BlockMap.class.getResourceAsStream("block-colors.txt");
			if( inputStream == null ) {
				throw new IOException("Failed to open internal block-colors.txt");
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			try {
				return load(br, "(default block colors)");
			} finally {
				br.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException("Error loading built-in color map", e);
		}
	}
}
