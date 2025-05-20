package protocol;

import static protocol.Heads.*;
import static protocol.Heads.packstr;

public class Msglog extends MsgStr {

    public Msglog(int tag) {
        super(tag);
    }

    public static String packstr_us_in(String dump_us) {
        return packstr(LOGIN, dump_us);
    }

    public static String packstr_us_out() {
        return packstr(LOGOUT, "");
    }

    public String unpackstr_srv_in() {     // Según contenido en el str de msg leído
        return msg.split(":", 2)[0];    // Recoger info del user
    }

    public static String packstr_srv_in(String name_srv, int idcli) {
        return packstr(INIACK, name_srv + ":" + idcli);
    }

    public static String packstr_srv_out() {
        return packstr(FINACK, "");
    }

    public Object[] unpack_us_in() {     // Según contenido en el str de msg leído
        var ss = msg.split(":");    // Recoger info del servidor
        return new Object[] { ss[0], Integer.parseInt(ss[1]) };
    }
}