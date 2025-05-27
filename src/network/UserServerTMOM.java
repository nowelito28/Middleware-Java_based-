package network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import services.SvcMom;

import static java.lang.Thread.sleep;

public class UserServerTMOM {
    private User user1;
    private User user2;
    private Server server;

    @Before
    public void setup() {
        // Lanzar el servidor en otro hilo:
        server = new Server("TestServer", 8080, new SvcMom());
        new Thread(server::start).start();
        // Conectar users:
        user1 = new User("noel", 8080, "0.0.0.0");
        user2 = new User("lucia", 8080, "0.0.0.0");
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * Probar acciones 1 a 1 <br>
     * Si sucede algún error o se recibe (cabecera ERR)
     * --> se eleva RunTimeException aposta para que la aplicación lo maneje como quiera <br>
     * PROBAR TAMBIÉN EXCEPCIONES
     */
    @Test
    public void test() {
        try {

             //User 1 -> Noel -> Se centra en desbloquear al User 2:
            new Thread (() -> {
                user1.mkChannel("ch1");
                user1.open("ch1");

                user1.writeChannel("hola");

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                user1.readChannel(true);
                user1.readChannel(true);
                user1.readChannel(true);

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                user1.writeChannel("hola2");
                user1.writeChannel("chau2");
                // ["hola", "chau", "hola2", "chau2"]

                user1.rmContent("chau");
                user1.rmContent("chau2");
                // ["hola", "", "hola2", ""]

                user1.writeChannel("hola3");
                user1.writeChannel("chau3");
                // ["hola", "", "hola2", "", "hola3", "chau3"]

                user1.rmContent("hola3");
                // ["hola", "", "hola2", "", "", "chau3"]

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                user1.writeChannel("hola4");
                // ["hola", "", "hola2", "", "", "chau3", "hola4"]

                try {
                    user1.rmChannel("ch1");
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                user1.close();
                try {
                    user1.rmChannel("ch1");
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                user1.rmChannel("ch1");

                user1.logout();

            }).start();

            // User 2 -> Lucia -> Se centra en bloquearse:
            user2.mkChannel("ch2");
            user2.rmChannel("ch2");

            try {
                user2.open("ch2");
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            user2.mkChannel("ch2");
            try {
                user2.mkChannel("ch2");
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            user2.open("ch1");

            user2.writeChannel("chau");

            user2.readChannel(false);
            user2.readChannel(false);
            user2.readChannel(false);
            user2.readChannel(false);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            user2.close();
            user2.open("ch1");

            user2.readChannel(false);
            user2.readChannel(false);
            user2.readChannel(false);
            user2.readChannel(false);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            user2.logout();     // Cierra canal activo y se desconecta

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
