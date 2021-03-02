package com.djfuzz.cluster;

import com.djfuzz.util.OfflineClusterUtil;

import java.util.List;
import java.util.Map;

public class ClusterTest {


    private static String path = "./tmpRecord";
    public static void main(String[] args) {
        outputClusterData(path);
    }

    public static void runDBScanCluster() {
        Cluster cluster = new DBScanCluster();
        System.out.println(cluster.getClusterInfo(null));
    }

    public static void runHACluster(List<InstructionFlow> mutateClassList) {
        System.out.println("----------- begin to invoke Cluster -------------");
        for (int i = 1; i < 7; i++) {
            HACluster cluster = new HACluster();
            System.out.printf("------ IsolationDistance = %d ----- \n", i);
            cluster.testParameter(mutateClassList, i);
        }

    }

    private static void outputClusterData(String path) {
        Map<String, List<InstructionFlow>> seedClassesMap = OfflineClusterUtil.getPureInstructionFlows(path);
//        System.out.println(seedClassesMap.keySet());

        for(String seedName: seedClassesMap.keySet()) {
            List<InstructionFlow> mutateClassList = seedClassesMap.get(seedName);
            System.out.println("/////////////////// Test seed name: " + seedName +" //////////////////////////");
            runHACluster(mutateClassList);
        }
    }

}
