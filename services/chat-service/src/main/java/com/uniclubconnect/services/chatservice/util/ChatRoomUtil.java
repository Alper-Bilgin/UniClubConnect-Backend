package com.uniclubconnect.services.chatservice.util;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomUtil {

    // Utility sınıflarının nesnesi oluşturulmasın diye private constructor
    private ChatRoomUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * İki kullanıcı arasındaki 1-1 sohbet odası ID'sini üretir.
     * Güvenlik ve veri bütünlüğü için trim, null, empty ve self-chat kontrollerini içerir.
     */
    public static String getChatRoomId(String userId1, String userId2) {

        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("Kullanıcı ID'leri null olamaz!");
        }

        // 1. Trim & Boşluk Temizliği
        userId1 = userId1.trim();
        userId2 = userId2.trim();

        if (userId1.isEmpty() || userId2.isEmpty()) {
            throw new IllegalArgumentException("Kullanıcı ID boş olamaz!");
        }

        // 2. Self-Chat Engeli (Kendi kendine konuşma bug'ını önler)
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Kullanıcı kendisiyle chat başlatamaz!");
        }

        // 3. Deterministic Sıralama (Kimin kime yazdığı fark etmeksizin aynı ID'yi üret)
        List<String> ids = new ArrayList<>();
        ids.add(userId1);
        ids.add(userId2);

        ids.sort(String::compareTo);

        return ids.get(0) + "_" + ids.get(1);
    }
}
