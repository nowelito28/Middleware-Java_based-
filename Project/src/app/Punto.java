package app;

public class Punto {
    public int x;
    public int y;

    public Punto(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    public static Punto parse(String info) {           // Fabricar punto --> mediante descripción
        var p = info.split(",");
        var x = Integer.parseInt(p[0]);
        var y = Integer.parseInt(p[1]);
        return new Punto(x, y);
    }
}
