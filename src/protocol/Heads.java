package protocol;

public enum Heads {

    /**
     * Cabeceras esenciales:
     */
    LOGIN((byte) 0x00),            // Login de usuario -> Connet
    LOGOUT((byte) 0x01),           // Logout del usuario -> Disconnet
    INIACK((byte) 0x08),           // Confirmación de un login
    FINACK((byte) 0x09),           // Confirmación de un logout -> Último mensaje para desconexión
    ERR((byte) 0x0A),              // Mensaje de error

    /**
     * Cabeceras para dibujos:
     */
    NEWD((byte) 0x02),             // Crear nuevo dibujo
    RMD((byte) 0x03),              // Borrar dibujo
    NEWF((byte) 0X04),             // Crear nueva figura en dibujo
    RMF((byte) 0x05),              // Borrar figura en dibujo
    MVD((byte) 0X06),              // Mover figuras en dibujo
    DUMP((byte) 0x07),             // Volcar descripción/figuras de un dibujo
    MVF((byte) 0x0B),              // Mover dibujo completo (todas las figuras de dentro)

    /**
     * Cabeceras para sistema MOM:
     */
    OPEN((byte) 0x0C),             // Abrir canal
    CLOSE((byte) 0x0D),            // Cerrar canal
    MKCH((byte) 0x0E),             // Crear canal
    RMCH((byte) 0x0F),             // Borrar canal
    WRCH((byte) 0x10),             // Escribir en canal
    RDCH((byte) 0x11),             // Leer del canal
    RMCONT((byte) 0x12);           // Borrar contenido del canal

    public final byte value;

    Heads(byte value) {
        this.value = value;
    }

    /**
     * Devolver enum correspondiente
     * @param value -> byte
     * @return -> enum --> Cabecera
     */
    public static Heads getcab(byte value) {
        for (Heads c : Heads.values()) {
            if (c.value == value) {
                return c;
            }
        }
        return null;
    }

    /**
     * Construir cabecera en String con enum correspondiente
     * @param cab -> enum -> Instrucción
     * @param sep -> separador
     * @return String -> ej -> LOGIN:
     */
    public static String wcab(Heads cab, String sep) {
        return cab.name() + sep;
    }

    /**
     * Construir String del mensaje<br>
     * Concatenar cabecera y contenido del String
     * @param cab -> enum
     * @param content -> String -> contenido a concatenar
     * @return String -> msg String del MsgStr
     */
    public static String packstr(Heads cab, String content) {
        return wcab(cab, ":") + content;
    }

}
