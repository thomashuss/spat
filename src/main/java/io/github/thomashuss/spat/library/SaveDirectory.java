package io.github.thomashuss.spat.library;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class SaveDirectory
{
    private static final String DB_NAME = "db";
    private static final String STATE_NAME = "state.json";
    @JsonIgnore
    File dbDir;
    @JsonIgnore
    private File stateFile;
    @JsonProperty("token")
    private Token token;
    @JsonProperty("mapSize")
    long mapSize = Library.INITIAL_MAP_SIZE;

    public static Library createNewLibrary(File directory, SpotifyClient client)
    {
        Path path = directory.toPath();
        File file = path.resolve(DB_NAME).toFile();
        if (file.exists() || file.mkdir()) {
            SaveDirectory state = new SaveDirectory();
            state.dbDir = file;
            state.stateFile = path.resolve(STATE_NAME).toFile();
            state.token = client.getToken();
            return new Library(state);
        } else
            return null;
    }

    public static Library loadData(File directory, SpotifyClient client)
    throws SaveFileException, IOException
    {
        if (!directory.isDirectory()) {
            throw new SaveFileException("The path `" + directory + "' is not a directory.");
        }
        Path path = directory.toPath();
        File dbDir = path.resolve(DB_NAME).toFile();
        if (!dbDir.exists()) {
            return null;
        }

        File stateFile = path.resolve(STATE_NAME).toFile();
        SaveDirectory state = stateFile.exists()
                ? Spat.mapper.readValue(stateFile, SaveDirectory.class) : new SaveDirectory();
        state.dbDir = dbDir;
        state.stateFile = stateFile;
        Token token = client.getToken();
        token.update(state.token);
        state.token = token;
        return new Library(state);
    }

    void saveData()
    throws IOException
    {
        Spat.mapper.writeValue(stateFile, this);
    }
}
