package com.djfuzz;

import com.djfuzz.differential.DifferentialLogger;
import com.djfuzz.process.JVMExecutor;
import com.djfuzz.process.Task;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentSetting {
    private static final String JVM_CONFIG_PATH = "./JVM.config";
    public static List<String> jvms = new ArrayList<>();
    public static List<String> cmds = new ArrayList<>();
    static {
        try {
            jvms.addAll(DifferentialLogger.parseClassPathRecordByFilePath(JVM_CONFIG_PATH));
        }catch (Exception e){
            e.printStackTrace();
            jvms.add("java");
            jvms.add("echo");
        }
    }

    public static int getJvmNum(){
        return jvms.size();
    }

    public static void generateJvmAndCmd(String className, String[] args, String jvmOptions) {
        for (int i = 0; i < jvms.size(); i ++) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(jvms.get(i) + " " + jvmOptions + " " + className + " ");
            for (String arg: args){
                tmp.append(arg + " ");
            }
            cmds.add(tmp.toString());
        }
    }

//  java -cp ./sootOutput/junit-ant/;../junit-4.12.jar;../hamcrest-core-1.3.jar;../tools.jar
//  org.junit.runner.JUnitCore org.apache.tools.mail.MailMessageTest
    public static void generateJunitJvmAndCmd(String jvmOptions) {
        String tmpJunitCmd = Main.generateJunitCmd(jvmOptions).trim().substring(4);
        for (int i = 0; i < jvms.size(); i ++) {
            cmds.add(jvms.get(i) + tmpJunitCmd);
        }
    }

    public static List<Task> generateTaskAccordingToClass() {
        List<Task> result = new ArrayList<>();
        for (int i = 0; i < jvms.size(); i ++) {
            String jvmType = jvms.get(i);
            String cmd = cmds.get(i);
            Task task = new Task();
            task.setInputInfo(JVMExecutor.generateInfo(jvmType, cmd));
            result.add(task);
        }
        return result;
    }
}
