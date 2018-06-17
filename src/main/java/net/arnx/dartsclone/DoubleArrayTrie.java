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
package net.arnx.dartsclone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import net.arnx.dartsclone.internal.DoubleArrayBuilder;
import net.arnx.dartsclone.util.IntList;

public class DoubleArrayTrie {
	public static DoubleArrayTrie wrap(int[] array) {
		return new DoubleArrayTrie(array);
	}
	
	public static class Builder {
		private Set<DoubleArrayEntry> keyset = new LinkedHashSet<>();
		
		public Builder put(byte[] key, int value) {
			keyset.add(new DoubleArrayEntry(key, value));
			return this;
		}
		
		public DoubleArrayTrie build() {
			return new DoubleArrayTrie(toArray());
		}
		
		public int[] toArray() {
			List<DoubleArrayEntry> list = new ArrayList<>(keyset);
			Collections.sort(list);
		    
			DoubleArrayBuilder builder = new DoubleArrayBuilder();
			for (DoubleArrayEntry entry : list) {
				builder.append(entry.key, entry.value);
			}
			return builder.build();
		}
	}
	
	private static class DoubleArrayEntry implements Comparable<DoubleArrayEntry> {
		byte[] key;
		int value;
		
		public DoubleArrayEntry(byte[] key, int value) {
			if (key == null || key.length == 0) {
				throw new IllegalArgumentException("key must not be empty.");
			}
			if (value < 0) {
				throw new IllegalArgumentException("value must not be negative.");
			}
			
			this.key = key;
			this.value = value;
		}
		
		@Override
		public int compareTo(DoubleArrayEntry o) {
			int result = 0;
			int min = Math.min(key.length, o.key.length);
	        for (int i = 0; i < min; i++) {
	          result = (key[i] & 0xFF) - (o.key[i] & 0xFF);
	          if (result != 0) {
	            return result;
	          }
	        }
	        result = key.length - o.key.length;
	        if (result != 0) {
	        	return result;
	        }
	        return value - o.value;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(key);
			result = prime * result + value;
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
			DoubleArrayEntry other = (DoubleArrayEntry) obj;
			if (!Arrays.equals(key, other.key)) {
				return false;
			}
			if (value != other.value) {
				return false;
			}
			return true;
		}
	}

	public static DoubleArrayTrie load(InputStream in) throws IOException {
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
		
		return new DoubleArrayTrie(list.toArray());
	}
	
	private int[] array;
	
	private DoubleArrayTrie(int[] array) {
		this.array = array;
	}
	
	public int get(byte[] key) {
		int unit = array[0];
		int pos = offset(unit);
		
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
			
			pos ^= c;
			unit = array[pos];
			if (label(unit) == c) {
				pos ^= offset(unit);
			} else {
				return -1;
			}
		}

		if (hasLeaf(unit)) {
			return value(array[pos]);
		} else {
			return -1;		
		}		
	}
	
	public IntStream findByCommonPrefix(byte[] key) {
		IntStream.Builder builder = IntStream.builder();
		
		int unit = array[0];
		int pos = offset(unit);
		
		for (int i = 0; i < key.length; i++) {
			int c = (key[i] & 0xFF);
		
			pos ^= c;
			unit = array[pos];
			if (label(unit) == c) {
				pos ^= offset(unit);
			} else {
				break;
			}
			
			if (hasLeaf(unit)) {
				builder.accept(value(array[pos]));
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
		DoubleArrayTrie other = (DoubleArrayTrie) obj;
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
