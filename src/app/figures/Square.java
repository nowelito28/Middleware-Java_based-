package app.figures;

import app.Figure;
import app.Point;

import java.util.ArrayList;

public class Square extends Rectangle {
    public Square(int lado, Point p, String color) {
        super(lado, lado, p, color);
        this.p = p;                                  // Asumir esquina izquierda inferior
    }

    @Override
    public String toString() {
        return "Square:" + p.toString() + ":" + color +
                ":" + this.altura +
                ":" + this.calcArea() + ":" + this.id + ":";
    }

    public static Figure parse(ArrayList<String> s) {
        if (s.size() < 3) {
            throw new RuntimeException("Square need at least 3 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        var l = Integer.parseInt(s.get(2));
        return new Square(l, p, color);
    }
}
