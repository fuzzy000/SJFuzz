package com.djfuzz.Vector;

import java.util.List;

public class MathTool {
    public static double sum(List<Double> data) {
        double sum = 0;
        for (int i = 0; i < data.size(); i++)
            sum = sum + data.get(i);
        return sum;
    }

    public static double mean(List<Double> data) {
        double mean = 0;
        mean = sum(data) / data.size();
        return mean;
    }

    // population variance
    public static double PopVariance(List<Double> data) {
        double variance = 0;
        double average = mean(data);
        for (int i = 0; i < data.size(); i++) {
            variance = variance + (Math.pow((data.get(i) - average), 2));
        }
        variance = variance / data.size();
        return variance;
    }

    // population standard deviation
    public static double standardDeviation(List<Double> data) {
        return Math.sqrt(PopVariance(data));
    }

    //sample variance
    public static double sampleVariance(List<Double> data) {
        double variance = 0;
        double average = mean(data);
        for (int i = 0; i < data.size(); i++) {
            variance = variance + (Math.pow((data.get(i) - average), 2));
        }
        variance = variance / (data.size() - 1);
        return variance;
    }

    // sample standard deviation
    public static double sampleStandardDeviation(List<Double> data) {
        return Math.sqrt(sampleVariance(data));
    }

}
