package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CourseDao {

    @Insert
    long insertar(Course curso);

    @Update
    void actualizar(Course curso);

    @Delete
    void eliminar(Course curso);

    @Query("SELECT * FROM cursos WHERE id = :id LIMIT 1")
    Course obtenerPorId(int id);

    @Query("SELECT * FROM cursos WHERE profesorId = :profesorId ORDER BY id DESC")
    List<Course> listarPorProfesor(int profesorId);

    @Query("SELECT * FROM cursos WHERE id NOT IN " +
            "(SELECT cursoId FROM inscripciones WHERE estudianteId = :estudianteId) " +
            "ORDER BY id DESC")
    List<Course> listarDisponiblesParaEstudiante(int estudianteId);

    @Query("SELECT COUNT(*) FROM inscripciones WHERE cursoId = :cursoId")
    int contarInscritos(int cursoId);
}