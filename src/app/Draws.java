package app;

import protocol.Perms;
import protocol.Tag;

import java.io.File;
import java.util.HashMap;

import static app.Draw.*;
import static app.Draw.getfname;
import static protocol.ToolDefs.drawlog;

/**
 * Agrupación de dibujos con sus permisos para un cliente concreto
 */
public class Draws {
    public HashMap<Draw, Integer> draws;      // Diccionario => cada dibujo con sus permisos
    public Tag tagsd;                         // Tags usados en dibujos


    public Draws() {
        draws = new HashMap<>();
        tagsd = new Tag();
    }

    /**
     * Volcar todos nombres ficheros/dibujos -> fnames<br>
     * @param autors -> Nombres de los autores
     * @param ids -> IDs de los dibujos en cuestión
     * @param sep -> Separador
     * @return /fname1/fname2/fname3... -> String
     */
    public static String getfnames(String[] autors, int[] ids, String sep) {
        if (ids.length != autors.length) {
            throw new RuntimeException("Different lengths");
        }
        var i = 0;
        var sb = new StringBuilder();
        for (int id : ids) {
            sb.append(sep).append(getfname(autors[i], id));
            i += 1;
        }
        return sb.toString();
    }

    // Draws del HashMap:

    /**
     * Crear nuevo dibujo en el hashmap y en local<br>
     * Desaplanando el string donde están -> separadas por //<br>
     * //dfig1//dfig2//dfig3...<br>
     * Dar todos los perms al creador<br>
     * Asignarle nuevo tag que no esté en uso<br>
     * Nombre del fichero del dibujo -> getfname(autor, tag.get())
     * @param sfigs -> Descripciones de figuras
     * @param dirname -> Directorio donde guardar el dibujo
     * @param autor -> Nombre del autor
     * @param sep -> Separador de descripciones de figuras(String)
     * @return Dibujo creado
     */
    public Draw newdraw(String sfigs, String dirname, String autor, String sep) {
        var perms = Perms.combine(Perms.RD, Perms.WR, Perms.EX, Perms.OW);
        var tag = tagsd.newtag();
        var fname = getfname(autor, tag.get());
        // Crear dibujo sin info:
        var draw = parser(getfname(autor, tag.get()));
        // Guardar dibujo en memoria:
        adddraw(draw, perms);
        // Guardar dibujo en local:
        draw.saveTo(fname, dirname, perms);
        // Si el mensaje lleva info, son las figuras -> añadirlas al dibujo -> Eliminar primer "//":
        if (!sfigs.isEmpty()) {
            addfigs(sfigs.substring(2), draw, sep, dirname);
        }
        return draw;
    }

    /**
     * Guardar en variables con los permisos <br>
     * -> Thread-safe
     * @param d -> Dibujo a añadir
     * @param p -> Permisos del dibujo
     * @throws RuntimeException -> Si existe el dibujo
     */
    public void adddraw(Draw d, int p) {
        synchronized (draws) {
            if (draws.containsKey(d)) {
                throw new RuntimeException("Draw already in memory -> " + getfname(d.autor, d.id.get()));
            }
            draws.put(d, p);
        }
    }

    /**
     * Borra dibujo de la memoria y del disco <br>
     * -> Thread-safe<br>
     * --> Solo pueden borrar propietarios OW
     * @param fname -> Nombre del dibujo (autor:id)
     * @param dirname -> Directorio donde guardar el dibujo
     * @throws RuntimeException -> Si no existe el dibujo
     */
    public void rmdraw(String fname, String dirname) {
        var draw = valedit(fname, Perms.combine(Perms.RD, Perms.WR, Perms.OW));
        synchronized (draws) {
            if (draws.containsKey(draw)) {
                draws.remove(draw);
                unloadd(fname, dirname);
                return;
            }
        }
        throw new RuntimeException("No such draw requested to remove -> " + getfname(draw.autor, draw.id.get()));
    }

    /**
     * Buscar dibujo por ID y autor
     * @param autor -> Nombre del autor
     * @param id -> ID del dibujo
     * @return Dibujo correspondiente a los argumentos
     * @throws RuntimeException -> Si no existe el dibujo
     */
    public Draw getdraw(String autor, int id) {
        synchronized (draws) {
            for (Draw d : draws.keySet()) {
                if (d.id.get() == id && d.autor.equals(autor)) {
                    return d;
                }
            }
        }
        throw new RuntimeException("No such draw requested -> " + getfname(autor, id));
    }

    /**
     * Buscar dibujo por nombre de fichero del dibujo -> autor:id
     * @param fname -> Nombre del dibujo/fichero donde se guarda en memoria
     * @return Dibujo correspondiente al nombre
     */
    public Draw getdraw(String fname) {
        var ss = fname.split(":");
        return getdraw(ss[0], Integer.parseInt(ss[1]));
    }

    /**
     * Buscar permisos del dibujo<br>
     * -> Thread-safe
     * @param draw -> Dibujo a buscar
     * @return Permisos del dibujo (int)
     * @throws RuntimeException -> Si no existe el dibujo
     */
    public int getperms(Draw draw) {
        synchronized (draws) {
            if (draws.containsKey(draw)) {
                return draws.get(draw);
            }
        }
        throw new RuntimeException("No such draw requested -> " + getfname(draw.autor, draw.id.get()));
    }

    /**
     * Verifica permisos del dibujo<br>
     * 011 &= 010 => 010(multiplicar) => negación(~)<br>
     * per/per_actual => Perms.RD/Persmisos.WR/Perms.EX
     * @param draw -> Dibujo a verificar
     * @param per -> Permisos a verificar(int) -> Perms(enum)
     * @return true si tiene permisos, false si no (boolean)
     */
    public boolean valdraw(Draw draw, int per) {
        int per_actual = getperms(draw);
        return (per_actual & per) != 0;
    }

    /**
     * Validar si tiene permisos por el nombre del dibujo
     * @param fname -> Nombre del dibujo (autor:id)
     * @param perms -> Permisos a verificar(int) -> Perms(enum)
     * @return Dibujo correspondiente al nombre
     * @throws RuntimeException -> Si no tiene permisos requeridos
     */
    public Draw valedit(String fname, int perms) {
        var draw = getdraw(fname);
        if(!valdraw(draw, perms)) {
            throw new RuntimeException("No enough permissions to edit draw");
        }
        return draw;
    }

    /**
     * Validar permisos de varios dibujos por sus nombres
     * @param sfnames -> Nombres de los dibujos aplanados en un solo string
     * @param sep -> Separador de los nombres de los dibujos
     * @param perms -> Permisos a verificar
     * @throws RuntimeException -> Si no tiene permisos requeridos algún dibujo
     */
    public void valsedit(String sfnames, String sep, int perms) {
        var fnames = sfnames.split(sep);
        for (var fname : fnames) {
            valedit(fname, perms);
        }
    }

    /**
     * Ver si se puede editar(RD, WR) figuras de dentro del dibujo, por su nombre
     * @param fname -> Nombre del dibujo
     * @param sfigs -> Figuras aplanadas en un solo string
     * @return Dibujo correspondiente al nombre después de ser validado
     * @throws RuntimeException -> Si no tiene permisos requeridos o no hay figuras
     */
    public Draw valeditfs(String fname, String sfigs) {
        if (sfigs.isEmpty()) {
            throw new RuntimeException("No figs arguments");
        }
        return valedit(fname, Perms.combine(Perms.RD, Perms.WR));
    }

    /**
     * Cargar cada dibujo -> s en local
     * @param f -> Nombre del fichero del dibujo -> name:id
     * @param dirname -> Directorio donde guardar el dibujo
     * @throws RuntimeException -> Si no existe el dibujo o fallo de carga
     */
    public void loadd(String f, String dirname) throws RuntimeException{
        // Crear dibujo sin info:
        var draw = parser(f);
        // Cargar info del dibujo -> devuelve permisos:
        var perms = draw.loadFrom(f, dirname);
        // Guardar tag en uso y añadir dibujo a memoria:
        tagsd.addtag(draw.id.get());
        adddraw(draw, perms);
    }

    /**
     * Cargar dibujos del user guardados en local<br>
     * Si no hay dibujos para cargar --> No hace nada
     * @param dirname -> Directorio donde guardar el dibujo
     * @param namecli -> Nombre del user
     * @throws RuntimeException -> Si no existe el dibujo o fallo de carga
     */
    public void loadds(String dirname, String namecli) {
        try {
            // Verificar si existe el directorio en workdir y crearlo si no:
            var dir = checkdir(dirname);
            // Listar los ficheros del directorio -> Draws (¡Ignorar ficheros ocultos -> empiezan por . !)
            File[] files = dir.listFiles((_, name) -> !name.startsWith("."));
            if (files == null) {
                return;
            }
            for (File f : files) {
                // Verificar que sea un fichero y cargarlo
                if (f.isFile()) {
                    loadd(f.getName(), dirname);
                } else {
                    throw new RuntimeException("Invalid file -> " + f.getName());
                }
            }
            drawlog.info(files.length + " Draws cargados de " + namecli);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Borrar dibujo/fichero de local<br>
     * Se verifica si existe el directorio y el fichero dentro del directorio dir
     * @param f -> Nombre del dibujo (autor:id)
     * @param dirname -> Directorio donde guardar el dibujo
     */
    public void unloadd(String f, String dirname) {
        var dir = checkdir(dirname);
        File file = new File(dir, f);
        // Borrar el fichero:
        if (!file.delete()) {
            throw new RuntimeException("Error al borrar el dibujo, puede que no exista: " + f);
        } else {
            // Borrar tag que ya no se usa:
            tagsd.rmtag(Integer.parseInt(f.split(":")[1]));
            drawlog.info("Draw borrado: " + f);
        }
    }

    /**
     * Actualizar contenido del dibujo en local<br>
     * Draw(clave) <-> Perms(valor)
     * @param draw -> Dibujo a actualizar
     * @param dirname -> Directorio donde guardar el dibujo
     * @return String -> Descripción del dibujo con permisos
     */
    public String update(Draw draw, String dirname) {
        var perms = getperms(draw);
        draw.saveTo(getfname(draw.autor, draw.id.get()), dirname, perms);
        return draw.dump(perms);
    }

    /**
     * Mover según nombre/s de dibujo/s <br>
     * Sacar cada nombre de dibujo, moverlo y actualizarlo
     * @param spnt -> Desplazamiento (x,y)
     * @param fnames -> Nombre/s de dibujo/s aplanados en un string
     * @param sep -> Separador de nombres de dibujos en el string fnames
     * @param dirname -> Directorio donde guardar los dibujos
     */
    public void mvdraws(String spnt, String fnames, String sep, String dirname) {
        var p = Point.parse(spnt);
        for (var fname : fnames.split(sep)) {
            var draw = getdraw(fname);
            draw.move(p.x, p.y);
            update(draw, dirname);
        }
    }

    /**
     * Volcar dibujo indv --> Mediante el nombre del dibujo
     * @param fname -> Nombre del dibujo (autor:id)
     * @return String -> Descripción del dibujo
     */
    public String dumpd(String fname) {
        var draw = getdraw(fname);
        return draw.dump(getperms(draw));
    }

    /**
     * Array de volcados de dibujos mediantes sus nombres
     * @param fnames -> String[] -> Array de nombres de dibujos
     * @return String[] -> Descripciones de dibujos solicitadas
     */
    public String[] dumpsmd(String[] fnames) {
        var ddraws = new String[fnames.length];
        var i = 0;
        for (var fname : fnames) {
            ddraws[i] = dumpd(fname);
            i += 1;
        }
        return ddraws;
    }

    /**
     * Volcar todos los dibujos guardados
     * @return String[] -> Array de descripciones de todos los dibujos de la memoria
     */
    public String[] dumpall() {
        String[] ddraws;
        synchronized (draws) {
            ddraws = new String[draws.size()];
            var i = 0;
            for (Draw draw : draws.keySet()) {
                ddraws[i] = draw.dump(getperms(draw));
                i += 1;
            }
        }
        return ddraws;
    }


    // Figuras en dibujos:

    /**
     * Añadir figuras en dibujo por sus descripciones<br>
     * Actualizar dibujo en local tras añadirlas
     * @param sfigs -> String aplanado de descripciones de figuras
     * @param draw -> Dibujo a actualizar con esas figuras
     * @param sep -> Separador de descripciones en sfigs
     * @param dirname -> Directorio donde guardar el dibujo
     * @return int[] -> Array de ids de las figuras añadidas
     */
    public int[] addfigs(String sfigs, Draw draw, String sep, String dirname) {
        //Si sep no está en sfigs = array de 1 elemento:
        var ssfigs = sfigs.split(sep);
        var idsf = new int[ssfigs.length];
        var i = 0;
        for (var sfig : sfigs.split(sep)) {
            try {
                // Fabricar figura:
                var fig = Figure.parse(sfig);
                // Guardar figura en dibujo:
                draw.addfig(fig, 0);
                idsf[i] = fig.id.get();
                i += 1;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        update(draw, dirname);
        return idsf;
    }

    /**
     * Borrar figura/s(ids) concreta/s en un dibujo<br>
     * Actualizar dibujo en local tras añadirlas
     * @param sfids -> String de ids de figuras aplanados
     * @param draw -> Dibujo a actualizar
     * @param sep -> Separador de ids en el string aplanado
     * @param dirname -> Directorio donde guardar el dibujo
     */
    public void rmfigs(String sfids, Draw draw, String sep, String dirname) {
        for (var fid : sfids.split(sep)) {
            try {
                // ID de figura a borrar:
                var id = Integer.parseInt(fid);
                // Figura del dibujo:
                var fig = draw.getfig(id);
                // Borrar figura:
                draw.delfig(fig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        update(draw, dirname);
    }

    /**
     * Mover figura/s(sfids) de 1 dibujo(fname)<br>
     * Actualizar dibujo en local tras añadirlas
     * @param spnt -> Desplazamiento(x,y)
     * @param idsf -> Array de ids de figuras (int[])
     * @param draw -> Dibujo a actualizar
     * @param dirname -> Directorio donde guardar el dibujo
     */
    public void mvfigs(String spnt, int[] idsf, Draw draw, String dirname) {
        var p = Point.parse(spnt);
        // Mover figuras:
        draw.mvfigs(idsf, p.x, p.y);
        update(draw, dirname);
    }


}
