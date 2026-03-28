package com.uniclubconnect.services.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageResponse<T> {

    // Spring Data'nın Page nesnesindeki veriler her zaman "content" adlı bir dizide gelir.
    private List<T> content;

}
