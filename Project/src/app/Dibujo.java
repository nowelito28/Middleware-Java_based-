package app;

import network.Tag;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class Dibujo {
    public AtomicInteger id;
    public final String autor;
    public LinkedList<Figura> figs;
    public Tag tags;                            // Tags de figuras en uso

    public Dibujo(int id, String autor, Figura... figs) {
        this.id = new AtomicInteger(id);                   // ID del dibujo
        this.autor = autor;             // Nombre del autor
        this.figs = new LinkedList<>();
        tags = new Tag();
        this.figs.addAll(List.of(figs));    // Añadir todas las figuras del dibujo con sus IDs
    }

    public String dump() {
        var sb = new StringBuilder();
        for (Figura fig : figs) {
            sb.append(fig.toString()).append("\n");   // Para separar entre figuras "\n"
        }
        return sb.toString();
    }

    public String dump(int perms) {             // Volcado del dibujo con permisos
        return this + "//" + perms + "\n\n";
    }

    public String getdate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm:ss"));

    }

    // Dibujo:id:autor:fecha
    @Override                                                   // Figura1
    public String toString() {                                  // Figura2...
        return "Dibujo:" + this.id + ":" + autor + ":" +
                getdate() + "\n" + dump();                            // Termina en "\n"
    }

    public void addfig(Figura fig, int id) {    // Suponer que la última figura se superpone a las anteriores
        if (tags.revise(id)) {      // Se pueden añadir figuras iguales --> pero no mismo id
            throw new RuntimeException("Have already figure id " + id);
        }
        if (id <= 0) {     // Genera nuevo tag y lo pone en uso
            fig.id = tags.newtag();
        } else {           // Añadir figura con id en uso
            fig.id = tags.addtag(id);
        }
        figs.add(fig);          // Añadir figura al dibujo
    }

    public void delfig(Figura fig) {
        if (figs.contains(fig)) {
            figs.remove(fig);        // Eliminar figura
            tags.rmtag(fig.id.get());     // Eliminar id en uso
        } else {
            throw new RuntimeException("No figure with id " + fig.id);
        }
    }

    public Figura getfig(int id) {              // Buscar figura por su id
        for (var fig : figs) {
            if (fig.id.get() == id) {
                return fig;
            }
        }
        throw new RuntimeException("No figure found in draw -> " + autor + ":" + id);
    }

    public void move(int dx, int dy) {
        for (Figura fig : figs) {
            fig.move(dx, dy);
        }
    }

    public void mvfigs(String fids, String sep, int x, int y) {   // Mover según ids de figuras específicas
        for (var fid : fids.split(sep)) {
            try {
                var id = Integer.parseInt(fid);         // ID de figura a borrar
                var fig = getfig(id);              // Figura del dibujo
                fig.move(x, y);                         // Borrar figura
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void expand(double factor) {
        for (Figura fig : figs) {
            fig.expand(factor);
        }
    }

    public List<Figura> isin(Punto p) {     // Comprueba si el punto está dentro de alguna figura,
        List<Figura> figsin = new ArrayList<>();    // y las guarda en una lista, si está vacía => está fuera del draw
        for (Figura fig : figs) {
            if (fig.ispnt(p)) {
                figsin.add(fig);
            }
        }
        return figsin;
    }

    public boolean ispt(Punto p) {
        return !isin(p).isEmpty();
    }

    public static File newdir(String dirpath) {         // Crear directorio en el path actual si no existe
        var dir = new File(dirpath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.err.println("Directorio creado: " + dirpath);
            } else {
                throw new RuntimeException("No se pudo crear el directorio: " + dirpath);
            }
        }   // Si el directorio ya existe, no hace nada
        return dir;
    }

    public static File checkdir(String dname) {         // Verificar si existe el directorio y crearlo si no
        String wdir = System.getProperty("user.dir");   // Path directorio de trabajo
        String dirpath = wdir + File.separator + dname;  // Path al directorio
        return newdir(dirpath);
    }

    public void saveTo(String f, String dname, int permisos) throws RuntimeException { // Guardar figuras en el fichero que se le pasa (dname/f), si no existe se crea en 'f'
        File dir = checkdir(dname);
        var file = new File(dir, f);    // Objeto File con el nombre/ruta al fichero en el directorio dir
        try (var fow = new FileOutputStream(file, false); // true = modo append/false = modo sobreescribir
             var osw = new OutputStreamWriter(fow, StandardCharsets.UTF_8);// Puedo especificar code UTF-8 al hacer el stream
             var pw = new PrintWriter(new BufferedWriter(osw))) { // Agregar buffering para mayor rapidez
            pw.print(dump(permisos));                 // Imprimir this + "//" + perms + "\n\n"
        } catch (IOException e) {       //`fw` y `pw` se cierran automáticamente al terminar el try (fw.close())
            throw new RuntimeException(e);
        }
    }

    public int loadFrom(String f, String dname) throws RuntimeException {    // Cargar figuras del fichero del dibujo que se le pasa(dirname/f), si no existe da error
        File dir = checkdir(dname);
        var file = new File(dir, f); // Archivo a leer // No carga el dibujo entero, solo las figuras
        if (!file.exists()) {   // Comprobar que existe el fichero adjuntado
            throw new RuntimeException("El archivo no existe -> " + f);
        }
        return loadFrom(file, f);
    }

    public int loadFrom(File file, String f) throws RuntimeException {
        try (var fis = new FileInputStream(file);
             var isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             var br = new BufferedReader(isr)) {   // Leer fichero linea a linea con buffering
            if (br.readLine() == null) {  // Lee y descarta la primera línea (nombre del dibujo)
                throw new RuntimeException("El archivo está vacío -> " + f);// Comprobamos también si está vacío o no
            } // Ver si hace falta o no parsear la primera línea -> de momento no
            return analyse(br);
        } catch (IOException e) {       // Al terminar el try, se cierra br (br.close()) automáticamente
            throw new RuntimeException(e);
        }
    }

    public int analyse(BufferedReader br) throws IOException {
        String line;
        var permisos = 0;
        while ((line = br.readLine()) != null) { // Leer línea por línea hasta final de fichero (EOF) desde la segunda línea
            if (!line.isEmpty()) {
                if (line.startsWith("//")) { // Devolver permisos que están en la última línea
                    permisos = Integer.parseInt(line.substring(2));
                } else {
                    if (line.endsWith("/")) {           // Detectar grupo de figuras
                        line = anlygroup(line, br);       // Extraer descripción del grupo
                    }
                    var fig = Figura.parse(line);       // Parsear figura con descripción
                    var tag = Figura.parseid(line);     // Y al final su id --> Depende del dibujo
                    addfig(fig, tag);  // Se construye la figura y se guarda en el ArrayList de figuras
                }
            }
        }
        return permisos;
    }

    public String anlygroup(String line, BufferedReader br) throws IOException {  // Analizar descripción de grupo
        var aux = new StringBuilder();
        aux.append(line);
        while ((line = br.readLine()) != null && line.startsWith("-")) {    // Detectar figura del grupo
            aux.append(line);                       // Añadir todas las líneas leídas del grupo
            if (line.startsWith("-/")) { break; }   // Detectar final de grupo de figuras
        }
        return aux.toString();      // Devolver descripción del grupo entero
    }

    public static String getfname(String autor, int id) {// Obtener nombre del fichero -> dibujos
        return autor + ":" + id;                    // ¡Cambiar aquí el nombre de los ficheros! -> "/" genera problemas
    }

    public static String getdname(String autor) {    // Obtener nombre del directorio -> autor
        return "Dibujos:" + autor;     // ¡Cambiar aquí nombre del dir! -> "/" crea directorio dentro con nombre "autor"
    }

    public static Dibujo parser(String s) {        // Factory method -> Dibujo -> s = name/id = nombre del fichero
        var parts = s.split(":", 2);                 // Dividir name:id
        var name = parts[0].trim();
        var id = Integer.parseInt(parts[1].trim());
        return new Dibujo(id, name);
    }

}
