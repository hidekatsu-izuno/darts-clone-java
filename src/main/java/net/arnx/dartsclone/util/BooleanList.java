/*
 * Copyright 2018 Hidekatsu Izuno <hidekatsu.izuno@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.dartsclone.util;

import java.util.Arrays;

public class BooleanList {
	private static final int[] EMPTY = new int[0];
	
	private int[] buf;
	private int size;
	
	public BooleanList() {
		this(0);
	}
	
	public BooleanList(int capacity) {
		this(capacity, 0);
	}
	
	public BooleanList(int capacity, int size) {
		if (capacity < size) {
			throw new IndexOutOfBoundsException();
		}
		
		if (capacity == 0) {
			this.buf = EMPTY;
		} else {
			this.buf = new int[(capacity + (32 - 1)) / 32];
		}
		this.size = size;
	}
	
	public void resize(int newSize) {
		if (size != newSize) {
			buf = Arrays.copyOf(buf, (newSize + (32 - 1)) / 32);
			size = newSize;
		}
	}
	
	public boolean get(int index) {
		if (index < size) {
			int pos = index / 32;
			int offset = index % 32;
			return (buf[pos] & (1L << offset)) != 0;
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	
	public void add(boolean value) {
		int pos = size / 32;
		int offset = size % 32;
		
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
			int pos = index / 32;
			int offset = index % 32;
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
			for (int shift = 0; shift < 32; shift++) {
				sb.append(((value >> shift) & 0x1) != 0 ? '1' : '0');
			}
		}
		return sb.toString();
	}
}
