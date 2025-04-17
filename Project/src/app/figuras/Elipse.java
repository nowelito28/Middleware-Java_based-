package app.figuras;

import app.Figura;
import app.Punto;

import java.util.ArrayList;

public class Elipse extends Figura {
    public int semiejeMayor;     // Protegemos variables de la clase para que Círculo pueda acceder
    public int semiejeMenor;
    public Punto p;

    public Elipse(int semiejeMayor, int semiejeMenor, Punto p, String color) {     // Constructor
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
        return "Elipse:" + p.toString()  + ":" + color +
                ":" + this.semiejeMayor + ":" + this.semiejeMenor +
                ":" + this.calcArea()  + ":" + this.id + ":";
    }

    @Override
    public boolean ispnt(Punto p) {     // Para el círculo es igual, ya que es un tipo de elipse
        var dx = (double) (p.x - this.p.x) / this.semiejeMayor;
        var dy = (double) (p.y - this.p.y) / this.semiejeMenor;
        return (dx * dx + dy * dy) <= 1;
    }

    public static Figura parse(ArrayList<String> s) {
        if (s.size() < 4) {
            throw new RuntimeException("Elipse need at least 4 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var ejMa = Integer.parseInt(s.get(2));
        var ejMe = Integer.parseInt(s.get(3));
        return new Elipse(ejMa, ejMe, p, color);
    }
}
