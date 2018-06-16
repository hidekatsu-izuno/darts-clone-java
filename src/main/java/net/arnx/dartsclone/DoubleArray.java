package net.arnx.dartsclone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.arnx.dartsclone.internal.DoubleArrayBuilder;
import net.arnx.dartsclone.internal.DoubleArrayEntry;
import net.arnx.dartsclone.util.IntList;

public class DoubleArray {
	public static DoubleArray wrap(int[] array) {
		return new DoubleArray(array);
	}
	
	public static class Builder {
		private List<DoubleArrayEntry> list = new ArrayList<>();
		
		public Builder put(byte[] key, int value) {
			list.add(new DoubleArrayEntry(key, value));
			return this;
		}
		
		public DoubleArray build() {
			DoubleArrayBuilder builder = new DoubleArrayBuilder();
			return new DoubleArray(builder.build(list));
		}
	}

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
	
	private DoubleArray(int[] array) {
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
	
	public IntList commonPrefixSearch(byte[] key) {
		IntList results = new IntList();
		int nodePos = 0;

		int unit = array[nodePos];
		nodePos ^= offset(unit);
		
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
		
			nodePos ^= c;
			unit = array[nodePos];
			if (label(unit) != c) {
				return results;
			}

			nodePos ^= offset(unit);
			if (hasLeaf(unit)) {
				results.add(value(array[nodePos]));
			}
		}

		return results;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DoubleArray other = (DoubleArray) obj;
		if (!Arrays.equals(array, other.array)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return IntList.wrap(array).toHexString();
	}
}
