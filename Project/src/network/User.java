package network;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static app.Dibujo.getfname;
import static app.Dibujos.getfnames;
import static network.Cabs.*;
import static network.Msg.intarrstr;
import static network.Tag.tag;
import static network.TcpDefs.ulog;         // Logger de depuración para cliente -> ulog

public class User {
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

    public User(String name, int port, String iph) {
        this.name = name;
        this.dirname = "Dibujos:" + name;
        this.porth = port;
        this.iph = iph;
        this.ID = 0;                            // Lo decide el servidor
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

    protected String close() {
        try {
            sk.close();
            return name + "->" + ID + " desconectado del server " + hname;
        } catch (IOException e) {
            throw new RuntimeException("User closing err: " + e.getMessage());
        }
    }

    public void send(String s, AtomicInteger tag) {      // Enviar msg al servidor
        try {
            var msg = Msg.MsgStr.doWrite(tag.get());
            msg.writeTo(sk, s);
            tags.addtag(tag.get());                     // Añadir tag en uso
            System.err.println("Mensaje enviado user: " + msg.msg);
        } catch (Exception e) {
            e.printStackTrace();
            ulog.error("user ex: " + e);
        }
    }

    public String receive(AtomicInteger tag) {     // Recibir y controlar msg del servidor
        try {
            var msg = Msg.MsgStr.rv(sk, msgsleft, tag);
            if (msg == null) {
                throw new RuntimeException("Conexión cerrada inesperadamente");
            }
            System.err.println("Mensaje recibido user: " + msg.msg);
            var result =  handlerRV(msg);
            tags.rmtag(tag.get());                      // Borrar tag ya usado
            return result;                              // Devolver mensaje recibido (str)
        } catch (Exception e) {
            e.printStackTrace();
            ulog.error("user ex: " + e);
            return "user ex: " + e;
        }
    }

    protected String handlerRV(Msg.MsgStr msg) {
        Cabs cab = getcab(msg.inst);   // Buscar el enum correspondiente en Cabs
        System.err.println("Received cab user: " + cab.name());
        if (cab.equals(INIACK)) {
            return loginrv(msg.msg);                     // Recoger info del servidor
        } else if (cab.equals(FINACK)) {
            return close();                              // Cierre de conexión controlado
        } else if (cab.equals(ERR)) {
            throw new RuntimeException(msg.msg);         // Error en el servidor
        }
        return msg.msg;                         // Devolver mensaje que ha recibido (resto de cabeceras)
    }

    protected String buildstr(Cabs cab, String base, String sep, String... infos) {
        var tag = tag();                 // Tag del mensaje
        var all = new StringBuilder(mk_cmsg(cab, base));
        for (String info : infos) {         // Construir msg str --> cab: base /info/info/info...
            all.append(sep).append(info);
        }                               // Añadir partes independientes dependiendo del separador
        send(all.toString(), tag);      // Mandar nuevo mensaje
        return receive(tag);
    }

    // Acciones disponibles --> triggers --> imprimir mensajes recibidos del servidor (respuestas <-> replies)

    protected void login() {            // Primer mensaje que enviamos al servidor automáticamente
        var reply = buildstr(LOGIN, this.toString(), "");   // Mandar descripción y dibujos cargados
       System.err.println(reply);
    }

    protected String loginrv(String msg) {      // Bienvenida al user
        var ss = msg.split(":");
        hname = ss[0];                      // Nombre del servidor
        ID = Integer.parseInt(ss[1]);       // El id nos lo da el server
        return name + "->" + ID + " conectado en " + add.getAddress().getHostAddress() +
                ":" + add.getPort() + " al servidor " + hname + " en " + iph + ":" + porth;
    }

    protected void logout() {           // Solicitar desconexión
        var reply = buildstr(LOGOUT, this.toString(), "");
        System.err.println(reply);
    }

    protected void newd(String... figs) {                  // Nuevo dibujo
        var reply = buildstr(NEWD, "", "//", figs);     // Crear dibujo vacío ó con figuras(separadas "//")
        System.err.println(reply);
    }

    protected void rmd(String autor, int id) {       // Eliminar dibujo -> id del autor
        var reply = buildstr(RMD, getfname(autor, id), "");
        System.err.println(reply);
    }

    protected void newf(String autor, int id, String... figs) {      // Nueva/s figura/s en dibujo(autor:id)
        if (figs.length == 0) {                                      // fname//fig1//fig2//fig3...
            System.err.println("No se puede añadir figura vacía");
            return;
        }                                                   // Añadir figura/s al mensaje, separadas por "//"
        var reply = buildstr(NEWF, getfname(autor, id), "//", figs);
        System.err.println(reply);
    }

    protected void rmf(String autor, int id, int... fids) {  // Borrar figura/s(sus ids) en dibujo(autor:id)
        if (fids.length == 0) {                                 // fname/fid1/fid2/fid3...
            System.err.println("Añade figuras para borrar");
            return;
        }                                    // Añadir ids(pasados a str) de figura/s al mensaje, separadas por "/"
        var reply = buildstr(RMF, getfname(autor, id), "/", intarrstr(fids));
        System.err.println(reply);
    }

    protected void mvf(String autor, int id, int x, int y, int... fids) {   // Mover figura/s unas coordenadas, dentro del dibujo
        if (fids.length == 0) {                                 // autor:id//x,y/id1/id2/id3...
            System.err.println("Añade figuras para mover");
            return;
        }
        var reply = buildstr(MVF, getfname(autor, id) + "//" + x + "," + y, "/", intarrstr(fids));
        System.err.println(reply);
    }

    protected void mvd(String[] autor, int[] id, int x, int y) {  // Mover dibujo/s unas coordenadas = mover sus figuras
        if(id.length == 0 || autor.length == 0 || (id.length != autor.length)) {
            System.err.println("Añadir correctamente datos de dibujo/s");
            return;
        }                                   // x,y//autor1:id1/autor2:id2/autor3:id3...
        var reply = buildstr(MVD, x + "," + y + "/" + getfnames(autor, id, "/"), "");  // Se separan con "//"
        System.err.println(reply);
    }

    protected void dump(String[] autor, int[] id) {      // Volcar dibujo(autor:id)
        String reply;
        if(id.length == 0 && autor.length == 0) {
            reply = buildstr(DUMP, "", "");      // Si no se especifican dibujos, volcar todos
        } else {                                        // Manda mensaje vacío
            reply = buildstr(DUMP, getfnames(autor, id, "/"), "");// Si se especifican dibujos, solo volcar esos
        }                                               // fname1/fname2/fname3...
        System.err.println(reply);
    }



    public String dump(String suf, String tipo) {
        var sb = new StringBuilder();
        sb.append(name).append(suf);                                // Nombre
        sb.append(tipo).append(suf);                                // Tipo (User, Admin)
        sb.append(add.getAddress().getHostAddress()).append(suf);   // IP
        sb.append(add.getPort()).append(suf).append("\n");          // Puerto
        return sb.toString();
    }

    protected String getTipo() {    // Obtener tipo de user
        return "User";
    }

    @Override
    public String toString() {
        return dump(":", getTipo());
    }

}
