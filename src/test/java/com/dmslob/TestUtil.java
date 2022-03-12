package com.dmslob;

public class TestUtil {

	public static void delay(long millis) {
		long startedAt = System.currentTimeMillis();
		while ((System.currentTimeMillis() - startedAt) <= millis) {
		}
	}
}
