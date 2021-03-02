package com.djfuzz.process;

public interface TaskExecutor extends Runnable {
    public void execute(Task task);
}
