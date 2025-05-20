package servicies;

import protocol.MsgStr;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interfaz de servicios --> NO se puede instanciar -> servicios tienen que implementar dichos métodos ó NO -> default<br>
 * Distinto servicio a utilizar al recibir el mensaje en el servidor -> manejador de mensajes
 */
public interface Svc {
    // Iniciar servicio para un nuevo cliente --> crear el contexto --> Si no tiene este método devuelve null
    default Object newCli(SocketChannel sk, String namesrv, AtomicInteger id) { return null; }
    // Policy -> command o strategy => pasar el contexto al cliente tmb -> Manejador de acciones solicitadas
    String handle(MsgStr msg, Object ctx);
    // Cuando termina el cliente se le pasa el contexto --> Termina la conexión
    default Object endCli(Object ctx) { return null; }
    // Siempre mandamos strings -> MsgStr -> Puede derivar en otros tipos de mensajes o en el mismo
    MsgStr send(String s, int tag, Object ctx) throws IOException, RuntimeException;
    // Siempre recibimos strings -> MsgStr -> Puede derivar en otros tipos de mensajes o en el mismo
    MsgStr receive(int tag, Object ctx) throws IOException, RuntimeException;
}
