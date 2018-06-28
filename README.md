[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.arnx/darts-clone-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.arnx/darts-clone-java)

# darts-clone-java

Double-ARray Trie System clone written in Java.

This library is based on [Darts Clone](https://github.com/s-yata/darts-clone).

## Getting Started

### Setup

To add a dependency using Maven, use the following:

```xml
<dependency>
  <groupId>net.arnx</groupId>
  <artifactId>darts-clone-java</artifactId>
  <version>0.6.0</version>
</dependency>
```

### Usage

```java
import net.arnx.dartsclone.DoubleArrayTrie;

// Build index
DoubleArrayTrie.Builder dab = new DoubleArrayTrie.Builder();
dab.put("ALGOL", 1);
dab.put("ANSI", 2);
dab.put("ARCO", 3);
dab.put("ARPA", 4);
dab.put("ARPANET", 5);
dab.put("ASCII", 6);
DoubleArrayTrie da = dab2.build();

// Gets value by searching in the index
int value = da.get("ALGOL");

// Search values by searching a common prefix
IntStream values = da.findByCommonPrefix("ARPANET");

// Write the index to a file
try (OutputStream out = Files.newOutputStream(Paths.get("./index.dat"))) {
    da.writeTo(out);
}

// Read the index from the file
try (InputStream in = Files.newInputStream(Paths.get("./index.dat"))) {
    da = DoubleArrayTrie.load(in);
}
```

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details
