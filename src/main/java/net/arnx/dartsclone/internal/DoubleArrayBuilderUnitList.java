package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.IntList;

public class DoubleArrayBuilderUnitList {
	private IntList list = new IntList();
	
	public int[] toArray() {
		return list.toArray();
	}
	
	public void resize(int newSize, int defaultValue) {
		list.resize(newSize, defaultValue);
	}
	
	public int get(int index) {
		return list.get(index);
	}
	
	public int size() {
		return list.size();
	}
	
	public void clear() {
		list.clear();
	}
	
	public int setHasLeaf(int index, boolean hasLeaf) {
		int unit = list.get(index);
		if (hasLeaf) {
			unit |= 1 << 8;
		} else {
			unit &= ~(1 << 8);
		}
		list.set(index, unit);
		return unit;
	}
	
	public int setValue(int index, int value) {
		int unit = list.get(index);
		unit = value | (1 << 31);
		list.set(index, unit);
		return unit;
	}
	
	public int setLabel(int index, int label) {
		int unit = list.get(index);
		unit = (unit & ~0xFF) | label;
		list.set(index, unit);
		return unit;
	}
	
	public int setOffset(int index, int offset) {
		if (offset >= 1 << 29) {
			throw new IllegalArgumentException("failed to modify unit: too large offset");
		}
		int unit = list.get(index);
		unit &= (1 << 31) | (1 << 8) | 0xFF;
		if (offset < 1 << 21) {
			unit |= (offset << 10);
		} else {
			unit |= (offset << 2) | (1 << 9);
		}
		list.set(index, unit);
		return unit;
	}
}
