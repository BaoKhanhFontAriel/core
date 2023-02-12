package vn.vnpay.receiver.utils;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import vn.vnpay.receiver.model.ApiResponse;

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

    public static String toJson(Object data){
        return instance.gson.toJson(data);
    }

    public static ApiResponse fromJson(String data){
        return instance.gson.fromJson(data, ApiResponse.class);
    }
}
