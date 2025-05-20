package network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import servicies.Svc;
import servicies.SvcEditD;

public class UserServerTDraws {
    private Server server;
    private Svc svc;
    private User user1;
    private User user2;
    static final String fig1 = "Circle:10,20:azul:5:";     // ¡Esencial para parsear figura!
    static final String fig2 = "Square:2,8:rojo:2:";
    static final String fig3 = "Ellipse:1,13:verde:4:6:";
    static final String fig4 = "Group:10,22:negro:0:";
    static final String fig5 = "Group:15,29:rosa:2:/" +
            "-:Circle:15,30:rojo:5:-:Rectangle:25,35:verde:5:3:-/:";

    @Before
    public void setup() {
        svc = new SvcEditD();
        server = new Server("TestServerEditDraws", 8080, svc);
        new Thread(server::start).start();
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
     * --> se eleva RunTimeException aposta para que la aplicación lo maneje como quiera
     */
    @Test
    public void test() {
        String[] autors = new String[2];
        autors[0] = "lucia";
        autors[1] = "lucia";
        int[] ids = new int[2];
        ids[0] = 1146071926;
        ids[1] = 845077042;
        String[] autors1 = new String[0];
        int[] ids1 = new int[0];
        try {
            user2.newd(fig1, fig3, fig5, fig2);
            user1.newd();
            user1.rmd(user1.name, 2041696554);
            user2.dump(autors, ids);        // Algunos dibujos
            user1.dump(autors1, ids1);      // Todos los dibujos
            user1.newf(user1.name, 187613959, fig1, fig2, fig3);
            user2.newf(user2.name, 845077042, fig4, fig5);
            user1.rmf(user1.name, 187613959, 1793720615, 1441077431);
            user2.mvd(autors, ids, 40, 100);
            user1.mvf("noel", 187613959, 200, 300, 102503165, 22761689);
            user1.logout();
            user2.logout();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }
}
