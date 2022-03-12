package com.dmslob.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.dmslob.model.Dish;
import com.dmslob.task.DishWasherTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DishWasherImpl implements DishWasher {

	private final ExecutorService executor;

	public DishWasherImpl(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public Future<Dish> wash(DishWasherTask dishTask) {
		return executor.submit(dishTask);
	}

	public void wash(List<DishWasherTask> dishTasks) {
		try {
			executor.invokeAll(dishTasks);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			log.error("Washing dishes was interrupted due to {}", e.getMessage());
		}
		finally {
			shutdownAndAwaitTermination();
		}
	}

	//Recommended way of shutdown from oracle documentation page of ExecutorService
	//https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
	private void shutdownAndAwaitTermination() {
		executor.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					log.error("Pool did not terminate");
				}
			}
		}
		catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
