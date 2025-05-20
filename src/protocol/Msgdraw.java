package protocol;

import java.util.Arrays;

import static app.Draw.getfname;
import static app.Draws.getfnames;
import static protocol.Heads.*;

public class Msgdraw extends MsgStr {

    public Msgdraw(int tag) {
        super(tag);
    }

    public static String packstr_us_newd(String sep, String... dfigs) {     // figuras(separadas por sep)
        return packstr(NEWD, packstr_parts(sep, dfigs));              // /dfig1/dfig2/dfig3...
    }

    public static String packstr_srv_newd(String ndraw) {            // Nombre del dibujo
        return packstr(NEWD, ndraw);                           // autor:id -> fname
    }

    public static String packstr_us_rmd(String autor, int id) {      // Nombre del dibujo
        return packstr(RMD, getfname(autor, id));              // autor:id -> fname
    }

    public static String packstr_srv_rmd(String dname) {            // Nombre del dibujo
        return packstr(RMD, dname);                           // autor:id
    }

    public static String packstr_us_newf(String autor, int id, String sep, String... dfigs) {  // Nombre del dibujo y figuras
        return packstr(NEWF, getfname(autor, id) + packstr_parts(sep, dfigs));         // autor:id//dfig1//dfig2//dfig3...
    }

    public static String packstr_srv_newf(String sep, int... idfigs) {          // IDs de las figuras creadas
        return packstr(NEWF, packstr_parts(sep, Arrays.toString(idfigs)));      // /id1/id2/id3...
    }

    public static String packstr_us_rmf(String autor, int id, String sep, int... idfigs) {     // Nombre del dibujo e ids de figuras
        return packstr(RMF, getfname(autor, id) + packstr_parts(sep, Arrays.toString(idfigs)));  // autor:id/idf1//idf2//idf3...
    }

    public static String packstr_srv_rmf(String sidsf) {    // IDs de las figuras borradas -> id1/id2/id3...
        return packstr(RMF, sidsf);                         // -> ya en un string aplanado -> lo que recibimos
    }

    public static String packstr_us_mvf(String autor, int id, int x, int y, String sep, int... idfigs) {  // Nombre del dibujo e ids de figuras
        return packstr(MVF, getfname(autor, id) +                                       // autor:id//x,y/idfig1/idfig2/idfig3...
                "//" + x + "," + y + packstr_parts(sep, Arrays.toString(idfigs)));
    }

    public static String packstr_srv_mvf(String sidsf) {    // IDs de las figuras desplazadas -> id1/id2/id3...
        return packstr(MVF, sidsf);                         // -> ya en un string aplanado -> lo que recibimos
    }

    public static String packstr_us_mvd(String[] autor, int[] id, int x, int y, String sep) {  // Desplazamiento y nombre de los dibujos
        return packstr(MVD, x + "," + y + getfnames(autor, id, sep));              // x,y/autor1:id1/autor2:id2/autor3:id3...
    }

    public static String packstr_srv_mvd(String fnames) {       // Nombres de los dibujos desplazados -> autor1:id1/autor2:id2/autor3:id3...
        return packstr(MVD, fnames);                            // -> ya en un string aplanado -> lo que recibimos
    }

    public static String packstr_us_dump(String[] autor, int[] id, String sep) {   // Nombre de los dibujos
        return packstr(DUMP, getfnames(autor, id, sep).substring(1)); // autor1:id1/autor2:id2/autor3:id3...
    }

    public static String packstr_srv_dump(String sep, String... ddraws) {    // Descripciones de dibujos separados por sep (\n//\n)
        var sb = new StringBuilder();
        for (var descrip : ddraws) {
            sb.append(descrip).append(sep);      // Mandar todo en un mismo string, separando descripciones por sep
        }
        return packstr(DUMP, sb.toString());
    }



}
