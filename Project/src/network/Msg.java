package network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;                      // jstack -l para debug -> ver el stack de threads (volcado de pila)
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static network.TcpDefs.mlog;         // Logger de depuración para mensajes -> mlog

public class Msg {
    protected ByteBuffer buf;           // Buffer -> CONVENIO: Después de utilizarse -> pos=0 -> LISTO PARA LEER

    protected int len;         // Longitud de bytes a leer a partir de las cabeceras
    protected byte pos;        // Indicar si es el primero o algún fragmento siguiente
    protected int more;        // Indicar si hay más que leer o no
    protected AtomicInteger tag;         // Tag del mensaje
    protected byte inst;       // Instrucción referida

    public static final int BUFSZ = 4096;      // Tamaño del buffer --> min: 14bytes(cabs=FINOFF)

    public static final int BSZOFF = 0;        // 4 bytes ->cabecera número de bytes del mensaje -> SIN CONTAR RESTO CABECERAS
    public static final int POSOFF = BSZOFF+4; // 1 bytes -> Posición del mensaje fragmentado, si se manda el primero o es el siguiente
    public static final int ALLOFF = POSOFF+1; // 4 bytes -> indicar cuantas partes faltan del mensaje -> volver a leer
    public static final int TAGOFF = ALLOFF+4; // 4 byte -> tag del mensaje
    public static final int HDRSZ = TAGOFF+4;  // 1 bytes -> cabecera del mensaje -> indica que hace el mensaje
    public static final int FINOFF = HDRSZ+1;  // Fin de cabeceras -> bytes totales de la cabecera

    public Msg(int len, int tag) {
        this.buf = mkbuf(len);
        this.tag = new AtomicInteger(tag);
    }

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
        this.inst = Cabs.valueOf(inst).value;
    }

    public void clean() {
        buf.clear();
    }

    public static String[] intarrstr(int... ints) {    // Convertir array de enteros en array de strs
        String[] strs = new String[ints.length];
        for (int i = 0; i < ints.length; i++) {
            strs[i] = String.valueOf(ints[i]);  // Convertir cada id a String
        }
        return strs;
    }

    public static ArrayList<byte[]> splitbytes(byte[] b, int bmax) {// Dividir bytes->varios byte[] para varios buffers
        var ss = new ArrayList<byte[]>();
        var bc = 0;                     // Bytes copiados
        var bleft = b.length-bc;        // Bytes restantes (total-copiados)
        while ((bleft) > bmax ) {
            ss.add(Arrays.copyOfRange(b, bc, bc+bmax));      // Copiar bytes que caben en un buffer
            bc += bmax;                 // Actualizar bytes copiados
            bleft = b.length-bc;        // Actualizar bytes restantes
        }
        ss.add(Arrays.copyOfRange(b, bc, b.length));    // Copiar bytes suficientes en un 1 buffer solo
        return ss;
    }

    public static List<String> divs(String s) {  // Parsear string para sacar instrucción -> Cabecera 4
        var ns = s.split(":", 2);
        return Arrays.asList(ns[0], ns[1]);    // ej -> LOG:noel:1:127.0.0.1:63048 => [LOG, noel:1:127.0.0.1:63048]
    }

    public static byte[] code(String s) {               // Codificar string -> si NO cabe, devuelve null
        return s.getBytes(StandardCharsets.UTF_8);     // esperar parte que si cabe
    }

    public static String decode(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    // Lectura y escritura para todas clases NIO: SocketChannel, FileChannel,
    // Pipe.SinkChannel(rd only) y Pipe.SourceChannel(wr only)

    protected void readFrom(ReadableByteChannel sk,int n, ByteBuffer bufrd) throws IOException { // Thread safe -> Con buffer auxiliar (bufrd)
        var nr = 0;
        var tot = 0;
        while (tot < n) {       // Obligar a leer solo n bytes
            synchronized (sk) { // Cerrar thread solo al leer
                nr = sk.read(bufrd); // Lee max n bytes del socket (llena el tamaño del bufrd)
            }
            if (nr < 0) {    // Socket cerrado -> eof found
                throw new RuntimeException("Unexpected closed");   // Se tiene que cerrar el socket de manera controlada
            }
            tot += nr;
        }
        bufrd.flip();       // Dejar buffer listo para leer con pos = 0
    }

    protected void readFrom(ReadableByteChannel sk, int n) throws IOException {   // Pasar n bytes leídos al buffer propio (this.buf)
        var b = new byte[n];    // 1 int -> 4 bytes =>  Buffer auxiliar
        var bufrd = mkbuf(b.length);
        readFrom(sk, n, bufrd);
        unpack(bufrd, b, n);   // Sacar bytes del buffer auxiliar -> Meter en el buffer propio
    }

    protected void writeTo(WritableByteChannel sk) throws IOException { // Thread safe -> Con buffer propio (this.buf) -> Escribir todo el buf
        var tw = 0;
        var nw = 0;
        while (tw < buf.limit()) {      // Asegurarnos de que mande todo el mensaje
            synchronized (sk) {         // Cerrar thread solo al escribir
                nw = sk.write(buf);
            }
            if (nw < 0) {
                throw new RuntimeException("Short write -> Unable to write full buffer");
            }
            tw += nw;
        }               // Dejar el buffer en en pos = 0 -> listo para leer
        buf.position(0);
    }




    public void unpack(ByteBuffer bufrd, byte[] b, int n) {    // Deja el buffer en la pos inicial para leer
        var pos = buf.position();      // Guardar posición actual
        bufrd.get(b);                  // Guardar bytes leídos en el buffer del mensaj e
        bufrd.clear();
        buf.limit(pos + n);   // Limite al final de lo último escrito en el buffer
        buf.put(b);                    // Meter en el buffer del mensaje los bytes leídos
        buf.position(pos);             // Mantener posición de antes de leer -> pos inicial
    }

    public void unpackcabs() {
        len = buf.getInt();
        pos = buf.get();       // Lee solo 1 byte desde la posición donde está el buffer
        more = buf.getInt();
        tag = new AtomicInteger(buf.getInt());
        inst = buf.get();
        buf.flip();           // Dejar buffer en listo para leer
    }

    public void packcabs() {    // Meter cabecera primero en el buffer
        buf.putInt(len);        // c1 -> int (4 bytes) -> len bytes rd del string (No contar cabeceras -> 10bytes)
        buf.put(pos);           // c2 -> 1 byte -> pos del mensaje completo a enviar, si solo es 1 parte -> pos = 1
        buf.putInt(more);       // c2 -> int (4 bytes) -> !0 = more, 0 = last -> mensajes que quedan por enviar
        buf.putInt(tag.get());        // c3 -> int (4 bytes) -> Random inmutable -> tag
        buf.put(inst);          // c4 -> 1 byte -> Indica acción a realizar en el servidor(4bytes)
    }

    public void pack(byte[] b) {     // Introducir mensaje con cabeceras en el buffer
        packcabs();            // Array bytes de cabeceras
        buf.put(b);            // Metemos todos los bytes restantes
        buf.flip();            // Dejar buffer listo para leer
    }




    public static class MsgStr extends Msg {
        public String msg;

        public MsgStr(int len, int tag) {      // Constructor
            super(len, tag);
            this.msg = "";  // Inicializar str vacío
        }

        public void clean() {
            super.clean();
            msg = "";
        }

        /*protected static String buildstr(Cabs cab, String base, String sep, String... infos) {
            var tag = tag();                 // Tag del mensaje
            var all = new StringBuilder(mk_cmsg(cab, base));
            for (String info : infos) {         // Construir msg str --> cab: base /info/info/info...
                all.append(sep).append(info);
            }                               // Añadir partes independientes dependiendo del separador
            return all.toString();
        }*/

        public void addcont(MsgStr msg, BufMsg bufmsg) {     // Añadir contenido restante al mensaje
            if (tag.get() == msg.tag.get() && more - 1 == msg.more) {
                more--;               // 1 mensaje pendiente menos -> último msg pone more en 0
                this.msg += msg.msg;    // Añadir contenido del mensaje
            } else {
                bufmsg.addmsg(msg);
            }

        }


        public void readcabs(ReadableByteChannel sk) throws IOException {     // Reconocer cabeceras
            readFrom(sk, FINOFF);   // Guardar en el buffer FINOFF bytes -> Buffer lo deja en pos = 0
            unpackcabs();       // Sacar info de las cabeceras
            buf.clear(); // Dejamos buffer limpio para leer el resto
        }

        public void readstr(ReadableByteChannel sk) throws IOException {       // Guardamos el string del buffer
            readFrom(sk, len);           // Leer bytes de string
            var b = new byte[len];   // Bytes a leer el buffer
            buf.get(b);              // Coger bytes para string
            msg += Msg.decode(b);    // Decodificar string
            buf.clear(); // Dejamos buffer limpio para leer el resto
        }

        public String readFrom(ReadableByteChannel sk) throws IOException, RuntimeException {// Si queremos volver a leer, hacer .clean()
            readcabs(sk);
            readstr(sk);   // Leer len bytes (primera cabecera) para el str
            return msg;
        }

        public static MsgStr doRead() {       // Pasar tag 0 porque se va a cambiar al tag leído
            return new MsgStr(BUFSZ, 1); // Objeto mensaje -> crea un buffer propio (tag a 1 para cambiarlo por el leído)
        }


        public static Msg.MsgStr check(ReadableByteChannel sk, BufMsg msgsleft,
                                    AtomicInteger tag, int pos) throws IOException, RuntimeException { // Recibir msg del cliente
            Msg.MsgStr msgrv = msgsleft.checkmsg(tag, pos);   // Ver si está en pendientes la respuesta que buscamos
            if (msgrv == null) {
                msgrv = Msg.MsgStr.doRead();        // Leer del socket hasta encontrar mensaje que buscamos
                while (true) {
                    msgrv.readFrom(sk);
                    if (tag.get() > 0 && msgrv.tag.get() == tag.get() && msgrv.pos == pos) {  // Si buscamos un tag concreto
                        break;                            // Leer del canal hasta encontrar siguiente mensaje que queremos
                    } else if (tag.get() <= 0 && msgrv.pos == pos) {             // Si no buscamos un tag concreto
                        break;
                    }
                    msgsleft.addmsg(msgrv);
                    msgrv.clean();  // Limpiar mensaje
                }
            }
            return msgrv;
        }

        public static Msg.MsgStr rv(ReadableByteChannel sk, BufMsg msgsleft,
                                       AtomicInteger tag) throws IOException {      // Recibir msg
            var pos = 1;    // Para leer mensaje desde el principio
            var msgrv = check(sk, msgsleft, tag, pos);
            while (msgrv.more != 0) {
                pos += 1;   // Pasa a ser la siguiente parte del mensaje
                var msgrv2 = check(sk, msgsleft, msgrv.tag, pos);
                msgrv.addcont(msgrv2, msgsleft);        // Añadir contenido
            }
            return msgrv;
        }



        public void wring(WritableByteChannel sk, byte[] bw) throws IOException {  // Envía todas las partes del mensaje
            var barr = splitbytes(bw, BUFSZ - FINOFF); // Dividir bytes en varios buffers si son muy grandes
            more = barr.size();            // 0 -> NO more ; !0 -> More (indica cuantos quedan)
            byte i = 0;                     // Contador de partes del mensaje
            for (byte[] b : barr) {
                putpos((byte) (i + 1));    // Aumentar número de partes por la que va (primero, segundo, tercero...)
                more -= 1;                 // Decrementar mensajes pendientes por enviar
                buf.clear();               // Limpiar buffer para meter siguiente trama de bytes
                putlen(b);                 // Longitud del string en bytes
                pack(b);      // Meter mensaje con cabecera codificado en el buffer
                writeTo(sk);
            }
        }

        public void writeTo(WritableByteChannel sk, String s) throws IOException {      // Buffer y msg completo
            var ss = divs(new String(s));      // Parsear copia del string -> diviidir info
            putinst(ss.get(0));         // Instrucción -> Cabecera
            msg = ss.get(1);            // String -> Argumentos
            var bw = Msg.code(msg);       // Codificar full string
            wring(sk, bw);
        }

        public static MsgStr doWrite(int tag) {       // Pasar tag elegido para escribir el mensaje
            return new MsgStr(BUFSZ, tag);  // Objeto mensaje -> crea un buffer propio (y el tag que le pasemos)
        }

    }

}
