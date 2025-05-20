package app.figures;

import app.Figure;
import app.Point;

import java.util.ArrayList;

public class Circle extends Ellipse {

    public Circle(int r, Point p, String color) {
        super(r, r, p, color);
        this.p = p;                                 // Asumir punto central
    }

    @Override
    public String toString() {
        return "Circle:" + p.toString()  + ":" + color +
                ":" + this.semiejeMayor +
                ":" + this.calcArea()  + ":" + this.id + ":";
    }

    public static Figure parse(ArrayList<String> s) {
        if (s.size() < 3) {
            throw new RuntimeException("Círculo need at least 3 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        var r = Integer.parseInt(s.get(2));
        return new Circle(r, p, color);
    }
}
