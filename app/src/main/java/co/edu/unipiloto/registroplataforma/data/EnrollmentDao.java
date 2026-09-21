package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EnrollmentDao {

    @Insert
    long insertar(Enrollment inscripcion);

    @Query("SELECT COUNT(*) FROM inscripciones WHERE cursoId = :cursoId AND estudianteId = :estudianteId")
    int existeInscripcion(int cursoId, int estudianteId);

    @Query("SELECT cursos.* FROM cursos " +
            "INNER JOIN inscripciones ON cursos.id = inscripciones.cursoId " +
            "WHERE inscripciones.estudianteId = :estudianteId " +
            "ORDER BY inscripciones.id DESC")
    List<Course> listarCursosInscritos(int estudianteId);

    @Query("SELECT COUNT(*) FROM inscripciones")
    int contarInscripciones();
}