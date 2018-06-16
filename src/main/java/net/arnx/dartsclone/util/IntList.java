package net.arnx.dartsclone.util;

import java.util.Arrays;

public class IntList {
	private static final int[] EMPTY = new int[0];
	
	private int[] buf;
	private int size;
	
	public IntList() {
		this(0);
	}
	
	public IntList(int capacity) {
		if (capacity == 0) {
			this.buf = EMPTY;
		} else {
			this.buf = new int[capacity];
		}
	}
	
	public int get(int index) {
		if (index < size) {
			return buf[index];
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	public void add(int value) {
		if (size + 1 > buf.length) {
			int newSize = Math.max(size + 1, 2);
			newSize += (newSize >> 1);
			
			buf = Arrays.copyOf(buf, Math.min(newSize, Integer.MAX_VALUE));
		}
		buf[size++] = value;
	}
	
	public int set(int index, int value) {
		if (index < size) {
			int old = buf[index];
			buf[index] = value;
			return old;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	public void resize(int newSize, int defaultValue) {
		if (size > newSize) {
			buf = Arrays.copyOf(buf, newSize);
			size = newSize;
		} else if (size < newSize) {
			buf = Arrays.copyOf(buf, newSize);
			if (defaultValue != 0) {
				Arrays.fill(buf, size, newSize, defaultValue);
			}
			size = newSize;
		}
	}
	
	public int remove(int index) {
		int old = get(index);
		if (index < size - 1) {
			System.arraycopy(buf, index + 1, buf, index, size - index - 1);
		}
		size--;
		return old;
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
	
	public int[] toArray() {
		return Arrays.copyOf(buf, size);
	}
}
