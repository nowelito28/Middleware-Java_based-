package servicies;

import protocol.BufMsg;
import protocol.Heads;
import protocol.MsgStr;
import protocol.Tag;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static protocol.Heads.*;
import static protocol.ToolDefs.svclog;
import static network.Server.channels;     // Channels a utilizar en este servicio --> El mismo para todos los clientes

/**
 * Servicio implementado como MOM --> Usado en la parte del cliente en el server
 */
public class SvcMom implements Svc {

    private final HashMap<Heads, Handler> handlers;     // Handlers --> No static: 1 handler por cada cliente

    /**
     * Definir handlers -> Cabecera relacionada con un a acción concreta:
     */
    public SvcMom() {
        handlers = new HashMap<>();
        handlers.put(LOGIN, this::login);
        handlers.put(LOGOUT, this::logout);
        handlers.put(OPEN, this::open);
        handlers.put(CLOSE, this::close);
        handlers.put(MKCH, this::mkChannel);
        handlers.put(RMCH, this::rmChannel);
        handlers.put(WRCH, this::writeChannel);
        handlers.put(RDCH, this::readChannel);
        handlers.put(RMCONT, this::rmContent);
    }

    /**
     * Contexto de este servicio --> Inicializaciones: <br>
     * 1 context por cada cliente -> Se crea nuevo cada vez que se conecta un nuevo cliente
     */
    protected static class Context {
        protected String namesrv;                     // Nombre del servidor
        protected SocketChannel sk;                   // SocketChannel del cliente
        protected Tag tagsm = new Tag();              // Tags usados en mensajes
        protected BufMsg msgsleft = new BufMsg();     // Buffer de mensajes pendientes

        protected AtomicInteger id;                   // ID del cliente asignado por el servidor
        protected String namecli;                     // Nombre del cliente
        protected Channel currentch = null;           // Canal abierto actualmente -> Si no hay = null
        protected boolean dontwait = true;            // true -> No esperar para leer | false -> Esperar para leer
        protected int posrd = 0;                      // Posición actual del contenido a leer en el canal
        protected String lastcontent = "";            // Último contenido leído del canal actual
    }

    /**
     * Enviar msg al cliente --> String
     * @param s -> String -> info
     * @param tag -> int
     * @param objctx -> Context
     * @return MsgStr
     * @throws IOException -> Al escribir en el socketchannel
     * @throws RuntimeException -> Fallos detectados
     */
    @Override
    public MsgStr send(String s, int tag, Object objctx) throws IOException, RuntimeException {
        // Comprobar si el contexto es de tipo Context y castear:
        if (!(objctx instanceof Context ctx)) {
            throw new RuntimeException("Contexto no reconocido");
        }
        // Nuevo mensaje de strings:
        var msgwr = new MsgStr(tag);
        // Escribir str por el socketchannel:
        msgwr.writeTo(ctx.sk, s);
        svclog.debug("Mensaje enviado server: {}", msgwr.msg);
        // Borrar tag msg ya usado:
        ctx.tagsm.rmtag(tag);
        return msgwr;
    }

    /**
     * Recibir msg del cliente --> MsgStr
     * @param tag -> int
     * @param objctx -> Context
     * @return MsgStr
     * @throws IOException --> Al leer del socketchannel
     * @throws RuntimeException -> Fallos detectados
     */
    @Override
    public MsgStr receive(int tag, Object objctx) throws IOException, RuntimeException {
        // Comprobar si el contexto es de tipo Context y castear
        if (!(objctx instanceof Context ctx)) {
            throw new RuntimeException("Contexto no reconocido");
        }
        // Comprobar mensaje recibido y construir dicho msg:
        var msgrv = MsgStr.rv(ctx.sk, ctx.msgsleft, tag);
        // Añadir tag msg ya en uso:
        ctx.tagsm.addtag(msgrv.tag.get());
        svclog.debug("Mensaje recibido server: {}", msgrv.msg);
        return msgrv;
    }

    /**
     * Comenzar conexión y crear contexto del servicio individual para el cliente
     * @param sk -> SocketChannel del cliente
     * @param namesrv -> Nombre del servidor
     * @param id -> ID asignado para el cliente
     * @return --> Context concreto
     */
    @Override
    public Object newCli(SocketChannel sk, String namesrv, AtomicInteger id) {
        // Crear contexto por cada cliente -> Sus datos:
        var ctx = new Context();
        ctx.namesrv = namesrv;
        ctx.sk = sk;
        ctx.id = id;
        return ctx;
    }

    /**
     * Manejador de mensajes recibidos del cliente
     * @param msg -> MsgStr recibido
     * @param objctx -> Context del cliente
     * @return String -> Respuesta para el cliente --> Que solicita <br>
     * --> Devuelve mensaje de error al detectarlo -> En manejadores o en otros métodos
     */
    @Override
    public String handle(MsgStr msg, Object objctx) {
        try {
            if (!(msg instanceof MsgStr rvmsg)) {
                return packstr(ERR, "Mensaje no reconocido");
            }
            if (!(objctx instanceof Context ctx)) {
                return packstr(ERR, "Contexto no reconocido");
            }
            // Extraer cabecera -> enum -> Identificar manejador:
            Heads cab = getcab(rvmsg.inst);
            svclog.debug("Received cab server: {}", cab.name());
            Handler handler = handlers.get(cab);
            if (handler == null) {
                return packstr(ERR, "Cabecera desconocida: " + rvmsg.inst);
            }
            // Respuesta concreta respecto el manejador:
            return handler.handler(rvmsg, ctx);
        } catch (Exception e) {
            e.printStackTrace();
            svclog.error(e);
            return packstr(ERR, e.getMessage());
        }
    }

    /**
     * Desconectar cliente del servicio y del servidor
     * @param objctx -> Context
     * @return null
     */
    @Override
    public Object endCli(Object objctx) {
        // Comprobar si el contexto es de tipo Context y castear
        if (!(objctx instanceof Context ctx)) {
            throw new RuntimeException("Contexto no reconocido");
        }
        try {
            // Cerrar socket:
            ctx.sk.close();
            svclog.info("Cliente {}->{} desconectado del servidor", ctx.namecli, ctx.id);
            // Poner contexto del servicio a null para perder referencia y limpiar memoria (al ser static):
            ctx = null;
            // Ignorar excepción:
        } catch (Exception _) {}
        return null;
    }

    /**
     * Declarar interfaz para implementar cada manejador según la cabecera:
     */
    @FunctionalInterface
    protected interface Handler {
        String handler(MsgStr msg, Context ctx);
    }

    @Override
    public String toString() {
        return "Sistema de mensajes --> MOM";
    }

    /**
     * Logar/Parsear usuario <br>
     * Recibimos dump/info del user
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return namesrv:idcli(asignado) --> String
     */
    protected String login(MsgStr msg, Context ctx) {
        ctx.namecli = msg.msg.split(":", 2)[0];
        return packstr(INIACK, ctx.namesrv + ":" + ctx.id.get());
    }

    /**
     * Desconectar usuario
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ""
     */
    protected String logout(MsgStr msg, Context ctx) {
        return packstr(FINACK, "");
    }

    /**
     * Abrir canal concreto del servidor --> El que recibimos --> MsgStr.msg = namech <br>
     * Agregamos IDcli del contexto a una lista del canal --> de clientes conectados <br>
     * Añadir canal abierto al contexto <br>
     * NO NECESITAMOS IDcli --> porque ya lo tenemos en el contexto
     * @param msg -> MsgStr --> donde se encuentra el namech (MsgStr.msg)
     */
    protected String open(MsgStr msg, Context ctx) {
        if (ctx.currentch != null) {
            throw new RuntimeException("Other channel already in use -> Only 1 channel at a time");
        }
        var ch = channels.getch(msg.msg);
        ch.addCli(ctx.id);
        ctx.currentch = ch;
        return packstr(OPEN, "");
    }

    /**
     * Cerrar canal abierto --> ctx.currentch <br>
     * Si no había canal abierto -> NO se hace nada <br>
     * Primero quitamos la ID de este cliente del canal <br>
     * Perder referencia al canal cerrado del contexto <br>
     * Poner posición del leer a 0 <br>
     */
    protected String close(MsgStr msg, Context ctx) {
        if (ctx.currentch != null) {
            ctx.currentch.rmCli(ctx.id);
            ctx.currentch = null;
            ctx.posrd = 0;
            ctx.lastcontent = "";
        }
        return packstr(CLOSE, "");
    }

    /**
     * Crear canal nuevo en el servidor --> MsgStr.msg = namech <br>
     * Al crear, se añade al array de channels <br>
     * No puede haber ningún canal abierto mientras
     * @param msg -> MsgStr.msg = namech -> Nombre del canal a crear
     * @param ctx -> Context del cliente
     */
    protected String mkChannel(MsgStr msg, Context ctx) {
        if (ctx.currentch != null) {
            throw new RuntimeException("Channel already in use => close it first");
        }
        channels.mkch(msg.msg);
        return packstr(MKCH, "");
    }

    /**
     * Borrar canal concreto en el servidor --> MsgStr.msg = namech <br>
     * Obtenemos el canal concreto y lo borramos <br>
     * Al borrar -> se borra del array de channels <br>
     * No puede haber ningún canal abierto mientras por el user que quiera hacerlo <br>
     * El canal a borrar no tiene que tener clientes conectados --> RuntimeException
     * @param msg -> MsgStr.msg = namech -> Nombre del canal a borrar
     * @param ctx -> Context del cliente
     */
    protected String rmChannel(MsgStr msg, Context ctx) {
        if (ctx.currentch != null) {
            throw new RuntimeException("Channel already in use => close it first");
        }
        var ch = channels.getch(msg.msg);
        if (ch.valactive()) {
            throw new RuntimeException("Channel still in use by clients");
        }
        channels.rmch(ch);
        return packstr(RMCH, "");
    }

    /**
     * Escribir contenido en el canal que esté abierto -> ctx.currentch <br>
     * NO se pueden escribir contenidos idénticos <br>
     * @param msg -> MsgStr.msg = str -> Info a escribir en el canal (contenido)
     * @param ctx -> Context del cliente
     */
    protected String writeChannel(MsgStr msg, Context ctx) {
        if (ctx.currentch == null) {
            throw new RuntimeException("No opened channel to write");
        } else if (msg.msg.isEmpty()) {
            throw new RuntimeException("No allowed to write empty content");
        }
        ctx.currentch.wrch(msg.msg);
        return packstr(WRCH, "");
    }

    /**
     * Borrar contenido específico del canal que esté abierto -> ctx.currentch <br>
     * Borramos la primera aparición del contenido si hay varios repetidos
     * @param msg -> MsgStr.msg = str -> Info a borrar del canal (contenido)
     * @param ctx -> Context del cliente
     */
    protected String rmContent(MsgStr msg, Context ctx) {
        if (ctx.currentch == null) {
            throw new RuntimeException("No opened channel to edit");
        } else if (msg.msg.isEmpty()) {
            throw new RuntimeException("No possible to delete empty content");
        }
        ctx.currentch.rminch(msg.msg);
        return packstr(RMCONT, "");
    }

    /**
     * Leer siguiente contenido del canal que esté abierto -> ctx.currentch <br>
     * Recibir: "1" --> dontwait = false (default) | "0" --> dontwait = true <br>
     * @param msg -> MsgStr -> **dontwait = true/false --> para ver si se espera a leer mensajes añadidos o no**
     * @param ctx -> Context del cliente
     */
    protected String readChannel(MsgStr msg, Context ctx) {
        if (ctx.currentch == null) {
            throw new RuntimeException("No channel to read");
        }
        if (msg.msg.equals("1")) {
            ctx.dontwait = false;
        }
        return getcont(ctx);
    }

    /**
     * Leer siguiente contenido del canal <br>
     * Después de leer del canal --> ctx.posrd += 1 <-> ctx.dontwait = true <br>
     * Solo aumentar posición de lectura si se ha leído algo --> content != null <br>
     * Si content == null -> Enviar => content = "" <br>
     * Si se ha leído lo mismo que el último contenido -> volver a leer el canal en la siguiente pos
     * @param ctx -> Context
     * @return -> String pos = ctx.posrd en el canal
     */
    protected String getcont(Context ctx) {
        var content = ctx.currentch.rdch(ctx.posrd, ctx.dontwait);
        if (content != null) {
            ctx.posrd += 1;
            if (content.equals(ctx.lastcontent)) { return getcont(ctx); }
        } else { content = ""; }
        ctx.dontwait = true;
        ctx.lastcontent = content;
        return packstr(RDCH, content);
    }

}
