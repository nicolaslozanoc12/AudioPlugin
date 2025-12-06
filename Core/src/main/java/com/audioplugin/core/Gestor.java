package com.audioplugin.core;

import com.audioplugin.interfaces.IPlugin;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Gestor {
    private final List<IPlugin> plugins = new ArrayList<>();

    public void loadPlugin(String jarPath) {
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                System.err.println("El archivo JAR no existe: " + jarPath);
                return;
            }

            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});
            
            try (JarFile jar = new JarFile(jarFile)) {
                for (JarEntry entry : Collections.list(jar.entries())) {
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace('/', '.').replace(".class", "");
                        Class<?> clazz = classLoader.loadClass(className);
                        if (IPlugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            IPlugin plugin = (IPlugin) clazz.getDeclaredConstructor().newInstance();
                            plugins.add(plugin);
                            System.out.println("Plugin cargado: " + plugin.getName());
                            return; // Cargamos solo el primer plugin que encontremos en el JAR
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<IPlugin> getPlugins() {
        return plugins;
    }
    
    public void clearPlugins() {
    plugins.clear();
}
    public String executePlugin(String pluginName, Map<String, Object> params) {
        for (IPlugin plugin : plugins) {
            if (plugin.getName().equals(pluginName)) {
                return plugin.execute(params);
            }
        }
        return "Error: Plugin '" + pluginName + "' no encontrado.";
    }
}