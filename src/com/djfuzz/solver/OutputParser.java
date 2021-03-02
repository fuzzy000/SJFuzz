package com.djfuzz.solver;

/**
 * Created by Yicheng Ouyang on 2021/1/8
 */
public interface OutputParser {
    // output should be in the format of JVM_Tag||****||stdout||****||stderr
    JVMOutputResult parseOutput(String output);
}
