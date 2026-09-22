package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class User {

    public static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    public static final String ROL_PROFESOR = "PROFESOR";

    public static final String GENERO_MASCULINO = "MASCULINO";
    public static final String GENERO_FEMENINO = "FEMENINO";
    public static final String GENERO_NO_BINARIO = "NO_BINARIO";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombre;
    private String usuario;
    private String correo;
    private String direccion;
    private Double latitud;
    private Double longitud;
    private String password;
    private String rol;
    private long fechaNacimiento;
    private String genero;

    public User(String nombre, String usuario, String correo, String direccion, Double latitud, Double longitud,
                String password, String rol, long fechaNacimiento, String genero) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.correo = correo;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.password = password;
        this.rol = rol;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }

    public String getUsuario() { return usuario; }

    public String getCorreo() { return correo; }

    public String getDireccion() { return direccion; }

    public Double getLatitud() { return latitud; }

    public Double getLongitud() { return longitud; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public long getFechaNacimiento() { return fechaNacimiento; }

    public String getGenero() { return genero; }

    public boolean esProfesor() { return ROL_PROFESOR.equals(rol); }
}