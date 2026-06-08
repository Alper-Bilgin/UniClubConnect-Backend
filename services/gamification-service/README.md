# Gamification Service (Oyunlaştırma Servisi)

`gamification-service`, UniClubConnect platformundaki kullanıcı aktivitelerini (giriş yapma, gönderi oluşturma, beğenme, yorum yapma vb.) ödüllendirerek kullanıcı bağlılığını ve etkileşimi artıran Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Puan ve Seviye Takibi (XP & Level)**: Kullanıcıların yaptıkları her etkileşim için standart +10 XP kazanması ve her 100 XP'de bir seviye (Level) atlaması.
- **Kural Motoru ve Rozetler (Rules Engine & Badges)**: Çeşitli kurallara göre kullanıcılara özel rozetler tanımlanması ve bu rozetleri kazanan kullanıcılara ekstra XP ödülleri verilmesi.
  - *Kurallar*: `FirstLoginBadgeRule`, `FirstPostBadgeRule`, `FivePostsBadgeRule` vb.
- **Günlük Giriş Serisi (Daily Streak)**: Kullanıcıların ardışık günlerde sisteme giriş yapma durumlarının izlenmesi, en uzun serinin (longest streak) tutulması.
  - *Hile Koruması*: Aynı gün içinde birden fazla kez giriş yapıldığında mükerrer puan kazanılması engellenir.
- **Liderlik Tablosu (Leaderboard)**: En yüksek puana sahip ilk 10 kullanıcının listelendiği genel bir sıralama tablosu sunar.
- **Olay Odaklı Mimari (Event-Driven)**: RabbitMQ üzerinden asenkron olarak fırlatılan `GamificationEvent` olaylarını dinler ve puanlama motorunu tetikler.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Kullanıcı puanları, kazanılan rozetler ve günlük serilerin kalıcı tutulması)
- **RabbitMQ Listener** (Etkileşim servislerinden gönderilen olayları dinlemek için)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 🏆 Oyunlaştırma ve Profil Bilgileri
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/gamification/{userId}/points` | Belirtilen kullanıcının toplam XP ve güncel seviye bilgilerini getirir. |
| `GET` | `/api/gamification/{userId}/badges` | Belirtilen kullanıcının kazandığı tüm rozetlerin listesini döner. |
| `GET` | `/api/gamification/{userId}/streak` | Kullanıcının ham günlük seri (streak) verilerini getirir. |
| `GET` | `/api/gamification/{userId}/streak-info` | Kullanıcının güncel serisi, en uzun serisi ve son giriş tarihi bilgilerini formatlı döner. |
| `GET` | `/api/gamification/{userId}/summary` | Özet Ekranı: Kullanıcının puan, rozet ve streak durumlarını tek bir istekte birleşik döner. |
| `GET` | `/api/gamification/leaderboard` | Puanlarına göre en yüksek ilk 10 kullanıcının liderlik tablosunu listeler. |
