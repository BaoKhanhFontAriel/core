package org.example.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Properties;

@Slf4j
public class AppConfig {
    public static final String TIME_SLEEP = "time.sleep";
    public static final String TIME_OUT = "time.out";

    private static final Properties p = new Properties();

    static {
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/application.properties"))) {
            // load a properties file
            p.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static int getTimeSleep() throws NumberFormatException{

        String timeSleep = p.getProperty(TIME_SLEEP);
        int time = -1;

        try {
             time = Integer.parseInt(timeSleep);
        }
        catch (NumberFormatException e){
            log.error("can not parse time.sleep from application.properties: ", e);
        }

        return time;
    }

    public static int getTimeOut() throws ParseException, RuntimeException {
        String timeOut = p.getProperty(TIME_OUT);
        int time = -1;

        try {
            time = Integer.parseInt(timeOut);
        }
        catch (NumberFormatException e){
            log.error("can not parse time.out from application.properties: ", e);
        }

        return time;
    }
}
