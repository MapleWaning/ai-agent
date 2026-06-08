package com.yupi.yuaiagent.model.dto;
import lombok.Data;

@Data
public class UserRegisterRequest {
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
