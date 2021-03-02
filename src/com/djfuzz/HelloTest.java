package com.djfuzz;

import org.junit.Test;

public class HelloTest {
    @Test
    public void testHello() {
        Hello hello = new Hello();
        Hello.process(null);
    }
}
