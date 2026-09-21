package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "foros")
public class Forum {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cursoId;
    private String tema;
    private String descripcion;
    private long fechaCreacion;

    public Forum(int cursoId, String tema, String descripcion, long fechaCreacion) {
        this.cursoId = cursoId;
        this.tema = tema;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }

    public String getTema() { return tema; }

    public String getDescripcion() { return descripcion; }

    public long getFechaCreacion() { return fechaCreacion; }
}