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
	
	private DawgNodeList nodes = new DawgNodeList();
	private DawgUnitList units = new DawgUnitList();
	private IntList labels = new IntList();
	private BitVector isIntersections = new BitVector();
	private IntList table = new IntList();
	private IntList nodeStack = new IntList();
	private IntList recycleBin = new IntList();
	private int numStates;
	
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
		return label(id) == '\0';
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

		nodes.setLabel(0, '\u00FF');
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
		int key_pos = 0;

		for ( ; key_pos <= key.length; ++key_pos) {
			int child_id = nodes.child(id);
			if (child_id == 0) {
				break;
			}

			int key_label = key[key_pos] & 0xFF;
			if (key_pos < key.length && key_label == 0) {
				throw new IllegalStateException("failed to insert key: invalid null character");
			}

			int unit_label = nodes.label(child_id);
			if (key_label < unit_label) {
				throw new IllegalStateException("failed to insert key: wrong key order");
			} else if (key_label > unit_label) {
				nodes.setHasSibling(child_id, true);
				flush(child_id);
				break;
			}
			id = child_id;
		}

		if (key_pos > key.length) {
			return;
		}

		for ( ; key_pos <= key.length; ++key_pos) {
			int key_label = (key_pos < key.length) ? (key[key_pos] & 0xFF) : 0;
			int child_id = appendNode();

			if (nodes.child(id) == 0) {
				nodes.setIsState(child_id, true);
			}
			nodes.setSibling(child_id, nodes.child(id));
			nodes.setLabel(child_id, key_label);
			nodes.setChild(id, child_id);
			nodeStack.add(child_id);

			id = child_id;
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
			nodes.add(0, 0, '\0', false, false);
		} else {
			id = recycleBin.get(recycleBin.size() - 1);
			nodes.set(id, 0, 0, '\0', false, false);
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
			int node_id = nodeStack.get(nodeStack.size() - 1);
			nodeStack.remove(nodeStack.size() - 1);

			if (numStates >= table.size() - (table.size() >> 2)) {
				expandTable();
			}

			int num_siblings = 0;
			for (int i = node_id; i != 0; i = nodes.sibling(i)) {
				++num_siblings;
			}

			int[] hash_id = new int[1];
			int match_id = find_node(node_id, hash_id);
			if (match_id != 0) {
				isIntersections.set(match_id, true);
			} else {
				int unit_id = 0;
				for (int i = 0; i < num_siblings; ++i) {
					unit_id = appendUnit();
				}
				for (int i = node_id; i != 0; i = nodes.sibling(i)) {
					units.set(unit_id, nodes.unit(i));
					labels.set(unit_id, nodes.label(i));
					--unit_id;
				}
				match_id = unit_id + 1;
				table.set(hash_id[0], match_id);
				++numStates;
			}

			for (int i = node_id, next; i != 0; i = next) {
				next = nodes.sibling(i);
				free_node(i);
			}

			nodes.setChild(nodeStack.get(nodeStack.size() - 1), match_id);
		}
		nodeStack.remove(nodeStack.size() - 1);
	}

	private void expandTable() {
		int table_size = table.size() << 1;
		table.clear();
		table.resize(table_size, 0);

		for (int i = 1; i < units.size(); ++i) {
			int id = i;
			if (labels.get(id) == 0 || units.isState(id)) {
				int[] hash_id = new int[1];
				find_unit(id, hash_id);
				table.set(hash_id[0], id);
			}
		}
	}
	
	private int find_unit(int id, int[] hash_id) {
		hash_id[0] = hash_unit(id) % table.size();
		for ( ; ; hash_id[0] = (hash_id[0] + 1) % table.size()) {
			int unit_id = table.get(hash_id[0]);
			if (unit_id == 0) {
				break;
			}

			// There must not be the same unit.
		}
		return 0;
	}

	private int find_node(int node_id, int[] hash_id) {
		hash_id[0] = hash_node(node_id) % table.size();
		for ( ; ; hash_id[0] = (hash_id[0] + 1) % table.size()) {
			int unit_id = table.get(hash_id[0]);
			if (unit_id == 0) {
				break;
			}

			if (are_equal(node_id, unit_id)) {
				return unit_id;
			}
		}
		return 0;
	}
	
	private void free_node(int id) {
		recycleBin.add(id);
	}
	
	private boolean are_equal(int node_id, int unit_id) {
		for (int i = nodes.sibling(node_id); i != 0;
				i = nodes.sibling(i)) {
			if (units.hasSibling(unit_id) == false) {
				return false;
			}
			++unit_id;
		}
		if (units.hasSibling(unit_id) == true) {
			return false;
		}

		for (int i = node_id; i != 0; i = nodes.sibling(i), --unit_id) {
			if (nodes.unit(i) != units.unit(unit_id) ||
					nodes.label(i) != labels.get(unit_id)) {
				return false;
			}
		}
		return true;
	}

	private int hash_unit(int id) {
		int hash_value = 0;
		for ( ; id != 0; ++id) {
			int unit = units.unit(id);
			int label = labels.get(id);
			hash_value ^= hash((label << 24) ^ unit);

			if (units.hasSibling(id) == false) {
				break;
			}
		}
		return hash_value;
	}
	
	private int hash_node(int id) {
		int hash_value = 0;
		for ( ; id != 0; id = nodes.sibling(id)) {
			int unit = nodes.unit(id);
			int label = nodes.label(id);
			hash_value ^= hash((label << 24) ^ unit);
		}
		return hash_value;
	}
}
