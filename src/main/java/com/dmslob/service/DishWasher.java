package com.dmslob.service;

import java.util.List;
import java.util.concurrent.Future;

import com.dmslob.model.Dish;
import com.dmslob.task.DishWasherTask;

public interface DishWasher {
	void wash(List<DishWasherTask> dishTasks);

	Future<Dish> wash(DishWasherTask dishTask);
}
