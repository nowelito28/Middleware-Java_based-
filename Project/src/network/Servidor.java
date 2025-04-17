package network;

import app.Dibujos;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static app.Dibujo.*;
import static network.Cabs.*;
import static network.Tag.tag;

public class Servidor {
    //private final Svc svc;
    public String name;
    public LinkedList<String> users;         // Usuarios conectados --> hacer métodos thread-safe para nombres en uso
    public int port;
    public InetSocketAddress add;       // Ip y puerto del servidor
    private final ServerSocketChannel ssk;    // Server Socket Channel

    /* Ver como hacer la clase Servicio para poder hacer el handler en el servicio => Hacer servicio aparte -> ej8
    public static interface Svc {       // Servicio a utilizar al recibir el mensaje en el servidor -> manejador de mensajes
        default Object newCli(SocketChannel sk) { return null; }
        Msg handle(Msg msg, Object ctx);            // Policy -> command o strategy   => pasar el contexto al cliente tmb
        default Object endCli(Object ctx) { return null; }      // Cuando termian el cliente se le pasa el contexto
    }       // hacer dohandle del servicio donde se diferencia el tipo de mensaje(instanceof) y hacer el handler para ese mensaje (probar clases implements visit)
    */

    public Servidor(String name, int port) {
        //this.svc = svc;//
        this.name = name;
        this.users = new LinkedList<>();
        this.port = port;
        this.add = new InetSocketAddress(this.port);
        try {
            ssk = ServerSocketChannel.open();       // Abro el socket channel con buffering, con el puerto
            ssk.bind(this.add);     // Pasarle la dirección al server socket channel (ip y puerto)
        } catch (Exception ex) {        // A cualquier error lo convierto en un RuntimeException
            throw new RuntimeException(ex);
        }
    }

    public StringBuilder dump(StringBuilder sb, String suf) {
        sb.append("Server: ").append(this.name).append(suf);
        sb.append(this.add.getPort()).append(suf);
        sb.append(this.add.getAddress().getHostAddress()).append(suf);
        return sb;
    }

    public String dump(String suf) {
        var sb = new StringBuilder();
        return dump(sb, suf).toString();
    }

    public void start() {
        new Thread(() -> {      // Lanzar hilo con lambda => métodos como argumentos
            System.err.println("Server " + name + " lanzado en el puerto " + add.getPort() +
                    " con ip " + add.getAddress().getHostAddress());
            while (true) {
                try {
                    SocketChannel sk = ssk.accept();
                    System.err.println("New client connected");
                    new Cli(sk).start();    // ¡¡¡Crea un nuevo hilo para manejar el cliente!!!
                } catch (ClosedChannelException _) {
                    break;                  // Terminar de aceptar clientes cuando se cierra el servidor
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.err.println("Server exception: " + ex);
                    break;
                }
            }
        }).start();
    }

    public void close() {
        try {
            ssk.close();
            System.err.println("Server " + name + " cerrado correctamente!!!");
        } catch (IOException _) {} // Ignorar excepción
    }



    // Servidor guarda estado de cada cliente en local después de cada iteración
    private class Cli extends Thread {
        protected final SocketChannel sk;             // Socket Channel
        protected HashMap<Cabs, Handler> handlers;      // Handlers --> No static: 1 handler por cada cliente
        protected String namecli;                     // Nombre del cliente
        protected AtomicInteger idcli;                       // Identificador del cliente
        protected String dirname;                   // Directorio de dibujos
        protected BufMsg msgsleft;              // Guardar mensaje que quedan por leer
        protected Tag tagsm;                        // Tags usados en mensajes
        protected Dibujos draws;                    // Diccionario => cada dibujo con sus permisos
        //private Object svcCtx;

        public Cli(SocketChannel sk) {
            this.sk = sk;
            namecli = "";
            idcli = new AtomicInteger();
            dirname = "";
            msgsleft = new BufMsg();
            tagsm = new Tag();
            draws = new Dibujos();

            handlers = new HashMap<>();            // Definir handlers -> Cabecera relacionada con un a acción concreta
            handlers.put(LOGIN, this::login);
            handlers.put(LOGOUT, this::logout);
            handlers.put(NEWD, this::newd);
            handlers.put(RMD, this::rmd);
            handlers.put(NEWF, this::newf);
            handlers.put(RMF, this::rmf);
            handlers.put(MVF, this::mvf);
            handlers.put(MVD, this::mvd);
            handlers.put(DUMP, this::dump);

            //svcCtx = svc.newCli(sk);
        }

        @FunctionalInterface
        public interface Handler {      // Declarar interfaz para implementar cada manejador según la cabecera
            String handler(Msg.MsgStr msg);
        }

        public void close() {
            try {
                sk.close();
                System.err.println("Cliente " + namecli + "->" + idcli + " desconectado del servidor");
            } catch (IOException _) {}  // Ignorar excepción
        }

        public Msg.MsgStr send(String s, int tag) throws IOException, RuntimeException {      // Enviar msg al cliente
            var msgwr = Msg.MsgStr.doWrite(tag);
            msgwr.writeTo(sk, s);
            System.err.println("Mensaje enviado server: " + msgwr.msg);
            tagsm.rmtag(tag);       // Borrar tag msg ya usado
            return msgwr;
        }

        public Msg.MsgStr receive(int tag) throws IOException, RuntimeException {      // Recibir msg al cliente
            var msgrv = Msg.MsgStr.rv(sk, msgsleft, new AtomicInteger(tag));
            tagsm.addtag(msgrv.tag.get());            // Añadir tag msg ya en uso
            System.err.println("Mensaje recibido server: " + msgrv.msg);
            return msgrv;
        }

        public void run() {     // Empezar el servidor a escuchar al cliente en otro thread -> para poder enviar tmb
            new Thread(() -> {
                try {
                    while (true) { // Devuelve mensaje leído -> MsgStr -> Lee el buffer ó mensaje pendiente
                        var msgrv = receive(0);
                        if (msgrv == null) {    // Si se cierra la conexión --> Finalizar (mensaje nulo)
                            break;
                        }
                        if (process(msgrv) == 1) {     // Procesar mensaje recibido
                            break;          // Si se manda asentimiento del logout(FINACK) => 1 --> Cerrar conexión
                        }                   // Si no es así => 0 --> Continuamos conexión
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("srv ex IOException: " + e);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    System.err.println("srv ex RunTimeException: " + e);
                } finally {
                    /*
                    try {
                        svc.endCli(svcCtx);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("srv ex: " + e);
                    }
                     */
                    close();    // Cerrar socket del cliente
                }
            }).start();
        }

        // PROCESAR MENSAJES:

        protected int process(Msg.MsgStr msg) throws RuntimeException {        // Procesar el msg -> según el servicio
            try {
                var resp = handler(msg);     // Analyse el msg para ver que hacer -> manejar respuesta en otro thread
                msg.clean();
                send(resp, msg.tag.get());          // Escribir respuesta al cliente -> Siempre responde
                if (resp.equals(wcab(FINACK, ":"))) {
                    return 1;                       // Flag de salida de conexión
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;                           // Flag de continuar en la conexión
        }

        protected String handler(Msg.MsgStr msg) {      // Parsear el msg para ver que hacer
            try {
                Cabs cab = getcab(msg.inst);            // Buscar el enum correspondiente en Cabs
                System.err.println("Received cab server: " + cab);
                Handler handler = handlers.get(cab);    // Identificar manejador concreto respecto cabecera
                if (handler == null) {                  // Si no se reconoce la cabecera --> Mensaje de error
                    return mk_cmsg(ERR, "Unknown action");
                }
                return handler.handler(msg);
            } catch (Exception e) {
                return mk_cmsg(ERR, e.getMessage());      // Enviar mensaje de error
            }
        }

        protected String login(Msg.MsgStr msg) {        // Logar/Parsear usuario --> Cargar en memoria sus dibujos
            this.namecli = msg.msg.split(":", 2)[0];     // Le pasa info del user
            this.idcli = tag();                   // Poner id al cliente --> Habría que ver si ya está en uso dicho tag
            this.dirname = getdname(namecli);     // Directorio de dibujos del user
            this.draws.loadds(dirname, namecli);                // Cargar dibujos del user que están en local -> si tiene
            return mk_cmsg(INIACK, Servidor.this.name + ":" + idcli);
        }

        protected String logout(Msg.MsgStr msg) {                // Desconectar usuario
            return mk_cmsg(FINACK, "");
        }

        protected String newd(Msg.MsgStr msg) {    // Crear y guardar nuevo dibujo
            var draw = draws.newdraw(msg.msg, dirname, namecli, "//"); // Crear nuevo dibujo, separador de figuras = "//"
            return mk_cmsg(NEWD, "Nuevo dibujo creado:\n" +
                    draws.dumpd(getfname(draw.autor, draw.id.get())));// Mandar descripción del dibujo con permisos
        }

        protected String rmd(Msg.MsgStr msg) {    // Borrar dibujo(autor:id) de memoria y local
            draws.rmdraw(msg.msg, dirname);    // Borrar dibujo de memoria -> si no existe no hace nada
            return mk_cmsg(RMD, "Dibujo borrado:\n" + msg.msg);      // Mandar de nuevo nombre del fichero/dibujo borrado
        }

        protected String newf(Msg.MsgStr msg) throws IndexOutOfBoundsException {  // Guardar figura/s en dibujo -> autor:id
            var ss = msg.msg.split("//", 2);  //--> autor:id//descripcion_fig1//descripcion_fig2...
            var draw = draws.valeditfs(ss[0], ss[1]);
            return mk_cmsg(NEWF, "Dibujo actualizado, después de añadir figura/s:\n" +
                    draws.addfigs(ss[1], draw, "//", dirname));
        }

        protected String rmf(Msg.MsgStr msg) throws IndexOutOfBoundsException {  // Eliminar figura/s(idfig/s) del dibujo(autor:id)
            var ss = msg.msg.split("/", 2);  //--> autor:id/idfig1/idfig2...
            var draw = draws.valeditfs(ss[0], ss[1]);
            return mk_cmsg(RMF, "Dibujo actualizado, después de figura/s borrada:\n" +
                    draws.rmfigs(ss[1], draw, "/", dirname));
        }

        protected String mvf(Msg.MsgStr msg) {  // Mover figura/s concreta/s de 1 dibujo
            var ss = msg.msg.split("//",2 );  //--> autor:id//x,y/idfig1/idfig2...
            var info = ss[1].split("/", 2);
            var draw = draws.valeditfs(ss[0], info[1]);   // Validar permisos del dibujo y si hay figuras
            return mk_cmsg(MVF, "Dibujo actualizado, después de mover figura/s:\n" +
                    draws.mvfigs(info[0], info[1], draw, "/", dirname));
        }

        protected String mvd(Msg.MsgStr msg) {  // Mover todas las figuras de dibujo/s
            var ss = msg.msg.split("//", 2);       // --> x,y//autor1:id1/autor2:id2/autor3:id3...
            draws.valsedit(ss[1], "/", Permisos.combine(Permisos.WR, Permisos.RD));     // Verificar permisos de todos los dibujos
            return mk_cmsg(MVD, "Dibujo/s actualizado/s, después de moverlos:\n" +
                    draws.mvdraws(ss[0], ss[1], "/", dirname, "///\n\n"));// Pasarle desplazamiento y dibujos que mover --> Responder descrips de dibujos
        }

        protected String dump(Msg.MsgStr msg) {      // Volcar descripción de uno o varios dibujos
            var sepdump = "///\n\n";                     // Separador de dibujos
            String reply;
            if (msg.msg.isEmpty()) {
                reply = draws.dumpall(sepdump);
            } else {            // Elimina primer char del string -> "/" (1º separador de ids de dibujos)
                reply = draws.dumpsmd(msg.msg.substring(1), "/", sepdump);
            }
            return mk_cmsg(DUMP, "Contenido solicitado:\n" + reply);
        }



    }

}
