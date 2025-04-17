package app.figuras;

import app.Figura;
import app.Punto;
import network.Tag;

import java.util.*;

public class Grupo extends Figura {
    public Punto p;
    public String color;
    public List<Figura> figs;
    public Tag tags;                    // IDs de las figuras que lo componen

    public Grupo(Punto p, String color, Figura... figs) {
        super(p, color);  // Usa el punto de referencia y color que se pasa como arg para el grupo
        this.figs = new ArrayList<>(Arrays.asList(figs));
        this.tags = new Tag();
        for (Figura fig : figs) {   // Jerarquía de IDs de figuras de dentro del conjunto
            fig.id = tags.newtag();
        }
        this.area = calcArea();
        this.p = p;
        this.color = color;
    }

    public void addfig(Figura fig) {    // Todas las figuras al mismo nivel => Todas las figuras = misma Figura(conjunto)
        if (figs.contains(fig)) {
            throw new RuntimeException("Group has already figure id " + fig.id);
        }
        fig.id = tags.newtag();     // Genera nuevo tag y lo pone en uso
        figs.add(fig);
        area = calcArea();  // Recalcular área
    }

    public void group(Figura... figs) {
        for (Figura fig : figs) {
            addfig(fig);
        }
    }

    public void delfig(Figura fig) {
        if (figs.contains(fig)) {
            figs.remove(fig);        // Eliminar figura
            tags.rmtag(fig.id.get());     // Eliminar id en uso
            area = calcArea();  // Recalcular área
        } else {
            throw new RuntimeException("Group does not have figure id " + fig.id);
        }
    }

    public void ungroup(Figura... figs) {
        for (Figura fig : figs) {
            delfig(fig);
        }
    }

    @Override
    public double calcArea() {  // Es la suma de las áreas, NO el área del conjunto
        double area_tot = 0;
        for (Figura fig : figs) {
            area_tot += fig.calcArea();
        }
        return area_tot;
    }

    @Override
    public void expand(double factor) {
        for (Figura fig : figs) { fig.expand(factor); }
    }

    public String printfigs() {
        StringBuilder s = new StringBuilder();
        if (!figs.isEmpty()) {
            s.append("/\n");                       // Inicio de figuras -> /\n
            for (Figura fig : figs) {   // Para separar entre descripciones de figuras --> "-:"..."\n"
                s.append("-:").append(fig.toString()).append("\n");
            }
            s.append("-/:");                       // Fin de figuras -> -/ + ":"(Añadir aquí los ":" por si no hay figs
            return s.toString();
        }
        return "";
    }

    @Override                                                   // Si no hay figuras -> Grupo:x,y:color:numfigs:area:id:
    public String toString() {                                  // Grupo:x,y:color:numfigs:/
        return "Grupo:" + p.toString() + ":" + color +          // -:Circulo:15,30:rojo:5:78.53981633974483:0:
                ":" + figs.size() + ":" + this.printfigs() +    // -:Rectangulo:25,35:verde:5:3:15.0:     ...
                 this.calcArea() + ":" + id + ":";              // -/:area:id:
    }

    @Override
    public boolean ispnt(Punto p) {
        for (Figura fig : figs) {
            if (fig.ispnt(p)) { return true; } // Si alguna figura contiene el punto, el grupo lo contiene
        }
        return false; // Si ninguna figura lo contiene, el grupo no lo contiene
    }

    public static Figura parse(ArrayList<String> s) throws RuntimeException {// Parse de grupo solo pasarle punto ref, color, (número figs + figs)
        if (s.size() < 3) {
            throw new RuntimeException("Grupo need at least 3 argument");
        }
        var p = Punto.parse(s.get(0));
        var color = s.get(1);
        var len = Integer.parseInt(s.get(2));   // Número de figuras
        for (int i = 0; i <= 2; i++) {      // Eliminar parámetros ya usados (3)
            s.removeFirst();                // Eliminamos primera posición, porque es dinámico, se reajustan posiciones
        }
        if (len > 0) {
            Figura[] figs = parse(len, s);                  // Crear con figuras
            return new Grupo(p, color, figs);
        }
        return new Grupo(p, color);           // Crear sin figuras
    }

    public static Figura[] parse(int len, ArrayList<String> ss) {    // Parsear figuras del grupo
        var pos = 0;
        var sfig = new StringBuilder();
        var figs = new Figura[len];
        var i = 0;                  // Contador de figuras del grupo (0 a len-1)
        while (i < len) {
            pos += 1;                               // Eliminar separador
            while (!ss.get(pos).startsWith("-")) {
                sfig.append(ss.get(pos)).append(":");        // Añadir componentes de figura a crear
                pos += 1;           // Siguiente posición
            }
            figs[i] = parse(sfig.toString());       // Añadir figura parseada
            i += 1;                                 // Siguiente posición de la figura
            sfig.setLength(0);                      // Vaciar string builder
        }
        return figs;
    }
}
