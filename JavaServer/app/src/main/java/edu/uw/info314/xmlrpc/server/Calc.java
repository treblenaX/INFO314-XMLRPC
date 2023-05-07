package edu.uw.info314.xmlrpc.server;

import java.util.Arrays;

public class Calc {
    public int add(int... args) {
        int result = 0;
        for (int arg : args) { result += arg; }
        return result;
    }
    public int subtract(int... args) {
        if (args.length == 0) { return 0; }
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            result -= args[i];
        }
        return result;
    }
    public int multiply(int... args) {
        if (args.length == 0) { return 0; }
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            result *= args[i];
        }
        return result;
    }
    public int divide(int... args) {
        if (args.length == 0) { return 0; }
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            result /= args[i];
        }
        return result;
    }
    public int modulo(int... args) {
        if (args.length == 0) { return 0; }
        System.out.println(Arrays.toString(args));
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            System.out.println(result);
            result %= args[i];
            System.out.println(result);
        }
        return result;
    }
}
