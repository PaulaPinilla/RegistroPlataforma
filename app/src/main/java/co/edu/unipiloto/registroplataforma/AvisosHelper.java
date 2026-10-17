package co.edu.unipiloto.registroplataforma;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Course;

/**
 * Genera los avisos del estudiante: actividades por vencer, nuevas y vencidas sin entregar.
 * Debe llamarse desde un hilo secundario porque consulta la base de datos.
 */
public class AvisosHelper {

    public static final int TIPO_PRONTO = 1;
    public static final int TIPO_NUEVA = 2;
    public static final int TIPO_VENCIDA = 3;

    /** Una actividad se avisa como "vence pronto" si faltan hasta estos días */
    public static final int DIAS_VENCE_PRONTO = 3;
    /** Una actividad se avisa como "nueva" durante estos días desde que se creó */
    public static final int DIAS_NUEVA = 7;
    /** Una actividad vencida sin entregar se avisa durante estos días después de vencer */
    public static final int DIAS_VENCIDA = 7;

    private static final long DIA_MS = 24L * 60 * 60 * 1000;

    public static class Aviso {
        public int tipo;
        public int actividadId;
        public int cursoId;
        public String cursoTitulo;
        public String titulo;
        public String detalle;
        /** Criterio de orden dentro de su grupo (menor primero) */
        public long orden;
    }

    public static List<Aviso> generar(AppDatabase db, int estudianteId) {
        List<Aviso> avisos = new ArrayList<>();
        long ahora = System.currentTimeMillis();
        SimpleDateFormat formatoFechaHora = new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault());
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        List<Course> cursos = db.enrollmentDao().listarCursosInscritos(estudianteId);
        for (Course curso : cursos) {
            // En un curso terminado ya no hay nada pendiente
            if (curso.yaTermino()) continue;

            List<Assignment> actividades = db.assignmentDao().listarPorCurso(curso.getId());
            for (Assignment a : actividades) {
                // Si todavía no empieza no se avisa
                if (!a.yaComenzo()) continue;
                // Si ya la entregó (o ya presentó el quiz) no hay aviso
                if (yaEntregada(db, a, estudianteId)) continue;

                long limite = a.getFechaLimite();
                boolean vencida = limite > 0 && ahora > limite;
                boolean nueva = a.getFechaCreacion() > 0
                        && (ahora - a.getFechaCreacion()) <= DIAS_NUEVA * DIA_MS;

                Aviso aviso = new Aviso();
                aviso.actividadId = a.getId();
                aviso.cursoId = curso.getId();
                aviso.cursoTitulo = curso.getTitulo();
                aviso.titulo = AssignmentListActivity.categoriaLegible(a.getCategoria()) + " — " + a.getTitulo();

                if (vencida) {
                    if (ahora - limite > DIAS_VENCIDA * DIA_MS) continue;
                    aviso.tipo = TIPO_VENCIDA;
                    aviso.detalle = "Venció el " + formatoFechaHora.format(new Date(limite))
                            + " y no la has entregado";
                    aviso.orden = ahora - limite;
                } else if (limite > 0 && (limite - ahora) <= DIAS_VENCE_PRONTO * DIA_MS) {
                    aviso.tipo = TIPO_PRONTO;
                    aviso.detalle = textoVence(limite, ahora) + (nueva ? " (actividad nueva)" : "");
                    aviso.orden = limite;
                } else if (nueva) {
                    aviso.tipo = TIPO_NUEVA;
                    aviso.detalle = "Publicada el " + formatoFecha.format(new Date(a.getFechaCreacion()))
                            + (limite > 0 ? " — vence el " + formatoFechaHora.format(new Date(limite)) : "");
                    aviso.orden = ahora - a.getFechaCreacion();
                } else {
                    continue;
                }

                avisos.add(aviso);
            }
        }
        return avisos;
    }

    private static boolean yaEntregada(AppDatabase db, Assignment a, int estudianteId) {
        if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(a.getTipoEntrega())) {
            return db.quizResultDao().obtenerUltimoResultado(a.getId(), estudianteId) != null;
        }
        return db.submissionDao().obtenerUltimaEntrega(a.getId(), estudianteId) != null;
    }

    private static String textoVence(long limite, long ahora) {
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat formatoDiaMes = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String hora = formatoHora.format(new Date(limite));

        long dias = Math.round((inicioDelDia(limite) - inicioDelDia(ahora)) / (double) DIA_MS);
        if (dias <= 0) return "Vence hoy a las " + hora;
        if (dias == 1) return "Vence mañana a las " + hora;
        return "Vence en " + dias + " días (" + formatoDiaMes.format(new Date(limite)) + " a las " + hora + ")";
    }

    private static long inicioDelDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}