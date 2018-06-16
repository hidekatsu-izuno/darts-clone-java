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
