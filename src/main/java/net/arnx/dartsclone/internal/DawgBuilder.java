package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.IntList;

public class DawgBuilder {
	private static final int INITIAL_TABLE_SIZE = 1 << 10;
	
	private static int hash(int key) {
		key = ~key + (key << 15);  // key = (key << 15) - key - 1;
		key = key ^ (key >> 12);
		key = key + (key << 2);
		key = key ^ (key >> 4);
		key = key * 2057;  // key = (key + (key << 3)) + (key << 11);
		key = key ^ (key >> 16);
		return key;
	}
	
	DawgNodeList nodes = new DawgNodeList();
	DawgUnitList units = new DawgUnitList();
	IntList labels = new IntList();
	BitVector isIntersections = new BitVector();
	IntList table = new IntList();
	IntList nodeStack = new IntList();
	IntList recycleBin = new IntList();
	int numStates;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("nodes: { ").append(nodes.toString()).append(" }, ");
		sb.append("units: ").append(units.toString()).append(", ");
		sb.append("labels: ").append(labels.toHexString()).append(", ");
		sb.append("isIntersections: { ").append(isIntersections.toString()).append(" }, ");
		sb.append("table: ").append(table.toHexString()).append(", ");
		sb.append("nodeStack: ").append(nodeStack.toHexString()).append(", ");
		sb.append("recycleBin: ").append(recycleBin.toHexString()).append(", ");
		sb.append("numStates: ").append(numStates);
		return sb.toString();
	}
	
	public int root() {
		return 0;
	}

	public int child(int id) {
		return units.child(id);
	}
	
	public int sibling(int id) {
		return units.hasSibling(id) ? (id + 1) : 0;
	}
	
	public int value(int id) {
		return units.value(id);
	}

	public boolean isLeaf(int id) {
		return label(id) == 0;
	}
	
	public int label(int id) {
		return labels.get(id);
	}

	public boolean isIntersection(int id) {
		return isIntersections.get(id);
	}
	
	public int intersectionId(int id) {
		return isIntersections.rank(id) - 1;
	}
	
	public int numIntersections() {
		return isIntersections.numOnes();
	}

	public int size() {
		return units.size();
	}
	  
	public void init() {
		table.resize(INITIAL_TABLE_SIZE, 0);

		appendNode();
		appendUnit();

		numStates = 1;

		nodes.setLabel(0, 0xFF);
		nodeStack.add(0);
	}
	
	public void finish() {
		flush(0);

		units.set(0, nodes.unit(0));
		labels.set(0, nodes.label(0));

		nodes.clear();
		table.clear();
		nodeStack.clear();
		recycleBin.clear();

		isIntersections.build();
	}
	
	public void insert(byte[] key, int value) {
		if (value < 0) {
			throw new IllegalArgumentException("failed to insert key: negative value");
		} else if (key.length == 0) {
			throw new IllegalArgumentException("failed to insert key: zero-length key");
		}

		int id = 0;
		int keyPos = 0;

		for ( ; keyPos <= key.length; keyPos++) {
			int childId = nodes.child(id);
			if (childId == 0) {
				break;
			}

			int keyLabel = key[keyPos] & 0xFF;
			if (keyPos < key.length && keyLabel == 0) {
				throw new IllegalStateException("failed to insert key: invalid null character");
			}

			int unitLabel = nodes.label(childId);
			if (keyLabel < unitLabel) {
				throw new IllegalStateException("failed to insert key: wrong key order");
			} else if (keyLabel > unitLabel) {
				nodes.setHasSibling(childId, true);
				flush(childId);
				break;
			}
			id = childId;
		}

		if (keyPos > key.length) {
			return;
		}

		for ( ; keyPos <= key.length; keyPos++) {
			int keyLabel = (keyPos < key.length) ? (key[keyPos] & 0xFF) : 0;
			int childId = appendNode();

			if (nodes.child(id) == 0) {
				nodes.setIsState(childId, true);
			}
			nodes.setSibling(childId, nodes.child(id));
			nodes.setLabel(childId, keyLabel);
			nodes.setChild(id, childId);
			nodeStack.add(childId);

			id = childId;
		}
		nodes.setValue(id, value);
	}
	
	public void clear() {
		nodes.clear();
		units.clear();
		labels.clear();
		isIntersections.clear();
		table.clear();
		nodeStack.clear();
		recycleBin.clear();
		numStates = 0;
	}

	private int appendNode() {
		int id;
		if (recycleBin.isEmpty()) {
			id = nodes.size();
			nodes.add(0, 0, 0, false, false);
		} else {
			id = recycleBin.get(recycleBin.size() - 1);
			nodes.set(id, 0, 0, 0, false, false);
			recycleBin.remove(recycleBin.size() - 1);
		}
		return id;
	}
	
	private int appendUnit() {
		isIntersections.add();
		units.add(0);
		labels.add(0);

		return isIntersections.size() - 1;
	}
	
	private void flush(int id) {
		while (nodeStack.get(nodeStack.size() - 1) != id) {
			int nodeId = nodeStack.get(nodeStack.size() - 1);
			nodeStack.remove(nodeStack.size() - 1);

			if (numStates >= table.size() - (table.size() >> 2)) {
				expandTable();
			}

			int numSiblings = 0;
			for (int i = nodeId; i != 0; i = nodes.sibling(i)) {
				numSiblings++;
			}

			int[] hashId = new int[1];
			int matchId = findNode(nodeId, hashId);
			if (matchId != 0) {
				isIntersections.set(matchId, true);
			} else {
				int unitId = 0;
				for (int i = 0; i < numSiblings; i++) {
					unitId = appendUnit();
				}
				for (int i = nodeId; i != 0; i = nodes.sibling(i)) {
					units.set(unitId, nodes.unit(i));
					labels.set(unitId, nodes.label(i));
					unitId--;
				}
				matchId = unitId + 1;
				table.set(hashId[0], matchId);
				numStates++;
			}

			for (int i = nodeId, next; i != 0; i = next) {
				next = nodes.sibling(i);
				freeNode(i);
			}

			nodes.setChild(nodeStack.get(nodeStack.size() - 1), matchId);
		}
		nodeStack.remove(nodeStack.size() - 1);
	}

	private void expandTable() {
		int tableSize = table.size() << 1;
		table.clear();
		table.resize(tableSize, 0);

		for (int i = 1; i < units.size(); i++) {
			int id = i;
			if (labels.get(id) == 0 || units.isState(id)) {
				int[] hashId = new int[1];
				findUnit(id, hashId);
				table.set(hashId[0], id);
			}
		}
	}
	
	private int findUnit(int id, int[] hashId) {
		hashId[0] = hashUnit(id) % table.size();
		for ( ; ; hashId[0] = (hashId[0] + 1) % table.size()) {
			int unitId = table.get(hashId[0]);
			if (unitId == 0) {
				break;
			}

			// There must not be the same unit.
		}
		return 0;
	}

	private int findNode(int nodeId, int[] hashId) {
		hashId[0] = hashNode(nodeId) % table.size();
		for ( ; ; hashId[0] = (hashId[0] + 1) % table.size()) {
			int unit_id = table.get(hashId[0]);
			if (unit_id == 0) {
				break;
			}

			if (areEqual(nodeId, unit_id)) {
				return unit_id;
			}
		}
		return 0;
	}
	
	private void freeNode(int id) {
		recycleBin.add(id);
	}
	
	private boolean areEqual(int nodeId, int unitId) {
		for (int i = nodes.sibling(nodeId); i != 0;
				i = nodes.sibling(i)) {
			if (!units.hasSibling(unitId)) {
				return false;
			}
			unitId++;
		}
		if (units.hasSibling(unitId)) {
			return false;
		}

		for (int i = nodeId; i != 0; i = nodes.sibling(i), unitId--) {
			if (nodes.unit(i) != units.unit(unitId) ||
					nodes.label(i) != labels.get(unitId)) {
				return false;
			}
		}
		return true;
	}

	private int hashUnit(int id) {
		int hashValue = 0;
		for ( ; id != 0; ++id) {
			int unit = units.unit(id);
			int label = labels.get(id);
			hashValue ^= hash((label << 24) ^ unit);

			if (!units.hasSibling(id)) {
				break;
			}
		}
		return hashValue;
	}
	
	private int hashNode(int id) {
		int hashValue = 0;
		for ( ; id != 0; id = nodes.sibling(id)) {
			int unit = nodes.unit(id);
			int label = nodes.label(id);
			hashValue ^= hash((label << 24) ^ unit);
		}
		return hashValue;
	}
}
