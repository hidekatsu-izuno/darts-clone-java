package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.IntList;

public class DawgUnitList {
	IntList list = new IntList();
	
	public void add(int unit) {
		list.add(unit);
	}
	
	public void set(int index, int unit) {
		list.set(index, unit);
	}
	
	public void clear() {
		list.clear();
	}
	
	public int size() {
		return list.size();
	}

	public void setUnit(int index, int unit) {
		list.set(index, unit);
	}

	public int unit(int index) {
		return list.get(index);
	}

	public int child(int index) {
		return list.get(index) >> 2;
	}

	public boolean hasSibling(int index) {
		return (list.get(index) & 1) == 1;
	}

	public int value(int index) {
		return list.get(index) >> 1;
	}
	
	public boolean isState(int index) {
		return (list.get(index) & 2) == 2;
	}
}
