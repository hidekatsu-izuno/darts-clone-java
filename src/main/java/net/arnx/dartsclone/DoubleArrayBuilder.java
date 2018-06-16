package net.arnx.dartsclone;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.arnx.dartsclone.internal.DawgBuilder;
import net.arnx.dartsclone.internal.DoubleArrayBuilderExtraUnitList;
import net.arnx.dartsclone.internal.DoubleArrayBuilderUnitList;
import net.arnx.dartsclone.util.IntList;

public class DoubleArrayBuilder {
	private static final int BLOCK_SIZE = 256;
	private static final int NUM_EXTRA_BLOCKS = 16;
	private static final int NUM_EXTRAS = BLOCK_SIZE * NUM_EXTRA_BLOCKS;
	private static final int UPPER_MASK = 0xFF << 21;
	private static final int LOWER_MASK = 0xFF;
	
	private Map<byte[], Integer> keyset = new HashMap<>();
	
	private DoubleArrayBuilderUnitList units = new DoubleArrayBuilderUnitList();
	private DoubleArrayBuilderExtraUnitList extras = new DoubleArrayBuilderExtraUnitList();
	private IntList labels = new IntList();
	private IntList table = new IntList();
	private int extrasHead;
	
	public void put(String key, int value) {
		keyset.put(key.getBytes(StandardCharsets.UTF_8), value);
	}
	
	public int[] build() {
		DawgBuilder dawg = new DawgBuilder();
	    buildDawg(dawg);
	    buildFromDawg(dawg);
	    dawg.clear();
	    
	    int[] result = new int[units.size()];
	    for (int i = 0; i < units.size(); i++) {
	    	result[i] = units.get(i);
	    }
	    return result;
	}
	
	public void writeTo(OutputStream out) throws IOException {
		byte[] buf = new byte[4];
		for (int i = 0; i < units.size(); i++) {
			int value = units.get(i);
			buf[0] = (byte)(value & 0xFF);
			buf[1] = (byte)((value >> 8) & 0xFF);
			buf[2] = (byte)((value >> 16) & 0xFF);
			buf[3] = (byte)((value >> 24) & 0xFF);
			out.write(buf);
		}
	}
	
	public void clear() {
		units.clear();
		extras.clear();
		labels.clear();
		table.clear();
		extrasHead = 0;
	}
	
	private void buildDawg(DawgBuilder dawg) {
		TreeMap<byte[], Integer> map = new TreeMap<>((x, y) -> {
			int min = Math.min(x.length, y.length);
	        for (int i = 0; i < min; i++) {
	          int result = (x[i] & 0xFF) - (y[i] & 0xFF);
	          if (result != 0) {
	            return result;
	          }
	        }
	        return x.length - y.length;
		});
		map.putAll(keyset);
		
		dawg.init();
		for (Map.Entry<byte[], Integer> entry : map.entrySet()) {
			dawg.insert(entry.getKey(), entry.getValue());
		}
		dawg.finish();
	}
	
	private void buildFromDawg(DawgBuilder dawg) {
		int num_units = 1;
		while (num_units < dawg.size()) {
			num_units <<= 1;
		}
		units.resize(num_units, 0);

		table.clear();
		table.resize(dawg.numIntersections(), 0);
		for (int i = 0; i < dawg.numIntersections(); ++i) {
			table.set(i, 0);
		}

		extras.clear();
		extras.resize(NUM_EXTRAS, 0, 0, false, false);

		reserveId(0);
		extras.setIsUsed(0, true);
		units.setOffset(0, 1);
		units.setLabel(0, '\0');

		if (dawg.child(dawg.root()) != 0) {
			buildFromDawg(dawg, dawg.root(), 0);
		}

		fixAllBlocks();

		extras.clear();
		labels.clear();
		table.clear();
	}
	
	private void buildFromDawg(DawgBuilder dawg, int dawg_id, int dic_id) {
		int dawg_child_id = dawg.child(dawg_id);
		if (dawg.isIntersection(dawg_child_id)) {
			int intersection_id = dawg.intersectionId(dawg_child_id);
			int offset = table.get(intersection_id);
			if (offset != 0) {
				offset ^= dic_id;
				if ((offset & UPPER_MASK) == 0 || (offset & LOWER_MASK) == 0) {
					if (dawg.isLeaf(dawg_child_id)) {
						units.setHasLeaf(dic_id, true);
					}
					units.setOffset(dic_id, offset);
					return;
				}
			}
		}

		int offset = arrange_from_dawg(dawg, dawg_id, dic_id);
		if (dawg.isIntersection(dawg_child_id)) {
			table.set(dawg.intersectionId(dawg_child_id), offset);
		}

		do {
			int child_label = dawg.label(dawg_child_id);
			int dic_child_id = offset ^ child_label;
			if (child_label != '\0') {
				buildFromDawg(dawg, dawg_child_id, dic_child_id);
			}
			dawg_child_id = dawg.sibling(dawg_child_id);
		} while (dawg_child_id != 0);
	}

	private int arrange_from_dawg(DawgBuilder dawg,
			int dawg_id, int dic_id) {
		labels.clear();

		int dawg_child_id = dawg.child(dawg_id);
		while (dawg_child_id != 0) {
			labels.add(dawg.label(dawg_child_id));
			dawg_child_id = dawg.sibling(dawg_child_id);
		}

		int offset = find_valid_offset(dic_id);
		units.setOffset(dic_id, dic_id ^ offset);

		dawg_child_id = dawg.child(dawg_id);
		for (int i = 0; i < labels.size(); ++i) {
			int dic_child_id = offset ^ labels.get(i);
			reserveId(dic_child_id);

			if (dawg.isLeaf(dawg_child_id)) {
				units.setHasLeaf(dic_id, true);
				units.setValue(dic_child_id, dawg.value(dawg_child_id));
			} else {
				units.setLabel(dic_child_id, labels.get(i));
			}

			dawg_child_id = dawg.sibling(dawg_child_id);
		}
		extras.setIsUsed(offset, true);

		return offset;
	}
	
	private int find_valid_offset(int id) {
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

		for (int i = 1; i < labels.size(); ++i) {
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
		int src_num_units = units.size();
		int src_num_blocks = numBlocks();

		int dest_num_units = src_num_units + BLOCK_SIZE;
		int dest_num_blocks = src_num_blocks + 1;

		if (dest_num_blocks > NUM_EXTRA_BLOCKS) {
			fixBlock(src_num_blocks - NUM_EXTRA_BLOCKS);
		}

		units.resize(dest_num_units, 0);

		if (dest_num_blocks > NUM_EXTRA_BLOCKS) {
			for (int id = src_num_units; id < dest_num_units; ++id) {
				extras.setIsUsed(id, false);
				extras.setIsFixed(id, false);
			}
		}

		for (int i = src_num_units + 1; i < dest_num_units; ++i) {
			extras.setNext(i - 1, i);
			extras.setPrev(i, i - 1);
		}

		extras.setPrev(src_num_units, dest_num_units - 1);
		extras.setNext(dest_num_units - 1, src_num_units);

		extras.setPrev(src_num_units, extras.prev(extrasHead));
		extras.setNext(dest_num_units - 1, extrasHead);

		extras.setNext(extras.prev(extrasHead), src_num_units);
		extras.setPrev(extrasHead, dest_num_units - 1);
	}
	
	private int numBlocks() {
		return units.size() / BLOCK_SIZE;
	}
	
	private void fixBlock(int blockId) {
		int begin = blockId * BLOCK_SIZE;
		int end = begin + BLOCK_SIZE;

		int unused_offset = 0;
		for (int offset = begin; offset != end; ++offset) {
			if (!extras.isUsed(offset)) {
				unused_offset = offset;
				break;
			}
		}

		for (int id = begin; id != end; ++id) {
			if (!extras.isFixed(id)) {
				reserveId(id);
				units.setLabel(id, id ^ unused_offset);
			}
		}
	}
	
	private void fixAllBlocks() {
		int begin = 0;
		if (numBlocks() > NUM_EXTRA_BLOCKS) {
			begin = numBlocks() - NUM_EXTRA_BLOCKS;
		}
		int end = numBlocks();

		for (int block_id = begin; block_id != end; ++block_id) {
			fixBlock(block_id);
		}
	}
}
