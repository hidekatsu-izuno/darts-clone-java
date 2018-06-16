package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.IntList;

public class BitVector {
	private static final int UNIT_SIZE = 32 * 8;
	
	private static int pop_count(int unit) {
		unit = ((unit & 0xAAAAAAAA) >> 1) + (unit & 0x55555555);
		unit = ((unit & 0xCCCCCCCC) >> 2) + (unit & 0x33333333);
		unit = ((unit >> 4) + unit) & 0x0F0F0F0F;
		unit += unit >> 8;
		unit += unit >> 16;
		return unit & 0xFF;
	}
	
	private IntList units = new IntList();
	private IntList ranks = new IntList();
	private int numOnes;
	private int size;
	
	public boolean get(int id) {
		return (units.get(id / UNIT_SIZE) >> (id % UNIT_SIZE) & 1) == 1;
	}
	
	
	public int rank(int id) {
		int unit_id = id / UNIT_SIZE;
		return ranks.get(unit_id) + pop_count(units.get(unit_id)
			& (~0 >> (UNIT_SIZE - (id % UNIT_SIZE) - 1)));
	}
	
	public void set(int id, boolean bit) {
		int unit = units.get(id / UNIT_SIZE);
		if (bit) {
			units.set(id / UNIT_SIZE, unit | (1 << (id % UNIT_SIZE)));
		} else {
			units.set(id / UNIT_SIZE, unit & ~(1 << (id % UNIT_SIZE)));
		}
	}
	
	public boolean isEmpty() {
	    return units.isEmpty();
	}
	
	public int numOnes() {
	    return numOnes;
	}
	
	public int size() {
		return size;
	}
	
	public void add() {
		if ((size % UNIT_SIZE) == 0) {
			units.add(0);
		}
		size++;
	}
	
	public void build() {
		ranks.clear();
		ranks.resize(units.size(), 0);

		numOnes = 0;
		for (int i = 0; i < units.size(); ++i) {
			ranks.set(i, numOnes);
			numOnes += pop_count(units.get(i));
		}
	}
	
	public void clear() {
		units.clear();
		ranks.clear();
		size = 0;
	}
}
