package network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserServerT {
    private Servidor server;
    private User user1;
    private User user2;
    static final String fig1 = "Circulo:10,20:azul:5:";     // Dar lo esencial para parsear figura, el resto lo hace el servidor
    static final String fig2 = "Cuadrado:2,8:rojo:2:";
    static final String fig3 = "Elipse:1,13:verde:4:6:";
    static final String fig4 = "Grupo:10,22:negro:0:";
    static final String fig5 = "Grupo:15,29:rosa:2:/" +
            "-:Circulo:15,30:rojo:5:-:Rectangulo:25,35:verde:5:3:-/:";

    @Before
    public void setup() {
        server = new Servidor("TestServer", 8080);
        server.start();
        user1 = new User("noel", 8080, "0.0.0.0");
        user2 = new User("lucia", 8080, "0.0.0.0");
    }

    @After
    public void tearDown() {
        server.close();

    }

    @Test
    public void test() { // Comprueba que se envie el mensaje => no saturar los puertos
        String[] autors = new String[1];
        autors[0] = "noel";
        int[] ids = new int[1];
        ids[0] = 187613959;
        user1.newd(fig1, fig5, fig2);
        user2.newd();
        user1.rmd(user1.name, 230039949);
        user1.dump(autors, ids);
        user1.newf(user1.name, 432216758, fig4, fig5);
        user1.rmf(user1.name, 187613959, 795399113, 613263498, 884255377);
        user1.mvd(autors, ids, 40, 100);
        user1.mvf("noel", 187613959, 200, 300, 615218450, 1589151636);
        user1.logout();
        user2.logout();
    }

}
