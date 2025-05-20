package protocol;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase encargada de tags --> AtomicInteger<br>
 * Todos los métodos NO static --> Thread-safe
 */
public class Tag {
    public ArrayList<AtomicInteger> tags;         // Buffer de tags

    public Tag() {
        this.tags = new ArrayList<>();
    }

    /**
     * Devuelve un tag aleatorio sin condiciones de carrera<br>
     * SOLO POSITIVOS -> tag > 0
     * @return tag -> AtomicInteger
     */
    public static AtomicInteger tag() {
        var n = 0;
        while (n <= 0) {
            n = new SecureRandom().nextInt();
        }
        return new AtomicInteger(n);
    }

    /**
     * Verificar si el tag está en uso en el buffer de tags
     * @param tag --> AtomicInteger.get() = int
     * @return true -> tag en uso | false -> tag no en uso
     */
    public boolean revise(int tag) {
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Devuelve nuevo tag que no esté en uso en el buffer de tags
     * @return tag -> AtomicInteger
     */
    public AtomicInteger newtag() {
        var tag = tag();
        synchronized (tags) {
            while (revise(tag.get())) {
                // Si está en uso, devolver otro tag
                tag = tag();
            }
            // Añadir a tags en uso:
            tags.add(tag);
        }
        return tag;
    }

    /**
     * Agregar tag a ya en uso al buffer de tags si no estaba
     * @param tag --> AtomicInteger.get() = int
     * @return tag -> AtomicInteger --> el mismo
     * @throws RuntimeException -> tag ya estaba en uso
     */
    public AtomicInteger addtag(int tag) {
        var ntag = new AtomicInteger(tag);
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    throw new RuntimeException("Tag ya en uso");
                }
            }
            tags.add(ntag);
        }
        return ntag;
    }

    /**
     * Eliminar tag en uso
     * @param tag --> AtomicInteger.get() = int
     * @throws RuntimeException -> tag no estaba en uso
     */
    public void rmtag(int tag) {
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    tags.remove(t);
                    return;
                }
            }
            throw new RuntimeException("Tag no en uso");
        }
    }

}
