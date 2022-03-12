package com.dmslob.utils;

public class DishWashUtil {
	private DishWashUtil() {
	}

	public static void takeTimeToWork(long millis) {
		long startedAt = System.currentTimeMillis();
		while ((System.currentTimeMillis() - startedAt) <= millis) {
		}
	}
}
