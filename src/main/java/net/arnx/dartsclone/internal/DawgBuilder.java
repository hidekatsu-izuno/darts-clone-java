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

public class DawgBuilder {
	private static final int INITIAL_TABLE_SIZE = 1 << 10;
	
	private static int hash(int key) {
		key = ~key + (key << 15);
		key = key ^ (key >> 12);
		key = key + (key << 2);
		key = key ^ (key >> 4);
		key = key * 2057;  // key = (key + (key << 3)) + (key << 11);
		key = key ^ (key >> 16);
		return key;
	}
	
	IntList nodeChilds = new IntList();
	IntList nodeSiblings = new IntList();
	IntList nodeLabels = new IntList();
	BooleanList isNodeStates = new BooleanList();
	BooleanList hasNodeSiblings = new BooleanList();
	
	IntList units = new IntList();
	IntList labels = new IntList();
	BitVector isIntersections = new BitVector();
	IntList table = new IntList();
	IntList nodeStack = new IntList();
	IntList recycleBin = new IntList();
	int numStates;
	
	public void dump() {
		System.out.print("DawgBuilder.java: { ");
		System.out.print("nodes: { ");
		System.out.print("childs: " + nodeChilds.toHexString() + ", ");
		System.out.print("siblings: " + nodeSiblings.toHexString() + ", ");
		System.out.print("labels: " + nodeLabels.toHexString() + ", ");
		System.out.print("isStates: " + isNodeStates.toBinaryString() + ", ");
		System.out.print("hasSiblings: " + hasNodeSiblings.toBinaryString() + " ");
		System.out.print("}, ");
		System.out.print("units: " + units.toHexString() +", ");
		System.out.print("labels: " + labels.toHexString() + ", ");
		System.out.print("isIntersections: { ");
		System.out.print("units: " + isIntersections.units.toHexString() + ", ");
		System.out.print("ranks: " + isIntersections.ranks.toHexString() + ", ");
		System.out.print("numOnes: " + isIntersections.numOnes + ", ");
		System.out.print("size: " + isIntersections.size + " ");
		System.out.print("}, ");
		System.out.print("table: " + table.toHexString() + ", ");
		System.out.print("nodeStack: " + nodeStack.toHexString() + ", ");
		System.out.print("recycleBin: " + recycleBin.toHexString() + ", ");
		System.out.print("numStates: " + numStates + " ");
		System.out.println("}");
	}
	
	public int root() {
		return 0;
	}

	public int child(int id) {
		return units.get(id) >> 2;
	}
	
	public int sibling(int id) {
		return hasSibling(id) ? (id + 1) : 0;
	}
	
	public int value(int id) {
		return units.get(id) >> 1;
	}

	private boolean hasSibling(int index) {
		return (units.get(index) & 1) == 1;
	}
	
	private boolean isState(int index) {
		return (units.get(index) & 2) == 2;
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
		table.resize(INITIAL_TABLE_SIZE);

		appendNode();
		appendUnit();

		numStates = 1;

		nodeLabels.set(0, 0xFF);
		nodeStack.add(0);
	}
	
	public void finish() {
		flush(0);

		units.set(0, nodeUnit(0));
		labels.set(0, nodeLabels.get(0));

		nodeChilds.clear();
		nodeSiblings.clear();
		nodeLabels.clear();
		isNodeStates.clear();
		hasNodeSiblings.clear();
		
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
			int childId = nodeChilds.get(id);
			if (childId == 0) {
				break;
			}

			int keyLabel = key[keyPos] & 0xFF;
			if (keyPos < key.length && keyLabel == 0) {
				throw new IllegalStateException("failed to insert key: invalid null character");
			}

			int unitLabel = nodeLabels.get(childId);
			if (keyLabel < unitLabel) {
				throw new IllegalStateException("failed to insert key: wrong key order");
			} else if (keyLabel > unitLabel) {
				hasNodeSiblings.set(childId, true);
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

			if (nodeChilds.get(id) == 0) {
				isNodeStates.set(childId, true);
			}
			nodeSiblings.set(childId, nodeChilds.get(id));
			nodeLabels.set(childId, keyLabel);
			nodeChilds.set(id, childId);
			nodeStack.add(childId);

			id = childId;
		}
		nodeChilds.set(id, value);
	}
	
	public void clear() {
		nodeChilds.clear();
		nodeSiblings.clear();
		nodeLabels.clear();
		isNodeStates.clear();
		hasNodeSiblings.clear();
		
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
			id = nodeChilds.size();
			nodeChilds.add(0);
			nodeSiblings.add(0);
			nodeLabels.add(0);
			isNodeStates.add(false);
			hasNodeSiblings.add(false);
		} else {
			id = recycleBin.get(recycleBin.size() - 1);
			nodeChilds.set(id, 0);
			nodeSiblings.set(id, 0);
			nodeLabels.set(id, 0);
			isNodeStates.set(id, false);
			hasNodeSiblings.set(id, false);
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
			for (int i = nodeId; i != 0; i = nodeSiblings.get(i)) {
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
				for (int i = nodeId; i != 0; i = nodeSiblings.get(i)) {
					units.set(unitId, nodeUnit(i));
					labels.set(unitId, nodeLabels.get(i));
					unitId--;
				}
				matchId = unitId + 1;
				table.set(hashId[0], matchId);
				numStates++;
			}

			for (int i = nodeId, next; i != 0; i = next) {
				next = nodeSiblings.get(i);
				freeNode(i);
			}

			nodeChilds.set(nodeStack.get(nodeStack.size() - 1), matchId);
		}
		nodeStack.remove(nodeStack.size() - 1);
	}

	private void expandTable() {
		int tableSize = table.size() << 1;
		table.clear();
		table.resize(tableSize);

		for (int i = 1; i < units.size(); i++) {
			int id = i;
			if (labels.get(id) == 0 || isState(id)) {
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
			int unitId = table.get(hashId[0]);
			if (unitId == 0) {
				break;
			}

			if (areEqual(nodeId, unitId)) {
				return unitId;
			}
		}
		return 0;
	}
	
	private void freeNode(int id) {
		recycleBin.add(id);
	}
	
	private boolean areEqual(int nodeId, int unitId) {
		for (int i = nodeSiblings.get(nodeId); i != 0;
				i = nodeSiblings.get(i)) {
			if (!hasSibling(unitId)) {
				return false;
			}
			unitId++;
		}
		if (hasSibling(unitId)) {
			return false;
		}

		for (int i = nodeId; i != 0; i = nodeSiblings.get(i), unitId--) {
			if (nodeUnit(i) != units.get(unitId) ||
					nodeLabels.get(i) != labels.get(unitId)) {
				return false;
			}
		}
		return true;
	}

	private int hashUnit(int id) {
		int hashValue = 0;
		for ( ; id != 0; ++id) {
			int unit = units.get(id);
			int label = labels.get(id);
			hashValue ^= hash((label << 24) ^ unit);

			if (!hasSibling(id)) {
				break;
			}
		}
		return hashValue;
	}
	
	private int hashNode(int id) {
		int hashValue = 0;
		for ( ; id != 0; id = nodeSiblings.get(id)) {
			int unit = nodeUnit(id);
			int label = nodeLabels.get(id);
			hashValue ^= hash((label << 24) ^ unit);
		}
		return hashValue;
	}
	
	private int nodeUnit(int index) {
		if (nodeLabels.get(index) == 0) {
			return (nodeChilds.get(index) << 1) | (hasNodeSiblings.get(index) ? 1 : 0);
		}
		return (nodeChilds.get(index) << 2) | (isNodeStates.get(index) ? 2 : 0) | (hasNodeSiblings.get(index) ? 1 : 0);
	}
}
