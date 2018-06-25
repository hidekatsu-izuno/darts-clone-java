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

public class DoubleArrayBuilder {
	private static final int BLOCK_SIZE = 256;
	private static final int NUM_EXTRA_BLOCKS = 16;
	private static final int NUM_EXTRAS = BLOCK_SIZE * NUM_EXTRA_BLOCKS;
	private static final int UPPER_MASK = 0xFF << 21;
	private static final int LOWER_MASK = 0xFF;
	
	private static void setHasLeaf(IntList units, int index, boolean hasLeaf) {
		int unit = units.get(index);
		if (hasLeaf) {
			unit |= 1 << 8;
		} else {
			unit &= ~(1 << 8);
		}
		units.set(index, unit);
	}
	
	private static void setValue(IntList units, int index, int value) {
		int unit = units.get(index);
		unit = value | (1 << 31);
		units.set(index, unit);
	}
	
	private static void setLabel(IntList units, int index, int label) {
		int unit = units.get(index);
		unit = (unit & ~0xFF) | label;
		units.set(index, unit);
	}
	
	private static void setOffset(IntList units, int index, int offset) {
		if (offset >= 1 << 29) {
			throw new IllegalArgumentException("failed to modify unit: too large offset");
		}
		int unit = units.get(index);
		unit &= (1 << 31) | (1 << 8) | 0xFF;
		if (offset < 1 << 21) {
			unit |= (offset << 10);
		} else {
			unit |= (offset << 2) | (1 << 9);
		}
		units.set(index, unit);
	}
	
	private DawgBuilder dawg = new DawgBuilder();
	
	private IntList prevs = new IntList(NUM_EXTRAS, NUM_EXTRAS);
	private IntList nexts = new IntList(NUM_EXTRAS, NUM_EXTRAS);
	private BooleanList isFixeds = new BooleanList(NUM_EXTRAS, NUM_EXTRAS);
	private BooleanList isUseds = new BooleanList(NUM_EXTRAS, NUM_EXTRAS);
	private int head;
	
	public DoubleArrayBuilder() {
		dawg.init();
	}
	
	public void append(byte[] key, int length, int value) {
		dawg.insert(key, length, value);
	}
	
	public int[] build() {
		dawg.finish();
		
		IntList units = new IntList();
		
		reserveId(units, 0);
		isUseds.set(0, true);
		setOffset(units, 0, 1);
		setLabel(units, 0, 0);

		IntList table = new IntList(dawg.numIntersections(), dawg.numIntersections());
		if (dawg.child(dawg.root()) != 0) {
			buildFromDawg(units, dawg, dawg.root(), 0, table);
		}

		fixAllBlocks(units);
		
		return units.toArray();
	}
	
	public void clear() {
		dawg.clear();
		
		prevs.clear();
		nexts.clear();
		isFixeds.clear();
		isUseds.clear();
	}
	
	private void buildFromDawg(IntList units, DawgBuilder dawg, int dawgId, int dicId, IntList table) {
		int dawgChildId = dawg.child(dawgId);
		if (dawg.isIntersection(dawgChildId)) {
			int intersectionId = dawg.intersectionId(dawgChildId);
			int offset = table.get(intersectionId);
			if (offset != 0) {
				offset ^= dicId;
				if ((offset & UPPER_MASK) == 0 || (offset & LOWER_MASK) == 0) {
					if (dawg.isLeaf(dawgChildId)) {
						setHasLeaf(units, dicId, true);
					}
					setOffset(units, dicId, offset);
					return;
				}
			}
		}

		int offset = arrangeFromDawg(units, dawg, dawgId, dicId);
		if (dawg.isIntersection(dawgChildId)) {
			table.set(dawg.intersectionId(dawgChildId), offset);
		}

		do {
			int childLabel = dawg.label(dawgChildId);
			int dicChildId = offset ^ childLabel;
			if (childLabel != '\0') {
				buildFromDawg(units, dawg, dawgChildId, dicChildId, table);
			}
			dawgChildId = dawg.sibling(dawgChildId);
		} while (dawgChildId != 0);
	}

	private int arrangeFromDawg(IntList units, DawgBuilder dawg, int dawgId, int dicId) {
		IntList labels = new IntList();

		int dawgChildId = dawg.child(dawgId);
		while (dawgChildId != 0) {
			labels.add(dawg.label(dawgChildId));
			dawgChildId = dawg.sibling(dawgChildId);
		}

		int offset = findValidOffset(units, dicId, labels);
		setOffset(units, dicId, dicId ^ offset);

		dawgChildId = dawg.child(dawgId);
		for (int i = 0; i < labels.size(); i++) {
			int dicChildId = offset ^ labels.get(i);
			reserveId(units, dicChildId);

			if (dawg.isLeaf(dawgChildId)) {
				setHasLeaf(units, dicId, true);
				setValue(units, dicChildId, dawg.value(dawgChildId));
			} else {
				setLabel(units, dicChildId, labels.get(i));
			}

			dawgChildId = dawg.sibling(dawgChildId);
		}
		isUseds.set(offset % NUM_EXTRAS, true);

		return offset;
	}
	
	private int findValidOffset(IntList units, int id, IntList labels) {
		if (head >= units.size()) {
			return units.size() | (id & LOWER_MASK);
		}

		int unfixedId = head;
		do {
			int offset = unfixedId ^ labels.get(0);
			if (isValidOffset(id, offset, labels)) {
				return offset;
			}
			unfixedId = nexts.get(unfixedId % NUM_EXTRAS);
		} while (unfixedId != head);

		return units.size() | (id & LOWER_MASK);
	}
	
	private boolean isValidOffset(int id, int offset, IntList labels) {
		if (isUseds.get(offset % NUM_EXTRAS)) {
			return false;
		}

		int relOffset = id ^ offset;
		if ((relOffset & LOWER_MASK) != 0 && (relOffset & UPPER_MASK) != 0) {
			return false;
		}

		for (int i = 1; i < labels.size(); i++) {
			if (isFixeds.get((offset ^ labels.get(i)) % NUM_EXTRAS)) {
				return false;
			}
		}

		return true;
	}
	
	private void reserveId(IntList units, int id) {
		if (id >= units.size()) {
			expandUnits(units);
		}

		if (id == head) {
			head = nexts.get(id % NUM_EXTRAS);
			if (head == id) {
				head = units.size();
			}
		}
		nexts.set(prevs.get(id % NUM_EXTRAS) % NUM_EXTRAS, nexts.get(id % NUM_EXTRAS));
		prevs.set(nexts.get(id % NUM_EXTRAS) % NUM_EXTRAS, prevs.get(id % NUM_EXTRAS));
		isFixeds.set(id % NUM_EXTRAS, true);
	}
	
	private void expandUnits(IntList units) {
		int srcNumUnits = units.size();
		int srcNumlocks = srcNumUnits / BLOCK_SIZE;

		int destNumUnits = srcNumUnits + BLOCK_SIZE;
		int destNumBlocks = srcNumlocks + 1;

		if (destNumBlocks > NUM_EXTRA_BLOCKS) {
			fixBlock(units, srcNumlocks - NUM_EXTRA_BLOCKS);
		}

		units.resize(destNumUnits);

		if (destNumBlocks > NUM_EXTRA_BLOCKS) {
			for (int id = srcNumUnits; id < destNumUnits; id++) {
				isUseds.set(id % NUM_EXTRAS, false);
				isFixeds.set(id % NUM_EXTRAS, false);
			}
		}

		for (int i = srcNumUnits + 1; i < destNumUnits; i++) {
			nexts.set((i - 1) % NUM_EXTRAS, i);
			prevs.set(i % NUM_EXTRAS, i - 1);
		}

		prevs.set(srcNumUnits % NUM_EXTRAS, destNumUnits - 1);
		nexts.set((destNumUnits - 1) % NUM_EXTRAS, srcNumUnits);

		prevs.set(srcNumUnits % NUM_EXTRAS, prevs.get(head % NUM_EXTRAS));
		nexts.set((destNumUnits - 1) % NUM_EXTRAS, head);

		nexts.set(prevs.get(head % NUM_EXTRAS) % NUM_EXTRAS, srcNumUnits);
		prevs.set(head % NUM_EXTRAS, destNumUnits - 1);
	}
	
	private void fixAllBlocks(IntList units) {
		int begin = 0;
		int numBlocks = units.size() / BLOCK_SIZE;
		if (numBlocks > NUM_EXTRA_BLOCKS) {
			begin = numBlocks - NUM_EXTRA_BLOCKS;
		}
		int end = numBlocks;

		for (int blockId = begin; blockId != end; blockId++) {
			fixBlock(units, blockId);
		}
	}
	
	private void fixBlock(IntList units, int blockId) {
		int begin = blockId * BLOCK_SIZE;
		int end = begin + BLOCK_SIZE;

		int unusedOffset = 0;
		for (int offset = begin; offset != end; offset++) {
			if (!isUseds.get(offset % NUM_EXTRAS)) {
				unusedOffset = offset;
				break;
			}
		}

		for (int id = begin; id != end; id++) {
			if (!isFixeds.get(id % NUM_EXTRAS)) {
				reserveId(units, id);
				setLabel(units, id, id ^ unusedOffset);
			}
		}
	}
}
