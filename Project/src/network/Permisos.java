package network;

import java.util.EnumSet;

public enum Permisos {
    RD(0x1), // 0001 - Lectura
    WR(0x2), // 0010 - Escritura
    EX(0x4), // 0100 - Ejecución
    OW(0x8);// 1000 - Dueño

    public final int value;

    Permisos(int value) {           // RD | WR | EX | OW = 1111
        this.value = value;
    }

    public static int combine(Permisos... per) {    // Combinar permisos
        int result = 0;
        for (Permisos p : per) {
            result |= p.value;
        }
        return result;
    }

    public static EnumSet<Permisos> fromInt(int permisos) {     // Para devolver que permisos hay en un entero
        var result = EnumSet.noneOf(Permisos.class);
        for (Permisos p : Permisos.values()) {
            if ((permisos & p.value) != 0) {    // Multiplicación binaria
                result.add(p);
            }
        }
        return result;
    }
}
