package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.BooleanList;
import net.arnx.dartsclone.util.IntList;

public class DoubleArrayBuilderExtraUnitList {
	IntList prevs = new IntList();
	IntList nexts = new IntList();
	BooleanList isFixeds = new BooleanList();
	BooleanList isUseds = new BooleanList();
	
	public void resize(int newSize) {
		prevs.resize(newSize);
		nexts.resize(newSize);
		isFixeds.resize(newSize);
		isUseds.resize(newSize);
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
