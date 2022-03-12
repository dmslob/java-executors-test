package com.dmslob.task;

import static com.dmslob.utils.DishWashUtil.takeTimeToWork;

import java.util.concurrent.Callable;

import com.dmslob.model.Dish;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DishWasherTask implements Callable<Dish> {

	private static final long TIME_TO_WASH_ONE_DISH = 3000L;

	private final Dish dish;

	public DishWasherTask(Dish dish) {
		this.dish = dish;
	}

	public boolean isDone() {
		return dish.isWashed();
	}

	@Override
	public Dish call() {
		return wash();
	}

	private Dish wash() {
		takeTimeToWork(TIME_TO_WASH_ONE_DISH);

		log.info("{} has been washed by {}",
				dish.getName(), Thread.currentThread().getName());

		dish.setWashed(true);

		return dish;
	}
}
