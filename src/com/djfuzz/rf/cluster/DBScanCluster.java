package com.djfuzz.rf.cluster;

import com.djfuzz.rf.State;

import java.util.*;

public class DBScanCluster implements Cluster {

    private static final int DBScan_minPts = 3;
    private static final double DBScan_radius = 5;

    @Override
    public List<Double> cluster(List<State> states) {
        DBScanCluster cluster = new DBScanCluster(states);
        Map<State, Integer> clusters = cluster.cluster();
        Map<Integer, Integer> counter = new HashMap<>();
        List<Double> distribution = new ArrayList<>(states.size());
        for (State state : clusters.keySet()) {
            int clusterId = clusters.get(state);
            int count = counter.getOrDefault(clusterId, 0);
            counter.put(clusterId, count + 1);
        }
        double sum = 0;
        for (int clusterId : counter.keySet()) {
            sum += 1.0 / counter.get(clusterId);
        }
        for (State state : states) {
            int id = clusters.get(state);
            double probability = 1.0 / counter.get(id) / counter.get(id) / sum;
            distribution.add(probability);
        }
        return distribution;
    }

    private List<Point> points;
    private List<Point> cores;
    private int groupNum;

    /**
     * 范围在minPts之内的都会被归于同一簇
     */
    private int minPts;

    /**
     * 同一簇的归纳范围
     */
    private double radius;

    public DBScanCluster() {

    }

    public DBScanCluster(List<State> states) {
        this(states, DBScan_minPts, DBScan_radius);
    }

    public DBScanCluster(List<State> states, int minPts, double radius) {
        this.minPts = minPts;
        this.radius = radius;
        this.points = new LinkedList<>();
        this.cores = new LinkedList<>();
        states.forEach(state -> points.add(new Point(state)));
        findCores();
        setCores();
    }

    /**
     * 从state到clusterId的映射, 如果clusterId == 0
     * 则该state是噪声点, 单独作为一簇
     *
     * @return state
     */
    public Map<State, Integer> cluster() {
        Map<State, Integer> map = new HashMap<>();
        points.forEach(point -> {
            if (point.getCluster() == 0) {
                map.put(point.getState(), ++groupNum);
            } else {
                map.put(point.getState(), point.getCluster());
            }
        });
        return map;
    }

    private void findCores() {
        for (Point point : points) {
            int cnt = 0;
            for (Point other : points) {
                if (point != other && point.distanceTo(other) < radius)
                    cnt++;
            }
            if (cnt >= minPts)
                cores.add(point);
        }
    }

    private void setCores() {
        int id = 0;
        for (Point p : cores) {
            if (p.isVisited())
                continue;
            p.setCluster(++id);
            densityConnected(p, id);
        }
        groupNum = id;
    }

    private void densityConnected(Point center, int id) {
        center.setVisited(true);
        for (Point point : points) {
            if (point.isVisited())
                continue;
            if (point.distanceTo(center) < radius) {
                point.setCluster(id);
                point.setVisited(true);
                if (cores.contains(point))
                    densityConnected(point, id);
            }
        }
    }


}
