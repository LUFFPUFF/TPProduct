package com.example.domain.tbank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Task3 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int n = scanner.nextInt();
        int m = scanner.nextInt();
        scanner.nextLine();

        int[] km = new int[n];
        for (int i = 0; i < n; i++) {
            km[i] = scanner.nextInt();
        }

        System.out.println(countCorrection(n, m, km));
    }

    private static int countCorrection(int n, int m, int[] km) {
        int element1 = km[0];
        int element2 = km[1];

        List<Integer> corrections = new ArrayList<>();

        for (int i = 2; i < n; i++) {
            int ai = km[i];

            if (ai < element1) {
                corrections.add(element1 - ai);
            } else if (ai > element2) {
                corrections.add(ai - element2);
            } else {
                corrections.add(0);
            }
        }

        Collections.sort(corrections);

        int totalCorrection = 0;
        for (int i = 0; i < m; i++) {
            totalCorrection += corrections.get(i);
        }

        return totalCorrection;
    }
}
