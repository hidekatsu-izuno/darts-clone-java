package net.arnx.dartsclone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

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
	
	private int[] array;
	
	private DoubleArray(int[] array) {
		this.array = array;
	}
	
	public int get(byte[] key) {
		int unit = array[0];
		int nodePos = 0;
		
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
			
			nodePos ^= offset(unit) ^ c;
			unit = array[nodePos];
			if (label(unit) != c) {
				return -1;
			}
		}

		if (!hasLeaf(unit)) {
			return -1;
		}
		
		return value(array[nodePos ^ offset(unit)]);
	}
	
	public IntStream findByCommonPrefix(byte[] key) {
		IntStream.Builder builder = IntStream.builder();
		
		int unit = array[0];
		int nodePos = offset(unit);
		
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
		
			nodePos ^= c;
			unit = array[nodePos];
			if (label(unit) != c) {
				return builder.build();
			}

			nodePos ^= offset(unit);
			if (hasLeaf(unit)) {
				builder.accept(value(array[nodePos]));
			}
		}
		
		return builder.build();
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
	
	private static boolean hasLeaf(int unit) {
		return ((unit >> 8) & 1) == 1;
	}
	
	private static int value(int unit) {
		return unit & ((1 << 31) - 1);
	}

	private static int label(int unit) {
		return unit & ((1 << 31) | 0xFF);
	}
	
	private static int offset(int unit) {
		return (unit >> 10) << ((unit & (1 << 9)) >> 6);
	}
}
