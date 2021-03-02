package com.djfuzz.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

public class JVMExecutor implements TaskExecutor {
    public static final String SPLIT_STRING = "||****||";
    @Override
    public void execute(Task task) {
        String inputInfo = task.getInputInfo();
        String[] allInfo = infoParser(inputInfo);
        String cmd = parseCommand(allInfo);
        String jvmType = parseJVM(allInfo);
        Task realResult = new Task();
        String outputCmd = this.parseResult(cmd);
        realResult.setInputInfo(generateInfo(jvmType, outputCmd));
        this.output.add(realResult);
    }

    public static String generateInfo(String jvmType, String outputCmd) {
        return jvmType + SPLIT_STRING + outputCmd;
    }

    private String parseResult(String cmd) {
        StringBuilder sb = new StringBuilder();
        StringBuilder errSb = new StringBuilder();
        Thread errThread = null;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            final InputStream is1 = p.getInputStream();
            final InputStream is2 = p.getErrorStream();
            errThread = new Thread(() -> {
                BufferedReader br2 = null;
                try {
                    br2 = new BufferedReader(new InputStreamReader(is2, "UTF-8"));
                    String line2 = null ;
                    while ((line2 = br2.readLine()) !=  null ){
                        errSb.append(line2);
//                        System.out.println(line2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally{
                    try {
                        br2.close();
                        is2.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            errThread.start();

            BufferedReader br1 = new BufferedReader(new InputStreamReader(is1, "UTF-8"));
            try {
                String line1 = null;
                while ((line1 = br1.readLine()) != null) {
                    sb.append(line1 + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                try {
                    br1.close();
                    is1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            errThread.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return sb.toString() + SPLIT_STRING + errSb.toString();
    }

    @Override
    public void run() {
        while(true) {
            try {
//                System.out.println(Thread.currentThread().getName()+ ": Waiting for task");
                Task task = input.take();
//                System.out.println(Thread.currentThread().getName()+ ": Executing task");
//                System.out.println(task.getInputInfo());
                this.execute(task);
            } catch (InterruptedException e) {
//                e.printStackTrace();
                System.out.println("END: in JVMexecutor");
                return;
            }
        }
    }

    private String parseCommand(String[] allInfo) {
        return allInfo[1];
    }

    private String parseJVM(String[] allInfo) {
        return allInfo[0];
    }

    private String[] infoParser(String inputInfo){
        return inputInfo.split(Pattern.quote(SPLIT_STRING));
    }

    public JVMExecutor(BlockingQueue<Task> input, BlockingQueue<Task> output) {
        this.input = input;
        this.output = output;
    }

    private BlockingQueue<Task> input;

    public BlockingQueue<Task> getOutput() {
        return output;
    }

    public void setOutput(BlockingQueue<Task> output) {
        this.output = output;
    }

    private BlockingQueue<Task> output;
}
