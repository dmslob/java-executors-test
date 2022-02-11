package com.luxoft;

import static com.luxoft.TestUtil.delay;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.luxoft.executors.MonitoringThreadPoolExecutor;
import com.luxoft.executors.ThreadExecutor;
import com.luxoft.model.Dish;
import com.luxoft.service.DishWasher;
import com.luxoft.service.DishWasherImpl;
import com.luxoft.service.DishWasherRejectedExecutionHandler;
import com.luxoft.task.DishWasherForkJoinTask;
import com.luxoft.task.DishWasherTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DishWasherTest {

	private static final ThreadFactory THREAD_FACTORY =
			new ThreadFactoryBuilder()
					.setNameFormat("[Dish-Washer-Worker-%d]")
					.build();

	@Test
	public void should_execute_new_threads() {
		// given
		ThreadExecutor executor = new ThreadExecutor();
		// when
		executor.execute(() -> log.info("{}", Thread.currentThread().getName()));
		executor.execute(() -> log.info("{}", Thread.currentThread().getName()));
		executor.execute(() -> log.info("{}", Thread.currentThread().getName()));
		executor.execute(() -> log.info("{}", Thread.currentThread().getName()));

		delay(1000L);
	}

	@Test
	public void should_not_cast_when_newSingleThreadExecutor() {
		// given | when | then
		assertThatThrownBy(() -> {
			ThreadPoolExecutor singleThreadPool =
					(ThreadPoolExecutor) Executors.newSingleThreadExecutor();
		}).isInstanceOf(ClassCastException.class);

		final ExecutorService executor = Executors.newSingleThreadExecutor();
	}

	@Test
	public void should_reconfigure_when_newFixedThreadPool() {
		// given
		ThreadPoolExecutor fixedThreadPool =
				(ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		// when
		fixedThreadPool.setMaximumPoolSize(2);
		fixedThreadPool.setCorePoolSize(2);

		// then
		assertThat(fixedThreadPool.getCorePoolSize()).isEqualTo(2);

		fixedThreadPool.shutdown();
	}

	@Test
	public void should_wash_all_dishes() {
		// given
		ExecutorService threadPool =
				Executors.newFixedThreadPool(10, THREAD_FACTORY);
		//		ExecutorService threadPool =
		//				Executors.newSingleThreadExecutor(THREAD_FACTORY);

		DishWasher dishWasher = new DishWasherImpl(threadPool);
		List<DishWasherTask> dishTasks = givenDishWasherTasks(50);

		// when
		dishWasher.wash(dishTasks);
		final boolean isAllMatch = dishTasks.stream().allMatch(DishWasherTask::isDone);

		// then
		assertThat(isAllMatch).isTrue();
	}

	@Test
	public void should_cache_threads() throws InterruptedException {
		// given
		var cachedPool = Executors.newCachedThreadPool(THREAD_FACTORY);
		Callable<String> task = () -> {
			long oneHundredMicroSeconds = 100_000L;
			long startedAt = System.nanoTime();
			while ((System.nanoTime() - startedAt) <= oneHundredMicroSeconds) {
			}
			log.info("Task is done by {}", Thread.currentThread().getName());
			return "Done";
		};
		var tasks = IntStream
				.rangeClosed(1, 1_000)
				.mapToObj(i -> task)
				.collect(toList());
		// when
		var result = cachedPool.invokeAll(tasks);
		delay(3_000L);

		cachedPool.shutdown();
	}

	@Test
	public void should_wash_dish_after_2_seconds_by_scheduled_thread_pool() {
		// given
		ScheduledExecutorService executor =
				Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
		final DishWasherTask smallRedPlateTask =
				new DishWasherTask(new Dish("SMALL RED PLATE"));

		// when
		// Will start command after 2 seconds
		executor.schedule(
				smallRedPlateTask,
				2L, TimeUnit.SECONDS
		);

		delay(6000L);
		executor.shutdown();

		// then
		assertThat(smallRedPlateTask.isDone()).isTrue();
	}

	@Test
	public void should_wash_all_dishes_at_fixed_rate_by_scheduled_thread_pool() {
		// given
		ScheduledExecutorService executor =
				Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
		// when
		// Will start command after 2 seconds, 3 seconds, 4 seconds ...
		executor.scheduleAtFixedRate(() -> {
			delay(500);
			System.out.println(Thread.currentThread().getName());
		}, 2L, 1L, TimeUnit.SECONDS);

		delay(10000L);
		executor.shutdown();
	}

	@Test
	public void should_increment_pool_number() {
		ExecutorService pool1 = Executors.newSingleThreadExecutor();
		ExecutorService pool2 = Executors.newFixedThreadPool(3);

		pool1.execute(() -> log.info("{}", Thread.currentThread().getName()));
		pool2.execute(() -> log.info("{}", Thread.currentThread().getName()));
		pool2.execute(() -> log.info("{}", Thread.currentThread().getName()));
		pool2.execute(() -> log.info("{}", Thread.currentThread().getName()));

		delay(1500L);

		pool1.shutdown();
		pool2.shutdown();
	}

	@Test
	public void should_invoke_any_task() throws ExecutionException, InterruptedException {
		// given
		ExecutorService executorService =
				Executors.newFixedThreadPool(5, THREAD_FACTORY);

		// when
		final String result = executorService.invokeAny(
				List.of(() -> {
					delay(3000L);
					log.info("{}", Thread.currentThread().getName());
					return "Done";
				})
		);

		// then
		assertThat(result).isEqualTo("Done");

		executorService.shutdown();
	}

	@Test
	public void should_invoke_all_task() throws ExecutionException, InterruptedException {
		// given
		ExecutorService executorService =
				Executors.newFixedThreadPool(5, THREAD_FACTORY);

		// when
		final List<Future<String>> futures = executorService.invokeAll(
				List.of(() -> {
					delay(5000L);
					log.info("{}", Thread.currentThread().getName());
					return "Done";
				})
		);
		log.info("Next to invoke");

		// then
		boolean allDone = futures.stream().allMatch(Future::isDone);
		assertThat(allDone).isTrue();
		assertThat(futures.get(0).get()).isEqualTo("Done");

		executorService.shutdown();
	}

	@Test
	public void should_get_result() {
		// given
		ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
		DishWasher dishWasher = new DishWasherImpl(executor);

		// when
		final Future<Dish> redSmallPlateFuture =
				dishWasher.wash(new DishWasherTask(new Dish("RED SMALL PLATE")));

		delay(4000L);
		assertThat(redSmallPlateFuture.isDone()).isTrue();

		try {
			final Dish dish = redSmallPlateFuture.get(100L, MILLISECONDS);
			assertThat(dish.isWashed()).isTrue();
		}
		catch (InterruptedException | ExecutionException | CancellationException | TimeoutException e) {
			log.error(e.getMessage());
		}

		executor.shutdown();
	}

	@Test
	public void should_use_abort_policy() {
		// given
		List<DishWasherTask> dishTasks = givenDishWasherTasks(10);

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				2, 2, 500,
				MILLISECONDS, new ArrayBlockingQueue<>(2),
				THREAD_FACTORY, new ThreadPoolExecutor.AbortPolicy());

		DishWasher dishWasher = new DishWasherImpl(executor);

		// when
		assertThatThrownBy(() -> dishWasher.wash(dishTasks))
				//then
				.isInstanceOf(RejectedExecutionException.class);
	}

	@Test
	public void should_use_custom_policy() {
		// given
		List<DishWasherTask> dishTasks = givenDishWasherTasks(10);

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				2, 2, 500,
				MILLISECONDS, new ArrayBlockingQueue<>(2),
				THREAD_FACTORY, new DishWasherRejectedExecutionHandler());
		DishWasher dishWasher = new DishWasherImpl(executor);
		// when
		dishWasher.wash(dishTasks);

		delay(2000L);
		executor.shutdown();
	}

	@Test
	public void should_cancel_not_started_task() {
		// given
		ExecutorService executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
		var smallRedPlateTask =
				new DishWasherTask(new Dish("SMALL RED PLATE"));
		var smallBlackPlateTask =
				new DishWasherTask(new Dish("SMALL BLACK PLATE"));

		Future<Dish> redPlateFuture = executor.submit(smallRedPlateTask);
		Future<Dish> blackPlateFuture = executor.submit(smallBlackPlateTask);
		delay(1000L);

		// when
		// If task is not started it will be cancelled
		// otherwise task is allowed to complete
		boolean cancelled =
				blackPlateFuture.cancel(false);
		// then
		assertThat(cancelled).isTrue();
		assertThat(blackPlateFuture.isCancelled()).isTrue();
		assertThat(blackPlateFuture.isDone()).isTrue();
		assertThat(smallBlackPlateTask.isDone()).isFalse();

		delay(2000L);
		executor.shutdown();
	}

	@Test
	public void should_cancel_running_task_when_mayInterruptIfRunning_is_true() {
		//given
		Dish redPlate = new Dish("SMALL RED PLATE");
		ExecutorService executor = Executors.newFixedThreadPool(3, THREAD_FACTORY);
		DishWasherTask smallRedPlateTask =
				new DishWasherTask(redPlate);

		final Future<Dish> redPlateFuture = executor.submit(smallRedPlateTask);
		delay(1000L);

		//when
		boolean cancelled =
				redPlateFuture.cancel(true);
		//then
		assertThat(cancelled).isTrue();
		assertThat(redPlateFuture.isCancelled()).isTrue();
		assertThat(redPlateFuture.isDone()).isTrue();
		assertThat(redPlate.isWashed()).isFalse();

		delay(2000L);
		executor.shutdown();
	}

	// TODO: Investigate
	@Test
	public void should_cancel_running_task_when_mayInterruptIfRunning_is_false() throws ExecutionException, InterruptedException {
		//given
		Dish dish = new Dish("SMALL RED PLATE");
		ExecutorService executor = Executors.newFixedThreadPool(2, THREAD_FACTORY);
		final DishWasherTask smallRedPlateTask =
				new DishWasherTask(dish);

		Future<Dish> redPlateFuture = executor.submit(smallRedPlateTask);
		delay(1000L);
		//when
		boolean cancelled =
				redPlateFuture.cancel(false);
		//then
		assertThat(cancelled).isTrue();
		assertThat(redPlateFuture.isCancelled()).isTrue();
		assertThat(redPlateFuture.isDone()).isTrue();
		assertThat(smallRedPlateTask.isDone()).isFalse();
		assertThatThrownBy(redPlateFuture::get)
				.isInstanceOf(CancellationException.class);
		assertThat(dish.isWashed()).isFalse();

		delay(2000L);
		executor.shutdown();
	}

	/**
	 * If a thread terminates due to an uncaught exception,
	 * the JVM notifies the thread's registered UncaughtExceptionHandler.
	 * If there is no registered handler, it prints the stack trace to System.err.
	 */
	@Test
	public void should_catch_exception_on_execute() {
		//given
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("[Dish-Wash-Worker-%d]")
				.setUncaughtExceptionHandler((t, e) ->
						log.error("{}", e.getMessage()))
				.build();

		ExecutorService executorService =
				Executors.newFixedThreadPool(1, threadFactory);

		Runnable command = () -> {
			var message = "RuntimeException from command";
			log.warn(message);
			throw new RuntimeException(message);
		};
		//when
		executorService.execute(command);
		delay(1500L);
		//then
		//No exceptions
		executorService.shutdown();
	}

	@Test
	public void should_throw_exception_on_FutureTask() throws ExecutionException, InterruptedException {
		//given
		var message = "RuntimeException from command";
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("[Dish-Wash-Worker-%d]")
				//				.setUncaughtExceptionHandler((t, e) ->
				//						log.error("Exception: {}", e.getMessage()))
				.build();
		ExecutorService executorService =
				Executors.newFixedThreadPool(1, threadFactory);

		Runnable command = () -> {
			log.warn(message);
			throw new RuntimeException(message);
		};
		FutureTask<String> futureTask = new FutureTask<>(command, "Error");

		//when
		Future<?> submittedTaskFuture = executorService.submit(futureTask);
		delay(1500L);

		String futureResult = (String) submittedTaskFuture.get();
		assertThat(futureResult).isNull();

		assertThatThrownBy(futureTask::get)
				.isInstanceOf(ExecutionException.class)
				.hasMessageContaining(message);

		assertThat(futureTask.isDone()).isTrue();

		executorService.shutdown();
	}

	/**
	 * The uncaught exception - if one occurs - is considered as a part of this Future.
	 * When we invoke the get method, an ExecutionException will be thrown wrapping the original RuntimeException.
	 */
	@Test
	public void should_throw_exception_when_invoke_get_on_future() throws ExecutionException, InterruptedException {
		// given
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("[Dish-Wash-Worker-%d]")
				.setUncaughtExceptionHandler((t, e) ->
						log.error("Exception: {}", e.getMessage()))
				.build();

		final ExecutorService executorService =
				Executors.newFixedThreadPool(1, threadFactory);

		var future = executorService.submit(() -> {
			throw new RuntimeException("RuntimeException from command");
		});
		delay(1000L);

		// when | then
		assertThatThrownBy(future::get)
				.isInstanceOf(ExecutionException.class)
				.hasMessageContaining("RuntimeException from command");
		assertThat(future.isDone()).isTrue();

		executorService.shutdown();
	}

	@Test
	public void should_catch_exception_with_custom_thread_pool() {
		// given
		final ExecutorService executorService =
				new MonitoringThreadPoolExecutor(1, 1, 0,
						TimeUnit.SECONDS, new LinkedBlockingQueue<>());

		// when
		executorService.execute(() -> {
			String message = "RuntimeException from command";
			throw new RuntimeException(message);
		});
		delay(1000L);

		executorService.shutdown();
	}

	@Test
	public void should_not_deadlock_when_invoke_itself_inside_with_no_get() {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		pool.submit(() -> {
			log.info("Start");
			pool.submit(() -> log.info("Submitted task"));
			log.info("Finish");
		});

		delay(2000L);
		pool.shutdown();
	}

	@Test
	public void should_deadlock_when_invoke_itself_inside() {
		// given
		ExecutorService pool = Executors.newSingleThreadExecutor(THREAD_FACTORY);

		// when
		pool.submit(() -> {
			try {
				log.info("Start by [{}]", Thread.currentThread().getName());
				pool.submit(() -> log.info("Submitted task")).get();
				log.info("Finish");
			}
			catch (InterruptedException | ExecutionException e) {
				log.error("Error", e);
			}
		});
		// then wait
		// uncomment to check test execution result
		//delay(300000L);
		pool.shutdown();
	}

	@Test
	public void should_wash_all_dished_by_fork_join_pool() throws InterruptedException {
		// given
		int dishNumber = 40;
		final List<Dish> dishesToWash = givenDishesToWash(dishNumber);

		// ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		// ExecutorService executor = Executors.newWorkStealingPool();
		// ForkJoinPool forkJoinPool = (ForkJoinPool) Executors.newWorkStealingPool();

		ForkJoinPool forkJoinPool = new ForkJoinPool
				(Runtime.getRuntime().availableProcessors(),
						ForkJoinPool.defaultForkJoinWorkerThreadFactory,
						(t, e) -> log.warn("Error message: {}", e.getMessage()),
						true);
		DishWasherForkJoinTask forkJoinTask = new DishWasherForkJoinTask(dishesToWash);

		// when
		forkJoinPool.execute(forkJoinTask);

		final boolean isTerminated = forkJoinPool.awaitTermination(10, TimeUnit.SECONDS);
		if (!isTerminated) {
			forkJoinPool.shutdownNow();
		}
		delay(1000L);
		// then
		assertThat(dishesToWash.stream().allMatch(Dish::isWashed)).isTrue();

		delay(1000L);
		DishWasherForkJoinTask.getWorkers().forEach(task -> {
			log.info("{}", task);
		});
		delay(2000L);
	}

	private List<Dish> givenDishesToWash(int numberOfDishes) {
		List<Dish> dishes = new ArrayList<>(numberOfDishes);
		for (long i = 0; i < numberOfDishes; i++) {
			dishes.add(new Dish(String.format("PLATE [%s]", i)));
		}
		return dishes;
	}

	private List<DishWasherTask> givenDishWasherTasks(int numberOfDishes) {
		List<DishWasherTask> tasks = new ArrayList<>(numberOfDishes);
		for (int i = 0; i < numberOfDishes; i++) {
			tasks.add(new DishWasherTask(new Dish(String.format("PLATE-%s", i))));
		}
		return tasks;
	}
}
