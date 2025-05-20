package app.figures;

import app.Figure;
import app.Point;

import java.util.ArrayList;

public class Triangle extends Figure {
    public int base;
    public int altura;
    public Point p;                                 // Asumir esquina izquierda inferior

    public Triangle(int base, int altura, Point p, String color) {
        super(p, color);
        this.base = base;
        this.altura = altura;
        this.area = this.calcArea();
        this.p = p;
    }

    @Override
    public double calcArea() {
        return (double) (base * altura) / 2;
    }

    @Override
    public String toString() {
        return "Triangle:" + p.toString() + ":" + color +
                ":" + this.base + ":" + this.altura +
                ":" + this.area + ":" + this.id + ":";
    }

    private int prodvec(Point a, Point b, Point p) {  // Producto vectorial 2D
        return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
    }

    /**
     * Suponer siempre el triángulo con el vertice para arriba <br>
     *  A -> Point base => Vertice izquierda de la base <br>
     *  B -> Vertice derecha de la base <br>
     *  C -> Vertice superior <br>
     *  Calcular productos vectoriales entre cada punto: <br>
     *  Si es mayor o igual que cero => el punto está dentro del triángulo <br>
     *  Si los tres tienen el mismo signo, el punto está dentro del triángulo
     * @param p -> Punto a ver si está dentro
     * @return -> Verdadero si el punto está dentro | Falso si no
     */
    @Override
    public boolean ispnt(Point p) {
        var A = this.p;
        var B = new Point(A.x + base, A.y);
        var C = new Point(A.x + base / 2, A.y + altura);
        var sig1 = prodvec(A, B, p) >= 0;
        var sig2 = prodvec(B, C, p) >= 0;
        var sig3 = prodvec(C, A, p) >= 0;
        return (sig1 == sig2) && (sig2 == sig3);
    }

    public static Figure parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Triangle need at least 4 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        var b = Integer.parseInt(s.get(2));
        var h = Integer.parseInt(s.get(3));
        return new Triangle(b, h, p, color);
    }
}
