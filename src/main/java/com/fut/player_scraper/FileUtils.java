package com.fut.player_scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fut.desktop.app.restObjects.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

@Slf4j
public final class FileUtils {


    // Futbin URL
    public static final String futBin = "https://www.futbin.com/";

    //Futhead URL
    public static final String futHead = "http://www.futhead.com/";

    //Min Player price to find.
    public static final Long lowestPrice = 1000L;

    // Final json file name
    public static final String PLAYERS_JSON = ".players.json";

    // Data directory
    public static final String DATA_DIR = "data/";

    //EA Json
    private static final String EA_JSON = "M:/players.json";

    // Global delay factor
    public static final Long TIMEOUT = 2000L;

    /**
     * Load in EA JSON
     *
     * @return JsonArray of players from EA json
     * @throws Exception
     */
    public static JsonArray getAllPlayers() throws Exception {
        //LOAD IN EA JSON
        JsonReader reader = new JsonReader(new FileReader(EA_JSON));
        JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
        return jsonObject.getAsJsonArray("Players");
    }


    /**
     * Write player list to file.
     *
     * @param playerList List of players to save
     * @param leagueName Name of league
     */
    public static void writeFile(Set<Player> playerList, String leagueName) {
        String fileName = FileUtils.DATA_DIR + leagueName + FileUtils.PLAYERS_JSON;
        File file = new File(fileName);

        if (!file.exists()) {
            log.info("File doesn't not exist so creating one at: " + file.getAbsolutePath());
            try {
                boolean created = file.createNewFile();
                boolean writable = file.setWritable(true);

                log.info("Created? " + created + " & writable: " + writable);
            } catch (IOException e) {
                boolean readOnly = file.setReadOnly();
                log.info("Readonly:" + readOnly);
                log.error("Can't create file: " + e.getMessage());
            }
        }

        boolean writable = file.setWritable(true);
        log.info("Writable: " + writable);
        write(file, playerList);
    }

    /**
     * Helper to write to file.
     *
     * @param file       File to write to.
     * @param playerList object to write.
     */
    private static void write(File file, Object playerList) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, playerList);
        } catch (IOException e) {
            log.error("Can't process player list: " + e.getMessage());
        }
    }
}
