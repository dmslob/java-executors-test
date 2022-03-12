package com.dmslob.service;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DishWasherRejectedExecutionHandler implements RejectedExecutionHandler {
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		log.info("Pool can not submit the last task");
		log.info("Pool size = {}", executor.getPoolSize());
		log.info("Size of queue = {}", executor.getQueue().size());
		log.info("Active count = {}", executor.getActiveCount());
	}
}
