package protocol;

import java.util.EnumSet;

/**
 * Permisos asignados a los dibujos --> Distintos para cada cliente
 */
public enum Perms {
    RD(0x1),        // 0001 - Lectura
    WR(0x2),        // 0010 - Escritura
    EX(0x4),        // 0100 - Ejecución
    OW(0x8);        // 1000 - Dueño

    public final int value;

    Perms(int value) {           // RD | WR | EX | OW = 1111
        this.value = value;
    }

    /**
     * Combinar permisos -> bytes <br>
     * Sino no se le pasan permisos -> devuelve 0
     * @param per -> Array de permisos a combinar
     * @return int -> Que representa los permisos requeridos en binario
     */
    public static int combine(Perms... per) {
        int result = 0;
        for (Perms p : per) {
            result |= p.value;
        }
        return result;
    }

    /**
     * Para devolver que permisos hay en un entero
     * @param permisos -> int que representa los permisos en binario
     * @return -> [RD, WR, EX, OW] -> Los que se le pasen
     */
    public static EnumSet<Perms> fromInt(int permisos) {
        var result = EnumSet.noneOf(Perms.class);
        for (Perms p : Perms.values()) {
            // Multiplicación binaria para verificar:
            if ((permisos & p.value) != 0) {
                result.add(p);
            }
        }
        return result;
    }
}
