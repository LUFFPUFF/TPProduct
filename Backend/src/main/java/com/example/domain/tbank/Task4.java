package com.example.domain.tbank;

import java.util.Scanner;

public class Task4 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n = sc.nextInt();
        int x = sc.nextInt();
        int y = sc.nextInt();
        int z = sc.nextInt();

        long[] arr = new long[n];
        for (int i = 0; i < n; i++) {
            arr[i] = sc.nextLong();
        }

        System.out.println(countOperations(n, x, y, z, arr));
    }

    private static long countOperations(int n, int x, int y, int z, long[] arr) {
        long[] costX = new long[n];
        long[] costY = new long[n];
        long[] costZ = new long[n];

        for (int i = 0; i < n; i++) {
            costX[i] = (x - (arr[i] % x)) % x;
            costY[i] = (y - (arr[i] % y)) % y;
            costZ[i] = (z - (arr[i] % z)) % z;
        }

        long minSingle = Long.MAX_VALUE;
        long minXY = Long.MAX_VALUE;
        long minXZ = Long.MAX_VALUE;
        long minYZ = Long.MAX_VALUE;
        long minX = Long.MAX_VALUE;
        long minY = Long.MAX_VALUE;
        long minZ = Long.MAX_VALUE;

        for (int i = 0; i < n; i++) {

            long allConditions = Math.max(costX[i], Math.max(costY[i], costZ[i]));
            minSingle = Math.min(minSingle, allConditions);

            minXY = Math.min(minXY, Math.max(costX[i], costY[i]));
            minXZ = Math.min(minXZ, Math.max(costX[i], costZ[i]));
            minYZ = Math.min(minYZ, Math.max(costY[i], costZ[i]));

            minX = Math.min(minX, costX[i]);
            minY = Math.min(minY, costY[i]);
            minZ = Math.min(minZ, costZ[i]);
        }

        long result = minSingle;
        result = Math.min(result, minXY + minZ);
        result = Math.min(result, minXZ + minY);
        result = Math.min(result, minYZ + minX);
        result = Math.min(result, minX + minY + minZ);

        return result;
    }
}
