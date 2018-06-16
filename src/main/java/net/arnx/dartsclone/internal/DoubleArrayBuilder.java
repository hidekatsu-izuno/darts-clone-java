package net.arnx.dartsclone.internal;

import java.util.Collections;
import java.util.List;
import net.arnx.dartsclone.util.IntList;

public class DoubleArrayBuilder {
	private static final int BLOCK_SIZE = 256;
	private static final int NUM_EXTRA_BLOCKS = 16;
	private static final int NUM_EXTRAS = BLOCK_SIZE * NUM_EXTRA_BLOCKS;
	private static final int UPPER_MASK = 0xFF << 21;
	private static final int LOWER_MASK = 0xFF;
	
	private IntList units = new IntList();
	
	public int[] build(List<DoubleArrayEntry> keyset) {
		DawgBuilder dawg = new DawgBuilder();
	    buildDawg(keyset, dawg);
	    buildFromDawg(dawg);
	    dawg.clear();
	    
	    return units.toArray();
	}
	
	public void clear() {
		units.clear();
	}
	
	public void dump() {
		System.out.print("DoubleArrayBuilder.java: { ");
		System.out.print("units: " + units.toHexString() + " ");
		System.out.println("}");
	}
	
	private void buildDawg(List<DoubleArrayEntry> keyset, DawgBuilder dawg) {
		Collections.sort(keyset, (x, y) -> {
			byte[] xkey = x.key();
			byte[] ykey = y.key();
			int min = Math.min(xkey.length, ykey.length);
	        for (int i = 0; i < min; i++) {
	          int result = (xkey[i] & 0xFF) - (ykey[i] & 0xFF);
	          if (result != 0) {
	            return result;
	          }
	        }
	        return xkey.length - ykey.length;
		});
		
		dawg.init();
		for (DoubleArrayEntry entry : keyset) {
			dawg.insert(entry.key(), entry.value());
		}
		dawg.finish();
	}
	
	private void buildFromDawg(DawgBuilder dawg) {
		IntList table = new IntList(dawg.numIntersections());

		DoubleArrayBuilderExtraUnitList extras = new DoubleArrayBuilderExtraUnitList(NUM_EXTRAS);

		reserveId(0, extras);
		extras.setIsUsed(0, true);
		setOffset(0, 1);
		setLabel(0, 0);

		if (dawg.child(dawg.root()) != 0) {
			buildFromDawg(dawg, dawg.root(), 0, table, extras);
		}

		fixAllBlocks(extras);
	}
	
	private void buildFromDawg(DawgBuilder dawg, int dawgId, int dicId, IntList table, 
			DoubleArrayBuilderExtraUnitList extras) {
		int dawgChildId = dawg.child(dawgId);
		if (dawg.isIntersection(dawgChildId)) {
			int intersectionId = dawg.intersectionId(dawgChildId);
			int offset = table.get(intersectionId);
			if (offset != 0) {
				offset ^= dicId;
				if ((offset & UPPER_MASK) == 0 || (offset & LOWER_MASK) == 0) {
					if (dawg.isLeaf(dawgChildId)) {
						setHasLeaf(dicId, true);
					}
					setOffset(dicId, offset);
					return;
				}
			}
		}

		int offset = arrangeFromDawg(dawg, dawgId, dicId, extras);
		if (dawg.isIntersection(dawgChildId)) {
			table.set(dawg.intersectionId(dawgChildId), offset);
		}

		do {
			int childLabel = dawg.label(dawgChildId);
			int dicChildId = offset ^ childLabel;
			if (childLabel != '\0') {
				buildFromDawg(dawg, dawgChildId, dicChildId, table, extras);
			}
			dawgChildId = dawg.sibling(dawgChildId);
		} while (dawgChildId != 0);
	}

	private int arrangeFromDawg(DawgBuilder dawg, int dawgId, int dicId,
			DoubleArrayBuilderExtraUnitList extras) {
		IntList labels = new IntList();

		int dawgChildId = dawg.child(dawgId);
		while (dawgChildId != 0) {
			labels.add(dawg.label(dawgChildId));
			dawgChildId = dawg.sibling(dawgChildId);
		}

		int offset = findValidOffset(dicId, labels, extras);
		setOffset(dicId, dicId ^ offset);

		dawgChildId = dawg.child(dawgId);
		for (int i = 0; i < labels.size(); i++) {
			int dicChildId = offset ^ labels.get(i);
			reserveId(dicChildId, extras);

			if (dawg.isLeaf(dawgChildId)) {
				setHasLeaf(dicId, true);
				setValue(dicChildId, dawg.value(dawgChildId));
			} else {
				setLabel(dicChildId, labels.get(i));
			}

			dawgChildId = dawg.sibling(dawgChildId);
		}
		extras.setIsUsed(offset, true);

		return offset;
	}
	
	private int findValidOffset(int id, IntList labels,
			DoubleArrayBuilderExtraUnitList extras) {
		if (extras.head() >= units.size()) {
			return units.size() | (id & LOWER_MASK);
		}

		int unfixedId = extras.head();
		do {
			int offset = unfixedId ^ labels.get(0);
			if (isValidOffset(id, offset, labels, extras)) {
				return offset;
			}
			unfixedId = extras.next(unfixedId);
		} while (unfixedId != extras.head());

		return units.size() | (id & LOWER_MASK);
	}
	
	private boolean isValidOffset(int id, int offset, IntList labels,
			DoubleArrayBuilderExtraUnitList extras) {
		if (extras.isUsed(offset)) {
			return false;
		}

		int relOffset = id ^ offset;
		if ((relOffset & LOWER_MASK) != 0 && (relOffset & UPPER_MASK) != 0) {
			return false;
		}

		for (int i = 1; i < labels.size(); i++) {
			if (extras.isFixed(offset ^ labels.get(i))) {
				return false;
			}
		}

		return true;
	}
	
	private void reserveId(int id, DoubleArrayBuilderExtraUnitList extras) {
		if (id >= units.size()) {
			expandUnits(extras);
		}

		if (id == extras.head()) {
			extras.setHead(extras.next(id));
			if (extras.head() == id) {
				extras.setHead(units.size());
			}
		}
		extras.setNext(extras.prev(id), extras.next(id));
		extras.setPrev(extras.next(id), extras.prev(id));
		extras.setIsFixed(id, true);
	}
	
	private void expandUnits(DoubleArrayBuilderExtraUnitList extras) {
		int srcNumUnits = units.size();
		int srcNumlocks = numBlocks();

		int destNumUnits = srcNumUnits + BLOCK_SIZE;
		int destNumBlocks = srcNumlocks + 1;

		if (destNumBlocks > NUM_EXTRA_BLOCKS) {
			fixBlock(srcNumlocks - NUM_EXTRA_BLOCKS, extras);
		}

		units.resize(destNumUnits);

		if (destNumBlocks > NUM_EXTRA_BLOCKS) {
			for (int id = srcNumUnits; id < destNumUnits; id++) {
				extras.setIsUsed(id, false);
				extras.setIsFixed(id, false);
			}
		}

		for (int i = srcNumUnits + 1; i < destNumUnits; i++) {
			extras.setNext(i - 1, i);
			extras.setPrev(i, i - 1);
		}

		extras.setPrev(srcNumUnits, destNumUnits - 1);
		extras.setNext(destNumUnits - 1, srcNumUnits);

		extras.setPrev(srcNumUnits, extras.prev(extras.head()));
		extras.setNext(destNumUnits - 1, extras.head());

		extras.setNext(extras.prev(extras.head()), srcNumUnits);
		extras.setPrev(extras.head(), destNumUnits - 1);
	}
	
	private int numBlocks() {
		return units.size() / BLOCK_SIZE;
	}
	
	private void fixBlock(int blockId, DoubleArrayBuilderExtraUnitList extras) {
		int begin = blockId * BLOCK_SIZE;
		int end = begin + BLOCK_SIZE;

		int unusedOffset = 0;
		for (int offset = begin; offset != end; offset++) {
			if (!extras.isUsed(offset)) {
				unusedOffset = offset;
				break;
			}
		}

		for (int id = begin; id != end; id++) {
			if (!extras.isFixed(id)) {
				reserveId(id, extras);
				setLabel(id, id ^ unusedOffset);
			}
		}
	}
	
	private void fixAllBlocks(DoubleArrayBuilderExtraUnitList extras) {
		int begin = 0;
		if (numBlocks() > NUM_EXTRA_BLOCKS) {
			begin = numBlocks() - NUM_EXTRA_BLOCKS;
		}
		int end = numBlocks();

		for (int blockId = begin; blockId != end; blockId++) {
			fixBlock(blockId, extras);
		}
	}
	
	
	private int setHasLeaf(int index, boolean hasLeaf) {
		int unit = units.get(index);
		if (hasLeaf) {
			unit |= 1 << 8;
		} else {
			unit &= ~(1 << 8);
		}
		units.set(index, unit);
		return unit;
	}
	
	public int setValue(int index, int value) {
		int unit = units.get(index);
		unit = value | (1 << 31);
		units.set(index, unit);
		return unit;
	}
	
	public int setLabel(int index, int label) {
		int unit = units.get(index);
		unit = (unit & ~0xFF) | label;
		units.set(index, unit);
		return unit;
	}
	
	public int setOffset(int index, int offset) {
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
		return unit;
	}
}
