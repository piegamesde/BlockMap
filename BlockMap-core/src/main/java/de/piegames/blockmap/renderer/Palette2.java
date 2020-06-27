package de.piegames.blockmap.renderer;

import java.util.Objects;

import de.piegames.nbt.regionfile.Chunk;

/**
 * Utility class to extract palette indices from the packed array containing all the block states. The data may be found in each chunk at
 * {@code /Level/Sections[i]/BlockStates}.
 * <p/>
 * This does exactly the same thing as {@link Chunk#extractFromLong(long[], int, int)}, but faster. Only use this if said method shows up in
 * your profiler with >1% CPU usage. It is intended for one-time sequential parsing, like an iterator. If you need only a few elements, or
 * random access, don't use it.
 * 
 * @see Chunk#extractFromLong(long[], int, int)
 * @author piegames
 */
public final class Palette2 {

	final long[]	data;
	final int bitsPerIndex, shortsPerLong;

	/**
	 * @param data
	 *            a long array densely packed with n-bit unsigned values, with @{code n = data.length/64 <= 64}. Therefore, {@code data.length}
	 *            must be divisible through 64. The data always contains 4096 packed values.
	 */
	public Palette2(long[] data, int paletteSize) {
		this.data = Objects.requireNonNull(data);
		bitsPerIndex = Math.max(4, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));

		shortsPerLong = Math.floorDiv(64, bitsPerIndex);
		if (shortsPerLong * data.length < 4096)
			throw new IllegalArgumentException("TODO, " + paletteSize + ", " + bitsPerIndex + ", " + shortsPerLong + ", " + data.length);
	}

	public long[] getData() {
		if (bitsPerIndex == 4) {
			return getData4();
		} else if (bitsPerIndex == 5) {
			return getData5();
		} else if (bitsPerIndex == 6) {
			return getData6();
		} else {
			int mask = (1 << bitsPerIndex) - 1;

			long[] result = new long[4096];

			int index = 0;
			for (long l : data) {
				for (int s = 0; s < shortsPerLong && index < 4096; s++) {
					result[index++] = l & mask;
					l >>= bitsPerIndex;
				}
			}
			return result;
		}
	}

	public long[] getData4() {
		long[] result = new long[4096];

		int index = 0;
		for (long l : data) {
			// Bit 64 ............................................................... Bit 0
			// ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ ┌──┐┌──┐ (from right to left)
			// xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
			result[index++] = (l >> 0x00) & 0xF;
			result[index++] = (l >> 0x04) & 0xF;
			result[index++] = (l >> 0x08) & 0xF;
			result[index++] = (l >> 0x0C) & 0xF;
			result[index++] = (l >> 0x10) & 0xF;
			result[index++] = (l >> 0x14) & 0xF;
			result[index++] = (l >> 0x18) & 0xF;
			result[index++] = (l >> 0x1C) & 0xF;
			result[index++] = (l >> 0x20) & 0xF;
			result[index++] = (l >> 0x24) & 0xF;
			result[index++] = (l >> 0x28) & 0xF;
			result[index++] = (l >> 0x2C) & 0xF;
			result[index++] = (l >> 0x30) & 0xF;
			result[index++] = (l >> 0x34) & 0xF;
			result[index++] = (l >> 0x38) & 0xF;
			result[index++] = (l >> 0x3C) & 0xF;
		}
		return result;
	}

	public long[] getData5() {
		long[] result = new long[4096];

		int index = 0;
		for (int i = 0; i < data.length - 1; i++) {
			long l = data[i];
			// Bit 64 ............................................................... Bit 0
			// ....┌─── ┐┌───┐┌─ ──┐┌───┐ ┌───┐┌── ─┐┌───┐┌ ───┐┌─── ┐┌───┐┌─ ──┐┌───┐ (from right to left)
			// xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
			result[index++] = (l >> 0) & 0x1F;
			result[index++] = (l >> 5) & 0x1F;
			result[index++] = (l >> 10) & 0x1F;
			result[index++] = (l >> 15) & 0x1F;
			result[index++] = (l >> 20) & 0x1F;
			result[index++] = (l >> 25) & 0x1F;
			result[index++] = (l >> 30) & 0x1F;
			result[index++] = (l >> 35) & 0x1F;
			result[index++] = (l >> 40) & 0x1F;
			result[index++] = (l >> 45) & 0x1F;
			result[index++] = (l >> 50) & 0x1F;
			result[index++] = (l >> 55) & 0x1F;
		}
		{ /* Last iteration – almost Duff's device! */
			long lastData = data[data.length - 1];
			switch (result.length - index) {
			case 10:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 9:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 8:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 7:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 6:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 5:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 4:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 3:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 2:
				result[index++] = lastData & 0x1F;
				lastData >>= 5;
			case 1:
				result[index++] = lastData & 0x1F;
			case 0:
				break;
			default:
				throw new InternalError();
			}
		}
		return result;
	}

	public long[] getData6() {
		long[] result = new long[4096];

		int index = 0;
		for (int i = 0; i < data.length - 1; i++) {
			long l = data[i];
			// Bit 64 ............................................................... Bit 0
			// ....┌─── ─┐┌────┐ ┌────┐┌─ ───┐┌─── ─┐┌────┐ ┌────┐┌─ ───┐┌─── ─┐┌────┐ (from right to left)
			// xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
			result[index++] = (l >> 0) & 0x3F;
			result[index++] = (l >> 6) & 0x3F;
			result[index++] = (l >> 12) & 0x3F;
			result[index++] = (l >> 18) & 0x3F;
			result[index++] = (l >> 24) & 0x3F;
			result[index++] = (l >> 30) & 0x3F;
			result[index++] = (l >> 36) & 0x3F;
			result[index++] = (l >> 42) & 0x3F;
			result[index++] = (l >> 48) & 0x3F;
			result[index++] = (l >> 54) & 0x3F;
		}
		{ /* Last iteration – almost Duff's device! */
			long lastData = data[data.length - 1];
			switch (result.length - index) {
			case 10:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 9:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 8:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 7:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 6:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 5:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 4:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 3:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 2:
				result[index++] = lastData & 0x3F;
				lastData >>= 6;
			case 1:
				result[index++] = lastData & 0x3F;
			case 0:
				break;
			default:
				throw new InternalError();
			}
		}
		return result;
	}
}
