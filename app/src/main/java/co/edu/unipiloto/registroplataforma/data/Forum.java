package co.edu.unipiloto.registroplataforma.data;

import androidx.room.ColumnInfo;
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
    private int corte;

    // Porcentaje que vale este foro dentro de su corte (0 a 100)
    @ColumnInfo(defaultValue = "0")
    private double porcentaje;

    public Forum(int cursoId, String tema, String descripcion, long fechaCreacion, long fechaApertura,
                 long fechaCierre, int corte) {
        this.cursoId = cursoId;
        this.tema = tema;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.corte = corte;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }

    public String getTema() { return tema; }

    public String getDescripcion() { return descripcion; }

    public long getFechaCreacion() { return fechaCreacion; }

    public long getFechaApertura() { return fechaApertura; }

    public long getFechaCierre() { return fechaCierre; }

    public int getCorte() { return corte; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }

    public boolean yaAbrio() { return fechaApertura <= 0 || System.currentTimeMillis() >= fechaApertura; }

    public boolean yaCerro() { return fechaCierre > 0 && System.currentTimeMillis() > fechaCierre; }
}