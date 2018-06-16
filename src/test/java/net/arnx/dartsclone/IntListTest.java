package net.arnx.dartsclone;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.arnx.dartsclone.util.IntList;

class IntListTest {

	@Test
	void test() {
		IntList list = new IntList();
		for (int i = 0; i < 100000; i++) {
			list.add(i);
		}
		for (int i = 0; i < 100000; i++) {
			assertEquals(i, list.get(i));
		}
	}

}
