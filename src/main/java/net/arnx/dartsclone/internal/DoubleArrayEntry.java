package net.arnx.dartsclone.internal;

public class DoubleArrayEntry {
	private byte[] key;
	private int value;
	
	public DoubleArrayEntry(byte[] key, int value) {
		this.key = key;
		this.value = value;
	}
	
	public byte[] key() {
		return key;
	}
	
	public int value() {
		return value;
	}
}
