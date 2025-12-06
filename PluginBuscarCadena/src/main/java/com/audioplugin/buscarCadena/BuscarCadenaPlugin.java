// Archivo: PluginBuscarCadena/src/main/java/com/audioplugin/buscarcadena/BuscarCadenaPlugin.java
package com.audioplugin.buscarCadena;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuscarCadenaPlugin implements IPlugin {

    private static final String DB_URL = "jdbc:h2:./audio_db";
    private static final String VOSK_MODEL_PATH = "vosk-models/vosk-model-small-es-0.42";

    public BuscarCadenaPlugin() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    @Override
    public JPanel getPanel(IController controller) {
        // 1. Panel principal con un layout claro
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5)); // Layout principal
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 2. Panel superior para la entrada y el botón
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JTextField txtCadenaBuscar = new JTextField();
        JButton btnBuscar = new JButton("Buscar en Audios Guardados");
        topPanel.add(new JLabel("Texto a buscar:"), BorderLayout.WEST);
        topPanel.add(txtCadenaBuscar, BorderLayout.CENTER);
        topPanel.add(btnBuscar, BorderLayout.EAST);

        // 3. Área de texto para los resultados (con scroll)
        JTextArea areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(areaResultados);

        // 4. Lógica del botón (sin cambios, ya estaba bien)
        btnBuscar.addActionListener(e -> {
            String cadena = txtCadenaBuscar.getText();
            if (cadena == null || cadena.trim().isEmpty()) {
                areaResultados.setText("Por favor, escribe algo para buscar.");
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("cadena", cadena);
            String resultado = controller.ejecutarPlugin(getName(), params);
            areaResultados.setText(resultado);
        });

        // 5. Añadir los sub-paneles al panel principal
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER); // <-- La pieza que faltaba

        // 6. Devolver el panel principal construido
        return mainPanel; // <-- La otra pieza que faltaba

    }

    @Override
    public String getName() {
        return "Buscar cadena en audios";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String cadenaABuscar = (String) parameters.get("cadena");
        if (cadenaABuscar == null || cadenaABuscar.trim().isEmpty()) {
            return "Error: Se necesita una cadena de texto para buscar.";
        }

        System.out.println("\n--- INICIANDO BÚSQUEDA DE LA CADENA: '" + cadenaABuscar + "' ---");

        List<String> archivosEncontrados = new ArrayList<>();
        List<String> rutasDeArchivos = obtenerRutasDeLaDB();

        if (rutasDeArchivos.isEmpty()) {
            return "No hay archivos de audio guardados en la base de datos para buscar.";
        }

        try (Model model = new Model(VOSK_MODEL_PATH)) {
            for (String rutaArchivo : rutasDeArchivos) {
                File audioFile = new File(rutaArchivo);
                if (!audioFile.exists()) {
                    System.out.println("DEBUG: El archivo no existe, saltando: " + rutaArchivo);
                    continue;
                }

                // --- INICIO DE LA MODIFICACIÓN ---
                // Vamos a imprimir el resultado de la transcripción para depurar
                System.out.println("DEBUG: Procesando archivo -> " + audioFile.getName());
                String textoTranscribido = transcribirAudio(audioFile, model);
                System.out.println("DEBUG: Texto transcribido -> \"" + textoTranscribido + "\"");
                // --- FIN DE LA MODIFICACIÓN ---

                if (textoTranscribido.toLowerCase().contains(cadenaABuscar.toLowerCase())) {
                    System.out.println("DEBUG: ¡Coincidencia encontrada!");
                    archivosEncontrados.add(audioFile.getName());
                } else {
                    System.out.println("DEBUG: No se encontró coincidencia.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al cargar el modelo de Vosk o al procesar los audios.";
        }

        System.out.println("--- BÚSQUEDA FINALIZADA ---");

        if (archivosEncontrados.isEmpty()){
            return "La cadena '" + cadenaABuscar + "' no se encontró en ningún audio.";
        }

        return String.join("\n", archivosEncontrados);
    }

    private List<String> obtenerRutasDeLaDB() {
        List<String> rutas = new ArrayList<>();
        String sql = "SELECT ruta_archivo FROM audios";
        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rutas.add(rs.getString("ruta_archivo"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rutas;
    }

    private String transcribirAudio(File audioFile, Model model) throws Exception {
        try (InputStream ais = new FileInputStream(audioFile)) {
            Recognizer recognizer = new Recognizer(model, 44100.0f); // Asumimos una frecuencia estándar
            StringBuilder textoFinal = new StringBuilder();
            int nbytes;
            byte[] b = new byte[4096];
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    JSONObject resultJson = new JSONObject(recognizer.getResult());
                    textoFinal.append(resultJson.getString("text")).append(" ");
                }
            }
            JSONObject finalResultJson = new JSONObject(recognizer.getFinalResult());
            textoFinal.append(finalResultJson.getString("text"));
            return textoFinal.toString();
        }
    }
}