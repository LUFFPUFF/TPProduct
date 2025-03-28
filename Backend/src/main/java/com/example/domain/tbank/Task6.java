package com.example.domain.tbank;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Task6 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        List<Point> points = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            points.add(new Point(x, y));
        }

        if (n < 3) {
            System.out.println(0);
            return;
        }


        boolean allCollinear = true;
        Point p1 = points.get(0);
        Point p2 = points.get(1);

        for (int i = 2; i < n; i++) {
            if (!isCollinear(p1, p2, points.get(i))) {
                allCollinear = false;
                break;
            }
        }

        if (allCollinear) {
            System.out.println(0);
        } else {
            System.out.println(n / 3);
        }
    }

    private static boolean isCollinear(Point a, Point b, Point c) {

        return (b.x - a.x) * (c.y - a.y) == (b.y - a.y) * (c.x - a.x);
    }

    static class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
