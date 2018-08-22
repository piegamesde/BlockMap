package togos.minecraft.maprend.renderer;

@Deprecated
public class RegionRendererOld
{

	// class RenderThread extends Thread {
	// public List<Region> regions;
	// public int startIndex;
	// public int endIndex;
	// public File outputDir;
	// public boolean force;
	//
	// RenderThread( List<Region> regions, File outputDir, boolean force) throws IOException {
	// this.regions = regions;
	// this.outputDir = outputDir;
	// this.force = force;
	// }
	//
	// @Override
	// public void run() {
	// try {
	// for( Region reg : regions ) renderRegion(reg, outputDir, force);
	// } catch (IOException e) {
	// System.err.println("Error in threaded renderer!");
	// e.printStackTrace(System.err);
	// }
	// }
	// }
	//
	// public final Set<Integer> defaultedBlockIds = new HashSet<>();
	// public final Set<Integer> defaultedBlockIdDataValues = new HashSet<>();
	// public final Set<Integer> defaultedBiomeIds = new HashSet<>();
	// public final BlockMap blockMap;
	// public final BiomeColorMap biomeMap;
	// public final int air16Color; // Color of 16 air blocks stacked
	//
	// /**
	// * Alpha below which blocks are considered transparent for purposes of shading
	// * (i.e. blocks with alpha < this will not be shaded, but blocks below them will be)
	// */
	// private int shadeOpacityCutoff = 0x20;
	//
	// public final RenderSettings settings;
	//
	// public RegionRendererOld(RenderSettings settings) throws IOException {
	// this.settings = settings;
	//
	// blockMap = settings.colorMapFile == null ? BlockMap.loadDefault() : BlockMap.load(settings.colorMapFile);
	// biomeMap = settings.biomeMapFile == null ? BiomeColorMap.loadDefault() : BiomeColorMap.load(settings.biomeMapFile);
	//
	// this.air16Color = Color.overlay(0, getColor(0, 0, 0), 16);
	// }
	//
	// //// Color look-up ////
	//
	// protected void defaultedBlockColor( int blockId ) {
	// defaultedBlockIds.add(blockId);
	// }
	// protected void defaultedSubBlockColor( int blockId, int blockDatum ) {
	// defaultedBlockIdDataValues.add(blockId | blockDatum << 16);
	// }
	// protected void defaultedBiomeColor( int biomeId ) {
	// defaultedBiomeIds.add(biomeId);
	// }
	//
	// protected int getColor( int blockId, int blockDatum, int biomeId ) {
	// assert blockId >= 0 && blockId < blockMap.blocks.length;
	// assert blockDatum >= 0;
	//
	// int blockColor;
	// int biomeInfluence;
	//
	// Block bc = blockMap.blocks[blockId];
	// if( bc.hasSubColors.length > blockDatum && bc.hasSubColors[blockDatum] ) {
	// blockColor = bc.subColors[blockDatum];
	// biomeInfluence = bc.subColorInfluences[blockDatum];
	// } else {
	// if( blockDatum != 0 ) {
	// defaultedSubBlockColor(blockId, blockDatum);
	// }
	// blockColor = bc.baseColor;
	// biomeInfluence = bc.baseInfluence;
	// }
	// if( bc.isDefault ) {
	// defaultedBlockColor(blockId);
	// }
	//
	// Biome biome = biomeMap.getBiome(biomeId);
	// int biomeColor = biome.getMultiplier( biomeInfluence );
	// if( biome.isDefault ) defaultedBiomeColor(biomeId);
	//
	// return Color.multiplySolid( blockColor, biomeColor );
	// }
	//
	//
	// //// Rendering ////
	//
	// /**
	// * Load color and height data from a region.
	// * @param rf
	// * @param colors color data will be written here
	// * @param heights height data (height of top of topmost non-transparent block) will be written here
	// */
	// protected void preRender(RegionFile rf, IntBuffer colors, ShortBuffer heights) {
	// int maxSectionCount = 16;
	// short[][] sectionBlockIds = new short[maxSectionCount][16*16*16];
	// byte[][] sectionBlockData = new byte[maxSectionCount][16*16*16];
	// boolean[] usedSections = new boolean[maxSectionCount];
	// byte[] biomeIds = new byte[16*16];
	//
	// for( int cz=0; cz<32; ++cz ) {
	// for( int cx=0; cx<32; ++cx ) {
	// DataInputStream cis = rf.getChunkDataInputStream(cx,cz);
	// if( cis == null ) continue;
	// NBTInputStream nis = null;
	// try {
	// nis = new NBTInputStream(cis);
	// CompoundTag rootTag = (CompoundTag)nis.readTag();
	// CompoundTag levelTag = (CompoundTag)rootTag.getValue().get("Level");
	// ChunkLoader.loadChunkData(levelTag, maxSectionCount, sectionBlockIds, sectionBlockData, usedSections, biomeIds);
	//
	// for( int s=0; s<maxSectionCount; ++s ) {
	// if( usedSections[s] ) {
	// }
	// }
	//
	// for( int z=0; z<16; ++z ) {
	// for( int x=0; x<16; ++x ) {
	// int pixelColor = 0;
	// short pixelHeight = 0;
	// int biomeId = biomeIds[z*16+x]&0xFF;
	//
	// for( int s=0; s<maxSectionCount; ++s ) {
	// int absY=s*16;
	//
	// if (absY >= settings.maxHeight)
	// continue;
	// if (absY + 16 <= settings.minHeight)
	// continue;
	//
	// if( usedSections[s] ) {
	// short[] blockIds = sectionBlockIds[s];
	// byte[] blockData = sectionBlockData[s];
	//
	// for( int idx=z*16+x, y=0; y<16; ++y, idx+=256, ++absY ) {
	// if (absY < settings.minHeight || absY >= settings.maxHeight)
	// continue;
	//
	// final short blockId = blockIds[idx];
	// final byte blockDatum = blockData[idx];
	// int blockColor = getColor( blockId&0xFFFF, blockDatum, biomeId );
	// pixelColor = Color.overlay( pixelColor, blockColor );
	// if( Color.alpha(blockColor) >= shadeOpacityCutoff ) {
	// pixelHeight = (short)absY;
	// }
	// }
	// } else {
	// if (settings.minHeight <= absY && settings.maxHeight >= absY + 16) {
	// // Optimize the 16-blocks-of-air case:
	// pixelColor = Color.overlay( pixelColor, air16Color );
	// } else {
	// // TODO: mix
	// }
	// }
	// }
	//
	// final int dIdx = 512*(cz*16+z)+16*cx+x;
	// colors.put(dIdx, pixelColor);
	// heights.put(dIdx, pixelHeight);
	// }
	// }
	// } catch( IOException e ) {
	// System.err.println("Error reading chunk from "+rf.getFile()+" at "+cx+","+cz);
	// e.printStackTrace(System.err);
	// } finally {
	// if( nis != null ) {
	// try {
	// nis.close();
	// } catch( IOException e ) {
	// System.err.println("Failed to close NBTInputStream!");
	// e.printStackTrace(System.err);
	// }
	// }
	// }
	// }
	// }
	// }
	//
	// public BufferedImage render( RegionFile rf ) {
	// int width=512, depth=512;
	//
	// IntBuffer surfaceColor = IntBuffer.allocate(width * depth);
	// ShortBuffer surfaceHeight = ShortBuffer.allocate(width * depth);
	//
	// preRender( rf, surfaceColor, surfaceHeight );
	// surfaceColor = new DefaultRegionShader().shadeRegion(settings, surfaceColor, surfaceHeight);
	//
	// BufferedImage bi = new BufferedImage( width, depth, BufferedImage.TYPE_INT_ARGB );
	// {// Convert IntBuffer to BufferedImage
	// final int[] a = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
	// final int[] data = surfaceColor.array();
	// System.arraycopy(data, 0, a, 0, data.length);
	// }
	//
	// return bi;
	// }
	//
	// protected static String pad( String v, int targetLength ) {
	// while( v.length() < targetLength ) v = " "+v;
	// return v;
	// }
	//
	// protected static String pad( int v, int targetLength ) {
	// return pad( ""+v, targetLength );
	// }
	//
	// public void renderAll( RegionMap rm, File outputDir, boolean force, int threadCount ) throws IOException, InterruptedException {
	// final long startTime = System.currentTimeMillis();
	//
	// if( !outputDir.exists() ) outputDir.mkdirs();
	//
	// if( rm.regions.size() == 0 ) {
	// System.err.println("Warning: no regions found!");
	// }
	//
	// final int regionCount = rm.regions.size();
	// final int regionsPerThread = (int)Math.ceil((float)regionCount / threadCount);
	//
	// List<RenderThread> renderThreads = new ArrayList<>();
	//
	// for( int i = 0; i < regionCount; i += regionsPerThread ) {
	// // You know, we really ought to figure out which regions need to be updated
	// // /before/ we start all these threads...
	// List<Region> threadRegions = rm.regions.subList(i, Math.min(i+regionCount, regionCount));
	// RenderThread renderThread = new RenderThread(threadRegions, outputDir, force);
	// renderThreads.add(renderThread);
	// }
	//
	// if (settings.debug)
	// System.err.println("Using " + renderThreads.size() + " render threads");
	//
	// for( RenderThread renderThread : renderThreads ) renderThread.start();
	//
	// for( RenderThread renderThread : renderThreads ) renderThread.join();
	// }
	//
	// public void renderRegion( Region r, File outputDir, boolean force ) throws IOException {
	// if( r == null ) return;
	//
	// if (settings.debug)
	// System.err.print("Region " + pad(r.rx, 4) + ", " + pad(r.rz, 4) + "...");
	//
	// String imageFilename = "tile."+r.rx+"."+r.rz+".png";
	// File fullSizeImageFile = r.imageFile = new File( outputDir, imageFilename );
	//
	// boolean fullSizeNeedsReRender = false;
	// if( force || !fullSizeImageFile.exists() || fullSizeImageFile.lastModified() < r.regionFile.lastModified() ) {
	// fullSizeNeedsReRender = true;
	// } else {
	// if (settings.debug)
	// System.err.println("image already up-to-date");
	// }
	//
	// boolean anyScalesNeedReRender = false;
	// for (int scale : settings.mapScales) {
	// if( scale == 1 ) continue;
	// File f = new File( outputDir, "tile."+r.rx+"."+r.rz+".1-"+scale+".png" );
	// if( force || !f.exists() || f.lastModified() < r.regionFile.lastModified() ) {
	// anyScalesNeedReRender = true;
	// }
	// }
	//
	// BufferedImage fullSize;
	// if( fullSizeNeedsReRender ) {
	// fullSizeImageFile.delete();
	// if (settings.debug)
	// System.err.println("generating " + imageFilename + "...");
	//
	// RegionFile rf = new RegionFile( r.regionFile );
	// try {
	// fullSize = render( rf );
	// } finally {
	// rf.close();
	// }
	//
	// try {
	// ImageIO.write(fullSize, "png", fullSizeImageFile);
	// } catch( IOException e ) {
	// System.err.println("Error writing PNG to "+fullSizeImageFile);
	// e.printStackTrace();
	// }
	// } else if( anyScalesNeedReRender ) {
	// fullSize = ImageIO.read(fullSizeImageFile);
	// } else {
	// return;
	// }
	//
	// for (int scale : settings.mapScales) {
	// if( scale == 1 ) continue; // Already wrote!
	// File f = new File( outputDir, "tile."+r.rx+"."+r.rz+".1-"+scale+".png" );
	// if (settings.debug)
	// System.err.println("generating " + f + "...");
	// int size = 512 / scale;
	// BufferedImage rescaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	// Graphics2D g = rescaled.createGraphics();
	// g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	// g.drawImage(fullSize, 0, 0, size, size, 0, 0, 512, 512, null);
	// g.dispose();
	// ImageIO.write(rescaled, "png", f);
	// }
	// }
}
