package com.djfuzz.solver;

import com.djfuzz.MutateClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yicheng Ouyang on 2021/2/20
 */
public class UniqueChildSaver {
    static List<MutateClass> uniqueChildren = new ArrayList<>();

    public static void saveChild(MutateClass mc){
        uniqueChildren.add(mc);
    }

    public static void printChildrenPath(){
        System.out.println("======== Unique Children BackPath ========");
        uniqueChildren.forEach(x -> {
            System.out.println(x.getBackPath());
        });
    }
}
