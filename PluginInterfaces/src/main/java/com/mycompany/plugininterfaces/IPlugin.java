package com.audioplugin.interfaces;

import com.mycompany.plugininterfaces.IController;

import javax.swing.*;
import java.util.Map;

public interface IPlugin {

    String getName();
    // Cada plugin recibe parámetros y devuelve un resultado en forma de texto.
    String execute(Map<String, Object> parameters);
    //Devuelve un panel de Swing (JPanel) con la UI específica para este plugin. Si no necesita UI, puede devolver null.
    default JPanel getPanel(IController controller) {
        return null;
    }
}