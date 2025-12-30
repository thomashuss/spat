package io.github.thomashuss.spat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;

import java.util.prefs.Preferences;

public class Spat
{
    public static final String PROGRAM_NAME = "spat";
    public static final String P_CLIENT_ID = "clientId";
    public static final String P_FILE_PATH = "dataPath";
    public static final String P_OPEN_IN_SPOTIFY = "shouldOpenInSpotifyClient";
    public static final String P_REDIRECT_URI = "redirectUri";
    public static final String P_PUSH_COOLDOWN = "pushCooldown";
    public static final Preferences preferences = Preferences.userNodeForPackage(Spat.class);
    public static final Fury fury = Fury.builder().withLanguage(Language.JAVA)
            .requireClassRegistration(true)
            .build();
    public static final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();
    public static final CsvMapper csvMapper = CsvMapper.builder().findAndAddModules().build();
}
