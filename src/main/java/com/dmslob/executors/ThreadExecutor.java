package com.dmslob.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadExecutor implements Executor {

    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private final AtomicLong threadCounter = new AtomicLong(0L);

    @Override
    public void execute(Runnable command) {
        final Thread thread = threadFactory.newThread(command);
        thread.setName(String.format("Worker - %s", threadCounter.addAndGet(1L)));
        thread.start();
    }
}
