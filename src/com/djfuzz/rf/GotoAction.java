package com.djfuzz.rf;

import com.djfuzz.MutateClass;

import java.io.IOException;
import java.util.List;

public class GotoAction implements Action{
    @Override
    public State proceedAction(MutateClass target, List<State> total) {
        try {
            MutateClass newOne = target.evoGotoIteration(false);
            State nextState = new State();
            if (newOne != null)
                newOne.setCorrespondingState(nextState);
            nextState.setTarget(newOne);
            nextState.setCurrentMethod(target.getCurrentMethod());
            return nextState;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
