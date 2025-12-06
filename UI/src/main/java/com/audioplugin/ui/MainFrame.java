package com.audioplugin.ui;

import com.audioplugin.interfaces.IPlugin;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private final MainController controller;
    private JButton btnCargarAudio, btnCargarComponentes, btnEjecutarPlugin;
    private JPanel panelFiltros;
    private ButtonGroup grupoFiltros;
    private JTextArea  areaSalidaMensajes;
    private JLabel labelArchivoActivo;
    private JPanel panelContenedorPlugin;

    public MainFrame() {
        setTitle("Aplicación de procesamiento de texto con Arquitectura Plugin");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        this.controller = new MainController(this);
        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        btnCargarAudio = new JButton("Grabar Nuevo Audio");
        btnCargarComponentes = new JButton("Cargar Plugins");
        btnEjecutarPlugin = new JButton("Ejecutar plugin seleccionado");
        panelFiltros = new JPanel();
        panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
        panelFiltros.setBorder(new TitledBorder("Plugins Cargados"));
        grupoFiltros = new ButtonGroup();
        labelArchivoActivo = new JLabel("Archivo activo: (ninguno)");
        labelArchivoActivo.setHorizontalAlignment(SwingConstants.CENTER);
        labelArchivoActivo.setBorder(new TitledBorder("Estado del Archivo"));
        areaSalidaMensajes = new JTextArea();
        areaSalidaMensajes.setEditable(false);
        panelContenedorPlugin = new JPanel(new BorderLayout());
        panelContenedorPlugin.setBorder(new TitledBorder("Interfaz del Plugin"));

        // --- Layout de la Izquierda ---
        gbc.gridx = 0; gbc.weightx = 0.4;
        gbc.gridwidth = 1;
        gbc.gridy = 0; gbc.weighty = 0; add(btnCargarAudio, gbc);
        gbc.gridy = 1; add(btnCargarComponentes, gbc);
        gbc.gridy = 2; add(labelArchivoActivo, gbc);
        gbc.gridy = 3; gbc.weighty = 0.6; add(new JScrollPane(panelFiltros), gbc);
        gbc.gridy = 4; gbc.weighty = 0; add(btnEjecutarPlugin, gbc);
        gbc.gridy = 5; gbc.weighty = 0.4; add(new JScrollPane(areaSalidaMensajes), gbc);
        ((JScrollPane) areaSalidaMensajes.getParent().getParent()).setBorder(new TitledBorder("Salida de Mensajes"));

        // --- Layout de la Derecha (Solo el lienzo) ---
        gbc.gridx = 1; gbc.weightx = 0.6;
        gbc.gridy = 0; gbc.gridheight = 5; gbc.weighty = 1; // Ocupa todo el alto
        add(panelContenedorPlugin, gbc);

        // --- Listeners ---
        btnCargarComponentes.addActionListener(e -> controller.cargarPlugins());
        btnCargarAudio.addActionListener(e -> controller.iniciarGrabacion());
        btnEjecutarPlugin.addActionListener(e -> controller.ejecutarPluginSeleccionado());
    }
    //  Método para que el controlador dibuje en el lienzo
    public void mostrarPanelPlugin(JPanel panelPlugin) {
        SwingUtilities.invokeLater(() -> {
            panelContenedorPlugin.removeAll();
            if (panelPlugin != null) {
                panelContenedorPlugin.add(panelPlugin, BorderLayout.CENTER);
            }
            panelContenedorPlugin.revalidate();
            panelContenedorPlugin.repaint();
        });
    }
    public void actualizarListaPlugins(List<IPlugin> plugins) {
        SwingUtilities.invokeLater(() -> {
            panelFiltros.removeAll();
            grupoFiltros = new ButtonGroup();
            for (IPlugin plugin : plugins) {
                JRadioButton radio = new JRadioButton(plugin.getName());
                radio.setActionCommand(plugin.getName());
                //Los radio buttons solo sirven para seleccionar.
                grupoFiltros.add(radio);
                panelFiltros.add(radio);
            }
            panelFiltros.revalidate();
            panelFiltros.repaint();
            // Limpiar el panel por si se recargan los plugins
            mostrarPanelPlugin(null);
        });
    }
    // Métodos públicos para el controlador
    public String getPluginSeleccionado() {
        if (grupoFiltros.getSelection() != null) {
            return grupoFiltros.getSelection().getActionCommand();
        }
        return null; // Devuelve null si no hay ninguno seleccionado
    }

    public void mostrarMensaje(String mensaje) {
        // Asegurarse de que las actualizaciones de la UI se hagan en el hilo de Swing
        SwingUtilities.invokeLater(() -> areaSalidaMensajes.setText(mensaje));
    }
    public void setArchivoActivo(String nombreArchivo) {
        SwingUtilities.invokeLater(() -> labelArchivoActivo.setText("Archivo activo: " + nombreArchivo));
    }
    public File mostrarSelectorDeArchivos(String descripcion, String... extensiones) {
        JFileChooser fileChooser = new JFileChooser();

        // Crea un filtro con la descripción y las extensiones que nos pasen
        if (extensiones != null && extensiones.length > 0) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(descripcion, extensiones);
            fileChooser.setFileFilter(filter);
        }

        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }


    public int preguntarNombrePersonalizado() {
        return JOptionPane.showConfirmDialog(this, "¿Deseas darle un nombre personalizado a la grabación?",
                "Nombre de la Grabación", JOptionPane.YES_NO_OPTION);
    }

    public String pedirNombrePersonalizado() {
        String nombre = JOptionPane.showInputDialog(this, "Introduce el nombre para la grabación (sin extensión .wav):",
                "Nombre Personalizado", JOptionPane.PLAIN_MESSAGE);
        if (nombre != null && !nombre.trim().isEmpty()) {
            return nombre.trim() + ".wav";
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}