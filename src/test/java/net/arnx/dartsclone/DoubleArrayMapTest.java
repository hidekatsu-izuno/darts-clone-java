package net.arnx.dartsclone;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class DoubleArrayMapTest {

	@Test
	void test() throws IOException {
		TreeMap<String, Integer> data = new TreeMap<>();
		data.put("ALGOL", 1);
		data.put("ANSI", 2);
		data.put("ARCO", 3);
		data.put("ARPA", 4);
		data.put("ARPANET", 5);
		data.put("ASCII", 6);
		
		DoubleArrayMap dam = new DoubleArrayMap(data);
		
		try (InputStream in = Files.newInputStream(Paths.get("index.dat"))) {
			dam = DoubleArrayMap.load(in);
		}
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./index2.dat"))) {
			dam.writeTo(out);
		}
		
		assertEquals(1, dam.exactMatchSearch("ALGOL"));
		assertEquals(2, dam.exactMatchSearch("ANSI"));
		assertEquals(3, dam.exactMatchSearch("ARCO"));
		assertEquals(4, dam.exactMatchSearch("ARPA"));
		assertEquals(5, dam.exactMatchSearch("ARPANET"));
		assertEquals(6, dam.exactMatchSearch("ASCII"));
		assertEquals(-1, dam.exactMatchSearch("APPARE"));
	}

}
