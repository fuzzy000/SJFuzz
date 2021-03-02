package com.djfuzz.Vector;

import java.util.ArrayList;
import java.util.List;

public class LevenshteinDistance {
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(List<String> lhs, List<String> rhs) {
        int[][] distance = new int[lhs.size() + 1][rhs.size() + 1];

        for (int i = 0; i <= lhs.size(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.size(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.size(); i++)
            for (int j = 1; j <= rhs.size(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((lhs.get(i - 1).equals(rhs.get(j - 1))) ? 0 : 1));

        return distance[lhs.size()][rhs.size()];
    }

    public static void main(String[] args) {
        List<String> stmt1 = new ArrayList<>();
        List<String> stmt2 = new ArrayList<>();
        stmt1.add("a = 1;");
        stmt1.add("b = 1;");
        stmt1.add("c = a + b");
        stmt2.add("a = 1;");
        stmt2.add("b = 2;");
        stmt2.add("c = 0;");
        System.out.println(stmt1.subList(0, 3).size());
        int result = computeLevenshteinDistance(stmt1, stmt2);
        System.out.println(result);
    }
}
