package com.djfuzz.util;

import com.djfuzz.cluster.ClusterResult;
import com.djfuzz.cluster.DBScanCluster;
import com.djfuzz.cluster.InstructionFlow;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineClusterUtil {

    private static final ExecutorService service = Executors.newCachedThreadPool();

    public static void dumpInstructions(String path, List<String> instructions) {
        service.submit(() -> {
            File file = new File(path);
            try (PrintWriter out = new PrintWriter(file)) {
                for (String instruction : instructions) {
                    out.println(instruction);
                }
                out.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public static Set<String> loadInstructionHistory(String path) {
        File dir = new File(path);
        return new HashSet<>(Arrays.asList(Objects.requireNonNull(dir.list())));
    }

    public static String convertBack2Log(String path) {
        String[] strings = path.split("/");
        return "./tmpRecord/" + strings[strings.length - 1] + ".log";
    }

    public static List<List<String>> getPureInstructions(String path) {
        // path should be ./tmpRecord
        List<List<String>> lists = new ArrayList<>();
        File dir = new File(path);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (!file.getName().endsWith(".log"))
                continue;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                List<String> list = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
                lists.add(list);
            } catch (Exception e) {
                System.err.println("load instructions failed!");
                System.exit(-1);
            }
        }
        return lists;
    }

    public static Map<String, List<InstructionFlow>> getPureInstructionFlows (String path) {
        Map<String, List<InstructionFlow>> seedClassesMap = new HashMap<>();

        int count = 1;
        File dir = new File(path);
        for (File file: Objects.requireNonNull(dir.listFiles())) {
            String fileName = file.getName();
            if (!fileName.endsWith(".log")) {
                continue;
            }
            try {
                String seedName = fileName.substring(fileName.indexOf("."), fileName.length());

                BufferedReader reader = new BufferedReader(new FileReader(file));
                List<String> list = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
                InstructionFlow instructionFlow = new InstructionFlow(list);

                if (seedClassesMap.containsKey(seedName)) {
                    List<InstructionFlow> mutateClassList = seedClassesMap.get(seedName);
                    mutateClassList.add(instructionFlow);
//                System.out.printf("Add to Map <%s>    ", seedName);
//                System.out.printf("the size of it: %d \n", seedClassesMap.get(seedName).size());
                }else {
                    List<InstructionFlow> mutateClassList = new ArrayList<>();
                    mutateClassList.add(instructionFlow);
                    seedClassesMap.put(seedName, mutateClassList);
//                System.out.printf("Create the Map<%s>     ", seedName);
//                System.out.printf("the size of it: %d \n", seedClassesMap.get(seedName).size());
                }
                int size = Objects.requireNonNull(dir.listFiles()).length;
                System.out.printf("load class %s, %04.2f%%\n", fileName, (count + 0.0) / size * 100);
                count++;
            } catch (Exception e) {
                System.err.println("load instructions failed!");
                System.err.println(e.getMessage());
                System.err.println(e.fillInStackTrace());
                System.exit(-1);
            }
        }
        return seedClassesMap;
    }

    public static ClusterResult getClusterInfo() {
        return new DBScanCluster().getClusterInfo(null);
    }

}
