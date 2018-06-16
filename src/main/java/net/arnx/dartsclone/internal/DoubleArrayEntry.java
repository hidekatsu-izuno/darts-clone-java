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
package net.arnx.dartsclone.internal;

import java.util.Arrays;

public class DoubleArrayEntry {
	private byte[] key;
	private int value;
	
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
	
	public byte[] key() {
		return key;
	}
	
	public int value() {
		return value;
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
