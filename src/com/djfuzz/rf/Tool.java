package com.djfuzz.rf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Tool {

    public static State randomStateByDistribution(List<Double> distribution, List<State> states){

        distribution = Renormalization(distribution);

        Random random = new Random();
        double aDouble = random.nextDouble();
//        System.out.println(aDouble);

        double cumulative = 0;
        for (int i = 0; i < distribution.size(); i++) {
            if( aDouble >= cumulative && aDouble <= (cumulative + distribution.get(i))) {
                return states.get(i);
            }else{
                cumulative += distribution.get(i);
            }
        }
        // avoid the accumulative error
        return states.get(distribution.size()-1);
    }

    public static Object randomSelectionByDistribution(List<Double> distribution, List states){

        distribution = Renormalization(distribution);

        Random random = new Random();
        double aDouble = random.nextDouble();
//        System.out.println(aDouble);

        double cumulative = 0;
        for (int i = 0; i < distribution.size(); i++) {
            if( aDouble >= cumulative && aDouble <= (cumulative + distribution.get(i))) {
                return states.get(i);
            }else{
                cumulative += distribution.get(i);
            }
        }
        // avoid the accumulative error
        return states.get(distribution.size()-1);
    }


    private static List<Double> Renormalization (List<Double> distribution) {
        double total = 0;
        for (int i = 0; i < distribution.size(); i++) {
            total += distribution.get(i);
        }

        List<Double> reDistribution = new ArrayList<>();
        for (int i = 0; i < distribution.size(); i++) {
            reDistribution.add(distribution.get(i)/total);
        }
        return reDistribution;
    }


    public static void main(String[] args) {
        List<Double> distribution = new ArrayList<Double>();
        distribution.add(0.4);
        distribution.add(0.2);
        distribution.add(0.3);
        distribution.add(0.1);
//        List<State> states = new ArrayList<State>();
//        for (int i = 0; i < distribution.size(); i++) {
//            states.add(new State(i));
//        }
//        // check it;
//        int[] times = new int[states.size()];
//        int len = 1000000;
//        for (int i = 0; i < len; i++) {
//            State nextState = randomStateByDistribution(distribution, states);
////            System.out.println("State " + nextState.getIndex());
//            times[nextState.getIndex()] += 1;
//        }
//
//        distribution = Renormalization(distribution);
//        for (int i = 0; i < distribution.size(); i++) {
//            System.out.println(distribution.get(i));
//        }
//
//        for (int i = 0; i < times.length; i++) {
//            System.out.println("State " + i + " : " + (double)times[i]/len);
//        }



    }

}
