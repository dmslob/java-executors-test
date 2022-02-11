package com.luxoft.task;

import static com.luxoft.utils.DishWashUtil.takeTimeToWork;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import com.luxoft.model.Dish;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DishWasherForkJoinTask extends RecursiveAction {

	private static final int SEQ_DISH_NUMBER = 10;
	private static final long TIME_TO_WASH_ONE_DISH = 1000L;
	private static final Set<String> workers = new HashSet<>();

	private final List<Dish> dishes;

	public DishWasherForkJoinTask(List<Dish> dishes) {
		this.dishes = dishes;
	}

	@Override
	protected void compute() {
		if (dishes.size() <= SEQ_DISH_NUMBER) {
			log.info("Wash dishes sequentially\n");
			wash();
		} else {
			log.info("Fork process\n");

			final List<Dish> dishesLeft = dishes.subList(0, dishes.size() / 2);
			final List<Dish> dishesRight = dishes.subList(dishes.size() / 2, dishes.size());

			final DishWasherForkJoinTask taskLeft = subTask(dishesLeft);
			final DishWasherForkJoinTask taskRight = subTask(dishesRight);

			invokeAll(taskLeft, taskRight);
			//taskLeft.fork(); taskRight.fotrk();
		}
	}

	private void wash() {
		dishes.forEach(dish -> {
			takeTimeToWork(TIME_TO_WASH_ONE_DISH);
			String worker = Thread.currentThread().getName();
			workers.add(worker);
			log.info("[{}] has been washed by [{}-Daemon={}]\n",
					dish.getName(),
					worker,
					Thread.currentThread().isDaemon()
			);
			dish.setWashed(true);
		});
	}

	public static Set<String> getWorkers() {
		return workers;
	}

	private DishWasherForkJoinTask subTask(List<Dish> dishes) {
		return new DishWasherForkJoinTask(dishes);
	}
}
