package com.luxoft.service;

import java.util.List;
import java.util.concurrent.Future;

import com.luxoft.model.Dish;
import com.luxoft.task.DishWasherTask;

public interface DishWasher {
	void wash(List<DishWasherTask> dishTasks);

	Future<Dish> wash(DishWasherTask dishTask);
}
