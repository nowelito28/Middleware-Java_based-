package protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;

import static protocol.Tag.tag;
import static org.junit.jupiter.api.Assertions.*;

public class MsgTest {
    private Pipe pipe;
    private Pipe.SinkChannel sink;
    private Pipe.SourceChannel source;
    private BufMsg bufmsg;

    @BeforeEach
    void setUp() throws IOException {
        pipe = Pipe.open();
        sink = pipe.sink();         // SinkChannel -> Extremo de escritura
        source = pipe.source();     // SourceChannel -> Extremo de lectura
        bufmsg = new BufMsg();
    }

    @Test
    void testMsgEncodingDecoding() {
        String originalMessage = "TEST_MESSAGE";
        byte[] encoded = Msg.code(originalMessage);
        String decoded = Msg.decode(encoded);
        assertEquals(originalMessage, decoded);
    }

    @Test
    void testTagGeneration() {
        var tag1 = tag();
        var tag2 = tag();
        assertNotEquals(tag1, tag2); // Deben ser valores aleatorios diferentes
    }

    @Test
    void testMsgStrWriteRead() throws IOException {
        MsgStr writerMsg1 = new MsgStr(tag().get());
        //Msg.MsgStr writerMsg2 = Msg.MsgStr.doWrite(tag());

        writerMsg1.writeTo(sink, "LOGIN:user:1:127.0.0.1:5000");
        writerMsg1.writeTo(sink, "LOGOUT:user:1");
        writerMsg1.writeTo(sink, "NEWD:user:newdibujo");
        //writerMsg2.writeTo(sink, "LOGOUT:user:1:127.0.0.1:5000");
        String sentMsg1 = "user:1:127.0.0.1:5000";          // Len = 21
        String sentMsg2 = "user:1";                         // Len = 6
        String sentMsg3 = "user:newdibujo";                 // Len = 14

        MsgStr readerMsg1 = new MsgStr(tag().get());
        String receivedMessage1 = readerMsg1.readFrom(source);

        System.err.println("Len: " + readerMsg1.len + " Tag: " + readerMsg1.tag +
                " Pos: " + readerMsg1.pos + " More: " + readerMsg1.more + " Inst: " + readerMsg1.inst);
        assertEquals(sentMsg1, readerMsg1.msg);
        readerMsg1.clean();                         // Limpia el buffer y msg = ""
        //Msg.MsgStr readerMsg2 = Msg.MsgStr.doRead();
        String receivedMessage2 = readerMsg1.readFrom(source);
        System.err.println("Len2: " + readerMsg1.len);
        assertEquals(sentMsg2, readerMsg1.msg);
        readerMsg1.clean();
        String receivedMessage3 = readerMsg1.readFrom(source);
        System.err.println("Len3: " + readerMsg1.len);
        assertEquals(sentMsg3, readerMsg1.msg);
        readerMsg1.clean();

        assertEquals(sentMsg1, receivedMessage1);
        assertEquals(writerMsg1.tag, readerMsg1.tag);
        assertEquals(writerMsg1.inst, readerMsg1.inst);
        assertEquals(writerMsg1.len, readerMsg1.len);
        assertEquals(sentMsg2, receivedMessage2);
        assertEquals(writerMsg1.tag, readerMsg1.tag);
        assertEquals(writerMsg1.inst, readerMsg1.inst);
        assertEquals(writerMsg1.len, readerMsg1.len);
        assertEquals(sentMsg3, receivedMessage3);
        assertEquals(writerMsg1.tag, readerMsg1.tag);
        assertEquals(writerMsg1.inst, readerMsg1.inst);
        assertEquals(writerMsg1.len, readerMsg1.len);
    }

    @Test
    void testSplitBytes() {
        byte[] data = "This is a test message for split function".getBytes(StandardCharsets.UTF_8);
        int chunkSize = 10;
        var splitData = Msg.splitbytes(data, chunkSize);

        assertFalse(splitData.isEmpty());
        assertEquals(splitData.get(0).length, chunkSize);
        assertTrue(splitData.get(splitData.size() - 1).length <= chunkSize);
    }
}

