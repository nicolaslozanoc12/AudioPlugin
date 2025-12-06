package com.audioplugin.reproduciraudio;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.*;

public class ReproducirAudioPlugin implements IPlugin {

    @Override
    public String getName() {
        return "Reproducir archivo de audio";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String rutaArchivo = (String) parameters.get("rutaArchivo");

        IController controller = (IController) parameters.get("controller");

        if (rutaArchivo == null || rutaArchivo.isEmpty()) {
            return "Error: No se ha especificado una ruta de archivo.";
        }
        if (controller == null) {
            return "Error: El controlador no está disponible para este plugin.";
        }

        try {
            File audioFile = new File(rutaArchivo);
            if (!audioFile.exists()) {
                return "Error: El archivo de audio no existe: " + rutaArchivo;
            }

            new Thread(() -> {
                try {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    Clip clip = AudioSystem.getClip();

                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                            //  Usamos el controlador para mostrar el mensaje en la UI ---
                            controller.mostrarMensaje("Recurso de audio liberado para: " + audioFile.getName());
                        }
                    });

                    clip.open(audioStream);
                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    // También mostramos los errores en la UI
                    controller.mostrarMensaje("Error interno al reproducir: " + e.getMessage());
                }
            }).start();

            return "Reproduciendo: " + audioFile.getName();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error al iniciar la reproducción: " + e.getMessage();
        }
    }
}