package com.djfuzz.process;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {
    private BlockingQueue<Task> inputQueue;
    private BlockingQueue<Task> outputQueue;
    private int threadLimit;
    private int currentTask = 0;
    private List<TaskExecutor> executors;
    private List<Thread> threadPool;

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    private boolean isStart = false;

    public void submit(Task task) {
        this.inputQueue.add(task);
        currentTask ++;
    }

    public Task retrieve() {
        if (currentTask == 0) {
            return null;
        }
        Task result = null;
        try {
            result = outputQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("interrupted in retrieve.");
        }
        currentTask --;
        return result;
    }

    public ThreadPool(int limit) {
        this.threadLimit = limit;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();
        this.executors = new ArrayList<>();
        for (int i = 0; i < this.threadLimit; i ++) {
            TaskExecutor jvmExecutors = new JVMExecutor(inputQueue, outputQueue);
            this.executors.add(jvmExecutors);
        }
    }

    public void start() {
        if (isStart) {
            return;
        }
        isStart = true;
        this.threadPool = new ArrayList<>();
        for (TaskExecutor executor: this.executors) {
            Thread thread = new Thread(executor);
            this.threadPool.add(thread);
            thread.start();
        }
    }

    public void close() {
        for (Thread thread: threadPool) {
            thread.interrupt();
        }
        isStart = false;
    }

    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(2);
        pool.start();
        String cmd1 = "ipconfig";
        String cmd2 = "ipconfig";
        String jvm1 = "jvm1";
        String jvm2 = "jvm2";
        Task task1 = new Task();
        task1.setInputInfo(JVMExecutor.generateInfo(jvm1, cmd1));
        pool.submit(task1);
        Task task2 = new Task();
        task2.setInputInfo(JVMExecutor.generateInfo(jvm2, cmd2));
        pool.submit(task2);
        Task result1 = pool.retrieve();
        Task result2 = pool.retrieve();
        System.out.println(result1.getInputInfo());
        System.out.println(result2.getInputInfo());
        pool.close();
    }

}
