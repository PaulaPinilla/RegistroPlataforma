package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AssignmentDao {

    @Insert
    long insertar(Assignment actividad);

    @Query("SELECT * FROM actividades WHERE id = :id LIMIT 1")
    Assignment obtenerPorId(int id);

    @Query("SELECT * FROM actividades WHERE unidadId = :unidadId ORDER BY id DESC")
    List<Assignment> listarPorUnidad(int unidadId);

    @Query("SELECT * FROM actividades WHERE unidadId IN (SELECT id FROM unidades WHERE cursoId = :cursoId) ORDER BY id DESC")
    List<Assignment> listarPorCurso(int cursoId);
}