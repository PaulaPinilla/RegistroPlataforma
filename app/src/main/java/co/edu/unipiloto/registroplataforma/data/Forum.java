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
    private long fechaApertura;
    private long fechaCierre;

    public Forum(int cursoId, String tema, String descripcion, long fechaCreacion, long fechaApertura, long fechaCierre) {
        this.cursoId = cursoId;
        this.tema = tema;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }

    public String getTema() { return tema; }

    public String getDescripcion() { return descripcion; }

    public long getFechaCreacion() { return fechaCreacion; }

    public long getFechaApertura() { return fechaApertura; }

    public long getFechaCierre() { return fechaCierre; }

    public boolean yaAbrio() { return fechaApertura <= 0 || System.currentTimeMillis() >= fechaApertura; }

    public boolean yaCerro() { return fechaCierre > 0 && System.currentTimeMillis() > fechaCierre; }
}