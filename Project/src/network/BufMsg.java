package network;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BufMsg {
    public ArrayList<Msg.MsgStr> msgs;      // Thread safe

    public BufMsg() {               // Buffer de mensajes pendientes
        msgs = new ArrayList<>();
    }

    public void addmsg(Msg.MsgStr msg) {
        synchronized (msgs) {
            msgs.add(msg);
        }
    }

    public void delmsg(Msg.MsgStr msg) {
        synchronized (msgs) {
            msgs.remove(msg);
        }
    }

    public Msg.MsgStr checkmsgs() {     // Ver si hay mensajes pendientes sin leer en nuestro buffer de mensajes
        synchronized (msgs) {
            for (var msg : msgs) {
                if (msg.pos == 1) {            // Encontramos primer mensaje
                    msgs.remove(msg);
                    return msg;
                }
            }
        }
        return null;
    }

    public Msg.MsgStr checkmsg(AtomicInteger tag, int pos) {     // Ver si hemos leído un mensaje del tag que buscamos
        synchronized (msgs) {
            for (var msg : msgs) {
                if (tag.get() > 0) {         // Asumir que si tag <= 0, no buscamos un tag concreto
                    if (msg.tag.get() == tag.get() && msg.pos == pos) {
                        msgs.remove(msg);
                        return msg;
                    }
                } else {
                    if (msg.pos == pos) {
                        msgs.remove(msg);
                        return msg;
                    }
                }
            }
        }
        return null;
    }
}
