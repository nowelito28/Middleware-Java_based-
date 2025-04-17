package app.figuras;

import app.Figura;
import app.Punto;

import java.util.ArrayList;

public class Circulo extends Elipse {

    public Circulo(int r, Punto p, String color) {
        super(r, r, p, color);
        this.p = p;
    }

    @Override
    public String toString() {
        return "Circulo:" + p.toString()  + ":" + color +
                ":" + this.semiejeMayor +
                ":" + this.calcArea()  + ":" + this.id + ":";
    }

    public static Figura parse(ArrayList<String> s) {       // Implementar para todas las figuras
        if (s.size() < 3) {
            throw new RuntimeException("Círculo need at least 3 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var r = Integer.parseInt(s.get(2));
        return new Circulo(r, p, color);
    }
}
