package com.luxoft.task;

import java.util.concurrent.RecursiveTask;

public class EvenNumberCounter extends RecursiveTask<Integer> {
	private final int[] array;
	private final int threshold;
	private final int start;
	private final int end;

	public EvenNumberCounter(int[] array, int start, int end, int threshold) {
		this.array = array;
		this.start = start;
		this.end = end;
		this.threshold = threshold;
	}

	@Override
	public Integer compute() {
		if (end - start < threshold) {
			return computeDirectly();
		} else {
			int middle = (end + start) / 2;

			EvenNumberCounter leftSubTask = new EvenNumberCounter(array, start, middle, threshold);
			EvenNumberCounter rightSubTask = new EvenNumberCounter(array, middle, end, threshold);

			invokeAll(leftSubTask, rightSubTask);

			return leftSubTask.join() + rightSubTask.join();
		}
	}

	private Integer computeDirectly() {
		int count = 0;
		for (int i = start; i < end; i++) {
			if (array[i] % 2 == 0) {
				count++;
			}
		}
		return count;
	}
}
