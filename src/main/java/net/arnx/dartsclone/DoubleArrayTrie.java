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
import java.util.Objects;
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
		private int maxLength = 0;
		
		public Builder put(String key, int value) {
			keyset.add(new DoubleArrayEntry(key, value));
			maxLength = Math.max(key.length(), maxLength);
			return this;
		}
		
		public DoubleArrayTrie build() {
			return new DoubleArrayTrie(toArray());
		}
		
		public int[] toArray() {
			List<DoubleArrayEntry> list = new ArrayList<>(keyset);
			Collections.sort(list);
		    
			DoubleArrayBuilder builder = new DoubleArrayBuilder();
			
			byte[] buf = new byte[maxLength * 3];
			for (DoubleArrayEntry entry : list) {
				int length = encodeModifiedUtf8(entry.key, buf);
				builder.append(buf, length, entry.value);
			}
			return builder.build();
		}
	}
	
	private static class DoubleArrayEntry implements Comparable<DoubleArrayEntry> {
		String key;
		int value;
		
		public DoubleArrayEntry(String key, int value) {
			if (key == null || key.isEmpty()) {
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
	        return key.compareTo(o.key);
		}
		
		@Override
		public int hashCode() {
			return key.hashCode();
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
			if (!Objects.equals(key, other.key)) {
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
	
	private static final ThreadLocal<byte[]> THREAD_LOCAL = 
			new ThreadLocal<byte[]>() {
		protected byte[] initialValue() {
			return new byte[256];
		};
	};
	
	private int[] array;
	
	private DoubleArrayTrie(int[] array) {
		this.array = array;
	}
	
	public int get(String key) {
		byte[] buf = THREAD_LOCAL.get();
		if (buf.length < key.length() * 3) {
			buf = new byte[key.length() * 3];
			THREAD_LOCAL.set(buf);
		}
		int length = encodeModifiedUtf8(key, buf);
		
		int unit = array[0];
		int pos = offset(unit);
		
		for (int i = 0; i < length; i++) {
			int c = (buf[i] & 0xFF);
			
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
	
	public IntStream findByCommonPrefix(String key) {
		byte[] buf = THREAD_LOCAL.get();
		if (buf.length < key.length() * 3) {
			buf = new byte[key.length() * 3];
			THREAD_LOCAL.set(buf);
		}
		int length = encodeModifiedUtf8(key, buf);
		
		IntStream.Builder builder = IntStream.builder();
		
		int unit = array[0];
		int pos = offset(unit);
		
		for (int i = 0; i < length; i++) {
			int c = (buf[i] & 0xFF);
		
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
	
	private static int encodeModifiedUtf8(String str, byte[] buf) {
		int pos = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c != '\0' && c < '\u0080') {
				buf[pos++] = (byte)c;
			} else if (c < '\u0800') {
				buf[pos++] = (byte)(((c >> 6) & 0x1f) | 0xc0);
				buf[pos++] = (byte)((c & 0x3f) | 0x80);
			} else {
				buf[pos++] = (byte)(((c >> 12) & 0x0f) | 0xe0);
				buf[pos++] = (byte)(((c >> 6) & 0x3f) | 0x80);
				buf[pos++] = (byte)((c & 0x3f) | 0x80);
			}
		}
		return pos;
	}
}
