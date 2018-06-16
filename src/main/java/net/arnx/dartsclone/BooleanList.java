package net.arnx.dartsclone;

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
	
	public void resize(int newSize, boolean defaultValue) {
		if (size > newSize) {
			buf = Arrays.copyOf(buf, (newSize + (64 - 1)) / 64);
			size = newSize;
		} else if (size < newSize) {
			buf = Arrays.copyOf(buf, (newSize + (64 - 1)) / 64);
			size = newSize;
			
			if (defaultValue) {
				for (int i = size; i < newSize; i++) {
					set(i, true);
				}
			}
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
}
