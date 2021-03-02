package com.djfuzz.rf.cluster;

import com.djfuzz.rf.State;

import java.util.List;

public interface Cluster {
    List<Double> cluster(List<State> states);
}
