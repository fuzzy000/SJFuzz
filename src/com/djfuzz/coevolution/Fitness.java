package com.djfuzz.coevolution;

import com.djfuzz.Vector.LevenshteinDistance;
import com.djfuzz.rf.State;

import java.util.List;

public class Fitness {
    public static double fitness(State state, List<State> states) {
        double totalDistance = 0.0;
        for (State each: states) {
            if (!each.equals(state)) {
                totalDistance += LevenshteinDistance.computeLevenshteinDistance(state.getTarget().getClassPureInstructionFlow(), each.getTarget().getClassPureInstructionFlow());
            }
        }
        return totalDistance / (states.size() - 1.0);
    }
}
