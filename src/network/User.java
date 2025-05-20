package network;

import protocol.*;
import servicies.Mom;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;

import static app.Draw.getfname;
import static app.Draws.getfnames;
import static protocol.Heads.*;
import static protocol.MsgStr.*;
import static protocol.ToolDefs.ulog;         // Logger de depuración para cliente -> ulog

/**
 * User -> implementación del cliente de red<br>
 * Preparado para conectar al servidor -> enviar y recibir mensajes
 */
public class User implements Mom {
    public String name;                 // Nombre del user
    public String dirname;              // Directorio de dibujos del user
    public String hname;                // Nombre del servidor al que nos conectamos
    public int porth;                   // Puerto del servidor
    public String iph;                  // Ip del servidor
    public InetSocketAddress add;       // Ip y puerto del user
    public int ID;                      // ID del user --> Lo asigna el servidor
    public SocketChannel sk;            // Socket channel del user, con el cual nos conectamos al servidor
    public Tag tags;                    // Lista de tags de mensajes enviados -> Vigilar si se recibe la respuesta
    public BufMsg msgsleft;             // Mensajes pendientes de leer

    /**
     * Constructor del cliente --> se conecta al servidor
     * @param name -> Nombre del user
     * @param port -> Puerto del servidor
     * @param iph -> Ip del servidor
     */
    public User(String name, int port, String iph) {
        this.name = name;
        this.dirname = "Draws:" + name;
        this.porth = port;
        this.iph = iph;
        this.ID = 0;
        this.tags = new Tag();
        this.msgsleft = new BufMsg();

        try {
            sk = SocketChannel.open(new InetSocketAddress(iph, porth));
            this.add = (InetSocketAddress) sk.getLocalAddress();
            login();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Volcado de datos del cliente
     * @param suf -> Separador
     * @param tipo -> Tipo de user (User, Admin)
     * @return -> Descripción de datos del cliente
     */
    public String dump(String suf, String tipo) {
        var sb = new StringBuilder();
        sb.append(name).append(suf);
        sb.append(tipo).append(suf);
        sb.append(add.getAddress().getHostAddress()).append(suf);
        sb.append(add.getPort()).append(suf);
        return sb.toString();
    }

    /**
     * Obtener tipo de user
     * @return tipo de user (String)
     */
    protected String getTipo() {
        return "User";
    }

    @Override
    public String toString() {
        return dump(":", getTipo());
    }

    /**
     * Cerrar conexión con el servidor
     * @throws RuntimeException -> Error al cerrar el SocketChannel
     */
    public void close_user() {
        try {
            sk.close();
            ulog.info("{}->{} desconectado del server {}", name, ID, hname);
        } catch (IOException e) {
            throw new RuntimeException("User closing err: " + e.getMessage());
        }
    }

    /**
     * Enviar MsgStr al servidor<br>
     * Asignar nuevo tag sin usar a un nuevo MsgStr --> Añadirlo a tags en uso
     * @param s -> Info (String) que envía al servidor
     * @return -> MsgStr que se envía
     * @throws RuntimeException --> Controlar cualquier fallo de conexión o fabricación del mensaje
     */
    protected MsgStr send(String s) {
        try {
            var tag = tags.newtag().get();
            var msg = new MsgStr(tag);
            msg.writeTo(sk, s);
            ulog.debug("Mensaje enviado user: {}", msg.msg);
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
            ulog.error("user ex: " + e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Recibir y controlar MsgStr del servidor<br>
     * Borrar tag ya usado
     * @param tag -> Tag específico del mensaje que buscamos
     * @return -> MsgStr que buscamos
     * @throws IOException --> Fallos de conexión o captación de mensajes
     */
    protected MsgStr receive(int tag) throws IOException {
        var msg = MsgStr.rv(sk, msgsleft, tag);
        ulog.debug("Mensaje recibido user: {}", msg.msg);
        var msg_ok = handlerRV(msg);
        tags.rmtag(tag);
        return msg_ok;
    }

    /**
     * Comprueba si hay error en el MsgStr recibido
     * @param msg -> MsgStr recibido
     * @return --> el mismo MsgStr
     * @throws RuntimeException --> Si se reconoce el mensaje como error por la cabecera
     */
    protected MsgStr handlerRV(MsgStr msg) {
        Heads cab = getcab(msg.inst);
        ulog.debug("Received cab user: {}", cab.name());
        if (Objects.equals(cab, ERR)) {
            throw new RuntimeException(msg.msg);
        }
        return msg;
    }

    /**
     * RPC (remote procedure call) --> mandar y recibir  <br>
     * 1º -> Mandar nuevo mensaje con nuevo tag que no esté en uso <br>
     * 2º -> Recibir mensaje de vuelta con el mismo tag
     * @param info -> String de información que se quiere enviar al servidor
     * @return -> MsgStr de respuesta del servidor
     * @throws IOException -> Fallo de conexión
     */
    protected MsgStr rpc(String info) throws IOException {
        var msg = send(info);
        return receive(msg.tag.get());
    }

    /**
     * Fabricar RPC --> mandar y recibir -> Controlar excepciones
     * @param head -> Instrucción que se quiere realizar en el servidor
     * @param info -> String de información que se quiere enviar al servidor
     * @return -> MsgStr de respuesta del servidor
     * @throws RuntimeException --> Si hay una excepción --> La devolvemos
     *  --> Controlar externamente excepciones en acciones que hagan RPC
     *  --> No se controlan aquí apropósito --> para quien lo quiera utilizar que lo controle
     */
    protected MsgStr mk_rpc(Heads head, String info) {
        try {
            var str = packstr(head, info);
            return rpc(str);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // Acciones disponibles --> triggers --> RPCs (respuestas <-> replies)
    // Excepciones generadas o recibidas --> ¡Se siguen elevando después del trigger apropósito!

    /**
     * Primer mensaje enviado al servidor automáticamente <br>
     * Mandar descripción del user <br>
     * Recibimos nombre del server y el ID que nos ha asignado
     * @return
     * Si algo está mal o se manda mal --> se detecta antes de desempaquetar respuesta
     */
    protected int login() {
        var msglog = mk_rpc(LOGIN, this.toString());
        String[] reply = msglog.msg.split(":");
        hname = reply[0];
        ID = Integer.parseInt(reply[1]);
        ulog.info("{}:{} conectado al server {}", name, ID, hname);
        return ID;
    }

    /**
     * Solicitar desconexión al server <br>
     * Cerramos el canal abierto primero --> si lo hay <br>
     * No manda info -> solo cabecera LOGOUT <br>
     * Una vez nos llega el FINACK --> Cerramos conexión --> Ignoramos errores
     */
    protected void logout() {
        close();
        mk_rpc(LOGOUT, "");
        close_user();
    }

    /*
      1º Servicio a utilizar con el server: MOM
      Interfaz => Acciones disponibles para el servicio MOM del servidor:
     */


    /**
     * Abrir channel en el servidor
     * @param namech -> Nombre del channel que abrimos del servidor
     */
    @Override
    public void open(String namech) {
        mk_rpc(OPEN, namech);
        ulog.info("Channel opened: {}", namech);
    }

    /**
     * Cerrar channel abierto en el servidor
     */
    @Override
    public void close() {
        mk_rpc(CLOSE, "");
        ulog.info("Current channel closed");
    }

    /**
     * Crear canal en el servidor
     * @param namech -> Nombre del channel que creamos en el servidor
     */
    @Override
    public void mkChannel(String namech) {
        mk_rpc(MKCH, namech);
        ulog.info("New channel created: {}", namech);
    }

    /**
     * Borrar canal en el servidor
     * @param namech -> Nombre del channel que queremos borrar
     */
    @Override
    public void rmChannel(String namech) {
        mk_rpc(RMCH, namech);
        ulog.info("Channel removed: {}", namech);
    }

    /**
     * Escribir en el canal de mensajes abierto actualmente
     * @param content -> Mensaje que guardamos en el canal/channel
     */
    @Override
    public void writeChannel(String content) {
        mk_rpc(WRCH, content);
        ulog.info("New content added to current channel: {}", content);
    }

    /**
     * Borrar contenido específico del canal abierto actualmente
     * @param content --> Contenido a borrar
     */
    @Override
    public void rmContent(String content) {
        mk_rpc(RMCONT, content);
        ulog.info("Content removed from current channel: {}", content);
    }

    /**
     * Leer del canal de len mensajes -> Servidor envía siguiente mensaje del canal <br>
     * Primero mandar rpc RDCH --> Mandar int (0 o 1): <br>
     * -> dontwait = true --> NO más mensajes en el canal --> Devuelve "" --> wait = 0 <br>
     * -> dontwait = false --> NO más mensajes en el canal --> Esperar a que haya más --> wait = 1 <br>
     * --> Server devuelve el siguiente contenido del canal (String) <br>
     * "" --> Indica que ya NO hay más mensajes en el canal -> cuando dontwait = true <br>
     * @return -> String[] con todos los contenidos del canal solicitados
     */
    @Override
    public String readChannel(boolean dontwait) {
        var wait = 0;
        if (!dontwait) { wait = 1; }
        var msg = mk_rpc(RDCH, String.valueOf(wait));
        var content = msg.msg;
        if (content.isEmpty()) {
            ulog.info("No more contents in current channel");
        } else {
            ulog.info("Next content read from current channel: {}", content);
        }
        return content;
    }



    /*
      2º servicio a utilizar con el server: Edición de dibujos
      Interfaz => Acciones disponibles para manejar funciones básicas de la app de Dibujos:
     */


    /**
     * Crear nuevo dibujo
     * @param dfigs -> Descripciones de las figuras a introducir en el nuevo dibujo (Strings) --> puede NO haber
     *              --> Mandar así: //dfig1//dfig2//dfig3... Descripciones de figuras (separadas "//") ó nada
     * @return -> (autor:id) del dibujo creado (nombre del dibujo) -> String
     */
    protected String newd(String... dfigs) {
        var msgnewd = mk_rpc(NEWD, packstr_parts("//", dfigs));
        ulog.info(msgnewd.msg);
        return msgnewd.msg;
    }

    /**
     * Eliminar dibujo
     * @param autor -> Autor del dibujo (String)
     * @param id -> ID del dibujo (int)
     *           --> Mandar nombre dibujo a borrar -> autor:id = fname
     */
    protected void rmd(String autor, int id) {
        var msgrmd = mk_rpc(RMD, getfname(autor, id));
        ulog.info("Dibujo borrado: {}", getfname(autor, id));
    }

    /**
     * Nueva/s figura/s en dibujo(autor:id)
     * @param autor -> Autor del dibujo (String)
     * @param id -> ID del dibujo (int)
     * @param dfigs -> Descripciones de figuras a introducir en el dibujo (Strings)
     *              --> Mandar: autor:id//dfig1//dfig2//dfig3...
     * @return -> IDs de las figuras creadas (int[])
     * @throws RuntimeException --> Si no se adjuntan descripciones de figuras o se introducen mal, o fallos de conexión
     */
    protected int[] newf(String autor, int id, String... dfigs) {
        if (dfigs.length == 0) {
            throw new RuntimeException("No se puede añadir figura vacía");
        }
        var msgnewf = mk_rpc(NEWF, getfname(autor, id) + packstr_parts("//", dfigs));
        // Eliminar "//" del inicio:
        msgnewf.msg = msgnewf.msg.substring(2);
        var idsf = msgnewf.unpackstr_ids("//");
        ulog.info("Figuras creadas: {}", Arrays.toString(idsf));
        return idsf;
    }

    /**
     * Borrar figura/s(sus ids) en dibujo(autor:id)
     * @param autor -> Autor del dibujo (String)
     * @param id -> ID del dibujo (int)
     * @param fids -> IDs de las figuras a borrar (int[]) -> Separadas por "/"
     *             --> Mandar: autor:id/fid1/fid2/fid3...
     * @throws RuntimeException --> Si no se adjuntan IDs de figuras o se introducen mal, o fallos de conexión
     */
    protected void rmf(String autor, int id, int... fids) {
        if (fids.length == 0) {
            throw new RuntimeException("Añade figura/s para borrar");
        }
        var msgrmf = mk_rpc(RMF, getfname(autor, id) + arrint_str("/", fids));
        ulog.info("Figuras borradas: {}", Arrays.toString(fids));
    }

    /**
     * Mover figura/s unas coordenadas --> dentro del dibujo (autor:id)
     * @param autor -> Autor del dibujo (String)
     * @param id -> ID del dibujo (int)
     * @param x -> Desplazamiento en x
     * @param y -> Desplazamiento en y
     *          => Punto = x,y -> entre nombre del dibujo y los IDs
     * @param fids -> IDs de las figuras a borrar (int[]) -> Separadas por "/"
     *             --> Mandar: autor:id//x,y/id1/id2/id3...
     * @throws RuntimeException --> Si no se adjuntan IDs de figuras o se introducen mal, o fallos de conexión
     */
    protected void mvf(String autor, int id, int x, int y, int... fids) {
        if (fids.length == 0) {
            throw new RuntimeException("Añade figura/s para mover");
        }
        var msgmvf = mk_rpc(MVF, getfname(autor, id) +
                "//" + x + "," + y + arrint_str("/", fids));
        ulog.info("Figuras desplazadas: {}", Arrays.toString(fids));
    }

    /**
     * Mover dibujo/s unas coordenadas = mover sus figuras -> (autor:id)
     * @param autor -> Autor/es del dibujo/s (String[])
     * @param id -> ID/s del dibujo/s (int[])
     *           --> Tienen que tener misma longitud autor y id --> Correspondiente a dibujos
     * @param x -> Desplazamiento en x
     * @param y -> Desplazamiento en y
     *          => Punto = x,y -> delante de los nombres de los dibujos<br>
     *          --> Mandar: x,y/autor1:id1/autor2:id2/autor3:id3...
     * @throws RuntimeException --> Si no se adjuntan nombres de los dibujos o se introducen mal, o fallos de conexión
     */
    protected void mvd(String[] autor, int[] id, int x, int y) {
        if(id.length == 0 || autor.length == 0 || (id.length != autor.length)) {
            throw new RuntimeException("Añadir correctamente datos de dibujo/s");
        }
        var msgmvd = mk_rpc(MVD, x + "," + y + getfnames(autor, id, "/"));
        ulog.info("Dibujo/s desplazado/s: {}", getfnames(autor, id, "/"));
    }

    /**
     * Volcar dibujo/s(autor:id) o todos si no se adjuntan nombres
     * @param autor -> Autor/es del dibujo/s (String[])
     * @param id -> ID/s del dibujo/s (int[])
     *           --> Tienen que tener misma longitud autor y id --> Correspondiente a dibujos
     *           --> Si no hay nada en ningún array --> Descripción de todos los dibujos del user <br>
     *           --> Mandar: autor1:id1/autor2:id2/autor3:id3... <--> ""
     * @return --> Array de descripciones de dibujos solicitados (String []) --> Vacío si no hay dibujos
     * @throws RuntimeException --> Si no se adjuntan nombres de los dibujos o se introducen mal, o fallos de conexión
     */
    protected String[] dump(String[] autor, int[] id) {
        if(id.length != autor.length) {
            throw new RuntimeException("Añadir correctamente datos de dibujo/s");
        }
        // Separador de descripciones -> para hacer en unpack:
        var sep = "\n//\n";
        String info;
        if (id.length == 0) {
            info = "";
        } else {
            info = getfnames(autor, id, "/").substring(1);
        }
        var msgdump = mk_rpc(DUMP, info);
        // Nº de descripciones recibidas = Nº nombres mandados:
        var len = id.length;
        // Cuantos strings partidos hay en función de su separador:
        if (id.length == 0) { len = matches(msgdump.msg, sep); }
        // Pasar a array de descripciones String[]:
        var ddraws = msgdump.unpackstr_parts(len, sep);
        ulog.info("Descripciones de dibujo/s: {}", Arrays.toString(ddraws));
        return ddraws;
    }

}