package com.audioplugin.obtenertexto;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ObtenerTextoPlugin implements IPlugin {
    private static final String VOSK_MODEL_PATH = "vosk-models/vosk-model-small-es-0.42";

    @Override
    public String getName() {
        return "Obtener texto de archivo de audio (Vosk)";
    }
    @Override
    public JPanel getPanel(IController controller) {
        // Panel principal y componentes de la UI
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnTranscribir = new JButton("Volver a Transcribir Audio Activo");
        btnTranscribir.setFont(new Font("Arial", Font.BOLD, 14));

        JTextArea areaResultado = new JTextArea();
        areaResultado.setEditable(false);
        areaResultado.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // --- INICIO DE LA MODIFICACIÓN 1: AJUSTE DE LÍNEA ---
        // Le decimos al área de texto que ajuste las líneas automáticamente...
        areaResultado.setLineWrap(true);
        // ...y que lo haga respetando las palabras (para no cortar una palabra por la mitad).
        areaResultado.setWrapStyleWord(true);
        // --- FIN DE LA MODIFICACIÓN 1 ---

        JScrollPane scrollPane = new JScrollPane(areaResultado);

        // Lógica del botón (sin cambios)
        btnTranscribir.addActionListener(e -> {
            String rutaAudio = controller.getRutaAudioActivo();
            Map<String, Object> params = new HashMap<>();
            params.put("rutaArchivo", rutaAudio);
            String textoTranscribido = execute(params);
            areaResultado.setText(textoTranscribido);
            if (textoTranscribido.startsWith("Error:")) {
                controller.mostrarMensaje(textoTranscribido);
            } else {
                controller.mostrarMensaje("Transcripción completada.");
            }
        });

        // Añadir componentes al panel
        panel.add(btnTranscribir, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);


        btnTranscribir.doClick();


        return panel;
    }


    @Override
    public String execute(Map<String, Object> parameters) {
        String rutaArchivoAudio = (String) parameters.get("rutaArchivo");

        if (rutaArchivoAudio == null) {
            return "Error: No hay un archivo de audio activo seleccionado.";
        }

        File audioFile = new File(rutaArchivoAudio);
        if (!audioFile.exists()) {
            return "Error: El archivo de audio no existe -> " + rutaArchivoAudio;
        }

        try (Model model = new Model(VOSK_MODEL_PATH);
             InputStream ais = new FileInputStream(audioFile)) {

            Recognizer recognizer = new Recognizer(model, 44100.0f);
            StringBuilder textoFinal = new StringBuilder();
            byte[] b = new byte[4096];
            int nbytes;

            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    JSONObject resultJson = new JSONObject(recognizer.getResult());
                    textoFinal.append(resultJson.getString("text")).append(" ");
                }
            }
            JSONObject finalResultJson = new JSONObject(recognizer.getFinalResult());
            textoFinal.append(finalResultJson.getString("text"));

            // Devuelve directamente el texto transcribido
            return textoFinal.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("Failed to create a model")) {
                return "Error: No se pudo cargar el modelo de Vosk. Asegúrate de que la carpeta '" + VOSK_MODEL_PATH + "' existe.";
            }
            return "Error al procesar el audio con Vosk: " + e.getMessage();
        }
    }
}