package network;

public enum Cabs {

    LOGIN((byte) 0x00),             // Login de usuario -> Connet
    LOGOUT((byte) 0x01),            // Logout del usuario -> Disconnet
    NEWD((byte) 0x02),             // Crear nuevo dibujo
    RMD((byte) 0x03),              // Borrar dibujo
    NEWF((byte) 0X04),             // Crear nueva figura en dibujo
    RMF((byte) 0x05),              // Borrar figura en dibujo
    MVD((byte) 0X06),               // Mover figuras en dibujo
    DUMP((byte) 0x07),              // Volcar descripción/figuras de un dibujo
    FIN((byte) 0x08),               // Pedir fin de conexión

    ACK((byte) 0x09),              // Confirmación de un mensaje
    INIACK((byte) 0x0A),           // Confirmación de un login
    FINACK((byte) 0x0B),           // Confirmación de un logout -> Último mensaje para desconexión
    ERR((byte) 0x0C),              // Mensaje de error
    MVF((byte) 0x0D);              // Mover dibujo completo (todas las figuras de dentro)

                                    // Resto de cabeceras, en función de lo que se mande al servidor
    public final byte value;

    Cabs(byte value) {
        this.value = value;
    }

    public static Cabs getcab(byte value) {     // Devolver enum correspondiente
        for (Cabs c : Cabs.values()) {
            if (c.value == value) {
                return c;
            }
        }
        return null;
    }

    public static String wcab(Cabs cab, String sep) {       // Escribir instrucción cabecera del mensaje
        return cab.name() + sep;
    }

    public static String mk_cmsg(Cabs cab, String content) { // Forma string de mensaje con cabecera al inicio
        return wcab(cab, ":") + content;
    }

}
