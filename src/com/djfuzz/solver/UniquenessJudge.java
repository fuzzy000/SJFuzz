package com.djfuzz.solver;

import com.djfuzz.differential.DifferentialLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.djfuzz.process.JVMExecutor.SPLIT_STRING;

/**
 * Created by Yicheng Ouyang on 2021/1/8
 */
public class UniquenessJudge{

    private static final String UNIQUE_RESULT_PATH = "./uniqueRecord.log";
    // START_STRING and END_STRING is used for recover
    private static final String START_STRING = "UJSTRT";
    private static final String END_STRING = "UJEND";

    public static Set<String> differenceHashSet = ConcurrentHashMap.newKeySet();

    public static boolean isDifferenceUnique(List<JVMOutputResult> outputResults){
        // first pass
        Set<String> tmpSet = new HashSet<>();
        for (JVMOutputResult outputResult: outputResults){
            tmpSet.add(outputResult.toString());
        }
        if (tmpSet.size() == 1){
            return false;
        }

        // second pass
        String internalResultsRepr = START_STRING + constructResultsString(outputResults) + END_STRING;
        boolean addSucceed = differenceHashSet.add(internalResultsRepr);
        if (addSucceed){
            try{
                DifferentialLogger.writeMsgToLogByFilePath(internalResultsRepr, UNIQUE_RESULT_PATH);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return addSucceed;
    }

    public static void recover(){
        try{
            BufferedReader br = new BufferedReader(new FileReader(UNIQUE_RESULT_PATH));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(START_STRING) && line.endsWith(END_STRING)){
                    differenceHashSet.add(line.trim());
                }else if (line.startsWith(START_STRING)){
                    sb.setLength(0);
                    sb.append(line + "\n");
                }else if (line.endsWith(END_STRING)){
                    sb.append(line + "\n");
                    differenceHashSet.add(sb.toString().trim());
                    sb.setLength(0);
                }else{
                    sb.append(line + "\n");
                }
            }
            br.close();
//            List<String> records = DifferentialLogger.parseClassPathRecordByFilePath(UNIQUE_RESULT_PATH);
//            differenceHashSet.addAll(records);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void clearRecords(){
        File f = new File(UNIQUE_RESULT_PATH);
        if (f.exists()){
            f.renameTo(new File(UNIQUE_RESULT_PATH + "." + System.currentTimeMillis()+".txt"));
        }
//        new File(UNIQUE_RESULT_PATH).delete();
    }

    public static String constructResultsString(List<JVMOutputResult> outputResults){
        StringBuilder sb = new StringBuilder();
        for (JVMOutputResult result: outputResults){
            sb.append(result.toStringWithJVMTagWithoutInst()+"\n");
        }
        return sb.toString();
    }

    public static boolean haveLBC(List<JVMOutputResult> outputResults){
        for (JVMOutputResult result: outputResults){
            if (result.getLiveInstructions().size()!=0){
                return true;
            }
        }
        return false;
    }
}
