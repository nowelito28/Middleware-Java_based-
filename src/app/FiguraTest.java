package app;

import app.figures.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FiguraTest {

    @org.junit.Test
    @Test
    public void testParseCirculo() {
        String figura = "Circle:10,20:azul:5:53:93217";
        Figure parsedFigura = Figure.parse(figura);
        assertNotNull(parsedFigura);
        assertInstanceOf(Circle.class, parsedFigura); //assertTrue(parsedFigura instanceof Circle);
        Circle circulo = (Circle) parsedFigura;
        assertEquals(10, circulo.p.x);
        assertEquals(20, circulo.p.y);
        assertEquals("azul", circulo.color);
        assertEquals(5, circulo.semiejeMayor);
    }

    @org.junit.Test
    @Test
    public void testParseElipse() {
        String figura = "Ellipse:15,30:rojo:7:3:39:910347";
        Figure parsedFigura = Figure.parse(figura);
        assertNotNull(parsedFigura);
        assertInstanceOf(Ellipse.class, parsedFigura);
        Ellipse elipse = (Ellipse) parsedFigura;
        assertEquals(15, elipse.p.x);
        assertEquals(30, elipse.p.y);
        assertEquals("rojo", elipse.color);
        assertEquals(7, elipse.semiejeMayor);
        assertEquals(3, elipse.semiejeMenor);
    }

    @org.junit.Test
    @Test
    public void testParseRectangulo() {
        String figura = "Rectangle:20,40:verde:8:6:88:912735";
        Figure parsedFigura = Figure.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Rectangle);
        Rectangle rectangulo = (Rectangle) parsedFigura;
        assertEquals(20, rectangulo.p.x);
        assertEquals(40, rectangulo.p.y);
        assertEquals("verde", rectangulo.color);
        assertEquals(8, rectangulo.altura);
        assertEquals(6, rectangulo.base);
    }

    @org.junit.Test
    @Test
    public void testParseTriangulo() {
        String figura = "Triangle:20,40:verde:8:6:88:912735";
        Figure parsedFigura = Figure.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Triangle);
        Triangle triangulo = (Triangle) parsedFigura;
        assertEquals(20, triangulo.p.x);
        assertEquals(40, triangulo.p.y);
        assertEquals("verde", triangulo.color);
        assertEquals(8, triangulo.base);
        assertEquals(6, triangulo.altura);
    }

    @org.junit.Test
    @Test
    public void testParseGrupo() {
        String figura = "Group:10,20:azul:2:Circle:15,30:rojo:5:77:939754:/:Rectangle:25,35:verde:8:6:99:9842:/:0983423";
        Figure parsedFigura = Figure.parse(figura);
        assertNotNull(parsedFigura);
        assertTrue(parsedFigura instanceof Group);
        Group grupo = (Group) parsedFigura;
        assertEquals(10, grupo.p.x);
        assertEquals(20, grupo.p.y);
        assertEquals("azul", grupo.color);
        assertEquals(2, grupo.figs.size());
        assertTrue(grupo.figs.get(0) instanceof Circle);
        assertTrue(grupo.figs.get(1) instanceof Rectangle);
        String fig = "Group:10,20:azul:0:0983423";
        Figure parsedFigura2 = Figure.parse(fig);
        assertNotNull(parsedFigura2);
        assertTrue(parsedFigura2 instanceof Group);
        Group grupo2 = (Group) parsedFigura2;
        assertEquals(10, grupo2.p.x);
        assertEquals(20, grupo2.p.y);
        assertEquals("azul", grupo2.color);
        assertEquals(0, grupo2.figs.size());
    }

    @org.junit.Test
    @Test
    public void testParseInvalidFigura() {
        String figura = "InvalidFigura:10,20:azul:5";
        assertThrows(IllegalArgumentException.class, () -> Figure.parse(figura));
    }
    @org.junit.Test
    @Test
    public void testParseInvalidFormat() {
        String figura = "Circle:10,20";
        assertThrows(RuntimeException.class, () -> Figure.parse(figura));
    }

    @org.junit.Test
    @Test
    public void testCirculoToString() {
        Circle circulo = new Circle(5, new Point(10, 20), "azul");
        assertEquals("Circle:10,20:azul:5:78.53981633974483:0", circulo.toString());
    }
    @org.junit.Test
    @Test
    public void testElipseToString() {
        Ellipse elipse = new Ellipse(5, 3, new Point(10, 20), "azul");
        assertEquals("Ellipse:10,20:azul:5:3:47.12388980384689:0", elipse.toString());
    }
    @org.junit.Test
    @Test
    public void testRectanguloToString() {
        Rectangle rectangulo = new Rectangle(5, 3, new Point(10, 20), "azul");
        assertEquals("Rectangle:10,20:azul:5:3:15.0:0", rectangulo.toString());
    }
    @org.junit.Test
    @Test
    public void testGrupoToString() {
        Group grupo = new Group(new Point(15, 30), "azul");
        Circle circulo = new Circle(5, new Point(15, 30), "rojo");
        Rectangle rectangulo = new Rectangle(5, 3, new Point(25, 35), "verde");
        grupo.addfig(circulo);
        grupo.addfig(rectangulo);
        assertEquals("Group:15,30:azul:2:Circle:15,30:rojo:5:78.53981633974483:0:/:" +
                "Rectangle:25,35:verde:5:3:15.0:1:/:0", grupo.toString());
        grupo.delfig(circulo);
        assertEquals("Group:15,30:azul:1:Rectangle:25,35:verde:5:3:15.0:0:/:0", grupo.toString());
    }
    @org.junit.Test
    @Test
    public void testCirculoArea() {
        Circle circulo = new Circle(5, new Point(10, 20), "azul");
        assertEquals(Math.PI * 5 * 5, circulo.calcArea(), 0.01);
    }
    @org.junit.Test
    @Test
    public void testElipseArea() {
        Ellipse elipse = new Ellipse(5, 3, new Point(10, 20), "azul");
        assertEquals(Math.PI * 5 * 3, elipse.calcArea(), 0.01);
    }

    @org.junit.Test
    @Test
    public void testRectanguloArea() {
        Rectangle rectangulo = new Rectangle(5, 3, new Point(10, 20), "azul");
        assertEquals(5 * 3, rectangulo.calcArea(), 0.01);
    }

    @org.junit.Test
    @Test
    public void testMove() {
        Circle circulo = new Circle(5, new Point(10, 20), "azul");
        circulo.move(3, 4);
        assertEquals(13, circulo.p.x);
        assertEquals(24, circulo.p.y);
    }

    @org.junit.Test
    @Test
    public void testExpand() {
        Circle circulo = new Circle(5, new Point(10, 20), "azul");
        circulo.expand(2);
        assertEquals(157.07963267948966, circulo.area);
    }
    @org.junit.Test
    @Test
    public void testGroup() {
        Circle circulo1 = new Circle(5, new Point(10, 20), "azul");
        Circle circulo2 = new Circle(3, new Point(15, 30), "rojo");
        Group grupo = new Group(new Point(0, 0), "verde");
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
