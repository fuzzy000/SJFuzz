package com.djfuzz.rf;

import com.djfuzz.MutateClass;
import com.djfuzz.record.Recover;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class BacktrackAction implements Action{
    @Override
    public State proceedAction(MutateClass target, List<State> total) {
        Random random = new Random();
        int selectedIndex = random.nextInt(total.size());
//        return total.get(selectedIndex);
        State current = total.get(selectedIndex);
        try {
            Recover.recoverFromPath(current.getTarget());
        } catch (IOException e) {
            System.out.println("should not recover failed.");
        }
        return current;
//        String actionString = current.selectActionWithoutBacktrack();
//        Action action = RfFramework.getActionContainer().get(actionString);
//        try {
//            Recover.recoverFromPath(current.getTarget());
//        } catch (IOException e) {
//            System.out.println("should not recover failed.");
//        }
//        return action.proceedAction(current.getTarget(), total);
    }
}
