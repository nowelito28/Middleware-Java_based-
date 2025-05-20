package servicies;

import app.Draws;
import protocol.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static app.Draw.getdname;
import static app.Draw.getfname;
import static protocol.Heads.*;
import static protocol.MsgStr.arrint_str;
import static protocol.MsgStr.str_arrint;
import static protocol.Tag.tag;
import static protocol.ToolDefs.svclog;    // Logger de depuración para servicios -> svclog

/**
 * Servicio de editar dibujos
 */

public class SvcEditD implements Svc {

    private final HashMap<Heads, Handler> handlers;     // Handlers --> No static: 1 handler por cada cliente

    /**
     * Definir handlers -> Cabecera relacionada con un a acción concreta:
     */
    public SvcEditD() {
        handlers = new HashMap<>();
        handlers.put(LOGIN, this::login);
        handlers.put(LOGOUT, this::logout);
        handlers.put(NEWD, this::newd);
        handlers.put(RMD, this::rmd);
        handlers.put(NEWF, this::newf);
        handlers.put(RMF, this::rmf);
        handlers.put(MVF, this::mvf);
        handlers.put(MVD, this::mvd);
        handlers.put(DUMP, this::dump);
    }

    @Override
    public String toString() {
        return "Edición de dibujos";
    }

    /**
     * Declarar interfaz para implementar cada manejador según la cabecera:
     */
    @FunctionalInterface
    protected interface Handler {
        String handler(MsgStr msg, Context ctx);
    }

    /**
     * Contexto de este servicio --> Inicializaciones:
     */
    protected static class Context {
        protected String namesrv;                     // Nombre del servidor
        protected SocketChannel sk;                   // SocketChannel del cliente
        protected Tag tagsm = new Tag();              // Tags usados en mensajes
        protected BufMsg msgsleft = new BufMsg();     // Buffer de mensajes pendientes

        protected AtomicInteger id;                   // ID del cliente
        protected String namecli;                     // Nombre del cliente
        protected String dirname;                     // Directorio de dibujos
        protected Draws draws = new Draws();          // Diccionario => cada dibujo con sus permisos
    }

    @Override
    public Object newCli(SocketChannel sk, String namesrv, AtomicInteger id) {
        var ctx = new Context();       // Crear contexto por cliente -> Sus datos
        ctx.namesrv = namesrv;
        ctx.sk = sk;
        ctx.id = id;
        return ctx;
    }

    /**
     * Distinguir acciones pedidas por el cliente según cabecera
     * @param msg -> MsgStr
     * @param objctx -> Context
     * @return Reply -> String
     */
    @Override
    public String handle(MsgStr msg, Object objctx) {
        try {
            // Comprobar si el msg es de tipo MsgStr y castear
            if (!(msg instanceof MsgStr rvmsg)) {
                return packstr(ERR, "Mensaje no reconocido");
            }
            // Comprobar si el contexto es de tipo Context y castear
            if (!(objctx instanceof Context ctx)) {
                return packstr(ERR, "Contexto no reconocido");
            }
            // Extraer cabecera -> enum
            Heads cab = getcab(rvmsg.inst);
            svclog.debug("Received cab server: " + cab.name());
            // Identificar manejador concreto respecto cabecera:
            Handler handler = handlers.get(cab);
            if (handler == null) {
                return packstr(ERR, "Cabecera desconocida: " + rvmsg.inst);
            }
            // Respuesta concreta respecto el manejador:
            return handler.handler(rvmsg, ctx);
        // Tmb detecta excepciones que eleven los manejadores
        } catch (Exception e) {
            e.printStackTrace();
            svclog.error(e);
            // Enviar mensaje de error si salta alguna excepción
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
            // Poner servicio a null para perder referencia y limpiar memoria (al ser static):
            ctx = null;
        // Ignorar excepción:
        } catch (Exception _) {}
        return null;
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
        svclog.info("Mensaje enviado server: {}", msgwr.msg);
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
        svclog.info("Mensaje recibido server: {}", msgrv.msg);
        return msgrv;
    }

    //Manejadores del servicio:

    /**
     * Logar/Parsear usuario --> Cargar en memoria sus dibujos
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return namesrv:idcli(asignado) --> String
     */
    protected String login(MsgStr msg, Context ctx) {
        // Dump/info del user:
        ctx.namecli = msg.msg.split(":", 2)[0];
        // Directorio de dibujos del user -> Draws:namecli
        ctx.dirname = getdname(ctx.namecli);
        // Cargar dibujos del user que están en local -> si tiene (en su dirname):
        ctx.draws.loadds(ctx.dirname, ctx.namecli);
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
     * Crear y guardar nuevo dibujo
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return autor:id -> fname --> String
     */
    protected String newd(MsgStr msg, Context ctx) {
        // Crear nuevo dibujo, separador de figuras = "//":
        var draw = ctx.draws.newdraw(msg.msg, ctx.dirname, ctx.namecli, "//");
        return packstr(NEWD, getfname(draw.autor, draw.id.get()));
    }

    /**
     * Borrar dibujo(autor:id) de memoria y local
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ""
     */
    protected String rmd(MsgStr msg, Context ctx) {
        // Borrar dibujo de memoria -> ¡si no existe no hace nada!
        ctx.draws.rmdraw(msg.msg, ctx.dirname);
        return packstr(RMD, "");
    }

    /**
     * Guardar/crear figura/s en dibujo -> autor:id
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return //id1//id2//id3... -> String (array de ids de figuras añadidas)
     */
    protected String newf(MsgStr msg, Context ctx) {
        // autor:id//dfig1//dfig2... -> autor:id | dfig1//dfig2...
        var ss = msg.unpack_2strs("//");
        // Validar permisos del dibujo y si hay figuras:
        var draw = ctx.draws.valeditfs(ss[0], ss[1]);
        // Añadir figuras:
        var idsf = ctx.draws.addfigs(ss[1], draw, "//", ctx.dirname);
        return packstr(NEWF, arrint_str("//", idsf));
    }

    /**
     * Eliminar figura/s(idfig/s) del dibujo(autor:id)
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ""
     */
    protected String rmf(MsgStr msg, Context ctx) {
        // autor:id/idfig1/idfig2... -> autor:id | idfig1/idfig2...
        var ss = msg.unpack_2strs("/");
        // Validar permisos del dibujo y si hay figuras:
        var draw = ctx.draws.valeditfs(ss[0], ss[1]);
        // Borrar figuras:
        ctx.draws.rmfigs(ss[1], draw, "/", ctx.dirname);
        return packstr(RMF, "");
    }

    /**
     * Mover figura/s concreta/s de 1 dibujo
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ""
     */
    protected String mvf(MsgStr msg, Context ctx) {
        // autor:id//x,y/idfig1/idfig2... -> autor:id | x,y | idfig1/idfig2...
        var ss = msg.unpack_3strs("//", "/");
        // Validar permisos del dibujo y si hay figuras:
        var draw = ctx.draws.valeditfs(ss[0], ss[2]);
        // Mover figuras:
        ctx.draws.mvfigs(ss[1], str_arrint("/", ss[2]), draw, ctx.dirname);
        return packstr(MVF, "");
    }

    /**
     * Mover todas las figuras de dibujo/s
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ""
     */
    protected String mvd(MsgStr msg, Context ctx) {
        // x,y//autor1:id1/autor2:id2... -> x,y | autor1:id1/autor2:id2...
        var ss = msg.unpack_2strs("/");
        // Verificar permisos de todos los dibujos:
        ctx.draws.valsedit(ss[1], "/", Perms.combine(Perms.WR, Perms.RD));
        // Mover dibujos:
        ctx.draws.mvdraws(ss[0], ss[1], "/", ctx.dirname);
        return packstr(MVD, "");
    }

    /**
     * Volcar descripción de uno o varios dibujos
     * @param msg -> MsgStr
     * @param ctx -> Context
     * @return ddraw1\n//\nddraw2\n//\nddraw3\n//\n... --> String
     */
    protected String dump(MsgStr msg, Context ctx) {
        String[] ddraws;
        var sep = "\n//\n";
        var sb = new StringBuilder();
        if (msg.msg.isEmpty()) {
            // Descripciones de todos los dibujos:
            ddraws = ctx.draws.dumpall();
        } else {
            // Sacar nombres de dibujos concretos del string -> autor1:id1/autor2:id2...
            var fnames = msg.unpackstr_tot("/");
            // Descripciones de dibujos concretos:
            ddraws = ctx.draws.dumpsmd(fnames);
        }
        // Mandar array de descripciones aplanadas en un str separadas por "\n//\n":
        for (var ddraw : ddraws) {
            sb.append(ddraw).append(sep);
        }
        return packstr(DUMP, sb.toString());
    }
    }
