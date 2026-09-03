package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actividades")
public class Assignment {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cursoId;
    private String titulo;
    private String instrucciones;
    private long fechaLimite;

    public Assignment(int cursoId, String titulo, String instrucciones, long fechaLimite) {
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.instrucciones = instrucciones;
        this.fechaLimite = fechaLimite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }

    public String getTitulo() { return titulo; }

    public String getInstrucciones() { return instrucciones; }

    public long getFechaLimite() { return fechaLimite; }
}
