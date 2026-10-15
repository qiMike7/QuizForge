package org.example.quizapp.dto.response;

import java.util.HashMap;
import java.util.Map;

public class ApiResponse {

    private boolean success;
    private String message;
    private Map<String, Object> data;

    public ApiResponse() {
        this.data = new HashMap<>();
    }

    public static ApiResponse success() {
        ApiResponse response = new ApiResponse();
        response.success = true;
        return response;
    }

    public static ApiResponse success(String message) {
        ApiResponse response = new ApiResponse();
        response.success = true;
        response.message = message;
        return response;
    }

    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    public ApiResponse addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", this.success);
        if (this.message != null) {
            result.put("message", this.message);
        }
        result.putAll(this.data);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
