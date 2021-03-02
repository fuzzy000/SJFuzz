package com.djfuzz.differential;

import com.djfuzz.*;
import com.djfuzz.Vector.LevenshteinDistance;
import com.djfuzz.process.Task;
import com.djfuzz.process.ThreadPool;
import com.djfuzz.record.Recover;
import com.djfuzz.rf.Action;
import com.djfuzz.rf.State;
import com.djfuzz.solver.*;
import com.djfuzz.strategy.RandomMutation;
import com.djfuzz.util.OfflineClusterUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DifferentialFramework implements Runnable {
    private List<State> queue;
    private BlockingQueue<State> passed;
    private String[] args;

    public boolean isShouldRecovery() {
        return shouldRecovery;
    }

    public void setShouldRecovery(boolean shouldRecovery) {
        this.shouldRecovery = shouldRecovery;
    }

    private boolean shouldRecovery = false;


    public ThreadPool getPool() {
        return pool;
    }

    private OutputParser outputParser;
    private UniquenessJudge judge;
    private ThreadPool pool = new ThreadPool(EnvironmentSetting.getJvmNum());
    private int iteration = 0;

    public boolean isDifferetialPath(String path) {
        try {
            for (State state : queue) {
                if (path.equals(state.getTarget().getBackPath())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log("Empty Path, should not");
        }
        return false;
    }

    public List<File> getDifferentialPaths(){
        List<File> paths = new ArrayList<>();
        for (State state: queue){
            try{
                paths.add(new File(state.getTarget().getBackPath()));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return paths;
    }

    public DifferentialFramework() {
        queue = new LinkedList<>();
        passed = new LinkedBlockingQueue<>();
        outputParser = new JVMOutputParser();
        judge = new UniquenessJudge();
    }

    public void submitClass(State state) {
        passed.add(state);
    }

    public State getItemFromQueue() throws IOException {
        State current = passed.poll();
        if (current != null) {
            return current;
//            return RandomMutation.regenerateState(current);
        } else {
            return null;
        }
    }

    public List<State> getQueue() {
        return queue;
    }

    public void setQueue(List<State> queue) {
        this.queue = queue;
    }

    public BlockingQueue<State> getPassed() {
        return passed;
    }

    public void setPassed(BlockingQueue<State> passed) {
        this.passed = passed;
    }

    public OutputParser getOutputParser() {
        return outputParser;
    }

    public void setOutputParser(OutputParser outputParser) {
        this.outputParser = outputParser;
    }

    public UniquenessJudge getJudge() {
        return judge;
    }

    public void setJudge(UniquenessJudge judge) {
        this.judge = judge;
    }

    public static void log(String content) {
        System.out.println(" *********** Differential Stage ***********: " + content);
    }

    public void recovery(String classPath, String className, String[] args, String jvmOptions) throws Exception {
        this.queue = new LinkedList<>();
        List<String> classBackPath = DifferentialLogger.parseClassPathRecord();
        for (String path: classBackPath) {
            State state = DjfuzzFrameworkResumableAdvanced.recoverStateFromBackPath(classPath, className, args, jvmOptions, path);
            this.queue.add(state);
        }
    }

    public void oneIteration() throws Exception {
        try {
            log("Current Itertion Begin: " + iteration);
            List<State> children = new LinkedList<>();
            pool.start();
            for (State state : queue) {
                if (state.getTarget() == null || state.getTarget().getBackPath() == null) {
                    log("should not be here!");
                    continue;
                }
                state.setTarget(Recover.recoverFromPath(state.getTarget()));
//                State newState = RandomMutation.randomMutation(state); // TODO: how to rerun a class here?
                String nextActionString = state.selectActionAndMutatedMethod();
                Action nextAction = DjfuzzFrameworkResumableAdvanced.getActionContainer().get(nextActionString);
                State nextState = nextAction.proceedAction(state.getTarget(), null);
                MutateClass newOne = nextState.getTarget();
                if (newOne != null) {
                    MethodCounter currentCounter = newOne.getCurrentMethod();
                    int distance = LevenshteinDistance.computeLevenshteinDistance(state.getTarget().getClassPureInstructionFlow(), newOne.getClassPureInstructionFlow());
                    System.out.println("Distance is " + distance + " signature is " + currentCounter.getSignature());
                    DjfuzzFramework.showListElement(newOne.getMethodLiveCodeStringBySignature(currentCounter.getSignature()));
                    DjfuzzFramework.showListElement(state.getTarget().getMethodLiveCodeStringBySignature(currentCounter.getSignature()));
                    nextState.getTarget().saveCurrentClass();
                    state.updateMethodScore(nextActionString, distance / 1.0);
                } else {
                    state.updateMethodScore(nextActionString, DjfuzzFrameworkResumableAdvanced.DEAD_END);
                }
                if (nextState.getTarget() == null) {
                    log("Generate a null children.");
                } else {
                    log("Generate a valid child in queue.");
//                    if (!nextState.getTarget().isFromDiversity()) {
//                        log("Find a child in discrepancy");
//                    }
                    queue.forEach(x -> {
                        System.out.println(x.getTarget().getBackPath() + " generated " + x.getTarget().getUniqueChildren() + " diff-unique children.");
                    });
                }
            }
//            UniqueChildSaver.printChildrenPath();
            queue.addAll(children);
            State otherSource = this.getItemFromQueue();
            if (otherSource != null) {
                queue.add(otherSource);
                log("Find other source in iteration:" + iteration +" " + otherSource.getTarget().getBackPath());
                DifferentialLogger.writeMsgToLog(otherSource.getTarget().getBackPath());
                // output unique class
            }
            log("There are total " + queue.size() + " differential now.");
            iteration++;
        } catch (Throwable e) {
            e.printStackTrace();
            log("UnexpectException in oneIteration().");
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                oneIteration();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
