package protocol;

import java.util.ArrayList;

/**
 * Buffer/Lista enlazada de mensajes pendientes<br>
 * Métodos Thread safe
 */
public class BufMsg {
    public ArrayList<Msg> msgs;

    public BufMsg() {
        msgs = new ArrayList<>();
    }

    /**
     * Añadir mensaje pendiente
     * @param msg -> Msg
     * @throws RuntimeException -> Si el msg ya estaba en el buffer
     */
    public void addmsg(Msg msg) {
        synchronized (msgs) {
            if (msgs.contains(msg)) {
                throw new RuntimeException("Msg ya en buffer de Msgs pendientes");
            }
            msgs.add(msg);
        }
    }

    /**
     * Eliminar mensaje pendiente
     * @param msg -> Msg
     * @throws RuntimeException -> Si el msg NO estaba en el buffer
     */
    public void delmsg(Msg msg) {
        synchronized (msgs) {
            if (!msgs.contains(msg)) {
                throw new RuntimeException("Msg NO en buffer de Msgs pendientes");
            }
            msgs.remove(msg);
        }
    }

    /**
     * Ver si hay mensajes pendientes sin leer en nuestro buffer de mensajes<br>
     * Solo se buscan mensajes con pos = 1 --> Primera parte de un mensaje
     * @return primer mensaje pendiente -> Msg | null si no hay
     */
    public Msg checkmsgs() {
        synchronized (msgs) {
            for (var msg : msgs) {
                // Encontramos primer mensaje
                if (msg.pos == 1) {
                    msgs.remove(msg);
                    return msg;
                }
            }
        }
        return null;
    }

    /**
     * Ver si hemos leído un mensaje del tag que buscamos<br>
     * si tag <= 0, no buscamos un tag concreto
     * @param tag -> AtomicInteger.get() = int
     * @param pos -> Orden de la parte del mensaje
     * @return
     */
    public Msg checkmsg(int tag, int pos) {
        synchronized (msgs) {
            for (var msg : msgs) {
                if (tag > 0) {
                    if (msg.tag.get() == tag && msg.pos == pos) {
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
