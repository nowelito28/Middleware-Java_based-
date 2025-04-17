package app.figuras;

import app.Figura;
import app.Punto;

import java.util.ArrayList;

public class Rectangulo extends Figura {
    public int altura;
    public int base;
    public Punto p;      // Asumir esquina izquierda inferior

    public Rectangulo(int altura, int base, Punto p, String color) {
        super(p, color);
        this.altura = altura;
        this.base = base;
        this.area = this.calcArea();
        this.p = p;
    }

    @Override           // Sobreescribir función de la clase padre Figura
    public double calcArea() {
        return (double) altura * base;
    }

    @Override
    public String toString() {
        return "Rectangulo:" + p.toString() + ":" + color +
                ":" + this.altura + ":" + this.base +
                ":" + this.calcArea() + ":" + this.id + ":";
    }

    @Override
    public boolean ispnt(Punto p) {
        return p.x >= this.p.x && p.x <= this.p.x + this.base &&
                p.y >= this.p.y && p.y <= this.p.y + this.altura;
    }

    public static Figura parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Rectángulo need at least 4 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var h = Integer.parseInt(s.get(2));
        var b = Integer.parseInt(s.get(3));
        return new Rectangulo(h, b, p, color);
    }
}
