package org.lexize.ulyanovsk;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class UlyanovskTranslation {
    HashMap<String, String> _fallbackTranslations = new HashMap<>();
    HashMap<String, String> _translations = new HashMap<>();
    public UlyanovskTranslation(File translationFile) throws Exception {
        String defaultTranslations;
        InputStream dtis = getClass().getResourceAsStream("/default_translation.yml");
        defaultTranslations = new String(dtis.readAllBytes());
        translationFile.getParentFile().mkdirs();
        dtis.close();
        if (!translationFile.exists()) {
            Ulyanovsk.Utils.writeFile(translationFile, defaultTranslations.getBytes(StandardCharsets.UTF_8));
        }
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.load(translationFile);
        for (String key :
                cfg.getKeys(false)) {
            if (!cfg.isString(key)) continue;
            _translations.put(key, cfg.getString(key));
        }

        cfg.loadFromString(defaultTranslations);
        for (String key :
                cfg.getKeys(false)) {
            if (!cfg.isString(key)) continue;
            _fallbackTranslations.put(key, cfg.getString(key));
        }
    }
    public String getTranslation(String key) {
        return _translations.getOrDefault(key, _fallbackTranslations.getOrDefault(key, key));
    }
}
