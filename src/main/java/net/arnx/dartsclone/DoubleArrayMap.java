package net.arnx.dartsclone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class DoubleArrayMap {
	public static DoubleArrayMap load(InputStream in) throws IOException {
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
		
		return new DoubleArrayMap(list.toArray());
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
	
	private DoubleArrayMap(int[] array) {
		this.array = array;
	}
	
	public DoubleArrayMap(SortedMap<String, Integer> map) {
		List<Map.Entry<String, Integer>> keyset = new ArrayList<>(map.entrySet());
		
		DoubleArrayBuilder builder = new DoubleArrayBuilder();
		this.array = builder.build(keyset);
	}
	
	public void writeTo(OutputStream out) throws IOException {
		byte[] buf = new byte[4];
		for (int i = 0; i < array.length; i++) {
			int value = array[i];
			buf[0] = (byte)(value & 0xFF);
			buf[1] = (byte)((value >> 8) & 0xFF);
			buf[2] = (byte)((value >> 16) & 0xFF);
			buf[3] = (byte)((value >> 24) & 0xFF);
			out.write(buf);
		}
	}
	
	public int exactMatchSearch(String key) {
		int result = -1;
		int nodePos = 0;

		int unit = array[nodePos];
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
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
