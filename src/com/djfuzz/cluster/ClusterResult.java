package com.djfuzz.cluster;

import java.util.List;

public class ClusterResult {

    // 每个item一个簇中有多少个类
    List<Integer> clusterList;

    @Override
    public String toString() {
        return "ClusterResult{" +
                "size = " + clusterList.size() + "clusterList=" + clusterList +
                '}';
    }
}
