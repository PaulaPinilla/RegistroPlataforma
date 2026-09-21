package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubmissionDao {

    @Insert
    long insertar(Submission entrega);

    @Delete
    void eliminar(Submission entrega);

    @Query("SELECT * FROM entregas WHERE actividadId = :actividadId AND estudianteId = :estudianteId LIMIT 1")
    Submission obtenerEntrega(int actividadId, int estudianteId);

    @Query("SELECT * FROM entregas WHERE actividadId = :actividadId AND estudianteId = :estudianteId " +
            "ORDER BY fechaEntrega DESC LIMIT 1")
    Submission obtenerUltimaEntrega(int actividadId, int estudianteId);

    @Query("SELECT * FROM entregas WHERE actividadId = :actividadId ORDER BY fechaEntrega DESC")
    List<Submission> listarPorActividad(int actividadId);

    @Query("UPDATE entregas SET calificacion = :calificacion, retroalimentacion = :retro WHERE id = :submissionId")
    void calificar(int submissionId, Double calificacion, String retro);
}