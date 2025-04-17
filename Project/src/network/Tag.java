package network;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Tag {
    public ArrayList<AtomicInteger> tags;         // Buffer de tags

    public Tag() {
        this.tags = new ArrayList<>();
    }

    public static AtomicInteger tag() {       // Devuelve un tag aleatorio sin condiciones de carrera
        var n = 0;
        while (n <= 0) {    // Solo pueden ser números enteros positivos
            n = new SecureRandom().nextInt();
        }
        return new AtomicInteger(n);
    }

    public boolean revise(int tag) {            // Verificar si el tag está en uso
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    return true;  // El tag está en uso
                }
            }
            return false;  // El tag no está en uso
        }
    }

    public AtomicInteger newtag() {     // Asegurar que no está en uso tag asignado
        var tag = tag();
        synchronized (tags) {
            while (revise(tag.get())) {
                tag = tag();        // Si está en uso, devolver otro tag
            }
            tags.add(tag);          // Añadir a tags en uso
        }
        return tag;
    }

    public AtomicInteger addtag(int tag) {   // Agregar tag a ya en uso
        var ntag = new AtomicInteger(tag);
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    throw new RuntimeException("Tag ya en uso");
                }
            }
            tags.add(ntag);  // Añadir tag si no está en uso
        }
        return ntag;
    }

    public void rmtag(int tag) {            // Eliminar tag en uso
        synchronized (tags) {
            for (var t : tags) {
                if (t.get() == tag) {
                    tags.remove(t);  // Eliminar tag de en uso
                    return;
                }
            }
            throw new RuntimeException("Tag no en uso");
        }
    }

}
