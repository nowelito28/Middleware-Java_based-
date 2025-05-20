package app.figures;

import app.Figure;
import app.Point;

import java.util.ArrayList;

public class Rectangle extends Figure {
    public int altura;
    public int base;
    public Point p;                             // Asumir esquina izquierda inferior

    public Rectangle(int altura, int base, Point p, String color) {
        super(p, color);
        this.altura = altura;
        this.base = base;
        this.area = this.calcArea();
        this.p = p;
    }

    @Override
    public double calcArea() {
        return (double) altura * base;
    }

    @Override
    public String toString() {
        return "Rectangle:" + p.toString() + ":" + color +
                ":" + this.altura + ":" + this.base +
                ":" + this.calcArea() + ":" + this.id + ":";
    }

    @Override
    public boolean ispnt(Point p) {
        return p.x >= this.p.x && p.x <= this.p.x + this.base &&
                p.y >= this.p.y && p.y <= this.p.y + this.altura;
    }

    public static Figure parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Rectángulo need at least 4 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        var h = Integer.parseInt(s.get(2));
        var b = Integer.parseInt(s.get(3));
        return new Rectangle(h, b, p, color);
    }
}
