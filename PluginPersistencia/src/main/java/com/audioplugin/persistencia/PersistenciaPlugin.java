package com.audioplugin.persistencia;

import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PersistenciaPlugin implements IPlugin {
    private static final String DB_URL = "jdbc:h2:./audio_db";
    private static final String DIRECTORIO_GRABACIONES = "grabaciones_guardadas";

    // ... (El constructor no cambia) ...
    public PersistenciaPlugin() {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
                 Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS audios (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "nombre_visible VARCHAR(255), " +
                        "ruta_archivo VARCHAR(255))";
                stmt.executeUpdate(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getName() {
        return "Persistir archivo en base de datos";
    }

    @Override
    public JPanel getPanel(IController controller) {
        // Panel principal
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Componentes
        JLabel labelInfo = new JLabel("<html>Guarda la grabación actual ('grabacion.wav') de forma permanente.</html>");
        JTextField txtNombrePersonalizado = new JTextField();
        JCheckBox chkUsarTimestamp = new JCheckBox("Usar nombre automático (con fecha y hora)", true);
        JButton btnGuardar = new JButton("Guardar Grabación");
        JTextArea areaResultado = new JTextArea(3, 20);
        areaResultado.setEditable(false);

        // Lógica para habilitar/deshabilitar el campo de texto
        txtNombrePersonalizado.setEnabled(false);
        chkUsarTimestamp.addActionListener(e -> {
            txtNombrePersonalizado.setEnabled(!chkUsarTimestamp.isSelected());
        });

        // Lógica del botón Guardar
        btnGuardar.addActionListener(e -> {
            String nombreFinal;
            if (chkUsarTimestamp.isSelected()) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                nombreFinal = "grabacion_" + timeStamp + ".wav";
            } else {
                String nombreBase = txtNombrePersonalizado.getText();
                if (nombreBase == null || nombreBase.trim().isEmpty()) {
                    areaResultado.setText("Por favor, introduce un nombre personalizado.");
                    return;
                }
                nombreFinal = nombreBase.trim() + ".wav";
            }

            Map<String, Object> params = new HashMap<>();
            params.put("rutaArchivo", "grabacion.wav"); // Siempre opera sobre el temporal
            params.put("nombreArchivo", nombreFinal);

            String resultado = controller.ejecutarPlugin(getName(), params);
            areaResultado.setText(resultado);

            // Si fue exitoso, refrescamos la lista de audios
            if (!resultado.startsWith("Error:")) {
            }
        });

        // Añadir componentes al panel
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.gridx = 0; gbc.gridy = 0; panel.add(labelInfo, gbc);
        gbc.gridy = 1; panel.add(chkUsarTimestamp, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(5, 0, 5, 0); panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 3; panel.add(new JLabel("Nombre personalizado:"), gbc);
        gbc.gridx = 1; panel.add(txtNombrePersonalizado, gbc);
        gbc.gridwidth = 2; gbc.insets = new Insets(10, 0, 0, 0);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(btnGuardar, gbc);
        gbc.gridy = 5; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0; panel.add(new JScrollPane(areaResultado), gbc);

        return panel;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // El plugin ahora solo recibe órdenes, no toma decisiones
        String rutaArchivoOriginal = (String) parameters.get("rutaArchivo");
        String nombreArchivoFinal = (String) parameters.get("nombreArchivo"); // Recibe el nombre final

        if (rutaArchivoOriginal == null || nombreArchivoFinal == null) {
            return "Error: Faltan los parámetros 'rutaArchivo' o 'nombreArchivo'.";
        }
        if (!new File(rutaArchivoOriginal).exists()) {
            return "Error: El archivo de grabación no se encuentra.";
        }

        try {
            File directorioDestino = new File(DIRECTORIO_GRABACIONES);
            System.out.println("DEBUG: Intentando crear/usar el directorio en: " + directorioDestino.getAbsolutePath()); //ruta donde se crea la carpeta
            if (!directorioDestino.exists()) {
                directorioDestino.mkdir();
            }

            Path fuente = Paths.get(rutaArchivoOriginal);
            Path destino = Paths.get(DIRECTORIO_GRABACIONES, nombreArchivoFinal);

            Files.move(fuente, destino, StandardCopyOption.REPLACE_EXISTING);

            String nuevaRuta = destino.toString();
            String nombreVisible = nombreArchivoFinal.replace(".wav", ""); // Guardamos el nombre sin extensión

            // INSERT MODIFICADO para guardar ambos campos
            String sql = "INSERT INTO audios (nombre_visible, ruta_archivo) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nombreVisible); // 1er parámetro: el nombre visible
                pstmt.setString(2, nuevaRuta);      // 2do parámetro: la ruta completa
                pstmt.executeUpdate();
                return "Archivo guardado como '" + nuevaRuta + "' y registrado.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error al procesar el archivo: " + e.getMessage();
        }
    }
}