package com.djfuzz.solver;

import java.util.ArrayList;
import java.util.List;

import static com.djfuzz.process.JVMExecutor.SPLIT_STRING;

/**
 * Created by Yicheng Ouyang on 2021/1/7
 */
public class JVMOutputResult {

    String jvmTag;
    List<String> liveInstructions;
    String parsedErr;
    String out;

    public String getJvmTag() {
        return jvmTag;
    }

    public void setJvmTag(String jvmTag) {
        this.jvmTag = jvmTag;
    }

    public List<String> getLiveInstructions() {
        return liveInstructions;
    }

    public void setLiveInstructions(List<String> liveInstructions) {
        this.liveInstructions = liveInstructions;
    }

    public String getParsedErr() {
        return parsedErr;
    }

    public void setParsedErr(String parsedErr) {
        this.parsedErr = parsedErr;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    JVMOutputResult(){
        liveInstructions = new ArrayList<>();
    }

    @Override
    public String toString() {
        return liveInstructions + SPLIT_STRING + parsedErr + SPLIT_STRING + out;
    }

    public String toStringWithJVMTag(){
        return jvmTag + SPLIT_STRING + liveInstructions + SPLIT_STRING + parsedErr + SPLIT_STRING + out;
    }

    public String toStringWithJVMTagWithoutInst(){
        return jvmTag + SPLIT_STRING + parsedErr + SPLIT_STRING + out;
    }
}
