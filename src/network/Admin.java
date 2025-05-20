package network;

/**
 * Tipo de cliente --> Administrador
 * Puede cambiar permisos --> No se llegó a pedir implementación
 */
public class Admin extends User {
    private int hash;

    public Admin(String name, int port, String host, int hash) {
        super(name, port, host);
        this.hash = hash;
    }

    public void admin() {
        System.out.println(name + " es Administrador.");
    }

    private void swap(int hash) {   // Cambia el hash del admin
        this.hash = hash;
    }

    @Override
    public String getTipo() {    // Obtener tipo de usuario
        return "Admin";
    }

    /*private void change(Draw draw, User user, Perms... pers) {     // Dar permisos => Perms.RD/WR/EX.value
        var pers_comb = Perms.combine(pers);
        pers_comb &= ~Perms.OW.value;    // Asegurar que no puede cambiar el permiso de dueño
        user.draws.put(draw, pers_comb);    // Lo añade si no lo tiene en el diccionario
        System.out.println("Perms actualizados para el dibujo con ID " +
        draw.id + " en " + user.name + ": " + Perms.fromInt(pers_comb));
    }*/
}
