package edu.uw.info314.xmlrpc.server;

import java.util.Arrays;

public class Calc {
    public int add(int... args) throws ArithmeticException {
        if (args.length == 0) return 0;
        if (args.length == 1) return args[0];
        int result = 0;
        for (int i : args) {
            result = Math.addExact(result, i);
        }
        return result;
    }
    public int subtract(int... args) {
        if (args.length == 0) return 0;
        if (args.length == 1) return args[0];
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            result = Math.subtractExact(result, args[i]);
        }
        return result;
    }
    public int multiply(int... args) {
        if (args.length == 0) return 0;
        if (args.length == 1) return args[0];
        int result = 1;
        for (int i : args) {
            result = Math.multiplyExact(result, i);
        }
        return result;
    }
    public int divide(int... args) throws ArithmeticException {
        if (args.length == 0) return 0;
        if (args.length == 1) return args[0];
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            int dividend = args[i];
            if (dividend == 0) throw new ArithmeticException("/ by zero");
            result /= dividend;
        }
        return result;
    }
    public int modulo(int... args) {
        if (args.length == 0) return 0;
        if (args.length == 1) return args[0];
        int result = args[0];
        for (int i = 1; i < args.length; i++) {
            int dividend = args[i];
            result %= dividend;
            if (dividend == 0) throw new ArithmeticException("/ by zero");
        }
        return result;
    }
}
