package protocol;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mensajes de tipo String y gestión de los mismos
 * Solo transporta un string -> msg
 */
public class MsgStr extends Msg {
    public String msg;                                  // Contenido del string del mensaje

    /**
     * ¡Si es para lectura el tag -> se puede modificar por el recibido!
     * @param tag -> tag del mensjase MsgStr -> int=>AtomicInteger
     */
    public MsgStr(int tag) {
        super(BUFSZ, tag);
        msg = "";
    }

    /**
     * Limpiar mensaje y su contenido
     */
    public void clean() {
        super.clean();
        msg = "";
    }

    /**
     * Añadir contenido restante al mensaje
     * Si tiene más partes porque no cabía en el buffer
     * @param msg -> Msg
     * @param bufmsg -> BufMsg -> Buffer de mensajes pendientes
     */
    public void addcont(MsgStr msg, BufMsg bufmsg) {     //
        if (tag.get() == msg.tag.get() && more - 1 == msg.more) {
            // 1 mensaje pendiente menos -> último msg pone more en 0
            more--;
            // Añadir contenido del mensaje
            this.msg += msg.msg;
        } else {
            // Si no pertenece a este mensaje --> lo ponemos en el buffer
            bufmsg.addmsg(msg);
        }

    }

    // UTILS para strings:

    /**
     *  Número de coincidencias de regex en str
     * @param str -> String
     * @param regex -> Expresión regular (String)
     * @return int -> Número de apariciones de regex en str
     */
    public static int matches(String str, String regex) {
        // Compilar la expresión regular regex:
        Pattern pattern = Pattern.compile(regex);
        // Crear un matcher para la cadena str:
        Matcher matcher = pattern.matcher(str);
        int count = 0;
        // Buscar las coincidencias con el matcher y contarlas:
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Aplanar strs en un único str<br>
     * //part//part//part...
     * @param sep -> Separador (String)
     * @param parts -> Array de strings
     * @return String aplanado
     */
    public static String packstr_parts(String sep, String... parts) {
        var info = new StringBuilder();
        for (String part : parts) {
            info.append(sep).append(part);
        }
        return info.toString();
    }

    /**
     * Construir array de string según un str aplanado<br>
     * part | part | part ...
     * @param len -> Número de partes (int)
     * @param sep -> Separador (String)
     * @param mainp -> Str aplanado
     * @return String[] -> con las partes indexadas
     */
    public static String[] unpackstr_parts(int len, String sep, String mainp) {
        var parts = new String[len];
        var i = 0;
        for (var part : mainp.split(sep)) {
            parts[i] = part;
            i += 1;
        }
        return parts;
    }

    /**
     * Ints en String separados por sep<br>
     * int1/int2/int3... -> int1 | int2 | int3 ...<br>
     * Si sep no está en str => array de 1 elemento
     * @param sep -> Separador
     * @param str -> Array de inst aplanados en un string
     * @return int[]
     */
    public static int[] str_arrint(String sep, String str) {
        var sstr = str.split(sep);
        var ints = new int[sstr.length];
        var i = 0;
        for (var s : sstr) {
            ints[i] = Integer.parseInt(s);
            i += 1;
        }
        return ints;
    }

    /**
     * Array de Ints a String aplanado separados por sep<br>
     * int1/int2/int3... <- int1 | int2 | int3 ...
     * @param sep -> Separador
     * @param ints -> Array de ints a aplanar
     * @return String aplanado
     */
    public static String arrint_str(String sep, int... ints) {
        var s = new StringBuilder();
        for (var i : ints) {
            s.append(sep).append(i);
        }
        return s.toString();
    }

    /**
     * Separar dos componentes principales<br>
     * ¡Sobre msg del MsgStr!<br>
     * ej-> autor:id/fig1/fig2/fig3... -> autor:id | fig1/fig2/fig3...<br>
     * sep = /
     * @param sep -> Separador
     * @return String[] -> con 2 strings
     */
    public String[] unpack_2strs(String sep) {
        return msg.split(sep, 2);
    }

    /**
     * Separar str en 3 componentes (strings) con 2 separadores<br>
     * Separar dos componentes principales -> 1º iteración -> sep1<br>
     * Separar 2º componente -> 2º iteración -> sep2<br>
     * ¡Sobre msg del MsgStr!<br>
     * ej-> autor:id//x,y/idfig1/idfig2... -> autor:id | x,y | idfig1/idfig2...<br>
     * sep1 = // <-> sep2 = /
     * @param sep1 -> Primer separador
     * @param sep2 -> Segundo separador
     * @return String[] -> con 3 strings
     */
    public String[] unpack_3strs(String sep1, String sep2) {
        var s1 = unpack_2strs(sep1);
        var s2 = s1[1].split(sep2, 2);
        var ss = new String[3];
        ss[0] = s1[0];
        ss[1] = s2[0];
        ss[2] = s2[1];
        return ss;
    }

    /**
     * Desempaquetar un str con ids separados por sep<br>
     * id1/id2/id3... -> id1 | id2 | id3 ...<br>
     * ¡Sobre msg del MsgStr!
     * @param sep -> Separador
     * @return int[] -> Array de ids
     */
    public int[] unpackstr_ids(String sep) {
        return str_arrint(sep, msg);
    }

    /**
     * Array concreto de len strings separados por sep<br>
     * ¡Sobre msg del MsgStr!
     * @param len -> Número de strings
     * @param sep -> Separador
     * @return String[] -> Array de strings con len elementos
     */
    public String[] unpackstr_parts(int len, String sep) {
        return unpackstr_parts(len, sep, msg);
    }

    /**
     * Dividir todo el string por sep -> rexp<br>
     * Desaplanar todo el string<br>
     * ¡Sobre msg del MsgStr!
     * @param sep -> Separador
     * @return String[] -> Array de strings
     */
    public String[] unpackstr_tot(String sep) {
        return  msg.split(sep);
    }

    // MÉTODOS DE LECTURA:

    /**
     * Leer y reconocer cabeceras<br>
     * Guardar en this.buf FINOFF bytes de las cabeceras<br>
     * Dejamos buffer limpio para leer el resto
     * @param sk -> ReadableByteChannel de donde leer
     * @throws IOException -> Problemas con la comunicación
     */
    public void readcabs(ReadableByteChannel sk) throws IOException {
        readFrom(sk, FINOFF);
        unpackcabs();
        buf.clear();
    }

    /**
     * Guardamos el string del buffer<br>
     * ¡Leer len bytes (primera cabecera) para el str!<br>
     * Dejamos buffer limpio para leer el resto
     * @param sk -> ReadableByteChannel
     * @throws IOException -> Problemas con la comunicación
     */
    public void readstr(ReadableByteChannel sk) throws IOException {
        // Leer bytes de string:
        readFrom(sk, len);
        // Bytes a leer el buffer:
        var b = new byte[len];
        // Coger bytes para string
        buf.get(b);
        // Decodificar string
        msg += Msg.decode(b);
        buf.clear();
    }

    /**
     * Leer msg (String) del buffer<br>
     * Si queremos volver a leer -> buf.clean()
     * @param sk -> ReadableByteChannel
     * @return  -> String -> mensaje leído y decodificado
     * @throws IOException -> Problemas con la comunicación
     * @throws RuntimeException -> Fallos detectados
     */
    public String readFrom(ReadableByteChannel sk) throws IOException, RuntimeException {
        readcabs(sk);
        readstr(sk);
        return msg;
    }

    /**
     * Recibir msg del cliente concreto --> MsgStr
     * @param sk -> ReadableByteChannel
     * @param msgsleft -> BufMsg -> Buffer de mensajes pendientes
     * @param tag -> AtomInteger.get() -> int
     * @param pos -> Orden de la parte del mensaje (int)
     * @return MsgStr solicitado por su tag y pos
     * @throws IOException -> Problemas con la comunicación
     * @throws RuntimeException -> Fallos detectados
     */
    public static MsgStr check(ReadableByteChannel sk, BufMsg msgsleft,
                               int tag, int pos) throws IOException, RuntimeException {
        // Ver si está en pendientes la respuesta que buscamos:
        MsgStr msgrv = (MsgStr) msgsleft.checkmsg(tag, pos);
        if (msgrv == null) {
            // Mensaje nuevo --> al ser lectura se le cambiará el tag(1) al recibir el nuevo
            msgrv = new MsgStr(1);
            // Leer del socket hasta encontrar mensaje que buscamos:
            while (true) {
                msgrv.readFrom(sk);
                // Si buscamos un tag concreto
                // -> Leer del canal hasta encontrar siguiente mensaje que queremos:
                if (tag > 0 && msgrv.tag.get() == tag && msgrv.pos == pos) {
                    break;
                // Si no buscamos un tag concreto:
                } else if (tag <= 0 && msgrv.pos == pos) {
                    break;
                }
                // Añadir mensaje a mensajes pendientes y limpiar con el que iteramos:
                msgsleft.addmsg(msgrv);
                msgrv.clean();
            }
        }
        return msgrv;
    }

    /**
     * Recibir msg del cliente --> MsgStr
     * @param sk -> ReadableByteChannel
     * @param msgsleft -> BufMsg -> Buffer de mensajes pendientes
     * @param tag -> AtomInteger.get() -> int
     * @return MsgStr recibido o del buffer de pendientes
     * @throws IOException -> Problemas con la comunicación
     */
    public static MsgStr rv(ReadableByteChannel sk, BufMsg msgsleft, int tag) throws IOException {
        // Para leer mensaje desde el principio:
        var pos = 1;
        var msgrv = check(sk, msgsleft, tag, pos);
        while (msgrv.more != 0) {
            // Pasa a ser la siguiente parte del mensaje:
            pos += 1;
            var msgrv2 = check(sk, msgsleft, msgrv.tag.get(), pos);
            // Añadir contenido del mensaje que falta:
            msgrv.addcont(msgrv2, msgsleft);
        }
        return msgrv;
    }

    // MÉTODOS DE ESCRITURA:

    /**
     * Enviar todas las partes del mensaje
     * @param sk -> WritableByteChannel
     * @param bw -> byte[] -> Bytes a enviar
     * @throws IOException -> Problemas con la comunicación
     */
    public void writeTo(WritableByteChannel sk, byte[] bw) throws IOException {
        // Dividir bytes en varios buffers si son muy grandes
        var barr = splitbytes(bw, BUFSZ - FINOFF);
        // 0 -> NO more ; !0 -> More (indica cuantos quedan)
        more = barr.size();
        // Contador de partes del mensaje:
        byte i = 0;
        for (byte[] b : barr) {
            // Aumentar número de partes por la que va (primero, segundo, tercero...):
            putpos((byte) (i + 1));
            // Decrementar mensajes pendientes por enviar:
            more -= 1;
            // Limpiar buffer para meter siguiente trama de bytes:
            buf.clear();
            // Poner longitud del string en bytes:
            putlen(b);
            // Meter mensaje con cabecera codificado en el buffer:
            pack(b);
            writeTo(sk);
        }
    }

    /**
     * Mandar mensaje de string
     * @param sk -> WritableByteChannel
     * @param s -> String a enviar
     * @throws IOException -> Problemas con la comunicación
     */
    public void writeTo(WritableByteChannel sk, String s) throws IOException {
        // Parsear copia del string -> dividir info
        var ss = divs(new String(s));
        // Instrucción -> Cabecera:
        putinst(ss.get(0));
        // String -> Argumentos
        msg = ss.get(1);
        // Codificar full string:
        var bw = Msg.code(msg);
        writeTo(sk, bw);
    }
}
