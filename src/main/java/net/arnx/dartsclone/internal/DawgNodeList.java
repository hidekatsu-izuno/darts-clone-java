package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.BooleanList;
import net.arnx.dartsclone.util.IntList;

public class DawgNodeList {
	IntList childs;
	IntList siblings;
	IntList labels;
	BooleanList isStates;
	BooleanList hasSiblings;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("childs: ").append(childs.toHexString()).append(", ");
		sb.append("siblings: ").append(siblings.toHexString()).append(", ");
		sb.append("labels: ").append(labels.toHexString()).append(", ");
		sb.append("isStates: ").append(isStates.toBinaryString()).append(", ");
		sb.append("hasSiblings: ").append(hasSiblings.toBinaryString());
		return sb.toString();
	}
	
	public DawgNodeList() {
		childs = new IntList();
		siblings = new IntList();
		labels = new IntList();
		isStates = new BooleanList();
		hasSiblings = new BooleanList();
	}
	
	public void add(int child, int sibling, int label, boolean isState, boolean hasSibling) {
		childs.add(child);
		siblings.add(sibling);
		labels.add(label);
		isStates.add(isState);
		hasSiblings.add(hasSibling);
	}
	
	public void set(int index, int child, int sibling, int label, boolean isState, boolean hasSibling) {
		childs.set(index, child);
		siblings.set(index, sibling);
		labels.set(index, label);
		isStates.set(index, isState);
		hasSiblings.set(index, hasSibling);
	}
	
	public void clear() {
		childs.clear();
		siblings.clear();
		labels.clear();
		isStates.clear();
		hasSiblings.clear();
	}
	
	public int size() {
		return childs.size();
	}
	
	public void setChild(int index, int child) {
		childs.set(index, child);
	}
	
	public void setSibling(int index, int sibling) {
		siblings.set(index, sibling);
	}
	
	public void setValue(int index, int value) {
		childs.set(index, value);
	}
	
	public void setLabel(int index, int label) {
		labels.set(index, label);
	}
	
	public void setIsState(int index, boolean isState) {
		isStates.set(index, isState);
	}
	
	public void setHasSibling(int index, boolean hasSibling) {
		hasSiblings.set(index, hasSibling);
	}

	public int child(int index) {
		return childs.get(index);
	}
	
	public int sibling(int index) {
		return siblings.get(index);
	}
	
	public int value(int index) {
		return childs.get(index);
	}
	
	public int label(int index) {
		return labels.get(index);
	}
	
	public boolean isState(int index) {
		return isStates.get(index);
	}
	
	public boolean hasSibling(int index) {
		return hasSiblings.get(index);
	}

	public int unit(int index) {
		if (labels.get(index) == 0) {
			return (childs.get(index) << 1) | (hasSiblings.get(index) ? 1 : 0);
		}
		return (childs.get(index) << 2) | (isStates.get(index) ? 2 : 0) | (hasSiblings.get(index) ? 1 : 0);
	}
}
