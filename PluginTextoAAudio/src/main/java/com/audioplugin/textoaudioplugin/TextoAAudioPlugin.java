package com.audioplugin.textoaudioplugin;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.swing.*;

public class TextoAAudioPlugin implements IPlugin {

    private static final String VOICE_NAME = "kevin16";
    private static final String OUTPUT_FILENAME = "grabacion";

    @Override
    public String getName() {
        return "Convertir texto a archivo de audio";
    }
    @Override
    public JPanel getPanel(IController controller) {
        // --- Creación de la Interfaz del Plugin ---
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnSeleccionarArchivo = new JButton("Seleccionar Archivo .txt y Convertir a Audio");
        btnSeleccionarArchivo.setFont(new Font("Arial", Font.BOLD, 14));

        JTextArea areaInfo = new JTextArea();
        areaInfo.setEditable(false);
        areaInfo.setLineWrap(true);
        areaInfo.setWrapStyleWord(true);
        areaInfo.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaInfo.setText("Haga clic en el botón para seleccionar un archivo de texto (.txt).\n\n" +
                "El contenido del archivo se convertirá en un audio temporal ('grabacion.wav') " +
                "y se establecerá como el audio activo.");

        // --- Lógica del Botón ---
        btnSeleccionarArchivo.addActionListener(e -> {
            // 1. Le pide al controlador que muestre el selector de archivos
            File archivoTxt = controller.solicitarArchivo("Archivos de Texto (*.txt)", "txt");

            if (archivoTxt == null) {
                controller.mostrarMensaje("Operación cancelada.");
                return;
            }

            try {
                // 2. Lee el contenido del archivo
                String texto = new String(Files.readAllBytes(archivoTxt.toPath()));

                if (texto.trim().isEmpty()) {
                    controller.mostrarMensaje("El archivo seleccionado está vacío.");
                    return;
                }

                // 3. Prepara y ejecuta la lógica del plugin
                Map<String, Object> params = new HashMap<>();
                params.put("texto", texto);
                String resultado = execute(params);

                // 4. Actualiza el estado de la aplicación a través del controlador
                controller.mostrarMensaje(resultado);
                if (!resultado.startsWith("Error:")) {
                    controller.setArchivoActivo("grabacion.wav", "grabacion.wav");
                }

            } catch (IOException ex) {
                controller.mostrarMensaje("Error al leer el archivo: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Añadir componentes al panel
        panel.add(btnSeleccionarArchivo, BorderLayout.NORTH);
        panel.add(new JScrollPane(areaInfo), BorderLayout.CENTER);

        return panel;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String texto = (String) parameters.get("texto");
        if (texto == null || texto.trim().isEmpty()) {
            return "Error: No se proporcionó texto para convertir.";
        }

        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice voice = voiceManager.getVoice(VOICE_NAME);

        if (voice == null) {
            return "Error: No se pudo encontrar la voz '" + VOICE_NAME + "'.";
        }

        try {
            voice.allocate();
            SingleFileAudioPlayer audioPlayer = new SingleFileAudioPlayer(OUTPUT_FILENAME, AudioFileFormat.Type.WAVE);
            voice.setAudioPlayer(audioPlayer);
            voice.speak(texto);
            voice.deallocate();
            audioPlayer.close();
            return "Archivo temporal 'grabacion.wav' creado/actualizado exitosamente.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al convertir texto a audio: " + e.getMessage();
        }
    }
}