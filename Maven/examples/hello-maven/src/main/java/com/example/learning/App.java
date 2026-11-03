package com.example.learning;

public final class App {
    private App() {
    }

    public static int addMoney(int firstInCents, int secondInCents) {
        if (firstInCents < 0 || secondInCents < 0) {
            throw new IllegalArgumentException("Amounts must not be negative");
        }
        return Math.addExact(firstInCents, secondInCents);
    }

    public static void main(String[] args) {
        System.out.println("Hello, Maven!");
        System.out.println("Total cents: " + addMoney(9900, 3000));
    }
}
