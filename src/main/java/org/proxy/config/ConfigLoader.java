package org.proxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class ConfigLoader {
    private static final String CONFIG_FILE = System.getenv().getOrDefault(
            "CONFIG_PATH", "config.local.yml"
    );

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public static AppConfig load() {
        try {
            return MAPPER.readValue(new File(CONFIG_FILE), AppConfig.class);
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException("Failed to parse YAML file", e);
        }
    }
}