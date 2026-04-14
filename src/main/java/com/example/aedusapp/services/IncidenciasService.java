package com.example.aedusapp.services;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.AulaDAO;
import com.example.aedusapp.database.daos.ConocimientoDAO;
import com.example.aedusapp.database.daos.MisionesDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Aula;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.ai.AIService;
import com.example.aedusapp.services.logging.LogService;
import com.example.aedusapp.services.media.PostImagesService;

import java.io.File;
import java.util.List;

/**
 * Servicio centralizado para gestionar la lógica de negocio de las Incidencias.
 * Desacopla el Controlador de JavaFX de la capa de acceso a datos (DAOs) y APIs externas.
 */
public class IncidenciasService {

    private final IncidenciaDAO incidenciaDAO;
    private final AulaDAO aulaDAO;
    private final ConocimientoDAO conocimientoDAO;
    private final MisionesDAO misionesDAO;
    private final AIService aiService;

    public IncidenciasService() {
        this.incidenciaDAO = new IncidenciaDAO();
        this.aulaDAO = new AulaDAO();
        this.conocimientoDAO = new ConocimientoDAO();
        this.misionesDAO = new MisionesDAO();
        this.aiService = new AIService();
    }

    public String[] buscarSugerenciaFAQ(String text) {
        return conocimientoDAO.buscarArticuloSimilar(text);
    }

    public String pedirSugerenciaIA(String consulta) {
        String systemPromptTecnico = "Eres un asistente técnico de soporte IT para un centro educativo. " +
                "El usuario va a describir un problema técnico. " +
                "Tu misión es dar pasos concretos y prácticos para que el profesor RESUELVA EL PROBLEMA POR SÍ MISMO sin necesidad de abrir un ticket de soporte. " +
                "Responde SIEMPRE en español. Sé breve y usa viñetas (•) para los pasos. " +
                "No menciones incidencias anteriores ni bases de datos. Solo responde al problema descrito.";
        String userMsg = "Tengo este problema técnico en el aula:\n\"" + consulta + "\"\n\n¿Cómo puedo solucionarlo?";
        // El override substituye completamente el prompt de métricas del Dashboard
        return aiService.askAI(userMsg, null, systemPromptTecnico);
    }

    public List<Aula> obtenerAulas() {
        return aulaDAO.getAll();
    }

    public boolean crearAula(Aula aula) {
        return aulaDAO.create(aula);
    }

    public List<Incidencia> obtenerIncidencias(String usuarioId, int pageSize, int offset) {
        return incidenciaDAO.getTicketsByUserPaginated(usuarioId, pageSize, offset);
    }

    public boolean borrarIncidencia(int id) {
        return incidenciaDAO.deleteTicket(id);
    }

    /**
     * Gestiona la subida de imagen (si existe), creación del ticket y registro en el Audit Log.
     */
    public boolean crearIncidenciaCompleta(Incidencia nueva, Usuario usuarioActual, File imagenSeleccionada) {
        if (imagenSeleccionada != null) {
            String url = PostImagesService.uploadImage(imagenSeleccionada);
            if (url != null) {
                nueva.setImagenUrl(url);
            } else {
                throw new RuntimeException("Error al subir imagen a la nube.");
            }
        }

        if (incidenciaDAO.createTicket(nueva)) {
            LogService.logCrearIncidencia(usuarioActual, 0, nueva.getTitulo());
            return true;
        }
        return false;
    }

    public boolean verificarYOtorgarMisionDiaria(String usuarioId) {
        return misionesDAO.registrarMisionDiaria(usuarioId, "CREAR_TICKET", 10);
    }
}
