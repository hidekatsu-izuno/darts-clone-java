package net.arnx.dartsclone.util;

import java.util.Arrays;

public class BooleanList {
	private static final long[] EMPTY = new long[0];
	
	private long[] buf;
	private int size;
	
	public BooleanList() {
		this(0);
	}
	
	public BooleanList(int capacity) {
		if (capacity == 0) {
			this.buf = EMPTY;
		} else {
			this.buf = new long[(capacity + (64 - 1)) / 64];
		}
	}
	
	public void resize(int newSize) {
		if (size != newSize) {
			buf = Arrays.copyOf(buf, (newSize + (64 - 1)) / 64);
			size = newSize;
		}
	}
	
	public boolean get(int index) {
		if (index < size) {
			int pos = index / 64;
			int offset = index % 64;
			return (buf[pos] & (1L << offset)) != 0;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	public void add(boolean value) {
		int pos = size / 64;
		int offset = size % 64;
		
		if (pos + 1 > buf.length) {
			int newSize = Math.max(pos + 1, 2);
			newSize += (newSize >> 1);
			
			buf = Arrays.copyOf(buf, Math.min(newSize, Integer.MAX_VALUE));
		}
		
		if (value) {
			buf[pos] |= (1L << offset);
		} else {
			buf[pos] &= ~(1L << offset);
		}
		
		size++;
	}
	
	public boolean set(int index, boolean value) {
		if (index < size) {
			int pos = index / 64;
			int offset = index % 64;
			boolean old = (buf[pos] & (1L << offset)) != 0;
			if (value) {
				buf[pos] |= (1L << offset);
			} else {
				buf[pos] &= ~(1L << offset);
			}
			return old;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	public void clear() {
		buf = EMPTY;
		size = 0;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public int size() {
		return size;
	}
	
	public String toBinaryString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			long value = buf[i];
			for (int shift = 0; shift < 64; shift++) {
				sb.append(((value >> shift) & 0x1) != 0 ? '1' : '0');
				
				if (shift % 8 == 7) {
					sb.append(" ");
				}
			}
		}
		return sb.toString();
	}
}
