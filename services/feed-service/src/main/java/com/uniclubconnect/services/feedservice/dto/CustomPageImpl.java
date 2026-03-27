package com.uniclubconnect.services.feedservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CustomPageImpl<T> {
    private List<T> content;
}
