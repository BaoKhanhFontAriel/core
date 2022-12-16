package org.example.utils;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GsonSingleton {

    private static GsonSingleton instance;
    private Gson gson;

    public GsonSingleton() {
        this.gson = new Gson();
    }

    public static GsonSingleton getInstance() {
        if (instance == null){
            instance = new GsonSingleton();
        }
        return instance;
    }
}
