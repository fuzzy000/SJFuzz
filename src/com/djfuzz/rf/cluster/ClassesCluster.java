package com.djfuzz.rf.cluster;

import com.djfuzz.Main;
import com.djfuzz.MutateClass;
import com.djfuzz.Vector.LevenshteinDistance;
import soot.G;
import soot.Scene;
import soot.options.Options;

import java.io.*;
import java.util.*;

public class ClassesCluster {

    /**
     * This variable is used to load all dependencies to ensure
     * tested class will load the relative classes correctly
     * <p>
     * How to make this directory:
     * select one of the project under sootOutput, then extract
     * all files and folders into this directory as the root folder
     * of packets like com.djfuzz.Hello, then it requires there
     * is an <em>com</em> folder directly lies on this root folder
     * <p>
     * Of course it is more convenient to set this variable to
     * somewhere like <em>./environment/eclipse</em>, but since
     * the tested classes are mixed with various classes from
     * different projects, so I recommend you to extract all folders
     * under each project into this single directory.
     */
    private static final String environment = "./environment/";

    /**
     * The folder with all the classes to be tested
     */
    private static final String classResources = "./testResources/";

    private static final String instructionHistory = "./tmpFlow/";

    public static void main(String[] args) {
        clusterAllIn(classResources);
    }

    public static void clusterAllIn(String directory) {
        getInstructionFlows(directory);
        outputClusterData();
    }

    private static void outputClusterData() {
        int minPts = 1, radius = 1;
        Map<Integer, Integer> clusterCntMap = Cluster.getClusterMap(minPts, radius);
        System.out.println("############################################");
        System.out.printf("minPts = %d, radius = %d\n", minPts, radius);
        System.out.println("cluster data: " + clusterCntMap);
        System.out.println("cluster size: " + clusterCntMap.size());
    }

    public static Map<Integer, Integer> getClusterCntMap(Map<MutateClass, Integer> map) {
        Map<Integer, Integer> clusterCntMap = new HashMap<>();
        for (MutateClass mClass : map.keySet()) {
            int clusterId = map.get(mClass);
            int cnt = clusterCntMap.getOrDefault(clusterId, 0);
            clusterCntMap.put(clusterId, cnt + 1);
        }
        return clusterCntMap;
    }

    private static void init() {
        Main.setGenerated(environment);
        String sootClassPath = environment + File.pathSeparator + System.getProperty("java.class.path");
        Scene.v().setSootClassPath(sootClassPath);
        Options.v().set_soot_classpath(sootClassPath);
        Scene.v().loadNecessaryClasses();
    }

    private static void saveInstructionFlow(String fileName, MutateClass mClass, String directory) {
        File dir = new File(directory);
        if (!dir.exists() && !dir.mkdirs())
            return;
        try {
            PrintWriter out = new PrintWriter(directory + "/" + fileName + ".flow");
            for (String line : mClass.getClassPureInstructionFlow())
                out.println(line);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            System.err.printf("error to save instruction flow: %s", fileName);
        }
    }


    private static void getInstructionFlows(String directory) {
        File file = new File(directory);
        if (!file.exists()) {
            System.err.println("no directory " + directory + " found");
            System.exit(-1);
        }
        String[] names = Objects.requireNonNull(file.list());
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
            saveInstructionFlow(name, mClass, instructionHistory);
            System.out.printf("load class %s, %04.2f%%\n", name, (i + 1.0) / size * 100);
        }
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

    private static String getOutputDir(String fileName) {
        String[] slice = fileName.split("\\.");
        StringBuilder s = new StringBuilder(environment);
        int size = slice.length;
        for (int i = 1; i < size - 2; i++)
            s.append('/').append(slice[i]);
        s.append('/').append(slice[size - 2]).append(".class");
        return s.toString();
    }

    private static String getExecuteName(String fileName) {
        int from = fileName.indexOf('.');
        int to = fileName.lastIndexOf('.');
        return fileName.substring(from + 1, to);
    }

    private static boolean createIfNotExist(String path) {
        String dir = path.substring(0, path.lastIndexOf('/'));
        File file = new File(dir);
        return file.exists() || file.mkdirs();
    }

    private static class Cluster {

        private static int DBScan_minPts = 3;
        private static double DBScan_radius = 5;

        private static List<Point> points;
        private static int[][] distance;

        public static Map<Integer, Integer> getClusterMap(int minPts, double radius) {
            if (points == null)
                points = loadPoints(instructionHistory);
            if (distance == null)
                distance = loadDistance();
            List<Point> cores = new ArrayList<>();
            DBScan_minPts = minPts;
            DBScan_radius = radius;
            //find cores
            int len = points.size();
            for (int i = 0; i < len; i++) {
                Point point = points.get(i);
                int cnt = 0;
                for (int j = 0; j < len; j++) {
                    Point other = points.get(j);
                    if (point != other && distance[i][j] < DBScan_radius)
                        cnt++;
                }
                if (cnt >= DBScan_minPts)
                    cores.add(point);
            }

            //set groups
            int id = 0;
            for (Point p : cores) {
                if (p.visited)
                    continue;
                p.clusterId = ++id;
                densityConnected(p, id, points, cores);
            }

            Map<Integer, Integer> map = new HashMap<>();
            for (Point p : points) {
                int clusterId = p.clusterId;
                int cnt = map.getOrDefault(clusterId, 0);
                map.put(clusterId, cnt + 1);
            }
            return map;
        }

        private static int[][] loadDistance() {
            System.out.println("calculating distance");
            int len = points.size();
            int total = len * len;
            double cnt = 0;
            int[][] distance = new int[len][len];
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    Point a = points.get(i);
                    Point b = points.get(j);
                    distance[i][j] = LevenshteinDistance.computeLevenshteinDistance(a.instructions, b.instructions);
                    cnt++;
                }
                System.out.printf("load distance %.3f%%\n", cnt * 100 / total);
            }
            System.out.println("calculating finished");
            return distance;
        }

        private static List<Point> loadPoints(String directory) {
            File file = new File(directory);
            List<Point> points = new ArrayList<>();
            System.out.println("load points ready to start");
            String[] fileNames = file.list();
            for (int i = 0, len = fileNames.length; i < len; i++) {
                String name = fileNames[i];
                List<String> instructions = new ArrayList<>();
                String line;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(directory + "/" + name));
                    while ((line = bufferedReader.readLine()) != null)
                        instructions.add(line);
                } catch (IOException e) {
                    System.err.println("error to load file: " + directory + "/" + name);
                }
                points.add(new Point(i, instructions));
                System.out.printf("load points %.2f%%\n", (i + 1.0) * 100 / len);
            }
            return points;
        }


        private static void densityConnected(Point center, int id, List<Point> points, List<Point> cores) {
            center.visited = true;
            for (Point point : points) {
                if (point.visited)
                    continue;
                if (point.distanceTo(center) < DBScan_radius) {
                    point.clusterId = id;
                    point.visited = true;
                    if (cores.contains(point))
                        densityConnected(point, id, points, cores);
                }
            }
        }

    }

    private static class Point {
        public List<String> instructions;
        public boolean visited;
        public int clusterId;
        public int pointId;

        public Point(int pointId, List<String> instructions) {
            this.instructions = instructions;
            this.visited = false;
            this.clusterId = 0;
            this.pointId = pointId;
        }

        public int distanceTo(Point other) {
            return LevenshteinDistance.computeLevenshteinDistance(this.instructions, other.instructions);
        }
    }

}
