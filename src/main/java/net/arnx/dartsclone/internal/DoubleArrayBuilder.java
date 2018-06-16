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
	
	private DoubleArrayBuilderUnitList units = new DoubleArrayBuilderUnitList();
	private DoubleArrayBuilderExtraUnitList extras = new DoubleArrayBuilderExtraUnitList();
	private IntList labels = new IntList();
	private IntList table = new IntList();
	private int extrasHead;
	
	public int[] build(List<DoubleArrayEntry> keyset) {
		DawgBuilder dawg = new DawgBuilder();
	    buildDawg(keyset, dawg);
	    dawg.dump();
	    buildFromDawg(dawg);
	    dawg.clear();
	    
	    this.dump();
	    
	    return units.toArray();
	}
	
	public void clear() {
		units.clear();
		extras.clear();
		labels.clear();
		table.clear();
		extrasHead = 0;
	}
	
	public void dump() {
		System.out.print("DoubleArrayBuilder.java: { ");
		System.out.print("units: " + units.list.toHexString() + ", ");
		/*
		System.out.print("extras: { ");
		System.out.print("prevs: " + extras.prevs.toHexString() + ", ");
		System.out.print("nexts: " + extras.nexts.toHexString() + ", ");
		System.out.print("isFixeds: " + extras.isFixeds.toBinaryString() + ", ");
		System.out.print("isUseds: " + extras.isUseds.toBinaryString() + " ");
		System.out.print("},");
		*/
		System.out.print("labels: " + labels.toHexString() + ", ");
		System.out.print("table: " + table.toHexString() + ", ");
		System.out.print("extrasHead: " + extrasHead + " ");
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
	    this.dump();

		table.clear();
		for (int i = 0; i < dawg.numIntersections(); i++) {
			table.add(0);
		}

		extras.clear();
		extras.resize(NUM_EXTRAS);

		reserveId(0);
		extras.setIsUsed(0, true);
		units.setOffset(0, 1);
		units.setLabel(0, '\0');
		
	    this.dump();

		if (dawg.child(dawg.root()) != 0) {
			buildFromDawg(dawg, dawg.root(), 0);
		}
		
	    this.dump();

		fixAllBlocks();

		extras.clear();
		labels.clear();
		table.clear();
	}
	
	private void buildFromDawg(DawgBuilder dawg, int dawgId, int dicId) {
		int dawgChildId = dawg.child(dawgId);
		if (dawg.isIntersection(dawgChildId)) {
			int intersection_id = dawg.intersectionId(dawgChildId);
			int offset = table.get(intersection_id);
			if (offset != 0) {
				offset ^= dicId;
				if ((offset & UPPER_MASK) == 0 || (offset & LOWER_MASK) == 0) {
					if (dawg.isLeaf(dawgChildId)) {
						units.setHasLeaf(dicId, true);
					}
					units.setOffset(dicId, offset);
					return;
				}
			}
		}

		int offset = arrangeFromDawg(dawg, dawgId, dicId);
		if (dawg.isIntersection(dawgChildId)) {
			table.set(dawg.intersectionId(dawgChildId), offset);
		}

		do {
			int child_label = dawg.label(dawgChildId);
			int dic_child_id = offset ^ child_label;
			if (child_label != '\0') {
				buildFromDawg(dawg, dawgChildId, dic_child_id);
			}
			dawgChildId = dawg.sibling(dawgChildId);
		} while (dawgChildId != 0);
	}

	private int arrangeFromDawg(DawgBuilder dawg, int dawgId, int dicId) {
		labels.clear();

		int dawgChildId = dawg.child(dawgId);
		while (dawgChildId != 0) {
			labels.add(dawg.label(dawgChildId));
			dawgChildId = dawg.sibling(dawgChildId);
		}

		int offset = findValidOffset(dicId);
		units.setOffset(dicId, dicId ^ offset);

		dawgChildId = dawg.child(dawgId);
		for (int i = 0; i < labels.size(); i++) {
			int dicChildId = offset ^ labels.get(i);
			reserveId(dicChildId);

			if (dawg.isLeaf(dawgChildId)) {
				units.setHasLeaf(dicId, true);
				units.setValue(dicChildId, dawg.value(dawgChildId));
			} else {
				units.setLabel(dicChildId, labels.get(i));
			}

			dawgChildId = dawg.sibling(dawgChildId);
		}
		extras.setIsUsed(offset, true);

		return offset;
	}
	
	private int findValidOffset(int id) {
		if (extrasHead >= units.size()) {
			return units.size() | (id & LOWER_MASK);
		}

		int unfixed_id = extrasHead;
		do {
			int offset = unfixed_id ^ labels.get(0);
			if (isValidOffset(id, offset)) {
				return offset;
			}
			unfixed_id = extras.next(unfixed_id);
		} while (unfixed_id != extrasHead);

		return units.size() | (id & LOWER_MASK);
	}
	
	private boolean isValidOffset(int id, int offset) {
		if (extras.isUsed(offset)) {
			return false;
		}

		int rel_offset = id ^ offset;
		if ((rel_offset & LOWER_MASK) != 0 && (rel_offset & UPPER_MASK) != 0) {
			return false;
		}

		for (int i = 1; i < labels.size(); i++) {
			if (extras.isFixed(offset ^ labels.get(i))) {
				return false;
			}
		}

		return true;
	}
	
	private void reserveId(int id) {
		if (id >= units.size()) {
			expandUnits();
		}

		if (id == extrasHead) {
			extrasHead = extras.next(id);
			if (extrasHead == id) {
				extrasHead = units.size();
			}
		}
		extras.setNext(extras.prev(id), extras.next(id));
		extras.setPrev(extras.next(id), extras.prev(id));
		extras.setIsFixed(id, true);
	}
	
	private void expandUnits() {
		int srcNumUnits = units.size();
		int srcNumlocks = numBlocks();

		int destNumUnits = srcNumUnits + BLOCK_SIZE;
		int destNumBlocks = srcNumlocks + 1;

		if (destNumBlocks > NUM_EXTRA_BLOCKS) {
			fixBlock(srcNumlocks - NUM_EXTRA_BLOCKS);
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

		extras.setPrev(srcNumUnits, extras.prev(extrasHead));
		extras.setNext(destNumUnits - 1, extrasHead);

		extras.setNext(extras.prev(extrasHead), srcNumUnits);
		extras.setPrev(extrasHead, destNumUnits - 1);
	}
	
	private int numBlocks() {
		return units.size() / BLOCK_SIZE;
	}
	
	private void fixBlock(int blockId) {
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
				reserveId(id);
				units.setLabel(id, id ^ unusedOffset);
			}
		}
	}
	
	private void fixAllBlocks() {
		int begin = 0;
		if (numBlocks() > NUM_EXTRA_BLOCKS) {
			begin = numBlocks() - NUM_EXTRA_BLOCKS;
		}
		int end = numBlocks();

		for (int blockId = begin; blockId != end; blockId++) {
			fixBlock(blockId);
		}
	}
}
