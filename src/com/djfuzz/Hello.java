package com.djfuzz;

public class Hello {
    public static void main(String[] args) {
        process(args);
    }
    public static void process(String[] args) {
        System.out.println("hello");
        int sum = 0;
        for (int i = 0; i < 100; i ++) {
            sum += i;
        }
        if (sum % 2 != 0) {
            System.out.println("nonono");
        }
        if (sum == 0) {
            System.out.println("nononooooooooooooooooooooo");
        }
        if (sum == 1) {
            System.out.println("nononooooooooooooooooooooo");
        }
        if (sum == 2) {
            System.out.println("nononooooooooooooooooooooo");
        }
        if (sum == 3) {
            System.out.println("nononooooooooooooooooooooo");
        }
        if (sum == 4) {
            System.out.println("nononooooooooooooooooooooo");
        }
        if (sum == 5) {
            System.out.println("nononooooooooooooooooooooo");
        }
        print("hello1");
        (new Hello()).test();
        System.out.println(sum);
    }

    public void test() {
        int sum = 0;
        for (int i = 0; i < 100; i ++) {
            sum += i;
        }
        if (sum == 0) {
            System.out.println("nononooooooooooooooooooooo");
        }
    }

    public static void print(String target) {
        int sum = 0;
        for (int i = 0; i < 100; i ++) {
            sum += i;
        }
        if (sum == 0) {
            System.out.println("nononooooooooooooooooooooo");
        }
        System.out.println(target);
    }
}
