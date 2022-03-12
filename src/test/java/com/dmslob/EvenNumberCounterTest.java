package com.dmslob;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import org.testng.annotations.Test;

import com.dmslob.task.EvenNumberCounter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EvenNumberCounterTest {
	private static final int SIZE = 10_000_000;
	private static final int THRESHOLD = 1000;

	@Test
	public void should_count_even_numbers_by_ForkJoinPool() {
		// given
		int[] array = givenRandomArray();
		EvenNumberCounter evenNumberCounter =
				new EvenNumberCounter(array, 0, SIZE, THRESHOLD);
		ForkJoinPool pool = new ForkJoinPool();

		// when
		Integer evenNumberCount = pool.invoke(evenNumberCounter);

		// then
		assertThat(evenNumberCount).isPositive();
	}

	private static int[] givenRandomArray() {
		int[] array = new int[SIZE];
		Random random = new Random();
		for (int i = 0; i < SIZE; i++) {
			array[i] = random.nextInt(100);
		}
		return array;
	}
}
