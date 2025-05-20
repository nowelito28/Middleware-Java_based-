package app;

import app.figures.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Figura --> clase abstracta -> NO se puede instanciar = modelo para figuras concretas
 */
public abstract class Figure {
    public Point p;                                     // Punto central donde se encuentra la figura
    public double area;                                 // Área de la figura
    public String color;                                // Color de la figura
    public AtomicInteger id;                            // Identificador de la figura
    private static final Map<String, Parser> parsers;   // Mapa de parsers/fabricas de figuras

    /**
     * Constructor básico de la figura
     * @param p -> Punto de referencia de la figura
     * @param color -> Color de la figura
     */
    public Figure(Point p, String color) {
        this.p = p;
        this.color = color;
    }

    /**
     * Declarar tipos de parse para cada Figure => Factory method <br>
     * parse => nombre del método con el que se fabrica cada figura
     */
    static  {
        parsers = new HashMap<>();
        parsers.put("circle", Circle::parse);
        parsers.put("rectangle", Rectangle::parse);
        parsers.put("square", Square::parse);
        parsers.put("triangle", Triangle::parse);
        parsers.put("ellipse", Ellipse::parse);
        parsers.put("group", Group::parse);
    }

    /**
     * Declarar interfaz para implementar en cada parser de cada figura por separado<br>
     * Mismo tipo de argumento para todos los parsers
     */
    @FunctionalInterface
    private interface Parser {
        Figure parse(ArrayList<String> s);
    }

    /**
     * Factory method -> Build figura -> con descripción de la figura en cuestión<br>
     * Parsear figuras con arrays de los componentes de las figuras en String
     * @param s -> Descripción de la figura
     * @return -> Figura pedida
     * @throws RuntimeException -> Si no se le pasa bien el formato
     */
    public static Figure parse(String s) throws RuntimeException {
        int p = s.indexOf(":");
        // Si no está el carácter que se le pasa -> devuelve índice -1:
        if (p < 0) {
            throw new IllegalArgumentException("Formato inválido, falta ':' en " + s);
        }
        // Quitar espacios de los extremos y poner en minúsculas:
        var name = s.substring(0, p).trim().toLowerCase();
        // Buscar parser correspondiente:
        Parser parse = parsers.get(name);
        if (parse == null) {
            throw new IllegalArgumentException("Figure desconocida: " + name);
        }
        // .substring()=Elimina nombre y primer ":".trim()=Quitar espacios en extremos:
        s = s.substring(p + 1).trim();
        // Hacer cambios generales de todas las figuras => punto ref y color:
        ArrayList<String> args = extract(s, ":");
        return parse.parse(args);
    }

    /**
     * Pasar a una lista con los componentes de la figura
     * @param s -> Descripción de la figura aplanada en un string
     * @param sep -> Separador de componentes en el string
     * @return -> Array con los componentes en formatos string
     */
    public static ArrayList<String> extract(String s, String sep) {
        return new ArrayList<>(List.of(s.split(sep)));
    }

    /**
     * Sacar id de la *descripción de una figura*<br>
     * ID -> al final de la descripción (último elemento-1) -> al terminar s en ":"
     * @param s -> Descripción de la figura (String) con elementos aplanados en el string separados por ":"
     * @return -> ID de la figura en cuestión (int)
     * @throws RuntimeException -> Si no se le pasa bien el formato
     */
    public static int parseid(String s) throws RuntimeException {
        var ss = s.split(":");
        return Integer.parseInt(ss[ss.length - 1]);
    }

    public void move(int dx, int dy) {
        p.move(dx, dy);
    }

    public void expand(double factor) {
        area *= factor;
    }

    /**
     * Métodos abstractos => obliga a programarlos en clases hijas
     */
    public abstract double calcArea();
    public abstract String toString() ;
    public abstract boolean ispnt(Point p);
}
