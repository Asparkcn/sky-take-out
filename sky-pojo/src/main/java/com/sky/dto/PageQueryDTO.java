package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PageQueryDTO implements Serializable {

    private String name;
    private Integer categoryId;
    private Integer type;
    private Integer page;
    private Integer pageSize;
    private Integer status;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private String number;
    private String phone;
}
