package com.djfuzz.rf.cluster;

import com.djfuzz.MutateClass;
import com.djfuzz.record.Recover;
import com.djfuzz.rf.Action;
import com.djfuzz.rf.State;
import com.djfuzz.rf.Tool;

import java.io.IOException;
import java.util.List;

public class RollBackAction implements Action {
    @Override
    public State proceedAction(MutateClass target, List<State> total) {
        Cluster cluster = new HACluster();
        List<Double> distribution = cluster.cluster(total);
        State state = Tool.randomStateByDistribution(distribution, total);
        try {
            Recover.recoverFromPath(state.getTarget());
        } catch (IOException e) {
            System.out.println("should not recover failed.");
        }

        return state;
    }
}
