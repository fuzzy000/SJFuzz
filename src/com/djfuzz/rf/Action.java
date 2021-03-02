package com.djfuzz.rf;

import com.djfuzz.MutateClass;

import java.util.List;

public interface Action {
    public State proceedAction(MutateClass target, List<State> total);
}
