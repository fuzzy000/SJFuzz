package com.djfuzz.rf;

import com.djfuzz.*;
import com.djfuzz.Vector.LevenshteinDistance;
import com.djfuzz.Vector.MathTool;
import com.djfuzz.record.Recover;

import java.io.IOException;
import java.util.*;

public class RfFramework {
    public static final double ALPHA = .2;
    private static Action gotoAction = new GotoAction();
    private static Action lookupAction = new LookupAction();
    private static Action returnAction = new ReturnAction();
    private static Action backtrackAction = new BacktrackAction();
    private static final int DEAD_END = -1;
    private static Map<String, Action> actionContainer = new HashMap<>();
    static {
        actionContainer.put(State.RETURN, returnAction);
        actionContainer.put(State.LOOK_UP, lookupAction);
        actionContainer.put(State.BACKTRACK, backtrackAction);
        actionContainer.put(State.GOTO, gotoAction);
    }

    public static Map<String, Action> getActionContainer() {
        return actionContainer;
    }

    // estimate the Q(s, a) = Q(s, a) + alpha * (R - Q(s, a))
    public void process(String className, int iterationCount, String[] args, String jvmOptions) throws IOException {
        MutateClass mutateClass = new MutateClass();
        Main.initial(args);
        mutateClass.initialize(className, args, null, jvmOptions);
        List<State> mutateAcceptHistory = new ArrayList<>();
        List<MutateClass> mutateRejectHistory = new ArrayList<>();
        List<Double> averageDistance = new ArrayList<>();
        Random random = new Random();
        State currentState = new State();
        currentState.setTarget(mutateClass);
        mutateClass.saveCurrentClass(); // in case 1st backtrack no backup
        mutateAcceptHistory.add(currentState);
        for (int i = 0; i < iterationCount; i ++) {
            System.out.println("Current size is : " + (mutateAcceptHistory.size() + mutateRejectHistory.size()) + ", iteration is :" + i);
//            MutateClass newOne = mutateClass.iteration(); // sootclass has changed here for all objects.
            String actionString = currentState.selectAction();
            Action action = actionContainer.get(actionString);
            State nextState = action.proceedAction(currentState.getTarget(), mutateAcceptHistory); // sootclass has changed here for all objects
            MutateClass newOne;
//            MutateClass newOne = nextState.getTarget();
            if (actionString.equals(State.BACKTRACK)) {
                System.out.println("backtrack here");
                currentState = nextState;
                String nestActionString = currentState.selectActionWithoutBacktrack();
                Action nestAction = RfFramework.getActionContainer().get(nestActionString);
                nextState = nestAction.proceedAction(currentState.getTarget(), mutateAcceptHistory);
                newOne = nextState.getTarget();
            } else {
                newOne = nextState.getTarget();
            }
            if (newOne != null) {
//                MutateClass previousClass = mutateAcceptHistory.get(mutateAcceptHistory.size() - 1).getTarget();
                newOne.saveCurrentClass(); // because only no backtrack can trigger backup, this line ensure class is saved.
                MutateClass previousClass = currentState.getTarget();
                MethodCounter current = newOne.getCurrentMethod();
                List<String> originalCode = previousClass.getMethodOriginalStmtListStringBySignature(current.getSignature());
                int distance = LevenshteinDistance.computeLevenshteinDistance(newOne.getMethodLiveCodeStringBySignature(current.getSignature()), previousClass.getMethodLiveCodeStringBySignature(current.getSignature()));
                double covScore = DjfuzzFramework.calculateCovScore(newOne);
                double rand = random.nextDouble();
                double fitnessScore = 0.7; // indicate big change in original code. no use now?
                if (originalCode != null) {
                    fitnessScore = DjfuzzFramework.fitness(DjfuzzFramework.calculateCovScore(mutateClass), covScore, originalCode.size());
                }
                if(rand < fitnessScore) {
                    currentState.updateScore(actionString, fitnessScore);
                    System.out.println(actionString);
                    System.out.println(covScore);
                    System.out.println("Distance is " + distance + " signature is " + current.getSignature());
                    DjfuzzFramework.showListElement(newOne.getMethodLiveCodeStringBySignature(current.getSignature()));
                    DjfuzzFramework.showListElement(previousClass.getMethodLiveCodeStringBySignature(current.getSignature()));
                    averageDistance.add(distance / 1.0);
                    mutateAcceptHistory.add(nextState);
                    currentState = nextState;
                } else {
                    currentState.updateScore(actionString, DEAD_END);
                    mutateRejectHistory.add(newOne);
                    currentState.setTarget(Recover.recoverFromPath(currentState.getTarget())); // because select action has changed the class
                }

            } else {
//                mutateClass = Recover.recoverFromPath(mutateAcceptHistory.get(mutateAcceptHistory.size() - 1).getTarget());
                mutateClass = Recover.recoverFromPath(currentState.getTarget());
                currentState.setTarget(mutateClass);
                currentState.updateScore(actionString, DEAD_END);
            }

        }
        System.out.println("Total size is : " + mutateAcceptHistory.size());
        System.out.println("Average distance is " + MathTool.mean(averageDistance));
        System.out.println("var is " + MathTool.standardDeviation(averageDistance));
        System.out.println("max is " + Collections.max(averageDistance));
        Recover.recoverFromPath(mutateAcceptHistory.get(0).getTarget());

    }

    public static void main(String[] args) throws IOException {
        RfFramework framework = new RfFramework();
        try {
            framework.process("com.djfuzz.Hello", 500, args, "");
        }catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

}
