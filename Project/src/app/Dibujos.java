package app;

import network.Permisos;
import network.Tag;

import java.io.File;
import java.util.HashMap;

import static app.Dibujo.*;
import static app.Dibujo.getfname;

public class Dibujos {
    public HashMap<Dibujo, Integer> draws;      // Diccionario => cada dibujo con sus permisos
    public Tag tagsd;                     // Tags usados en dibujos


    public Dibujos() {
        draws = new HashMap<>();
        tagsd = new Tag();
    }

    public static String getfnames(String[] autors, int[] ids, String sep) {     // Volcar todos nombres ficheros dibs
        if (ids.length != autors.length) {
            throw new RuntimeException("Different lengths");
        }
        var i = 0;
        var sb = new StringBuilder();
        for (int id : ids) {
            sb.append(sep).append(getfname(autors[i], id));
        }
        return sb.toString();
    }

    // Dibujos del HashMap:

    public Dibujo newdraw(String sfigs, String dirname, String namecli, String sep) {
        var perms = Permisos.combine(Permisos.RD, Permisos.WR, Permisos.EX, Permisos.OW);   // Dar todos los perms
        var tag = tagsd.newtag();                    // Asignar nuevo tag que no este en uso
        var fname = getfname(namecli, tag.get());  // Nombre del fichero del dibujo
        var draw = parser(getfname(namecli, tag.get()));            // Crear dibujo sin info
        adddraw(draw, perms);                  // Guardar dibujo en memoria
        draw.saveTo(fname, dirname, perms);    // Guardar dibujo en local
        if (!sfigs.isEmpty()) {                  // Si el mensaje lleva info, son las figuras
            addfigs(sfigs.substring(2), draw, sep, dirname);// Añadir figuras, si lleva como argumento
        }           // Eliminar primer "//"
        return draw;
    }

    public void adddraw(Dibujo d, int p) {       // Guardar en variables con los permisos -> Thread-safe
        synchronized (draws) {
            if (!draws.containsKey(d)) {
                draws.put(d, p);
            }
        }
    }

    public void rmdraw(String fname, String dirname) { // Borra dibujo de la memoria -> Thread-safe
        var draw = valedit(fname, Permisos.combine(Permisos.RD, Permisos.WR, Permisos.OW)); //--> Solo pueden borrar propietarios OW
        synchronized (draws) {
            if (draws.containsKey(draw)) {
                draws.remove(draw);
                unloadd(fname, dirname);                       // Borrar dibujo local
                return;
            }
        }
        throw new RuntimeException("No such draw requested to remove -> " + getfname(draw.autor, draw.id.get()));
    }

    public Dibujo getdraw(String autor, int id) {    // Buscar dibujo por ID y autor
        synchronized (draws) {
            for (Dibujo d : draws.keySet()) {
                if (d.id.get() == id && d.autor.equals(autor)) {
                    return d;
                }
            }
        }           // Si no está en dibujo, mandar mensaje de error
        throw new RuntimeException("No such draw requested -> " + getfname(autor, id));
    }

    public Dibujo getdraw(String fname) {    // Buscar dibujo por nombre de fichero del dibujo
        var ss = fname.split(":");
        return getdraw(ss[0], Integer.parseInt(ss[1]));     // autor:id
    }

    public int getperms(Dibujo draw) {      // Buscar permisos de dibujo
        synchronized (draws) {
            if (draws.containsKey(draw)) {
                return draws.get(draw);
            }
        }               // Si no lo tenemos --> Error
        throw new RuntimeException("No such draw requested -> " + getfname(draw.autor, draw.id.get()));
    }

    public boolean valdraw(Dibujo draw, int per) {  //Verifica permisos uno a uno
        int per_actual = getperms(draw);        // Permisos del dibujo
        return (per_actual & per) != 0;    // 011 &= 010 => 010(multiplicar) => negacion(~)
    }                                      // per/per_actual => Permisos.RD/Persmisos.WR/Permisos.EX

    public Dibujo valedit(String fname, int perms) {   // Validar si tiene permisos que se le pasan
        var draw = getdraw(fname);              // Buscar dibujo
        if(!valdraw(draw, perms)) {
            throw new RuntimeException("No enough permissions to edit draw");
        }
        return draw;
    }

    public void valsedit(String sfnames, String sep, int perms) {   // Validar permisos de varios dibujos
        var fnames = sfnames.split(sep);
        for (var fname : fnames) {
            valedit(fname, perms);
        }
    }

    public Dibujo valeditfs(String fname, String sfigs) {         // Ver si se puede editar figuras de dentro del dibujo
        if (sfigs.isEmpty()) {          // Descripción necesaria figuras
            throw new RuntimeException("No figs arguments");
        }
        return valedit(fname, Permisos. combine(Permisos.RD, Permisos.WR)); // Validar permisos fname --> dibujo
    }

    public void loadd(String f, String dirname) throws RuntimeException{// Cargar cada dibujo -> s = name:id = nombre del fichero
        var draw = parser(f);            // Crear dibujo sin info
        var perms = draw.loadFrom(f, dirname);  // Cargar info del dibujo -> devuelve permisos
        tagsd.addtag(draw.id.get());                   // Guardar tag en uso
        adddraw(draw, perms);                   // Añadir dibujo a memoria
    }

    public void loadds(String dirname, String namecli) {           // Cargar dibujos del user guardados en local
        try {
            var dir = checkdir(dirname);            // Verificar si existe el directorio en workdir y crearlo si no
            File[] files = dir.listFiles(           // Listar los ficheros del directorio --> Dibujos
                    (dir1, name) -> !name.startsWith(".")); // ¡¡Ignorar ficheros ocultos!!
            if (files == null) {
                return;     // Si no hay dibujos para cargar --> No hacer nada
            }
            for (File f : files) {
                if (f.isFile()) {           // Verificar que sea un fichero
                    loadd(f.getName(), dirname);           // Cargar cada dibujo
                } else {
                    throw new RuntimeException("Invalid file -> " + f.getName());
                }
            }
            System.err.println(files.length + " Dibujos cargados de " + namecli);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unloadd(String f, String dirname) {          // Borrar dibujo/fichero -> f = name:id
        var dir = checkdir(dirname);            // Verificar si existe el directorio
        File file = new File(dir, f);           // Fichero dentro del directorio dir
        if (!file.delete()) {                   // Borrar el fichero
            throw new RuntimeException("Error al borrar el dibujo, puede que no exista: " + f);
        } else {
            tagsd.rmtag(Integer.parseInt(f.split(":")[1]));    // Borrar tag que ya no se usa
            System.err.println("Dibujo borrado: " + f);
        }
    }

    public String update(Dibujo draw, String dirname) {          // Actualizar contenido del dibujo
        var perms = getperms(draw);                 // Permisos(valor) <-> Dibujo(clave)
        draw.saveTo(getfname(draw.autor, draw.id.get()), dirname, perms);   // Actualizar dibujo en local
        return draw.dump(perms);              // Mandar de nueva descripción del dibujo con permisos
    }

    public String mvdraws(String spnt, String fnames, String sep, String dirname, String sepdump) {   // Mover según nombre/s de dibujo/s
        var p = Punto.parse(spnt);               // Fabricar desplazamiento
        for (var fname : fnames.split(sep)) {
            var draw = getdraw(fname);                      // Obtener dibujo
            draw.move(p.x, p.y);                            // Mover dibujo
            update(draw, dirname);                          // Actualizar dibujo
        }
        return dumpsmd(fnames, sep, sepdump);    // Devolver descripción de dibujos actualizados
    }

    public String dumpd(String fname) {      // Volcar dibujo indv --> Mediante el nombre
        var draw = getdraw(fname);              // Descripción del dibujo
        return draw.dump(getperms(draw));
    }

    public String dumpsmd(String sfnames, String sep, String sepdump) {      // Volcar algun/os dibujo/s
        var fnames = sfnames.split(sep);         // Array de nombres de dibujos(fnames), separados por sep
        var sb = new StringBuilder();
        for (var fname : fnames) {                     // Volcar dibujo a dibujo -> separados por sepdump
            sb.append(dumpd(fname)).append(sepdump);
        }
        return sb.toString();
    }

    public String dumpall(String sep) {      // Volcar todos los dibujos
        var sb = new StringBuilder();
        for (Dibujo draw : draws.keySet()) {
            sb.append(draw.dump(getperms(draw))).append(sep);
        }       // Volcar dibujo a dibujo con separación de "\n///\n"
        return sb.toString();
    }



    // Figuras en dibujos:

    public String addfigs(String sfigs, Dibujo draw, String sep, String dirname) {    // Añadir figuras en dibujo
        for (var sfig : sfigs.split(sep)) {     // Si sep no está en sfigs = array de 1 elemento
            try {
                var fig = Figura.parse(sfig);                  // Fabricar figura
                draw.addfig(fig, 0);        // Guardar figuras en dibujo
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return update(draw, dirname);
    }

    public String rmfigs(String fids, Dibujo draw, String sep, String dirname) {      // Borrar figura/s(ids) concreta/s en un dibujo
        for (var fid : fids.split(sep)) {
            try {
                var id = Integer.parseInt(fid);         // ID de figura a borrar
                var fig = draw.getfig(id);              // Figura del dibujo
                draw.delfig(fig);                       // Borrar figura
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return update(draw, dirname);
    }

    public String mvfigs(String spnt, String sfids, Dibujo draw, String sep, String dirname) {   // Mover figura/s(sfids) de 1 dibujo(fname)
        var p = Punto.parse(spnt);               // Fabricar desplazamiento
        draw.mvfigs(sfids, sep, p.x, p.y);  // Mover figuras
        return update(draw, dirname);
    }


}
