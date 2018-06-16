package net.arnx.dartsclone.internal;

import net.arnx.dartsclone.util.IntList;

public class BitVector {
	private static final int UNIT_SIZE = 32 * 8;
	
	private static int popCount(int unit) {
		unit = ((unit & 0xAAAAAAAA) >> 1) + (unit & 0x55555555);
		unit = ((unit & 0xCCCCCCCC) >> 2) + (unit & 0x33333333);
		unit = ((unit >> 4) + unit) & 0x0F0F0F0F;
		unit += unit >> 8;
		unit += unit >> 16;
		return unit & 0xFF;
	}
	
	IntList units = new IntList();
	IntList ranks = new IntList();
	int numOnes;
	int size;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("units: ").append(units.toHexString()).append(", ");
		sb.append("ranks: ").append(ranks.toHexString()).append(", ");
		sb.append("numOnes: ").append(numOnes).append(", ");
		sb.append("size: ").append(size);
		return sb.toString();
	}
	
	public boolean get(int id) {
		return (units.get(id / UNIT_SIZE) >> (id % UNIT_SIZE) & 1) == 1;
	}
	
	public int rank(int id) {
		int unitId = id / UNIT_SIZE;
		return ranks.get(unitId) + popCount(units.get(unitId)
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
		ranks.resize(units.size());

		numOnes = 0;
		for (int i = 0; i < units.size(); ++i) {
			ranks.set(i, numOnes);
			numOnes += popCount(units.get(i));
		}
	}
	
	public void clear() {
		units.clear();
		ranks.clear();
		size = 0;
	}
}
