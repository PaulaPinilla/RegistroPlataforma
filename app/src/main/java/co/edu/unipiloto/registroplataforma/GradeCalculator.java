package co.edu.unipiloto.registroplataforma;

import java.util.ArrayList;
import java.util.List;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.data.ForumGrade;
import co.edu.unipiloto.registroplataforma.data.QuizResult;
import co.edu.unipiloto.registroplataforma.data.Submission;

/**
 * Calcula las notas de un estudiante en un curso, por corte y la nota final.
 * Debe llamarse desde un hilo secundario porque consulta la base de datos.
 */
public class GradeCalculator {

    /** Una actividad o foro evaluable con su nota (null si aún no está calificado) */
    public static class Registro {
        public final int id;
        public final String titulo;
        public final Double nota;
        public final double porcentaje;

        Registro(int id, String titulo, Double nota, double porcentaje) {
            this.id = id;
            this.titulo = titulo;
            this.nota = nota;
            this.porcentaje = porcentaje;
        }
    }

    public static class CorteResultado {
        public int corte;
        /** Peso del corte en la nota final (0 si el cálculo no es ponderado) */
        public double pesoCorte;
        public boolean cuentaEnNotaFinal;
        /** Ponderado: nota acumulada del corte. Simple: promedio de lo calificado */
        public double nota;
        /** Ponderado: actividades con porcentaje que aún no tienen nota */
        public int pendientes;
        public int calificados;
        public final List<Registro> registros = new ArrayList<>();
    }

    public static class Resultado {
        public boolean ponderado;
        public double notaGeneral;
        public boolean hayNotas;
        public boolean todosCalificados;
        public final List<CorteResultado> cortes = new ArrayList<>();
    }

    public static Resultado calcular(AppDatabase db, Course curso, int cursoId, int estudianteId) {
        List<Assignment> actividades = db.assignmentDao().listarPorCurso(cursoId);
        List<Forum> foros = db.forumDao().listarPorCurso(cursoId);

        // 1) Se reúnen todas las notas agrupadas por corte (posiciones 1 a 3)
        List<List<Registro>> porCorte = new ArrayList<>();
        for (int i = 0; i <= 3; i++) porCorte.add(new ArrayList<>());

        for (Assignment a : actividades) {
            if (a.getCorte() < 1 || a.getCorte() > 3) continue;

            Double nota = null;
            if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(a.getTipoEntrega())) {
                QuizResult resultado = db.quizResultDao().obtenerUltimoResultado(a.getId(), estudianteId);
                if (resultado != null) nota = resultado.getCalificacion();
            } else {
                Submission entrega = db.submissionDao().obtenerUltimaEntrega(a.getId(), estudianteId);
                if (entrega != null) nota = entrega.getCalificacion();
            }

            porCorte.get(a.getCorte()).add(new Registro(a.getId(),
                    AssignmentListActivity.categoriaLegible(a.getCategoria()) + " — " + a.getTitulo(),
                    nota, a.getPorcentaje()));
        }

        for (Forum f : foros) {
            if (f.getCorte() < 1 || f.getCorte() > 3) continue;

            ForumGrade calificacion = db.forumGradeDao().obtenerPorForoYEstudiante(f.getId(), estudianteId);
            Double nota = calificacion != null ? calificacion.getCalificacion() : null;

            porCorte.get(f.getCorte()).add(new Registro(f.getId(), "Foro — " + f.getTema(),
                    nota, f.getPorcentaje()));
        }

        // 2) Se decide si se puede calcular con porcentajes:
        //    el curso debe tener los pesos de los cortes (suman 100) y cada corte que cuenta
        //    debe tener sus actividades y foros sumando exactamente 100%
        boolean ponderado = curso != null && curso.tienePesosCortesValidos();
        if (ponderado) {
            for (int corte = 1; corte <= 3; corte++) {
                if (curso.getPesoCorte(corte) <= 0) continue;
                double sumaPct = 0;
                for (Registro r : porCorte.get(corte)) sumaPct += r.porcentaje;
                if (Math.abs(sumaPct - 100.0) > 0.001) {
                    ponderado = false;
                    break;
                }
            }
        }

        // 3) Cálculo por corte y general
        Resultado res = new Resultado();
        res.ponderado = ponderado;

        double notaFinalPonderada = 0;
        int pendientesPonderado = 0;
        int calificadosPonderado = 0;

        double sumaSimpleGeneral = 0;
        int calificadosSimple = 0;
        int totalSimple = 0;

        for (int corte = 1; corte <= 3; corte++) {
            List<Registro> registros = porCorte.get(corte);

            CorteResultado cr = new CorteResultado();
            cr.corte = corte;
            cr.registros.addAll(registros);
            cr.pesoCorte = ponderado ? curso.getPesoCorte(corte) : 0;
            cr.cuentaEnNotaFinal = ponderado && cr.pesoCorte > 0;

            double acumulado = 0;
            double sumaSimpleCorte = 0;
            int calificados = 0;
            int pendientes = 0;

            for (Registro r : registros) {
                totalSimple++;
                if (r.nota != null) {
                    calificados++;
                    calificadosSimple++;
                    sumaSimpleCorte += r.nota;
                    sumaSimpleGeneral += r.nota;
                    acumulado += r.nota * r.porcentaje / 100.0;
                    if (cr.cuentaEnNotaFinal && r.porcentaje > 0) calificadosPonderado++;
                } else if (cr.cuentaEnNotaFinal && r.porcentaje > 0) {
                    pendientes++;
                }
            }

            cr.calificados = calificados;
            cr.pendientes = pendientes;

            if (ponderado) {
                cr.nota = acumulado;
                if (cr.cuentaEnNotaFinal) {
                    notaFinalPonderada += acumulado * cr.pesoCorte / 100.0;
                    pendientesPonderado += pendientes;
                }
            } else {
                cr.nota = calificados > 0 ? sumaSimpleCorte / calificados : 0;
            }

            res.cortes.add(cr);
        }

        // 4) Resultado general
        if (ponderado) {
            res.notaGeneral = notaFinalPonderada;
            res.hayNotas = calificadosPonderado > 0;
            res.todosCalificados = pendientesPonderado == 0 && calificadosPonderado > 0;
        } else {
            res.notaGeneral = calificadosSimple > 0 ? sumaSimpleGeneral / calificadosSimple : 0;
            res.hayNotas = calificadosSimple > 0;
            res.todosCalificados = totalSimple > 0 && calificadosSimple == totalSimple;
        }

        return res;
    }
}