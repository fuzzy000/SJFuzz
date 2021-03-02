package com.djfuzz.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinateHelper {

    private static Map<String, List<String>> basePoint = new HashMap<>();

    public static void addMethodBasePoint(String signature, List<String> stmt) {
        basePoint.put(signature, stmt);
    }


}
