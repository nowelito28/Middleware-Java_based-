package app.figuras;

import app.Figura;
import app.Punto;

import java.util.ArrayList;

public class Cuadrado extends Rectangulo {
    public Cuadrado(int lado, Punto p, String color) {
        super(lado, lado, p, color);
        this.p = p;
    }

    @Override
    public String toString() {
        return "Cuadrado:" + p.toString() + ":" + color +
                ":" + this.altura +
                ":" + this.calcArea() + ":" + this.id + ":";
    }

    public static Figura parse(ArrayList<String> s) {
        if (s.size() < 3) {
            throw new RuntimeException("Cuadrado need at least 3 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var l = Integer.parseInt(s.get(2));
        return new Cuadrado(l, p, color);
    }
}
