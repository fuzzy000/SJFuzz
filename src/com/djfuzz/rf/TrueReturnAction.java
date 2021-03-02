package com.djfuzz.rf;

import com.djfuzz.MutateClass;

import java.io.IOException;
import java.util.List;

/**
 * Created by Yicheng Ouyang on 2021/1/14
 */
public class TrueReturnAction implements Action {
    @Override
    public State proceedAction(MutateClass target, List<State> total) {
        try {
            MutateClass newOne = target.evoReturnIteration(true);
            newOne.setFromDiversity(false);
            State nextState = new State();
            nextState.setTarget(newOne);
            nextState.setCurrentMethod(target.getCurrentMethod());
            return nextState;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
