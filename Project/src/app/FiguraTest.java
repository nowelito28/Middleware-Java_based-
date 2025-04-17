package app;

import app.figuras.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FiguraTest {

    @org.junit.Test
    @Test
    public void testParseCirculo() {
        String figura = "Circulo:10,20:azul:5:53:93217";
        Figura parsedFigura = Figura.parse(figura);
        assertNotNull(parsedFigura);
        assertInstanceOf(Circulo.class, parsedFigura); //assertTrue(parsedFigura instanceof Circulo);
        Circulo circulo = (Circulo) parsedFigura;
        assertEquals(10, circulo.p.x);
        assertEquals(20, circulo.p.y);
        assertEquals("azul", circulo.color);
        assertEquals(5, circulo.semiejeMayor);
    }

    @org.junit.Test
    @Test
    public void testParseElipse() {
        String figura = "Elipse:15,30:rojo:7:3:39:910347";
        Figura parsedFigura = Figura.parse(figura);
        assertNotNull(parsedFigura);
        assertInstanceOf(Elipse.class, parsedFigura);
        Elipse elipse = (Elipse) parsedFigura;
        assertEquals(15, elipse.p.x);
        assertEquals(30, elipse.p.y);
        assertEquals("rojo", elipse.color);
        assertEquals(7, elipse.semiejeMayor);
        assertEquals(3, elipse.semiejeMenor);
    }

    @org.junit.Test
    @Test
    public void testParseRectangulo() {
        String figura = "Rectangulo:20,40:verde:8:6:88:912735";
        Figura parsedFigura = Figura.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Rectangulo);
        Rectangulo rectangulo = (Rectangulo) parsedFigura;
        assertEquals(20, rectangulo.p.x);
        assertEquals(40, rectangulo.p.y);
        assertEquals("verde", rectangulo.color);
        assertEquals(8, rectangulo.altura);
        assertEquals(6, rectangulo.base);
    }

    @org.junit.Test
    @Test
    public void testParseTriangulo() {
        String figura = "Triangulo:20,40:verde:8:6:88:912735";
        Figura parsedFigura = Figura.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Triangulo);
        Triangulo triangulo = (Triangulo) parsedFigura;
        assertEquals(20, triangulo.p.x);
        assertEquals(40, triangulo.p.y);
        assertEquals("verde", triangulo.color);
        assertEquals(8, triangulo.base);
        assertEquals(6, triangulo.altura);
    }

    @org.junit.Test
    @Test
    public void testParseGrupo() {
        String figura = "Grupo:10,20:azul:2:Circulo:15,30:rojo:5:77:939754:/:Rectangulo:25,35:verde:8:6:99:9842:/:0983423";
        Figura parsedFigura = Figura.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Grupo);
        Grupo grupo = (Grupo) parsedFigura;
        assertEquals(10, grupo.p.x);
        assertEquals(20, grupo.p.y);
        assertEquals("azul", grupo.color);
        assertEquals(2, grupo.figs.size());
        assertTrue(grupo.figs.get(0) instanceof Circulo);
        assertTrue(grupo.figs.get(1) instanceof Rectangulo);
        String fig = "Grupo:10,20:azul:0:0983423";
        Figura parsedFigura2 = Figura.parse(fig);
        assertNotNull(parsedFigura2);
        assertTrue(parsedFigura2 instanceof Grupo);
        Grupo grupo2 = (Grupo) parsedFigura2;
        assertEquals(10, grupo2.p.x);
        assertEquals(20, grupo2.p.y);
        assertEquals("azul", grupo2.color);
        assertEquals(0, grupo2.figs.size());
    }

    @org.junit.Test
    @Test
    public void testParseInvalidFigura() {
        String figura = "InvalidFigura:10,20:azul:5";
        assertThrows(IllegalArgumentException.class, () -> Figura.parse(figura));
    }
    @org.junit.Test
    @Test
    public void testParseInvalidFormat() {
        String figura = "Circulo:10,20";
        assertThrows(RuntimeException.class, () -> Figura.parse(figura));
    }

    @org.junit.Test
    @Test
    public void testCirculoToString() {
        Circulo circulo = new Circulo(5, new Punto(10, 20), "azul");
        assertEquals("Circulo:10,20:azul:5:78.53981633974483:0", circulo.toString());
    }
    @org.junit.Test
    @Test
    public void testElipseToString() {
        Elipse elipse = new Elipse(5, 3, new Punto(10, 20), "azul");
        assertEquals("Elipse:10,20:azul:5:3:47.12388980384689:0", elipse.toString());
    }
    @org.junit.Test
    @Test
    public void testRectanguloToString() {
        Rectangulo rectangulo = new Rectangulo(5, 3, new Punto(10, 20), "azul");
        assertEquals("Rectangulo:10,20:azul:5:3:15.0:0", rectangulo.toString());
    }
    @org.junit.Test
    @Test
    public void testGrupoToString() {
        Grupo grupo = new Grupo(new Punto(15, 30), "azul");
        Circulo circulo = new Circulo(5, new Punto(15, 30), "rojo");
        Rectangulo rectangulo = new Rectangulo(5, 3, new Punto(25, 35), "verde");
        grupo.addfig(circulo);
        grupo.addfig(rectangulo);
        assertEquals("Grupo:15,30:azul:2:Circulo:15,30:rojo:5:78.53981633974483:0:/:Rectangulo:25,35:verde:5:3:15.0:1:/:0", grupo.toString());
        grupo.delfig(circulo);
        assertEquals("Grupo:15,30:azul:1:Rectangulo:25,35:verde:5:3:15.0:0:/:0", grupo.toString());
    }
    @org.junit.Test
    @Test
    public void testCirculoArea() {
        Circulo circulo = new Circulo(5, new Punto(10, 20), "azul");
        assertEquals(Math.PI * 5 * 5, circulo.calcArea(), 0.01);
    }
    @org.junit.Test
    @Test
    public void testElipseArea() {
        Elipse elipse = new Elipse(5, 3, new Punto(10, 20), "azul");
        assertEquals(Math.PI * 5 * 3, elipse.calcArea(), 0.01);
    }

    @org.junit.Test
    @Test
    public void testRectanguloArea() {
        Rectangulo rectangulo = new Rectangulo(5, 3, new Punto(10, 20), "azul");
        assertEquals(5 * 3, rectangulo.calcArea(), 0.01);
    }

    @org.junit.Test
    @Test
    public void testMove() {
        Circulo circulo = new Circulo(5, new Punto(10, 20), "azul");
        circulo.move(3, 4);
        assertEquals(13, circulo.p.x);
        assertEquals(24, circulo.p.y);
    }

    @org.junit.Test
    @Test
    public void testExpand() {
        Circulo circulo = new Circulo(5, new Punto(10, 20), "azul");
        circulo.expand(2);
        assertEquals(157.07963267948966, circulo.area);
    }
    @org.junit.Test
    @Test
    public void testGroup() {
        Circulo circulo1 = new Circulo(5, new Punto(10, 20), "azul");
        Circulo circulo2 = new Circulo(3, new Punto(15, 30), "rojo");
        Grupo grupo = new Grupo(new Punto(0, 0), "verde");
        grupo.group(circulo1, circulo2);
        assertEquals(2, grupo.figs.size());
        assertTrue(grupo.figs.contains(circulo1));
        assertTrue(grupo.figs.contains(circulo2));
        grupo.ungroup(circulo1);
        assertEquals(1, grupo.figs.size());
        assertFalse(grupo.figs.contains(circulo1));
        assertTrue(grupo.figs.contains(circulo2));
    }


}
