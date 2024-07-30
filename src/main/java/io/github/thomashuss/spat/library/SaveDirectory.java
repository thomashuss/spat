package io.github.thomashuss.spat.library;

import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.client.SpotifyToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SaveDirectory
{
    private static final String DB_NAME = "db";
    private static final String TOKEN_NAME = "token";

    static {
        Spat.fury.register(SpotifyToken.class);
    }

    public static Library createNewLibrary(File directory)
    {
        File file = directory.toPath().resolve(DB_NAME).toFile();
        if (file.exists() || file.mkdir())
            return new Library(file);
        else
            return null;
    }

    public static Library loadData(File directory, SpotifyClient client)
    throws SaveFileException, IOException
    {
        if (!directory.isDirectory()) {
            throw new SaveFileException("The path `" + directory + "' is not a directory.");
        }
        Path path = directory.toPath();
        File dbFile = path.resolve(DB_NAME).toFile();
        if (!dbFile.exists()) {
            return null;
        }

        Path tokenPath = path.resolve(TOKEN_NAME);
        if (tokenPath.toFile().exists()) {
            Object token = Spat.fury.deserialize(Files.readAllBytes(tokenPath));
            if (token != null) client.setToken((SpotifyToken) token);
        }

        return new Library(dbFile);
    }

    public static void saveData(File directory, SpotifyClient client)
    throws SaveFileException, IOException
    {
        if (directory.exists()) {
            if (directory.isFile()) {
                throw new SaveFileException("The path `" + directory + "' is a file.");
            }
        } else {
            if (!directory.mkdir()) {
                throw new SaveFileException("Could not create directory `" + directory + "'.");
            }
        }

        Path path = directory.toPath();

        SpotifyToken token = client.getToken();
        if (token != null) {
            Files.write(path.resolve(TOKEN_NAME), Spat.fury.serialize(token), StandardOpenOption.CREATE);
        }
    }
}
