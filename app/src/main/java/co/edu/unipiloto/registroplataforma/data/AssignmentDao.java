package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AssignmentDao {

    @Insert
    long insertar(Assignment actividad);

    @Update
    void actualizar(Assignment actividad);

    @Delete
    void eliminar(Assignment actividad);

    @Query("SELECT * FROM actividades WHERE id = :id LIMIT 1")
    Assignment obtenerPorId(int id);

    @Query("SELECT * FROM actividades WHERE unidadId = :unidadId ORDER BY id DESC")
    List<Assignment> listarPorUnidad(int unidadId);

    @Query("SELECT * FROM actividades WHERE unidadId IN (SELECT id FROM unidades WHERE cursoId = :cursoId) ORDER BY id DESC")
    List<Assignment> listarPorCurso(int cursoId);

    /** Suma de porcentajes de las actividades de un corte del curso (se puede excluir una actividad al editarla) */
    @Query("SELECT COALESCE(SUM(porcentaje), 0) FROM actividades " +
            "WHERE corte = :corte AND id != :excluirId " +
            "AND unidadId IN (SELECT id FROM unidades WHERE cursoId = :cursoId)")
    double sumarPorcentajeActividadesCorte(int cursoId, int corte, int excluirId);

    /** Suma de porcentajes de los foros de un corte del curso */
    @Query("SELECT COALESCE(SUM(porcentaje), 0) FROM foros WHERE cursoId = :cursoId AND corte = :corte")
    double sumarPorcentajeForosCorte(int cursoId, int corte);

    /** Igual que la anterior, pero sin contar un foro (para cuando se edita ese foro) */
    @Query("SELECT COALESCE(SUM(porcentaje), 0) FROM foros " +
            "WHERE cursoId = :cursoId AND corte = :corte AND id != :excluirId")
    double sumarPorcentajeForosCorteExcluyendo(int cursoId, int corte, int excluirId);
}