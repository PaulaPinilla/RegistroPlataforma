package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class User {

    public static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    public static final String ROL_PROFESOR = "PROFESOR";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombre;
    private String correo;
    private String password;
    private String rol;

    public User(String nombre, String correo, String password, String rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
        this.rol = rol;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean esProfesor() { return ROL_PROFESOR.equals(rol); }
}