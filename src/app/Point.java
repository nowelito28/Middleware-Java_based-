package app;

public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
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

    /**
     * // Fabricar punto --> mediante descripción
     * @param info -> x,y
     * @return -> Point(x, y)
     */
    public static Point parse(String info) {
        var p = info.split(",");
        var x = Integer.parseInt(p[0]);
        var y = Integer.parseInt(p[1]);
        return new Point(x, y);
    }
}
