package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cursos")
public class Course {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String titulo;
    private String descripcion;
    private int profesorId;
    private String profesorNombre;
    private String contrasenaCurso;

    public Course(String titulo, String descripcion, int profesorId, String profesorNombre, String contrasenaCurso) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.profesorId = profesorId;
        this.profesorNombre = profesorNombre;
        this.contrasenaCurso = contrasenaCurso;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getProfesorId() { return profesorId; }

    public String getProfesorNombre() { return profesorNombre; }

    public String getContrasenaCurso() { return contrasenaCurso; }
    public void setContrasenaCurso(String contrasenaCurso) { this.contrasenaCurso = contrasenaCurso; }
}
