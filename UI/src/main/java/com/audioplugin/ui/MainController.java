package com.audioplugin.ui;

import com.audioplugin.core.Gestor;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.*;
import com.audioplugin.interfaces.IPlugin;
import com.mycompany.plugininterfaces.IController;

public class MainController implements IController {
    private String rutaAudioActivo = "grabacion.wav";
    private final Gestor gestor;
    private final MainFrame view;
    public MainController(MainFrame view) {
        this.view = view;
        this.gestor = new Gestor();
        view.setArchivoActivo("grabacion.wav");
    }

    public void cargarPlugins() {
        gestor.clearPlugins();
        File pluginDir = new File("plugins");
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            view.mostrarMensaje("Carpeta 'plugins' no encontrada. Creando...");
            pluginDir.mkdir();
            return;
        }

        File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                gestor.loadPlugin(file.getAbsolutePath());
            }
        }
        view.actualizarListaPlugins(gestor.getPlugins());
    }
    public void ejecutarPluginSeleccionado() {String nombrePlugin = view.getPluginSeleccionado();
        if (nombrePlugin == null) {
            view.mostrarMensaje("Por favor, selecciona un plugin de la lista.");
            return;
        }

        IPlugin pluginActivo = gestor.getPlugins().stream()
                .filter(p -> p.getName().equals(nombrePlugin))
                .findFirst()
                .orElse(null);

        if (pluginActivo != null) {
            // Intentamos obtener un panel del plugin
            JPanel panelUI = pluginActivo.getPanel(this);

            if (panelUI != null) {
                // --- CASO 1: El plugin TIENE interfaz ---
                // Si devuelve un panel, lo mostramos en el lienzo.
                view.mostrarPanelPlugin(panelUI);
                view.mostrarMensaje("Interfaz del plugin '" + nombrePlugin + "' cargada.");
            } else {
                // Si el plugin NO tiene interfaz, es de acción directa
                Map<String, Object> params = new HashMap<>();
                params.put("rutaArchivo", getRutaAudioActivo());
                //  El controlador se pasa a sí mismo como parámetro
                params.put("controller", this);

                String resultado = ejecutarPlugin(pluginActivo.getName(), params);
                view.mostrarMensaje(resultado);
            }
        }
    }


    @Override  //Logica de ejecucion, los plug-in llamaran a este metodo desde su propia UI
    public String ejecutarPlugin(String nombrePlugin, Map<String, Object> params) {
        return gestor.executePlugin(nombrePlugin, params);
    }

    //Metodos Auxiliares
    @Override
    public void mostrarMensaje(String mensaje) {
        view.mostrarMensaje(mensaje);
    }
    @Override
    public String getRutaAudioActivo() {
        return rutaAudioActivo;
    }
    @Override
    public void setArchivoActivo(String rutaCompleta, String nombreVisible) {
        this.rutaAudioActivo = rutaCompleta;
        view.setArchivoActivo(nombreVisible); // Le pasamos solo el nombre a la vista
    }
    @Override
    public File solicitarArchivo(String descripcionFiltro, String... extensiones) {
        return view.mostrarSelectorDeArchivos(descripcionFiltro, extensiones);
    }
    public void iniciarGrabacion() {
        Mixer.Info[] microphoneOptions = getAvailableMicrophones();
        if (microphoneOptions.length == 0) {
            view.mostrarMensaje("No se encontraron micrófonos disponibles.");
            return;
        }

        String[] micNames = new String[microphoneOptions.length];
        for (int i = 0; i < microphoneOptions.length; i++) {
            micNames[i] = microphoneOptions[i].getName();
        }

        String selectedMicName = (String) JOptionPane.showInputDialog(
                view, "Selecciona un dispositivo de entrada:", "Selector de Micrófono",
                JOptionPane.QUESTION_MESSAGE, null, micNames, micNames[0]);

        if (selectedMicName == null) return;

        Mixer.Info selectedMixerInfo = null;
        for (Mixer.Info micInfo : microphoneOptions) {
            if (micInfo.getName().equals(selectedMicName)) {
                selectedMixerInfo = micInfo;
                break;
            }
        }

        if (selectedMixerInfo == null) {
            view.mostrarMensaje("Error: No se pudo encontrar el micrófono seleccionado.");
            return;
        }

        final Mixer.Info finalSelectedMixerInfo = selectedMixerInfo;

        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);

                Mixer mixer = AudioSystem.getMixer(finalSelectedMixerInfo);
                final TargetDataLine microphone = (TargetDataLine) mixer.getLine(dataLineInfo);

                microphone.open(format);
                microphone.start();
                view.mostrarMensaje("Grabando con '" + finalSelectedMixerInfo.getName() + "'... Cierra el siguiente diálogo para detener.");

                Thread recorderThread = new Thread(() -> {
                    try {
                        AudioSystem.write(new AudioInputStream(microphone), AudioFileFormat.Type.WAVE, new File("grabacion.wav"));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                recorderThread.start();

                JOptionPane.showMessageDialog(view, "Grabando. Cierra esta ventana para detener.");

                microphone.stop();
                microphone.close();
                view.mostrarMensaje("Grabación guardada en 'grabacion.wav'");
                // Restablecer el archivo activo a la nueva grabación
                this.rutaAudioActivo = "grabacion.wav";
                view.setArchivoActivo("grabacion.wav");

            } catch (LineUnavailableException e) {
                e.printStackTrace();
                view.mostrarMensaje("Error: No se pudo acceder al micrófono.");
            }
        }).start();
    }

    private Mixer.Info[] getAvailableMicrophones() {
        ArrayList<Mixer.Info> availableMixers = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, null))) {
                availableMixers.add(mixerInfo);
            }
        }
        return availableMixers.toArray(new Mixer.Info[0]);
    }
}