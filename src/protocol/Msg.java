package protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase principal de mensajes -> Métodos fundamentales
 */
public class Msg {
    protected ByteBuffer buf;      // Buffer -> CONVENIO: Después de utilizarse -> pos=0 -> LISTO PARA LEER

    protected int len;             // Longitud/número de bytes a leer -> (No contar cabeceras -> 10bytes+)
    protected byte pos;            // Indicar si es el primero o algún fragmento siguiente
    protected int more;            // Indicar si hay más que leer o no
    public AtomicInteger tag;      // Tag del mensaje
    public byte inst;              // Instrucción referida

    public static final int BUFSZ = 4096;      // Tamaño del buffer --> min: 14bytes(cabs=FINOFF)

    public static final int BSZOFF = 0;        // 4 bytes -> Número de bytes del mensaje -> SIN CONTAR RESTO CABECERAS
    public static final int POSOFF = BSZOFF+4; // 1 bytes -> Posición del Msg fragmentado, si solo 1 parte -> pos = 1
    public static final int ALLOFF = POSOFF+1; // 4 bytes -> Indica si hay que seguir leyendo o no ->!0 = more, 0 = last
    public static final int TAGOFF = ALLOFF+4; // 4 byte -> Tag del mensaje
    public static final int HDRSZ = TAGOFF+4;  // 1 bytes -> Instrucción del mensaje -> a qué acción pertenece
    public static final int FINOFF = HDRSZ+1;  // Fin de cabeceras -> bytes totales de la cabecera

    /**
     * Inicializar Msg con buffer y tag
     * @param len -> Número de bytes del buffer
     * @param tag -> Tag del mensaje -> int=>AtomicInteger
     */
    public Msg(int len, int tag) {
        this.buf = mkbuf(len);
        this.tag = new AtomicInteger(tag);
    }

    /**
     * Crear ByteBuffer de longitud len
     * @param len -> Longitud en bytes del buffer
     * @return ByteBuffer de longitud len
     */
    public static ByteBuffer mkbuf(int len) {
        var buf = ByteBuffer.allocate(len);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }

    public void putlen(byte[] b) {
        len = b.length;
    }

    public void putpos(byte pos) {
        this.pos = pos;
    }

    public void putinst(String inst) {
        this.inst = Heads.valueOf(inst).value;
    }

    /**
     * Limpiar el buffer -> clear
     */
    public void clean() {
        buf.clear();
    }

    /**
     * Dividir bytes->varios byte[] para varios buffers<br>
     * Dividir en varios buffers mientras queden bytes por dividir y copiarlos<br>
     * @param b -> Array de bytes a dividir
     * @param bmax -> Número de bytes por buffer
     * @return ArrayList de arrays de bytes con los bytes divididos
     */
    public static ArrayList<byte[]> splitbytes(byte[] b, int bmax) {
        var ss = new ArrayList<byte[]>();
        // Bytes copiados:
        var bc = 0;
        // Bytes restantes (total-copiados):
        var bleft = b.length-bc;
        while ((bleft) > bmax ) {
            // Copiar bytes que caben en un buffer:
            ss.add(Arrays.copyOfRange(b, bc, bc+bmax));
            // Actualizar bytes copiados:
            bc += bmax;
            // Actualizar bytes restantes:
            bleft = b.length-bc;
        }
        // Copiar bytes restantes en un 1 buffer solo
        ss.add(Arrays.copyOfRange(b, bc, b.length));
        return ss;
    }

    /**
     * Parsear string para sacar instrucción -> Cabecera 4<br>
     * ej -> LOG:noel:1:127.0.0.1:63048 => [LOG, noel:1:127.0.0.1:63048]<br>
     * @param s -> String -> info a enviar
     * @return ArrayList -> [Instrucción, Argumentos->String de info]
     */
    public static List<String> divs(String s) {
        var ns = s.split(":", 2);
        return Arrays.asList(ns[0], ns[1]);
    }

    /**
     * Codificar string -> si NO cabe, devuelve null<br>
     * UTF_8<br>
     * @param s -> String a codificar
     * @return Array de Bytes codificados
     */
    public static byte[] code(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Decodificar bytes<br>
     * UTF_8<br>
     * @param b -> Array de bytes a decodificar
     * @return String decodificado
     */
    public static String decode(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    /**
     * Lectura directa del ReadableByteChannel<br>
     * --> Thread safe<br>
     * Lee n bytes y los guarda en bufrd (buffer auxiliar) -> hasta llenar su tamaño<br>
     * Dejar buffer -> bufrd listo para leer -> pos = 0<br>
     * Escritura para todas clases NIO: SocketChannel, FileChannel, Pipe.SinkChannel(rd only).<br>
     * @param sk -> ReadableByteChannel
     * @param n -> Número de bytes a leer
     * @param bufrd -> Buffer auxiliar donde guardar los bytes leídos
     * @throws IOException -> Problemas con la comunicación
     */
    protected void readFrom(ReadableByteChannel sk,int n, ByteBuffer bufrd) throws IOException {
        var nr = 0;
        var tot = 0;
        while (tot < n) {
            synchronized (sk) {
                nr = sk.read(bufrd);
            }
            // Socket cerrado -> eof found:
            if (nr < 0) {
                throw new RuntimeException("Unexpected closed");
            }
            tot += nr;
        }
        bufrd.flip();
    }

    /**
     * Preparación para realizar lectura de n bytes en el ReadableByteChannel<br>
     * Crear buffer auxiliar de tamaño n --> Para leer justo n bytes -> bufrd<br>
     * Pasar n bytes leídos al buffer propio (this.buf)<br>
     * @param sk -> ReadableByteChannel
     * @param n -> Número de bytes a leer
     * @throws IOException -> Problemas con la comunicación
     */
    protected void readFrom(ReadableByteChannel sk, int n) throws IOException {
        var b = new byte[n];
        var bufrd = mkbuf(b.length);
        readFrom(sk, n, bufrd);
        unpack(bufrd, b, n);
    }

    /**
     * Escritura directa del WritableByteChannel<br>
     * Escribe todo el buffer propio (this.buf)<br>
     * Dejar el buffer en pos = 0 -> listo para leer<br>
     * --> Thread safe<br>
     * Escritura para todas clases NIO: SocketChannel, FileChannel, Pipe.SourceChannel(wr only).<br>
     * @param sk -> WritableByteChannel
     * @throws IOException -> Problemas con la comunicación
     */
    protected void writeTo(WritableByteChannel sk) throws IOException {
        var tw = 0;
        var nw = 0;
        // Asegurarnos de que mande todo el mensaje:
        while (tw < buf.limit()) {
            synchronized (sk) {
                nw = sk.write(buf);
            }
            if (nw < 0) {
                throw new RuntimeException("Short write -> Unable to write full buffer");
            }
            tw += nw;
        }
        buf.position(0);
    }

    // PACKS y UNPACKS:

    /**
     * Pasar bytes del buffer auxiliar bufrd a this.buf<br>
     * Deja el buffer en la pos inicial (pos=0) para leer<br>
     * @param bufrd -> Buffer auxiliar
     * @param b -> Array de bytes con el que hacer el traspaso
     * @param n -> Número de bytes a pasar
     */
    public void unpack(ByteBuffer bufrd, byte[] b, int n) {
        var pos = buf.position();
        // Guardar bytes leídos del buf aux al array b:
        bufrd.get(b);
        bufrd.clear();
        // Limite al final de lo último escrito en el buffer -> pos + n bytes a añadir
        buf.limit(pos + n);
        // Traspasar bytes leídos del buf aux al buffer propio:
        buf.put(b);
        buf.position(pos);
    }

    /**
     * Desempaquetar 14 bytes de cabecera en this.buf -> están en orden c1, c2, c3, c4<br>
     * Analizrlas y asignar valores de cabeceras<br>
     * .get() -> Lee solo 1 byte desde la posición donde está el buffer<br>
     * .getInt() -> Lee 4 bytes desde la posición donde está el buffer<br>
     * Dejar buffer en listo para leer<br>
     */
    public void unpackcabs() {
        len = buf.getInt();
        pos = buf.get();
        more = buf.getInt();
        tag = new AtomicInteger(buf.getInt());
        inst = buf.get();
        buf.flip();
    }

    /**
     * Meter cabecera primero en this.buf en orden c1, c2, c3, c4<br>
     * .put(byte) -> Escribe solo 1 byte desde la posición donde está el buffer<br>
     * .putInt(int) -> Escribe 4 bytes desde la posición donde está el buffer<br>
     */
    public void packcabs() {
        buf.putInt(len);
        buf.put(pos);
        buf.putInt(more);
        buf.putInt(tag.get());
        buf.put(inst);
    }

    /**
     * Introducir todos los datos del mensaje con cabeceras en this.buf<br>
     * Metemos todos los bytes restantes después de las cabeceras<br>
     * Dejar buffer listo para leer -> pos = 0 (con .flip())<br>
     * @param b -> Array de bytes de info, aparte de las cabeceras
     */
    public void pack(byte[] b) {
        packcabs();
        buf.put(b);
        buf.flip();
    }

}
