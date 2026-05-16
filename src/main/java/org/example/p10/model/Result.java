package org.example.p10.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "success", data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }
}