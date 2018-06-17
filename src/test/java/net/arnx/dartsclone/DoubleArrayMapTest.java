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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class DoubleArrayMapTest {

	@Test
	void test() throws IOException {
		DoubleArrayTrie.Builder dab1 = new DoubleArrayTrie.Builder();
		dab1.put("ALGOL", 1);
		DoubleArrayTrie da1 = dab1.build();
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./data/index1.dat"))) {
			da1.writeTo(out);
		}
		
		DoubleArrayTrie.Builder dab2 = new DoubleArrayTrie.Builder();
		dab2.put("ALGOL", 1);
		dab2.put("ANSI", 2);
		dab2.put("ARCO", 3);
		dab2.put("ARPA", 4);
		dab2.put("ARPANET", 5);
		dab2.put("ASCII", 6);
		DoubleArrayTrie da2 = dab2.build();
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./data/index6.dat"))) {
			da2.writeTo(out);
		}
		
		DoubleArrayTrie da3;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test1.dat"))) {
			da3 = DoubleArrayTrie.load(in);
		}
		assertEquals(1, da3.get("ALGOL"));
		assertEquals(-1, da3.get("APPARE"));
		
		DoubleArrayTrie da4;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test6.dat"))) {
			da4 = DoubleArrayTrie.load(in);
		}
		assertEquals(1, da4.get("ALGOL"));
		assertEquals(2, da4.get("ANSI"));
		assertEquals(3, da4.get("ARCO"));
		assertEquals(4, da4.get("ARPA"));
		assertEquals(5, da4.get("ARPANET"));
		assertEquals(6, da4.get("ASCII"));
		assertEquals(-1, da4.get("APPARE"));
		
		assertArrayEquals(new int[] { 4, 5 }, da4.findByCommonPrefix("ARPANET").toArray());
		
		DoubleArrayTrie test1;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test1.dat"))) {
			test1 = DoubleArrayTrie.load(in);
		}
		DoubleArrayTrie index1;
		try (InputStream in = Files.newInputStream(Paths.get("./data/index1.dat"))) {
			index1 = DoubleArrayTrie.load(in);
		}
		assertEquals(test1, index1);
	}
}
