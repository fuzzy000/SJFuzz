package com.djfuzz.coevolution;

import com.djfuzz.MutateClass;
import com.djfuzz.rf.State;
import com.djfuzz.rf.Tool;
import com.djfuzz.rf.cluster.Cluster;
import com.djfuzz.rf.cluster.DBScanCluster;
import com.djfuzz.rf.cluster.HACluster;

import java.util.ArrayList;
import java.util.List;

public class ClusterTool {

    private static final int MUTATE_SIZE = 50;

    private static ClusterMode MODE = ClusterMode.HA;

    public static List<State> SelectMutateByCluster(List<State> mutateAcceptHistory) {
        if (mutateAcceptHistory.size() > MUTATE_SIZE) {
            List<State> mutateList = new ArrayList<>();
            Cluster cluster = switchCluster();
            List<Double> distribution = cluster.cluster(mutateAcceptHistory);
            for (int i = 0; i < MUTATE_SIZE; i++) {
                State state = Tool.randomStateByDistribution(distribution, mutateAcceptHistory);
                mutateList.add(state);
            }
            return mutateList;
        } else {
            return mutateAcceptHistory;
        }
    }


    public static void getEvoClusterData(List<State> accept, List<State> reject) {
        List<State> stateList = new ArrayList<>();
        stateList.addAll(accept);
        stateList.addAll(reject);
        Cluster cluster = switchCluster();
        cluster.cluster(stateList);
    }

    public static void getClassmingClusterData(List<MutateClass> accept) {
        List<State> stateList = new ArrayList<>();
        for (MutateClass sClass : accept) {
            State state = new State();
            state.setTarget(sClass);
            stateList.add(state);
        }

        Cluster cluster = switchCluster();
        cluster.cluster(stateList);
    }

    private enum ClusterMode {
        HA, DBScan
    }

    private static Cluster switchCluster() {
        if (MODE.equals(ClusterMode.HA)) return new HACluster();
        if (MODE.equals(ClusterMode.DBScan)) return new DBScanCluster();
        return null;
    }

}
