package servicies;

/**
 * Interfaz de MOM (Message Oriented Middleware) -> Distintos canales/flujos de mensajes con estado
 */
public interface Mom {
    void open(String namech);
    void close();

    void mkChannel(String name);
    void rmChannel(String name);
    void writeChannel(String content);
    String readChannel(boolean dontwait);

    void rmContent(String content);

}
