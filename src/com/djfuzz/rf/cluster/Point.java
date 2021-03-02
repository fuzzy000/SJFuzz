package com.djfuzz.rf.cluster;

import com.djfuzz.MutateClass;
import com.djfuzz.Vector.LevenshteinDistance;
import com.djfuzz.rf.State;

import java.util.List;

public class Point {

    private boolean visited;
    private int cluster;
    private State state;

    public Point(State state) {
        this(false, 0, state);
    }

    public Point(boolean visited, int cluster, State state) {
        this.visited = visited;
        this.cluster = cluster;
        this.state = state;
    }

    public int distanceTo(Point other) {
        MutateClass a = this.state.getTarget();
        MutateClass b = other.state.getTarget();
        List<String> aMethodLiveCode = a.getClassPureInstructionFlow();
        List<String> bMethodLiveCode = b.getClassPureInstructionFlow();
        return LevenshteinDistance.computeLevenshteinDistance(aMethodLiveCode, bMethodLiveCode);
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Point{" +
                "visited=" + visited +
                ", cluster=" + cluster +
                ", state=" + state +
                '}';
    }

}
