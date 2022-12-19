package vn.vnpay.receiver.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class AppConfigSingleton {

    public static final String FILE_DIR = "src/main/resources/application.properties";
    private static final Logger log = LoggerFactory.getLogger(AppConfigSingleton.class);

    private static AppConfigSingleton instance;
    private static final Properties p = new Properties();

    public static AppConfigSingleton getInstance() {
        if (instance == null) {
            instance = new AppConfigSingleton();
            instance.readConfig();
        }
        return instance;
    }

    public String getStringProperty(String key) {
        return p.getProperty(key);
    }

    public int getIntProperty(String key) {
        int n = -1;

        try {
            String str = p.getProperty(key);
            n = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.error("can not parse string to integer: ", e);
        }
        return n;
    }

    public void readConfig() {
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(FILE_DIR);
            p.load(inputStream);
        } catch (FileNotFoundException e) {
            log.error("file not found: ", e);
        } catch (IOException e) {
            log.error("can not load file: ", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("can not close input stream: ", e);
            }
        }
    }
}
