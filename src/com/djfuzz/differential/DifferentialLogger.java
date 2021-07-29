package com.djfuzz.differential;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DifferentialLogger {
    public static String getFileName() {
        return fileName;
    }

    public static void setFileName(String fileName) {
        DifferentialLogger.fileName = fileName;
    }

    private static String fileName = "./differential-log.txt";

    public static void writeMsgToLog(String content) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName,true));
        out.write(content.trim() + "\n");
        out.close();
    }

    public static void writeMsgToLogByFilePath(String content, String path) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path,true));
        out.write(content.trim() + "\n");
        out.close();
    }

//    public static void generateNewLog(String previous) {
//        fileName = previous + "-" + fileName;
//    }

    public static List<String> parseClassPathRecord() throws Exception {
        List<String> result = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String singlePath;
        while ((singlePath = in.readLine()) != null) {
            result.add(singlePath);
        }
        in.close();
        return result;
    }

    public static List<String> parseClassPathRecordByFilePath(String path) throws Exception {
        List<String> result = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(path));
        String singlePath;
        while ((singlePath = in.readLine()) != null) {
            result.add(singlePath);
        }
        in.close();
        return result;
    }

    public static void clearRecords(){
        File f = new File(fileName);
        if (f.exists()){
            f.renameTo(new File(fileName + "." + System.currentTimeMillis()+".txt"));
        }
//        new File(fileName).delete();
    }

    public static void main(String[] args) throws Exception {
        writeMsgToLog("./tmp/1610632901384.com.rjdiff.Hello.class");
        writeMsgToLog("./tmp/1610632900409.com.rjdiff.Hello.class");
        writeMsgToLog("./tmp/1610632903311.com.rjdiff.Hello.class");
        System.out.println(parseClassPathRecord());
    }
}
