package net.arnx.dartsclone;

import static org.junit.jupiter.api.Assertions.*;

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
		DoubleArrayBuilder dab = new DoubleArrayBuilder();
		dab.put("ALGOL", 1);
		dab.put("ANSI", 2);
		dab.put("ARCO", 3);
		dab.put("ARPA", 4);
		dab.put("ARPANET", 5);
		dab.put("ASCII", 6);
		dab.build();
		
		try (OutputStream out = Files.newOutputStream(Paths.get("./data/index_new.dat"))) {
			dab.writeTo(out);
		}
		
		DoubleArray da;
		try (InputStream in = Files.newInputStream(Paths.get("./data/index.dat"))) {
			da = DoubleArray.load(in);
		}
				
		assertEquals(1, da.exactMatchSearch("ALGOL".getBytes(StandardCharsets.UTF_8)));
		assertEquals(2, da.exactMatchSearch("ANSI".getBytes(StandardCharsets.UTF_8)));
		assertEquals(3, da.exactMatchSearch("ARCO".getBytes(StandardCharsets.UTF_8)));
		assertEquals(4, da.exactMatchSearch("ARPA".getBytes(StandardCharsets.UTF_8)));
		assertEquals(5, da.exactMatchSearch("ARPANET".getBytes(StandardCharsets.UTF_8)));
		assertEquals(6, da.exactMatchSearch("ASCII".getBytes(StandardCharsets.UTF_8)));
		assertEquals(-1, da.exactMatchSearch("APPARE".getBytes(StandardCharsets.UTF_8)));
	}

}
