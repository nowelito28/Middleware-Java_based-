package app;

import app.figuras.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Figura {
    public Punto p;
    public double area;
    public String color;
    public AtomicInteger id;
    private static final Map<String, Parser> parsers;

    public Figura(Punto p, String color) {  // Al declarar figura en dibujo, ponerle un ID, sumando uno cada id
        this.p = p;
        this.color = color;
    }

    static  {                       // Declarar tipos de parse para cada Figura => Factory method
        parsers = new HashMap<>();
        parsers.put("circulo", Circulo::parse); //parse => nombre del método de círculo
        parsers.put("rectangulo", Rectangulo::parse);
        parsers.put("cuadrado", Cuadrado::parse);
        parsers.put("triangulo", Triangulo::parse);
        parsers.put("elipse", Elipse::parse);
        parsers.put("grupo", Grupo::parse);
    }

    @FunctionalInterface
    private interface Parser {      // Declarar interfaz para implementar en cada figura por separado
        Figura parse(ArrayList<String> s);
    }

    public static Figura parse(String s) throws RuntimeException {  // Factory method -> Build figura -> con descripción
        int p = s.indexOf(":");
        if (p < 0) {                // Si no está el carácter que se le pasa, devuelve índice -1
            throw new IllegalArgumentException("Formato inválido, falta ':' en " + s);
        }
        var name = s.substring(0, p).trim().toLowerCase(); // Quitar espacios de los extremos y poner en minúsculas
        Parser parse = parsers.get(name); // Buscar parser correspondiente
        if (parse == null) {
            throw new IllegalArgumentException("Figura desconocida: " + name);
        }
        s = s.substring(p + 1).trim();// .substring()=Elimina nombre y primer ":"|.trim()=Quitar espacios en extremos
        ArrayList<String> args = extract(s, ":");    // Hacer cambios generales de todas las figuras => punto ref y color
        return parse.parse(args);       // Parsear figuras con arrays de los componentes de las figuras
    }

    public static ArrayList<String> extract(String s, String sep) { // Pasar a una lista con los componentes
        return new ArrayList<>(List.of(s.split(sep)));    // Separados por sep
    }

    public static int parseid(String s) throws RuntimeException {                  // Sacar el id de la *descripción de una figura*
        var ss = s.split(":");                       // Array con los elementos de la figura
        return Integer.parseInt(ss[ss.length - 1]);    // ID -> al final de la descripción (último elemento-1) -> al terminar s en ":"
    }   // Devolvemos el id

    public void move(int dx, int dy) {
        p.move(dx, dy);
    }

    public void expand(double factor) {
        area *= factor;
    }

    public abstract double calcArea();  // Método abstracto => obliga a programarlo en clases hijas
    public abstract String toString() ;
    public abstract boolean ispnt(Punto p);
}
