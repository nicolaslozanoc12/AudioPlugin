package com.audioplugin.listaraudios;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ListarAudiosPlugin implements IPlugin {
    private static final String DB_URL = "jdbc:h2:./audio_db";
    private final Map<String, String> mapaAudios = new HashMap<>();
    public ListarAudiosPlugin() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Listar archivos de audio guardados";
    }
    @Override
    public JPanel getPanel(IController controller) {
        // --- Creación de la Interfaz del Plugin ---
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Modelo de la lista que se actualizará dinámicamente
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> listaAudios = new JList<>(listModel);

        JButton btnRefrescar = new JButton("Refrescar Lista");

        // --- Lógica de los Componentes ---

        // Acción del botón "Refrescar"
        btnRefrescar.addActionListener(e -> {
            // Llama a la lógica principal del plugin para obtener los datos
            String resultado = execute(new HashMap<>());

            // Limpia los datos anteriores
            listModel.clear();
            mapaAudios.clear();

            if (resultado.startsWith("Error:") || resultado.equals("No hay audios guardados en la base de datos.")) {
                listModel.addElement(resultado); // Muestra el mensaje en la lista
            } else {
                // Procesa la respuesta y llena la lista y el mapa
                String[] lineas = resultado.split("\n");
                for (String linea : lineas) {
                    if (linea.contains(";")) {
                        String[] partes = linea.split(";", 2);
                        String nombreVisible = partes[0];
                        String rutaCompleta = partes[1];
                        listModel.addElement(nombreVisible);
                        mapaAudios.put(nombreVisible, rutaCompleta);
                    }
                }
            }
        });

        // Acción al seleccionar un elemento de la lista
        listaAudios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String seleccionado = listaAudios.getSelectedValue();
                if (seleccionado != null && mapaAudios.containsKey(seleccionado)) {
                    // Obtiene la ruta completa y se la pasa al controlador
                    String rutaSeleccionada = mapaAudios.get(seleccionado);
                    controller.setArchivoActivo(rutaSeleccionada, seleccionado);
                }
            }
        });

        // Añadir componentes al panel
        panel.add(btnRefrescar, BorderLayout.NORTH);
        panel.add(new JScrollPane(listaAudios), BorderLayout.CENTER);

        // Disparamos una actualización inicial al mostrar el panel
        btnRefrescar.doClick();

        return panel;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        StringBuilder resultado = new StringBuilder();
        String sql = "SELECT nombre_visible, ruta_archivo FROM audios";

        try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Formato: nombre_visible;ruta_completa\n
                resultado.append(rs.getString("nombre_visible"))
                        .append(";")
                        .append(rs.getString("ruta_archivo"))
                        .append("\n");
            }
            return resultado.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error al leer la base de datos: " + e.getMessage();
        }
    }
}