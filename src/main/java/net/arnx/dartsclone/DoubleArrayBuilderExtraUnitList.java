package net.arnx.dartsclone;

public class DoubleArrayBuilderExtraUnitList {
	private IntList prevs = new IntList();
	private IntList nexts = new IntList();
	private BooleanList isFixeds = new BooleanList();
	private BooleanList isUseds = new BooleanList();
	
	public void resize(int newSize, int prev, int next, boolean isFixed, boolean isUsed) {
		prevs.resize(newSize, prev);
		nexts.resize(newSize, next);
		isFixeds.resize(newSize, isFixed);
		isUseds.resize(newSize, isUsed);
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
