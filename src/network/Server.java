package network;

import protocol.MsgStr;
import protocol.Tag;
import servicies.Channels;
import servicies.Svc;
import servicies.SvcMom;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static protocol.Heads.*;
import static protocol.ToolDefs.slog;         // Logger de depuración para server -> slog

/**
 * Servidor responsable de manejar clientes conectados y responderles correctamente
 */
public class Server {
    private final Svc svc;                   // Servicio a utilizar en este servidor
    public String name;                      // Nombre del servidor
    public int port;                         // Puerto del servidor
    public InetSocketAddress add;            // Ip y puerto del servidor
    private final ServerSocketChannel ssk;   // Server Socket Channel

    /**
     * Variables estáticas para acceder a ellas desde todos los clientes en el server:
     */
    protected static Tag idscli;             // IDs de clientes asignados
    public static Channels channels;         // Canales creados para el sistema MOM

    public Server(String name, int port, Svc svc) {
        this.svc = svc;
        this.name = name;
        this.port = port;
        this.add = new InetSocketAddress(this.port);
        try {
            // Abro el socket channel con buffering, con el puerto
            ssk = ServerSocketChannel.open();
            // Pasarle la dirección al server socket channel (ip y puerto)
            ssk.bind(this.add);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        idscli = new Tag();
        channels = new Channels();
    }

    /**
     * Lanzar el servidor: Puerto = 8080 <-> Name = "TestServer" <-> Svc = SvcMom
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            var svc = new SvcMom();
            server = new Server("TestServerMOM", 8080, svc);
            server.start();
        } catch (Exception _) {
        } finally {
            server.close();
        }
    }

    /**
     * @param suf -> Separador
     * @return -> Descripción del servidor
     */
    public String dump(String suf) {
        var sb = new StringBuilder();
        sb.append("Server=").append(this.name).append(suf);
        sb.append(this.add.getPort()).append(suf);
        sb.append(this.add.getAddress().getHostAddress()).append(suf);
        sb.append(this.svc).append(suf);
        return sb.toString();
    }

    /**
     * Thread que lanza el servidor a escuchar y responder clientes<br>
     * jstack -l para debug -> ver el stack de threads (volcado de pila)
     */
    public void start() {
        slog.info(dump(":"));
        while (true) {
            try {
                SocketChannel sk = ssk.accept();
                slog.info("New client connected");
                // Nuevo hilo por cliente --> con el mismo servicio y su propio contexto
                new Cli(sk, svc).start();
            } catch (ClosedChannelException _) {
                // Terminar de aceptar clientes cuando se cierra el servidor
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
                slog.error("svr ex: {}", String.valueOf(ex));
                break;
            }
        }
    }

    /**
     * Cerrar conexión/socketchannel del servidor
     */
    public void close() {
        try {
            ssk.close();
            slog.info("Server {} cerrado correctamente!!!", name);
        } catch (IOException _) {} // Ignorar excepción
    }


    /**
     * Clase que se lanza en otro hilo para manejar a cada cliente por separado<br>
     * Server guarda estado de cada cliente en local después de cada iteración
     */
    private class Cli extends Thread {
        private final Svc svc;                        // Servicio a utilizar
        private Object ctx;                           // Contexto que nos da el servicio
        protected AtomicInteger id;                   // ID del cliente asignado

        public Cli(SocketChannel sk, Svc svc) {
            // Guardar servicio en uso
            this.svc = svc;
            // Asignar ID de cliente que no esté en uso:
            this.id = idscli.newtag();
            // Inicializar contexto del servicio --> sk del cliente y name del server
            this.ctx = svc.newCli(sk, name, id);
        }

        /**
         * Servidor empieza a gestionar RPCs del cliente en otro thread<br>
         * -> para poder enviar por otro thread si es necesario
         */
        public void run() {
            new Thread(() -> {
                try {
                    while (true) {
                        // Devuelve mensaje leído -> MsgStr
                        var msgrv = svc.receive(0, ctx);
                        // Terminar conexión con asentimiento de LOGOUT -> FINACK
                        if (process(msgrv) == 1) break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    slog.error("srv ex: {}", String.valueOf(e));
                } finally {
                    // Quitar id del cliente en uso:
                    idscli.rmtag(id.get());
                    // Borrar contexto de memoria y finalizar el servicio:
                    svc.endCli(ctx);
                }
            }).start();
        }

        /**
         * Procesar el msg -> según el servicio
         * @param msg -> MsgStr
         * @return -> Flag de salida (1) o continuar (0)
         * @throws Exception -> En caso de error en gestión de mensajes en el servicio
         */
        protected int process(MsgStr msg) throws Exception {
            // Mensaje String de respuesta después del manejador
            String resp = svc.handle(msg, ctx);
            // Enviar respuesta al cliente --> Siempre responde el server
            svc.send(resp, msg.tag.get(), ctx);
            if (resp.equals(wcab(FINACK, ":"))) {
                // Flag de salida de conexión --> 1
                return 1;
            }
            // Flag de continuar en la conexión --> 0
            return 0;
        }

    }

}
