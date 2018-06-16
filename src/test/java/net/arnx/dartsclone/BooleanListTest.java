package net.arnx.dartsclone;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.arnx.dartsclone.util.BooleanList;

class BooleanListTest {

	@Test
	void test() {
		BooleanList list = new BooleanList();
		for (int i = 0; i < 100000; i++) {
			list.add(i % 3 == 0);
		}
		for (int i = 0; i < 100000; i++) {
			assertEquals(i % 3 == 0, list.get(i), "i == " + i);
		}
	}

}
