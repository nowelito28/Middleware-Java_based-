package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Canal de mensajes:<br>
 * Tiene su nombre y utilizar para almacenar distintos tipos de objetos/cosas
 */
public class Channel {
    public String name;                              // Nombre del canal
    protected ArrayList<AtomicInteger> clients;      // Usuarios conectados al canal
    protected HashMap<Integer, String> content;      // Contenido del canal --> Array de Strings guardando su posición

    public Channel(String name) {
        this.name = name;
        this.clients = new ArrayList<>();
        this.content = new HashMap<>();
    }

    /**
     * Añadir ID conectado cuando abra el canal
     * @param id -> ID del cliente conectado
     * @throws RuntimeException --> Si el ID ya estaba en memoria
     */
    public synchronized void addCli(AtomicInteger id) {
        if (clients.contains(id)) {
            throw new RuntimeException("User already in channel");
        }
        this.clients.add(id);
    }

    /**
     * Borrar ID conectado cuando cierre el canal --> Se desconecta del canal
     * @param id -> ID del cliente desconectado
     * @throws RuntimeException --> Si el ID no estaba en memoria
     */
    public synchronized void rmCli(AtomicInteger id) {
        if (!clients.contains(id)) {
            throw new RuntimeException("User not in channel");
        }
        this.clients.remove(id);
    }

    /**
     * Verificar si hay alguien conectado al canal --> Ver ids conectados <br>
     * Si hay alguien conectado -> true <br>
     * Si no hay alguien conectado -> false
     * @return boolean
     */
    public synchronized boolean valactive() {
        return !clients.isEmpty();
    }

    /**
     * Buscar clave del contenido que se le pasa <br>
     * Hacer bucle for para todas las clave-valor del HashMap <br>
     * Ver una a una si coincide el valor buscado <br>
     * Si coincide -> retornar la clave --> ¡Primera aparición si hay repetidas! <br>
     * Si no coincide -> lanzar RuntimeException
     * @param cont -> Contenido a buscar
     * @return int -> Clave del contenido
     */
    public synchronized int getkey(String cont) {
        for (Map.Entry<Integer, String> entry : content.entrySet()) {
            if (entry.getValue().equals(cont)) {
                return entry.getKey();
            }
        }
        throw new RuntimeException("No such content in channel: " + cont);
    }

    /**
     * Escribir contenido en el canal, en la siguiente posición libre <br>
     * Calcular siguiente posición libre como el tamaño del HashMap (contando los vacíos -> "")
     * --> No perdemos referencia porque la calave (int) se mantiene<br>
     * Así aunque borremos contenido -> pasamos a la siguiente posición en la clave <br>
     * Avisamos a hilos que esten esperando que hay más contenido en el canal -> notifyAll <br>
     * @param newcont -> Contenido a escribir
     */
    public synchronized void wrch(String newcont) {
        if (content.containsValue(newcont)) {
            throw new RuntimeException("Content already added");
        }
        int nextindex = content.size();
        content.put(nextindex, newcont);
        notifyAll();
    }

    /**
     * Borrar contenido concreto (valor) del canal --> Poner como vacío --> "" <br>
     * @param rmcont -> Contenido a borrar
     */
    public void rminch(String rmcont) {
        var index = getkey(rmcont);
        synchronized (content) {
            content.put(index, "");
        }
    }

    /**
     * Leer contenido de la posición indicada (index) del canal <br>
     * Si el contenido leído -> "" -> se ha borrado dicho contenido, pero se mantiene la clave/pos
     * --> Seguir leyendo en los siguientes índices hasta encontrar string o null --> No devolver "" !!!!<br>
     * dontwait --> tener en cuenta cuando no hay más contenido en el canal -> cont == null
     * @param index -> Posición del contenido
     * @param dontwait **Dontwait = true -> Devolver null si no más hay contenido para leer <br>
     *                 **Dontwait = false -> Poner en espera para leer actualizaciones --> wait -> notify
     * @return String -> Contenido leído --> null si no hay más contenido para leer
     */
    public synchronized String rdch(int index, boolean dontwait) {
        String cont = content.get(index);
        while (cont == null || cont.isEmpty()) {
            if (cont == null) {
                if (dontwait) { return null; }
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                index += 1;
            }
            cont = content.get(index);
        }
        return cont;
    }


}
