package com.djfuzz.rf.cluster;

import com.djfuzz.rf.State;

import java.util.ArrayList;
import java.util.List;

public class UniformCluster implements Cluster {

    @Override
    public List<Double> cluster(List<State> states) {
        List<Double> distribution = new ArrayList<>(states.size());
        for (int i = 0, size = states.size(); i < size; i++)
            distribution.add(1.0 / size);
        return distribution;
    }
}
