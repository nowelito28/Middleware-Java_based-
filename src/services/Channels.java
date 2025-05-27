package services;

import java.util.ArrayList;

/**
 * Clase que agrupa e interactua con varios canales de mensajes
 */
public class Channels {
    public ArrayList<Channel> channels;         // Lista de canales de mensajes

    public Channels() {
        channels = new ArrayList<>();
    }

    /**
     * Añadir canal
     * @param ch -> Canal a agregar
     * @throws RuntimeException -> Si el canal ya estaba en memoria
     */
    public synchronized void addch(Channel ch) {
        for (var channel : channels) {
            if (channel.name.equals(ch.name)) {
                throw new RuntimeException("Channel '" + ch.name + "' already exists");
            }
        }
        channels.add(ch);
    }

    /**
     * Crear nuevo con el nombre que se le pasa y añadirlo al array de canales
     * @param namech -> Nombre del canal
     * @return -> Canal con ese nombre creado
     */
    public Channel mkch(String namech) {
        var ch = new Channel(namech);
        addch(ch);
        return ch;
    }

    /**
     * Borrar canal <br>
     * FALTA SI SE BORRA UN CANAL ABIERTO POR ALGÚN CLIENTE --> NOTIFICAR DE QUE EL CANAL SE HA BORRADO!!!!
     * @param ch -> Canal a borrar
     * @throws RuntimeException -> Si el canal no estaba en memoria
     */
    public synchronized void rmch(Channel ch) {
        if (!channels.contains(ch)) {
            throw new RuntimeException("No such channel in memory");
        }
        channels.remove(ch);
    }

    /**
     * Devolver canal específico por su nombre
     * @param namech -> Nombre del canal
     * @return -> Canal con ese nombre
     * @throws RuntimeException -> Si el canal no estaba en memoria
     */
    public synchronized Channel getch(String namech) {
        for (var ch : channels) {
            if (ch.name.equals(namech)) {
                return ch;
            }
        }
        throw new RuntimeException("No such channel in memory");
    }





}
