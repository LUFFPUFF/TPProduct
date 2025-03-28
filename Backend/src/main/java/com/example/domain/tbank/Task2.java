package com.example.domain.tbank;

import java.util.ArrayList;
import java.util.Scanner;

public class Task2 {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int day = sc.nextInt();

        for (int i = 1; i <= day; i++) {
            int a = sc.nextInt();
            System.out.println(maxFlowers(a));
        }
    }

    private static long maxFlowers(int a) {
        if (a < 7) return -1;

        ArrayList<Long> powers = new ArrayList<>();
        for (long p = 1; p <= a; p *= 2) {
            powers.add(p);
        }

        long initSum = -1;

        for (int i = 0; i < powers.size() - 1; i++) {
            for (int j = i + 1; j < powers.size(); j++) {
                for (int k = j + 1; k < powers.size(); k++) {

                    long sum = powers.get(i) + powers.get(j) + powers.get(k);

                    if (sum <= a) {
                        initSum = Math.max(initSum, sum);
                    }
                }
            }
        }

        return initSum;

    }
}
