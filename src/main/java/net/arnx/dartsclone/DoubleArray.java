package net.arnx.dartsclone;

import java.io.IOException;
import java.io.InputStream;

import net.arnx.dartsclone.util.IntList;

public class DoubleArray {
	public static DoubleArray load(InputStream in) throws IOException {
		IntList list = new IntList();

		int value = 0;
		int pos = 0;
		
		int buf;
		while ((buf = in.read()) != -1) {
			value |= ((buf & 0xFF) << pos * 8);
			
			pos++;
			if (pos > 3) {
				list.add(value);
				value = 0;
				pos = 0;
			}
		}
		
		return new DoubleArray(list.toArray());
	}
	
	/**
	 *  hasLeaf() returns whether a leaf unit is immediately derived from the
	 *  unit (true) or not (false).
	 */
	private static boolean hasLeaf(int unit) {
		return ((unit >> 8) & 1) == 1;
	}
	
	/**
	 * value() returns the value stored in the unit, and thus value() is
	 * available when and only when the unit is a leaf unit.
	 */
	private static int value(int unit) {
		return unit & ((1 << 31) - 1);
	}

	/**
	 * label() returns the label associted with the unit. Note that a leaf unit
	 * always returns an invalid label. For this feature, leaf unit's label()
	 * returns an <id_type> that has the MSB of 1.
	 */
	private static int label(int unit) {
		return unit & ((1 << 31) | 0xFF);
	}
	
	/**
	 * offset() returns the offset from the unit to its derived units.
	 */
	private static int offset(int unit) {
		return (unit >> 10) << ((unit & (1 << 9)) >> 6);
	}
	
	private int[] array;
	
	public DoubleArray(int[] array) {
		this.array = array;
	}
	
	public int exactMatchSearch(byte[] key) {
		int result = -1;
		int nodePos = 0;

		int unit = array[nodePos];
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
			nodePos ^= offset(unit) ^ c;
			unit = array[nodePos];
			if (label(unit) != c) {
				return result;
			}
		}

		if (!hasLeaf(unit)) {
			return result;
		}
		unit = array[nodePos ^ offset(unit)];
		result = value(unit);
		return result;
	}
	
	public void clear() {
		array = null;
	}
}
