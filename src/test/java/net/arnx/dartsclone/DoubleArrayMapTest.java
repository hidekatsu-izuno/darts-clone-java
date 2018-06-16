package net.arnx.dartsclone;

import static org.junit.jupiter.api.Assertions.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class DoubleArrayMapTest {

	@Test
	void test() throws IOException {
		DoubleArray.Builder dab1 = new DoubleArray.Builder();
		dab1.put("ALGOL".getBytes(UTF_8), 1);
		DoubleArray da1 = dab1.build();
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./data/index1.dat"))) {
			da1.writeTo(out);
		}
		
		DoubleArray.Builder dab2 = new DoubleArray.Builder();
		dab2.put("ALGOL".getBytes(UTF_8), 1);
		dab2.put("ANSI".getBytes(UTF_8), 2);
		dab2.put("ARCO".getBytes(UTF_8), 3);
		dab2.put("ARPA".getBytes(UTF_8), 4);
		dab2.put("ARPANET".getBytes(UTF_8), 5);
		dab2.put("ASCII".getBytes(UTF_8), 6);
		DoubleArray da2 = dab2.build();
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./data/index6.dat"))) {
			da2.writeTo(out);
		}
		
		DoubleArray da3;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test1.dat"))) {
			da3 = DoubleArray.load(in);
		}
		assertEquals(1, da3.get("ALGOL".getBytes(StandardCharsets.UTF_8)));
		assertEquals(-1, da3.get("APPARE".getBytes(StandardCharsets.UTF_8)));
		
		DoubleArray da4;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test6.dat"))) {
			da4 = DoubleArray.load(in);
		}
		assertEquals(1, da4.get("ALGOL".getBytes(UTF_8)));
		assertEquals(2, da4.get("ANSI".getBytes(UTF_8)));
		assertEquals(3, da4.get("ARCO".getBytes(UTF_8)));
		assertEquals(4, da4.get("ARPA".getBytes(UTF_8)));
		assertEquals(5, da4.get("ARPANET".getBytes(UTF_8)));
		assertEquals(6, da4.get("ASCII".getBytes(UTF_8)));
		assertEquals(-1, da4.get("APPARE".getBytes(UTF_8)));
		
		assertArrayEquals(new int[] { 4, 5 }, da4.findByCommonPrefix("ARPANET".getBytes(UTF_8)).toArray());
		
		DoubleArray test1;
		try (InputStream in = Files.newInputStream(Paths.get("./data/test1.dat"))) {
			test1 = DoubleArray.load(in);
		}
		DoubleArray index1;
		try (InputStream in = Files.newInputStream(Paths.get("./data/index1.dat"))) {
			index1 = DoubleArray.load(in);
		}
		assertEquals(test1, index1);
	}
}
