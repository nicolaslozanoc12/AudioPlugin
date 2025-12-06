package com.mycompany.plugininterfaces;

import java.io.File;

public interface IController {
    String ejecutarPlugin(String nombrePlugin, java.util.Map<String, Object> params);
    String getRutaAudioActivo();
    void setArchivoActivo(String rutaCompleta, String nombreArchivo);
    void mostrarMensaje(String mensaje);
    File solicitarArchivo(String descripcion, String... extensiones);
}
