package com.djfuzz.rf;

import com.djfuzz.MethodCounter;
import com.djfuzz.MutateClass;
import soot.SootMethod;

import java.util.*;

public class State {

    private Map<String, Integer> mappingToIndex = new HashMap<>();
    private List<String> actions = new ArrayList<>();
    private List<Double> scores = new ArrayList<>();

    private Map<String, List<Double>> methodScores = new HashMap<>();

    public Map<String, List<Double>> getMethodScores() {
        return methodScores;
    }

    public void setMethodScores(Map<String, List<Double>> methodScores) {
        this.methodScores = methodScores;
    }

    public double getCoFitnessScore() {
        return coFitnessScore;
    }

    public void setCoFitnessScore(double coFitnessScore) {
        this.coFitnessScore = coFitnessScore;
    }

    private double coFitnessScore;

    public MethodCounter getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(MethodCounter currentMethod) {
        this.currentMethod = currentMethod;
    }

    private MethodCounter currentMethod;

    private static double exploreRate = 0.1;
    public static final String GOTO = "goto";
    public static final String BACKTRACK = "backtrack";
    public static final String LOOK_UP = "lookup";
    public static final String RETURN = "return";
    private static final int ACTION_LIMIT = 4;

    public Map<String, Integer> getMappingToIndex() {
        return mappingToIndex;
    }

    public void setMappingToIndex(Map<String, Integer> mappingToIndex) {
        this.mappingToIndex = mappingToIndex;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public State() {
        mappingToIndex.put(BACKTRACK, 0);
        actions.add(BACKTRACK);
        mappingToIndex.put(GOTO, 1);
        actions.add(GOTO);
        mappingToIndex.put(LOOK_UP, 2);
        actions.add(LOOK_UP);
        mappingToIndex.put(RETURN, 3);
        actions.add(RETURN);

        for (int i = 0; i < ACTION_LIMIT; i ++) {
            scores.add(0.0);
        }
    }

    public MutateClass getTarget() {
        return target;
    }

    public void setTarget(MutateClass target) {
        this.target = target;
        if (target != null) {
            List<String> liveMethod = target.getLiveMethodSignature();
            for (String eachMethod : liveMethod) {
                List<Double> currentScore = new ArrayList<>();
                for (int i = 0; i < ACTION_LIMIT; i++) {
                    currentScore.add(0.0);
                }
                this.methodScores.put(eachMethod, currentScore);
            }
        }
    }

    public State deepCopyBySettingTarget(MutateClass target) {
        State state = new State();
        state.setTarget(target);
        List<Double> scores = new ArrayList<>();
        for (Double current: this.getScores()) {
            scores.add(current);
        }
        state.setScores(scores);
        Map<String, List<Double>> methods = state.getMethodScores();
        for (String method: methods.keySet()) {
            if (!methodScores.containsKey(method))
                continue;
            List<Double> newScore = new ArrayList<>();
            for (Double score: this.methodScores.get(method)) {
                newScore.add(score);
            }
            methods.put(method, newScore);
        }
        state.setMethodScores(methods);
        return state;
    }

    private MutateClass target;

    // estimate the Q(s, a) = Q(s, a) + alpha * (R - Q(s, a))
    public void updateScore(String action, double reward) {
        int scoreIndex = mappingToIndex.get(action);
        double previous = scores.get(scoreIndex);
        previous += RfFramework.ALPHA * (reward - previous);
        scores.set(scoreIndex, previous);
    }


    public String selectAction() {
        Random random = new Random();
        if (random.nextDouble() < exploreRate) {
            return actions.get(random.nextInt(actions.size()));
        }
        double maxScore = Collections.max(scores);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < scores.size(); i ++) {
            if (maxScore == scores.get(i)) {
                candidates.add(i);
            }
        }
        int resultIndex = candidates.get(random.nextInt(candidates.size()));
        return actions.get(resultIndex);
    }

    public void updateMethodScore(String action, double reward) {
        MethodCounter currentMethod = this.target.getCurrentMethod();
        List<Double> currentScore = methodScores.get(currentMethod.getSignature());
        int scoreIndex = mappingToIndex.get(action);
        double previous = currentScore.get(scoreIndex);
        previous += RfFramework.ALPHA * (reward - previous);
        currentScore.set(scoreIndex, previous);
    }

    public MethodCounter generateRankBasedDistribution() {
        String signature = (String)this.methodScores.keySet().toArray()[0];
        double maxValue = Collections.max(this.methodScores.get(signature));
        List<String> signatures = new ArrayList<>(this.methodScores.keySet());

        return new MethodCounter(signature, 0);
    }

    public String selectActionAndMutatedMethod() {
        MethodCounter counter = this.target.getMethodByDistribution();
        this.target.setCurrentMethod(counter);
        Random random = new Random();
        try{
            if (random.nextDouble() < exploreRate) {
                return actions.get(random.nextInt(actions.size() - 1) + 1);
            }
            List<Double> currentScore = methodScores.get(counter.getSignature());
            List<Double> newScore = new ArrayList<>();
            for (int i = 1; i < currentScore.size(); i ++) {  // may throw NullPointerException
                newScore.add(currentScore.get(i));
            }
            double maxScore = Collections.max(newScore);
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < newScore.size(); i ++) {
                if (maxScore == newScore.get(i)) {
                    candidates.add(i + 1);
                }
            }
            int resultIndex = candidates.get(random.nextInt(candidates.size()));
            return actions.get(resultIndex);
        }catch (NullPointerException e){
            e.printStackTrace();
            // if throw Exception, return random action
            return actions.get(random.nextInt(actions.size() - 1) + 1);
        }
    }

    public String selectActionWithoutBacktrack() {
        Random random = new Random();
        if (random.nextDouble() < exploreRate) {
            return actions.get(random.nextInt(actions.size() - 1) + 1);
        }
        List<Double> newScore = new ArrayList<>();
        for (int i = 1; i < scores.size(); i ++) {
            newScore.add(scores.get(i));
        }
        double maxScore = Collections.max(newScore);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < newScore.size(); i ++) {
            if (maxScore == newScore.get(i)) {
                candidates.add(i + 1);
            }
        }
        int resultIndex = candidates.get(random.nextInt(candidates.size()));
        return actions.get(resultIndex);
    }

}
