package co.edu.unipiloto.registroplataforma.data;

import androidx.room.ColumnInfo;
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
    private String semestre;
    private long fechaFinalizacion;

    // Porcentaje que vale cada corte en la nota final (deben sumar 100)
    @ColumnInfo(defaultValue = "0")
    private double pesoCorte1;
    @ColumnInfo(defaultValue = "0")
    private double pesoCorte2;
    @ColumnInfo(defaultValue = "0")
    private double pesoCorte3;

    public Course(String titulo, String descripcion, int profesorId, String profesorNombre,
                  String contrasenaCurso, String semestre, long fechaFinalizacion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.profesorId = profesorId;
        this.profesorNombre = profesorNombre;
        this.contrasenaCurso = contrasenaCurso;
        this.semestre = semestre;
        this.fechaFinalizacion = fechaFinalizacion;
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

    public String getSemestre() { return semestre; }
    public void setSemestre(String semestre) { this.semestre = semestre; }

    public long getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(long fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }

    public double getPesoCorte1() { return pesoCorte1; }
    public void setPesoCorte1(double pesoCorte1) { this.pesoCorte1 = pesoCorte1; }

    public double getPesoCorte2() { return pesoCorte2; }
    public void setPesoCorte2(double pesoCorte2) { this.pesoCorte2 = pesoCorte2; }

    public double getPesoCorte3() { return pesoCorte3; }
    public void setPesoCorte3(double pesoCorte3) { this.pesoCorte3 = pesoCorte3; }

    /** Peso (en porcentaje) del corte 1, 2 o 3 */
    public double getPesoCorte(int corte) {
        switch (corte) {
            case 1: return pesoCorte1;
            case 2: return pesoCorte2;
            case 3: return pesoCorte3;
            default: return 0;
        }
    }

    /** true si el profesor definió los pesos de los cortes y suman 100 */
    public boolean tienePesosCortesValidos() {
        return Math.abs((pesoCorte1 + pesoCorte2 + pesoCorte3) - 100.0) < 0.001;
    }

    /** true solo desde el día siguiente a la fecha de finalización */
    public boolean yaTermino() {
        return fechaFinalizacion > 0 && System.currentTimeMillis() > fechaFinalizacion;
    }
}