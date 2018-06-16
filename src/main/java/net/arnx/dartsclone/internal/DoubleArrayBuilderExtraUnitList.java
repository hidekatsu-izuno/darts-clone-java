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

import net.arnx.dartsclone.util.BooleanList;
import net.arnx.dartsclone.util.IntList;

public class DoubleArrayBuilderExtraUnitList {
	IntList prevs;
	IntList nexts;
	BooleanList isFixeds;
	BooleanList isUseds;
	int head;
	
	public DoubleArrayBuilderExtraUnitList(int size) {
		prevs = new IntList(size, size);
		nexts = new IntList(size, size);
		isFixeds = new BooleanList(size, size);
		isUseds = new BooleanList(size, size);
	}
	
	public void setHead(int id) {
		head = id;
	}
	
	public int head() {
		return head;
	}
	
	public void clear() {
		prevs.clear();
		nexts.clear();
		isFixeds.clear();
		isUseds.clear();
	}
	
	public void setPrev(int index, int prev) {
		prevs.set(index, prev);
	}
	
	public void setNext(int index, int next) {
		nexts.set(index, next);
	}
	
	public void setIsFixed(int index, boolean isFixed) {
		isFixeds.set(index, isFixed);
	}
	
	public void setIsUsed(int index, boolean isUsed) {
		isUseds.set(index, isUsed);
	}

	public int prev(int index) {
		return prevs.get(index);
	}
	
	public int next(int index) {
		return nexts.get(index);
	}
	
	public boolean isFixed(int index) {
		return isFixeds.get(index);
	}
	
	public boolean isUsed(int index) {
		return isUseds.get(index);
	}
}
