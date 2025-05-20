package app.figures;

import app.Figure;
import app.Point;

import java.util.ArrayList;

public class Ellipse extends Figure {
    public int semiejeMayor;
    public int semiejeMenor;
    public Point p;                         // Asumir punto central

    public Ellipse(int semiejeMayor, int semiejeMenor, Point p, String color) {
        super(p, color);
        this.semiejeMayor = semiejeMayor;
        this.semiejeMenor = semiejeMenor;
        this.area = this.calcArea();
        this.p = p;
    }

    @Override
    public double calcArea() {
        return Math.PI * semiejeMayor * semiejeMenor;
    }

    @Override
    public String toString() {
        return "Ellipse:" + p.toString()  + ":" + color +
                ":" + this.semiejeMayor + ":" + this.semiejeMenor +
                ":" + this.calcArea()  + ":" + this.id + ":";
    }

    @Override
    public boolean ispnt(Point p) {
        var dx = (double) (p.x - this.p.x) / this.semiejeMayor;
        var dy = (double) (p.y - this.p.y) / this.semiejeMenor;
        return (dx * dx + dy * dy) <= 1;
    }

    public static Figure parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Ellipse need at least 4 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        var ejMa = Integer.parseInt(s.get(2));
        var ejMe = Integer.parseInt(s.get(3));
        return new Ellipse(ejMa, ejMe, p, color);
    }
}
