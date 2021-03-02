package com.djfuzz;

import soot.jimple.Stmt;

import java.util.*;

public class UsedStatementHelper {
    public static Map<String, Map<String, Set<String>>> classMethodUsedStmt = new HashMap<>();
    private static Map<String, Map<String, String>> classMethodStringToStmt = new HashMap<>(); // mapping soot string stmt to stdout string stmt

    public static void addClassMethodUsedStmt(String className, String signature, Set<String> usedStmt) {
        if (!classMethodUsedStmt.containsKey(className)) {
            classMethodUsedStmt.put(className, new HashMap<>());
        }
        Map<String, Set<String>> methods = classMethodUsedStmt.get(className);
        if (!methods.containsKey(signature)) {
            methods.put(signature, new HashSet<>());
        }
        Set<String> stmts = methods.get(signature);
        stmts.addAll(usedStmt);
    }

    public static void addMethodStringToStmt(String signature, Map<String, String> mapping) {
        classMethodStringToStmt.put(signature, mapping);
    }

    public static boolean queryIfHasInstructionsAlready(String className, String signature, String statement) {
        String realStatement = classMethodStringToStmt.get(signature).get(statement);
        if (classMethodUsedStmt.containsKey(className)) {
            return classMethodUsedStmt.get(className).get(signature).contains(realStatement);
        }
        return false;
    }

    public static String getMappingStdoutStmtString(String signature, String original) {
        Map<String, String> map = classMethodStringToStmt.get(signature);
        return map.get(original);
    }
}
