package com.djfuzz.rf.cluster;

import com.djfuzz.Main;
import com.djfuzz.MutateClass;
import com.djfuzz.Vector.LevenshteinDistance;
import org.jf.dexlib2.iface.instruction.Instruction;
import soot.G;
import soot.Scene;
import soot.options.Options;

import java.io.*;
import java.util.*;

public class ClusterTest {

    private static class InstructionFlow {
        List<String> stateInstruction;
        InstructionFlow (List<String> stateInstruction) {
            this.stateInstruction = stateInstruction;
        }
    }

    private static final String environment = "./environment/";

    private static final String classResources = "./testResources/";

    public static void main(String[] args) {
        clusterAllIn(classResources);
    }

    public static void clusterAllIn(String directory) {
        Map<String, List<InstructionFlow>> seedClassesMap = getInstructionFlows(directory);
//        System.out.println(seedClassesMap.keySet());

        for(String seedName: seedClassesMap.keySet()) {
            List<InstructionFlow> testClasses = seedClassesMap.get(seedName);
            System.out.println("/////////////////// Test seed name: " + seedName +" //////////////////////////");
            outputClusterData(testClasses);
        }


    }

    private static void outputClusterData(List<InstructionFlow> testClasses) {
        System.out.println("----------- begin to invoke Cluster -------------");

        for (int i = 0; i < 10; i++) {
            Cluster cluster = new Cluster();
            System.out.printf("------ IsolationDistance = %d ----- \n", i);
            cluster.testParameter(testClasses, i);
        }
    }

    private static Map<String, List<InstructionFlow>> getInstructionFlows(String directory) {
        File file = new File(directory);
        if (!file.exists()) {
            System.err.println("no directory " + directory + " found");
            System.exit(-1);
        }

        String[] names = Objects.requireNonNull(file.list());
//        List<InstructionFlow> testClasses = new ArrayList<>();
        Map<String, List<InstructionFlow>> seedClassesMap = new HashMap<>();

        for (int i = 0, size = names.length; i < size; i++) {
            String name = names[i];
            if (!name.endsWith(".class"))
                continue;
            G.reset();
            init();
            String srcPath = directory + "/" + name;
            String dstPath = getOutputDir(name);
            createIfNotExist(dstPath);
            copy(srcPath, dstPath);
            MutateClass mClass = new MutateClass();
            try {
                mClass.initialize(getExecuteName(name), null, null, "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            String seedName = name.substring(name.indexOf("."), name.length());
            if(seedClassesMap.containsKey(seedName)) {
                List<InstructionFlow> testClasses = seedClassesMap.get(seedName);
                testClasses.add(new InstructionFlow(mClass.getClassPureInstructionFlow()));
//                System.out.printf("Add to Map <%s>    ", seedName);
//                System.out.printf("the size of it: %d \n", seedClassesMap.get(seedName).size());
            } else {
                List<InstructionFlow> testClasses = new ArrayList<>();
                testClasses.add(new InstructionFlow(mClass.getClassPureInstructionFlow()));
                seedClassesMap.put(seedName, testClasses);
//                System.out.printf("Create the Map<%s>     ", seedName);
//                System.out.printf("the size of it: %d \n", seedClassesMap.get(seedName).size());
            }

            System.out.printf("load class %s, %04.2f%%\n", name, (i + 1.0) / size * 100);
        }

        return seedClassesMap;
    }

    private static void init() {
        Main.setGenerated(environment);
        String sootClassPath = environment + File.pathSeparator + System.getProperty("java.class.path");
        Scene.v().setSootClassPath(sootClassPath);
        Options.v().set_soot_classpath(sootClassPath);
        Scene.v().loadNecessaryClasses();
    }

    private static String getOutputDir(String fileName) {
        String[] slice = fileName.split("\\.");
        StringBuilder s = new StringBuilder(environment);
        int size = slice.length;
        for (int i = 1; i < size - 2; i++)
            s.append('/').append(slice[i]);
        s.append('/').append(slice[size - 2]).append(".class");
        return s.toString();
    }

    private static boolean createIfNotExist(String path) {
        String dir = path.substring(0, path.lastIndexOf('/'));
        File file = new File(dir);
        return file.exists() || file.mkdirs();
    }

    private static void copy(String from, String to) {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getExecuteName(String fileName) {
        int from = fileName.indexOf('.');
        int to = fileName.lastIndexOf('.');
        return fileName.substring(from + 1, to);
    }


    // ---------------------------------------------------- Cluster -----------------------------------------
    private static class Cluster {

        private Set<Point> pointSet = new HashSet<>();
        private Queue<Segment> segmentPQ = new PriorityQueue<Segment>(new Comparator<Segment>() {
            @Override
            public int compare(Segment o1, Segment o2) {
                if(o1.distance > o2.distance) {
                    return 1;
                }else if (o1.distance < o2.distance) {
                    return -1;
                }else{
                    return 0;
                }
            }
        });

        private static int IsolationDistance = 2;
        private Map<InstructionFlow, Integer> stateOrder = new HashMap<>();

        private class Point {
            List<InstructionFlow> stateCluster = new ArrayList<>();

            void addState(InstructionFlow state) {
                stateCluster.add(state);
            }

            double distanceTo(Point point) {
                double total = 0;
                for (int i = 0; i < this.stateCluster.size(); i++) {
                    for (int j = 0; j < point.stateCluster.size(); j++) {
                        total += statesDistance(this.stateCluster.get(i), point.stateCluster.get(j));
                    }
                }
                return total/(this.stateCluster.size() * point.stateCluster.size() * 1.0);
            }

            Point mergeTo(Point point) {
                Point newPoint = new Point();
                for (int i = 0; i < this.stateCluster.size(); i++) {
                    newPoint.addState(this.stateCluster.get(i));
                }
                for (int i = 0; i < point.stateCluster.size(); i++) {
                    newPoint.addState(point.stateCluster.get(i));
                }
                return newPoint;
            }
        }

        private class Segment {
            Point begin;
            Point end;
            double distance;

            Segment(Point begin, Point end, double distance) {
                this.begin = begin;
                this.end = end;
                this.distance = distance;
            }
        }

        public void testParameter (List<InstructionFlow> states, int IsolationDistance) {
            this.IsolationDistance = IsolationDistance;
            cluster(states);
        }

        public List<Double> cluster(List<InstructionFlow> states){
            initialPointSet(states);
            initialSegment();

            while(true) {
                Segment shortestSegment = segmentPQ.poll();
                if (shortestSegment != null && shortestSegment.distance < IsolationDistance){
                    if(pointSet.contains(shortestSegment.begin) && pointSet.contains(shortestSegment.end)){

//                    System.out.println("shortest: " + shortestSegment.distance);

                        Point newPoint = shortestSegment.begin.mergeTo(shortestSegment.end);
                        pointSet.remove(shortestSegment.begin);
                        pointSet.remove(shortestSegment.end);
                        pointSet.add(newPoint);
                        pointToPointsDistance(newPoint);

                    }
                }else {
                    break;
                }
            }

            Double[] distribution = new Double[states.size()];
            double sum = 0;
            for(Point point: pointSet){
                sum += 1.0/point.stateCluster.size();
            }
            for(Point point: pointSet) {
                for (int i = 0; i < point.stateCluster.size(); i++) {
                    distribution[stateOrder.get(point.stateCluster.get(i))] =
                            1.0 / point.stateCluster.size() / point.stateCluster.size() / sum;
                }
            }
//        System.out.println(Arrays.asList(distribution).toString());
            printClusterInfo();
            return Arrays.asList(distribution);
        }


        private void initialPointSet(List<InstructionFlow> states) {
            for (int i = 0; i < states.size(); i++) {
                Point point = new Point();
                point.addState(states.get(i));
                pointSet.add(point);
                stateOrder.put(states.get(i), i);
            }
        }

        private void initialSegment() {
            for(Point begin: pointSet) {
                for(Point end: pointSet){
                    if(begin != end) {
                        segmentPQ.add(new Segment(begin, end, begin.distanceTo(end)));
//                    System.out.println("initial segment: " + begin.distanceTo(end));
                    }
                }
            }
        }

        private void pointToPointsDistance(Point point) {
            for(Point target: pointSet) {
                if(point != target) {
                    segmentPQ.add(new Segment(point, target, point.distanceTo(target)));
//                System.out.println("distance between clusters: " + point.distanceTo(target));
                }
            }
        }

        private double statesDistance(InstructionFlow aMethodLiveCode, InstructionFlow bMethodLiveCode) {
//        System.out.println("stateDistance: " + LevenshteinDistance.computeLevenshteinDistance(aMethodLiveCode, bMethodLiveCode));
            return LevenshteinDistance.computeLevenshteinDistance(aMethodLiveCode.stateInstruction, bMethodLiveCode.stateInstruction);
        }

        private void printClusterInfo() {
            System.out.println("cluster number: " + pointSet.size());
            for (Point point: pointSet) {
                System.out.print(" " + point.stateCluster.size() + " ");
            }
            System.out.println();
        }
    }

}
