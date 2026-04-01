package com.uniclubconnect.services.gamificationservice.dto;

public enum EventType {
    USER_LOGIN,       // Günlük giriş serisi için
    POST_CREATED,     // İlk post, 5 post vb. rozetler için
    POST_LIKED,       // Beğeni rozetleri için
    EVENT_JOINED      // Etkinlik katılım rozetleri için
}
