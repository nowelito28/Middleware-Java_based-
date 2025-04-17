package app.figuras;

import app.Figura;
import app.Punto;

import java.util.ArrayList;

public class Triangulo extends Figura {
    public int base;
    public int altura;
    public Punto p;

    public Triangulo(int base, int altura, Punto p, String color) {
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
        return "Triangulo:" + p.toString() + ":" + color +
                ":" + this.base + ":" + this.altura +
                ":" + this.area + ":" + this.id + ":";
    }

    private int prodvec(Punto a, Punto b, Punto p) {  // Producto vectorial 2D
        return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
    }

    @Override
    public boolean ispnt(Punto p) {     // Suponer siempre el triángulo con el vertice para arriba
        var A = this.p;  // Punto base => Vertice izquierda de la base
        var B = new Punto(A.x + base, A.y);  // Vertice derecha de la base
        var C = new Punto(A.x + base / 2, A.y + altura);  // Vertice superior
        var sig1 = prodvec(A, B, p) >= 0;
        var sig2 = prodvec(B, C, p) >= 0; // Calcular productos vectoriales entre cada punto,
        var sig3 = prodvec(C, A, p) >= 0; // si es mayor o igual que cero => el punto está dentro
        return (sig1 == sig2) && (sig2 == sig3);    // Si los tres tienen el mismo signo, el punto está dentro del triángulo
    }

    public static Figura parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Triangulo need at least 4 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var b = Integer.parseInt(s.get(2));
        var h = Integer.parseInt(s.get(3));
        return new Triangulo(b, h, p, color);
    }
}
