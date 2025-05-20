package app;

import protocol.Tag;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dibujo -> Agrupación de figuras, con metadatos -> Autor, fecha, id...
 */
public class Draw {
    public AtomicInteger id;                    // ID del dibujo
    public final String autor;                  // Nombre del autor
    public LinkedList<Figure> figs;             // Figuras del dibujo
    public Tag tags;                            // Tags de figuras en uso

    /**
     * Añadir todas las figuras del dibujo con sus IDs:
     * @param id -> ID del dibujo
     * @param autor -> Nombre del autor
     * @param figs -> Figuras del dibujo a añadir
     */
    public Draw(int id, String autor, Figure... figs) {
        this.id = new AtomicInteger(id);
        this.autor = autor;
        this.figs = new LinkedList<>();
        tags = new Tag();
        this.figs.addAll(List.of(figs));
    }

    /**
     * Volcado del dibujo termiando en "\n"
     * @return String
     */
    public String dump() {
        var sb = new StringBuilder();
        for (Figure fig : figs) {
            // "\n" -> separación entre figuras
            sb.append(fig.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Volcado del dibujo con permisos<br>
     * Permisos --> Al final de la descripción<br>
     * //perms\n\n
     * @param perms -> Permisos del dibujo
     * @return -> String
     */
    public String dump(int perms) {
        return this + "//" + perms + "\n\n";
    }

    public String getdate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm:ss"));

    }

    /**
     * Draw:id:autor:fecha<br>
     * Figura1<br>
     * Figura2<br>
     * ...\n<br>
     * @return -> String
     */
    @Override
    public String toString() {
        return "Draw:" + this.id + ":" + autor + ":" +
                getdate() + "\n" + dump();
    }

    /**
     * Suponer que la última figura se superpone a las anteriores<br>
     * Se pueden añadir figuras iguales --> pero no mismo id
     * @param fig -> Figura
     * @param id -> ID
     */
    public void addfig(Figure fig, int id) {
        if (tags.revise(id)) {
            throw new RuntimeException("Have already figure id " + id);
        }
        // Genera nuevo tag y lo pone en uso:
        if (id <= 0) {
            fig.id = tags.newtag();
        // Añadir figura con id en uso:
        } else {
            fig.id = tags.addtag(id);
        }
        figs.add(fig);
    }

    /**
     * Borrar figura del dibujo
     * @param fig -> Figura
     */
    public void delfig(Figure fig) {
        if (figs.contains(fig)) {
            figs.remove(fig);
            // Eliminar id en uso:
            tags.rmtag(fig.id.get());
        } else {
            throw new RuntimeException("No figure with id " + fig.id);
        }
    }

    /**
     * Buscar figura por su id
     * @param id -> ID de la figura
     * @return -> Figura
     * @throws RuntimeException --> Si no encuentra la figura
     */
    public Figure getfig(int id) {
        for (var fig : figs) {
            if (fig.id.get() == id) {
                return fig;
            }
        }
        throw new RuntimeException("No figure " + id +
                " found in draw -> " + getfname(autor, this.id.get()));
    }

    /**
     * Mover dibujo = mover todas sus figuras
     */
    public void move(int dx, int dy) {
        for (Figure fig : figs) {
            fig.move(dx, dy);
        }
    }

    /**
     * Mover según ids de figuras específicas
     * @param idsf -> IDs de las figuras
     * @param x -> Desplazamiento x
     * @param y -> Desplazamiento y
     * @throws RuntimeException -> Si no encuentra las figuras
     */
    public void mvfigs(int[] idsf, int x, int y) {
        for (var id : idsf) {
            try {
                // Figura del dibujo con esa id:
                var fig = getfig(id);
                // Borrar figura:
                fig.move(x, y);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void expand(double factor) {
        for (Figure fig : figs) {
            fig.expand(factor);
        }
    }

    /**
     *Comprueba si el punto está dentro de alguna figura<br>
     * y las guarda en una lista, si está vacía => está fuera del draw
     */
    public List<Figure> isin(Point p) {
        List<Figure> figsin = new ArrayList<>();
        for (Figure fig : figs) {
            if (fig.ispnt(p)) {
                figsin.add(fig);
            }
        }
        return figsin;
    }

    public boolean ispt(Point p) {
        return !isin(p).isEmpty();
    }

    /**
     * Crear directorio en el path actual si no existe<br>
     * Si el directorio ya existe, no hace nada
     * @param dirpath -> Path al directorio donde guardar el dibujo
     * @return -> Directorio
     */
    public static File newdir(String dirpath) {
        var dir = new File(dirpath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.err.println("Directorio creado: " + dirpath);
            } else {
                throw new RuntimeException("No se pudo crear el directorio: " + dirpath);
            }
        }
        return dir;
    }

    /**
     * Verificar si existe el directorio y crearlo si NO existe
     * @param dname -> Nombre del directorio
     * @return -> Path completo al directorio
     */
    public static File checkdir(String dname) {
        // Path directorio de trabajo:
        String wdir = System.getProperty("user.dir");
        String dirpath = wdir + File.separator + dname;
        return newdir(dirpath);
    }

    /**
     * Gestionar fichero de escritura
     * @param file -> Fichero
     * @param permisos -> Permisos del dibujo
     * @throws RuntimeException --> Cualquier fallo al manejar ficheros
     */
    public void saveTo(File file, int permisos) throws RuntimeException {
        // true = modo append / false = modo sobreescribir --> Fichero preparado para recibir datos
        try (var fow = new FileOutputStream(file, false);
             // Puedo especificar code UTF-8 al hacer el stream:
             var osw = new OutputStreamWriter(fow, StandardCharsets.UTF_8);
             // Agregar buffering para mayor rapidez
             var pw = new PrintWriter(new BufferedWriter(osw))) {
            // Imprimir this + "//" + perms + "\n\n"
            pw.print(dump(permisos));
            //`fw` y `pw` se cierran automáticamente al terminar el try (fw.close())
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Guardar figuras en el fichero que se le pasa (dname/f)<br>
     * Si no existe se crea 'f'
     * @param f -> Nombre del fichero donde guarda el dibujo
     * @param dname -> Directorio donde guardar el dibujo
     * @param permisos -> Permisos del dibujo
     * @throws RuntimeException --> Cualquier fallo al manejar ficheros
     */
    public void saveTo(String f, String dname, int permisos) throws RuntimeException {
        File dir = checkdir(dname);
        // Objeto File con el nombre/ruta al fichero en el directorio dir
        var file = new File(dir, f);
        saveTo(file, permisos);
    }

    /**
     * Cargar figuras del fichero del dibujo que se le pasa(dirname/f)
     * @param f --> Nombre del fichero del dibujo
     * @param dname --> Directorio del dibujo
     * @return --> Permisos del dibujo cargado
     * @throws RuntimeException --> Si NO encuentra el fichero o manejo
     */
    public int loadFrom(String f, String dname) throws RuntimeException {
        File dir = checkdir(dname);
        var file = new File(dir, f);
        if (!file.exists()) {
            throw new RuntimeException("El archivo no existe -> " + f);
        }
        return loadFrom(file, f);
    }

    /**
     * Gestionar fichero de lectura
     * @param file -> Fichero
     * @param f -> Nombre del fichero
     * @return -> Permisos del dibujo
     * @throws RuntimeException --> Cualquier fallo al manejar ficheros
     */
    public int loadFrom(File file, String f) throws RuntimeException {
        // Fichero preparado para sacar datos:
        try (var fis = new FileInputStream(file);
             var isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             // Leer fichero linea a linea con buffering:
             var br = new BufferedReader(isr)) {
            // Lee y descarta la primera línea (nombre del dibujo) -> Comprobamos también si está vacío o no
            // Ver si hace falta o no parsear la primera línea -> de momento NO
            if (br.readLine() == null) {
                throw new RuntimeException("El archivo está vacío -> " + f);
            }
            return analyse(br);
            // Al terminar el try, se cierra br (br.close()) automáticamente
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Analizar el contenido del fichero línea a línea<br>
     * Menos la primera línea
     * @param br -> BufferedReader
     * @return -> Permisos del dibujo
     * @throws IOException --> Cualquier fallo al manejar ficheros
     */
    public int analyse(BufferedReader br) throws IOException {
        String line;
        var permisos = 0;
        // Leer línea por línea hasta final de fichero (EOF) desde la segunda línea
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty()) {
                // Devolver permisos que están en la última línea:
                if (line.startsWith("//")) {
                    permisos = Integer.parseInt(line.substring(2));
                } else {
                    // Detectar grupo de figuras
                    if (line.endsWith("/")) {
                        // Extraer descripción del grupo:
                        line = anlygroup(line, br);
                    }
                    // Parsear figura con descripción:
                    var fig = Figure.parse(line);
                    // Y al final su id/tag --> Depende del dibujo
                    var tag = Figure.parseid(line);
                    // Se construye la figura y se guarda en el ArrayList de figuras
                    addfig(fig, tag);
                }
            }
        }
        return permisos;
    }

    /**
     * Analizar descripción de grupo de figuras
     * @param line -> Línea leída
     * @param br -> BufferedReader
     * @return -> Descripción del grupo entero --> String
     * @throws IOException --> Cualquier fallo al manejar ficheros
     */
    public String anlygroup(String line, BufferedReader br) throws IOException {
        var aux = new StringBuilder();
        aux.append(line);
        // Detectar figura del grupo con "-" al inicio de la línea:
        while ((line = br.readLine()) != null && line.startsWith("-")) {
            // Añadir todas las líneas leídas del grupo:
            aux.append(line);
            // Detectar final de grupo de figuras con "-/" al inicio de la línea:
            if (line.startsWith("-/")) { break; }
        }
        return aux.toString();
    }

    /**
     * Obtener nombre del fichero -> dibujo<br>
     * ¡Cambiar aquí el nombre de los ficheros!<br>
     * "/" genera un subdirectorio --> Mejor usar ":"
     * @param autor -> Nombre del autor
     * @param id -> ID del dibujo
     * @return autor:id -> String
     */
    public static String getfname(String autor, int id) {
        return autor + ":" + id;
    }

    /**
     * Obtener nombre del directorio donde se guardan los dibujos del usuario<br>
     * ¡Cambiar aquí nombre del dir!<br>
     * "/" crea subdirectorio dentro con nombre "autor"
     * @param autor -> Nombre del autor
     * @return Draws:autor
     */
    public static String getdname(String autor) {
        return "Draws:" + autor;
    }

    /**
     * Factory method -> Draw
     * @param s -> String => autor:id = nombre del fichero/dibujo
     * @return -> Draw
     */
    public static Draw parser(String s) {
        var parts = s.split(":", 2);
        var name = parts[0].trim();
        var id = Integer.parseInt(parts[1].trim());
        return new Draw(id, name);
    }
}
