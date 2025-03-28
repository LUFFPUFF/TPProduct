package com.example.domain.tbank;

import java.util.Scanner;

public class Task1 {

    public static void main(String[] args) {
        String line = new Scanner(System.in).nextLine();
        System.out.println(definitionPositionR(line));
    }

    private static String definitionPositionR(String line) {
        int posR = line.indexOf('R');
        int posM = line.indexOf('M');

        if (posR < posM) {
            return "Yes";
        }

        return "No";
    }
}
