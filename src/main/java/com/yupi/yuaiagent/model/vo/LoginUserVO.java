package com.yupi.yuaiagent.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUserVO implements Serializable {

    private Integer userId;

    private String userName;

    private String role;
}
