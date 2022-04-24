package com.example.duosentence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class ConfigPropUtil {

    private static final String CONFIG_FILE = "sentence.properties";
    private Properties prop;

    public ConfigPropUtil() {
        prop = new Properties();
        InputStream is;
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
            prop.load(new InputStreamReader(in, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object get(String key) {
        return prop.get(key);
    }
}