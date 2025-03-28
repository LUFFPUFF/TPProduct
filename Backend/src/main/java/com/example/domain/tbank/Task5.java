package com.example.domain.tbank;

import java.util.Scanner;

public class Task5 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n = sc.nextInt();
        long s = sc.nextLong();

        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextLong();
        }

        System.out.println(calculateSum(n, s, a));
    }

    private static long calculateSum(int n, long s, long[] a) {
        long total = 0;

        for (int l = 0; l < n; l++) {
            long sum = 0;
            int parts = 1;

            for (int r = l; r < n; r++) {
                if (a[r] > s) {
                    break;
                }

                sum += a[r];
                if (sum > s) {
                    parts++;
                    sum = a[r];
                }

                total += parts;
            }
        }

        return total;
    }
}
