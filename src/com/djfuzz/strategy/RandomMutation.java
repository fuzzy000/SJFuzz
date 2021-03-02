package com.djfuzz.strategy;

import com.djfuzz.record.Recover;
import com.djfuzz.rf.*;

import java.io.IOException;
import java.util.Random;

public class RandomMutation {
    private static Action gotoAction = new TrueGotoAction();
    private static Action lookupAction = new TrueLookupAction();
    private static Action returnAction = new TrueReturnAction();
    private static Random randomGenerator = new Random();

    public static State regenerateState(State state) throws IOException {
        State newState = new State();
        newState.setTarget(Recover.recoverFromPath(state.getTarget()));
        return newState;
    }

    public static State randomMutation(State current) {
        int mark = randomGenerator.nextInt(3);
        switch (mark) {
            case 0: return gotoAction.proceedAction(current.getTarget(), null);
            case 1: return lookupAction.proceedAction(current.getTarget(), null);
            case 2: return returnAction.proceedAction(current.getTarget(), null);

        }
        return null;
    }

    public static void main(String[] args) {

    }
}
