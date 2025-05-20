package app.figures;

import app.Figure;
import app.Point;
import protocol.Tag;

import java.util.*;

public class Group extends Figure {
    public Point p;
    public String color;
    public List<Figure> figs;
    public Tag tags;                        // IDs de las figuras que lo componen

    /**
     * Grupo de figuras
     * Con sus IDs correspondientes
     * @param p -> punto de referencia
     * @param color -> color de referencia para el grupo
     * @param figs -> Figuras que lo compone
     */
    public Group(Point p, String color, Figure... figs) {
        super(p, color);
        this.figs = new ArrayList<>(Arrays.asList(figs));
        this.tags = new Tag();
        for (Figure fig : figs) {
            fig.id = tags.newtag();
        }
        this.area = calcArea();
        this.p = p;
        this.color = color;
    }

    /**
     * Añadir figura al grupo <br>
     * Todas las figuras al mismo nivel
     * @param fig -> Figura a añadir
     *            --> Genera nuevo tag en uso y recalcular área
     */
    public void addfig(Figure fig) {
        if (figs.contains(fig)) {
            throw new RuntimeException("Group has already figure id " + fig.id);
        }
        fig.id = tags.newtag();
        figs.add(fig);
        area = calcArea();
    }

    public void group(Figure... figs) {
        for (Figure fig : figs) {
            addfig(fig);
        }
    }

    /**
     * Eliminar figura del grupo <br>
     * Todas las figuras al mismo nivel
     * @param fig -> Figura a eliminar
     *            --> Eliminar id en uso y recalcular área
     */
    public void delfig(Figure fig) {
        if (figs.contains(fig)) {
            figs.remove(fig);
            tags.rmtag(fig.id.get());
            area = calcArea();
        } else {
            throw new RuntimeException("Group does not have figure id " + fig.id);
        }
    }

    public void ungroup(Figure... figs) {
        for (Figure fig : figs) {
            delfig(fig);
        }
    }

    /**
     * Es la suma de las áreas, NO el área del conjunto
     */
    @Override
    public double calcArea() {
        double area_tot = 0;
        for (Figure fig : figs) {
            area_tot += fig.calcArea();
        }
        return area_tot;
    }

    @Override
    public void expand(double factor) {
        for (Figure fig : figs) { fig.expand(factor); }
    }

    /**
     * Imprimir figuras del grupo -> aplanarlas en un string: <br>
     * Inicio de figuras -> "/\n" <br>
     * Para separar entre descripciones de figuras --> "-:"..."\n" <br>
     * Fin de figuras -> "-/" + ":" -> (Añadir aquí los ":" por si no hay figs
     * @return -> String con figuras aplanadas
     */
    public String printfigs() {
        StringBuilder s = new StringBuilder();
        if (!figs.isEmpty()) {
            s.append("/\n");
            for (Figure fig : figs) {
                s.append("-:").append(fig.toString()).append("\n");
            }
            s.append("-/:");
            return s.toString();
        }
        return "";
    }

    /**
     * **Si no hay figuras: <br>
     * Group:x,y:color:numfigs=0:area:id: <br>
     * **Si hay figuras: <br>
     * Group:x,y:color:numfigs:/ <br>
     * -:Circle:15,30:rojo:5:78.53981633974483:0: <br>
     * -:Rectangle:25,35:verde:5:3:15.0:     ... <br>
     * -/:area:id:
     * @return -> Descripción del grupo de figuras
     */
    @Override
    public String toString() {
        return "Group:" + p.toString() + ":" + color +
                ":" + figs.size() + ":" + this.printfigs() +
                 this.calcArea() + ":" + id + ":";
    }

    @Override
    public boolean ispnt(Point p) {
        for (Figure fig : figs) {
            if (fig.ispnt(p)) { return true; }
        }
        return false;
    }

    /**
     * Parse de grupo solo pasarle:
     * @param s -> ArrayList de strings necesarios para crear el grupo -> punto ref, color, (número figs + figs)
     * @return Grupo de figuras
     * @throws RuntimeException -> Si no hay suficientes argumentos
     */
    public static Figure parse(ArrayList<String> s) throws RuntimeException {
        if (s.size() < 3) {
            throw new RuntimeException("Group need at least 3 argument");
        }
        var p = Point.parse(s.get(0));
        var color = s.get(1);
        // Número de figuras:
        var len = Integer.parseInt(s.get(2));
        // Eliminar parámetros ya usados (3):
        for (int i = 0; i <= 2; i++) {
            // Vamos eliminando primera posición -> dinámico -> se reajustan posiciones:
            s.removeFirst();
        }
        if (len > 0) {
            // Crear con figuras:
            Figure[] figs = parse(len, s);
            return new Group(p, color, figs);
        }
        // Crear sin figuras:
        return new Group(p, color);
    }

    /**
     * Parsear figuras del grupo
     * @param len -> Número de figuras
     * @param ss -> ArrayList de descripciones de figuras (strings)
     * @return -> Array de figuras a añadir al grupo creado
     */
    public static Figure[] parse(int len, ArrayList<String> ss) {
        var pos = 0;
        var sfig = new StringBuilder();
        var figs = new Figure[len];
        var i = 0;
        // Contador de figuras del grupo (0 a len-1):
        while (i < len) {
            pos += 1;
            // Eliminar separador -> "-":
            while (!ss.get(pos).startsWith("-")) {
                // Añadir componentes de figura a crear:
                sfig.append(ss.get(pos)).append(":");
                pos += 1;
            }
            // Añadir figura parseada:
            figs[i] = parse(sfig.toString());
            i += 1;
            // Vaciar string builder para siguiente figura:
            sfig.setLength(0);
        }
        return figs;
    }
}
