package net.arnx.dartsclone;

public class DawgNodeList {
	private IntList childs;
	private IntList siblings;
	private StringBuilder labels;
	private BooleanList isStates;
	private BooleanList hasSiblings;
	
	public DawgNodeList() {
		childs = new IntList();
		siblings = new IntList();
		labels = new StringBuilder();
		isStates = new BooleanList();
		hasSiblings = new BooleanList();
	}
	
	public void add(int child, int sibling, char label, boolean isState, boolean hasSibling) {
		childs.add(child);
		siblings.add(sibling);
		labels.append(label);
		isStates.add(isState);
		hasSiblings.add(hasSibling);
	}
	
	public void set(int index, int child, int sibling, char label, boolean isState, boolean hasSibling) {
		childs.set(index, child);
		siblings.set(index, sibling);
		labels.setCharAt(index, label);
		isStates.set(index, isState);
		hasSiblings.set(index, hasSibling);
	}
	
	public void clear() {
		childs.clear();
		siblings.clear();
		labels.setLength(0);
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
	
	public void setLabel(int index, char label) {
		labels.setCharAt(index, label);
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
	
	public char label(int index) {
		return labels.charAt(index);
	}
	
	public boolean isState(int index) {
		return isStates.get(index);
	}
	
	public boolean hasSibling(int index) {
		return hasSiblings.get(index);
	}

	public int unit(int index) {
		if (labels.charAt(index) == '\0') {
			return (childs.get(index) << 1) | (hasSiblings.get(index) ? 1 : 0);
		}
		return (childs.get(index) << 2) | (isStates.get(index) ? 2 : 0) | (hasSiblings.get(index) ? 1 : 0);
	}
}
