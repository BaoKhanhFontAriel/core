package org.example.model;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiResponse {
    private String resCode;
    private String message;
    private String data;
}
