# Gamification Service (Oyunlaştırma Servisi)

`gamification-service`, UniClubConnect platformundaki kullanıcı aktivitelerini (giriş yapma, gönderi paylaşma, beğenme, yorum yapma, bilet alma vb.) ödüllendirerek kullanıcı bağlılığını ve etkileşimini artıran Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, esnek ve genişletilebilir ödüllendirme mekanizmaları sunmak amacıyla **Rules Engine (Kural Motoru)** tasarım deseni ve asenkron **Olay-Odaklı (Event-Driven)** tüketim üzerine kurulmuştur.
- **Kural Motoru Tasarım Deseni (Rules Engine)**: Serviste rozetlerin verilmesini yöneten esnek bir kural mimarisi bulunur. `BadgeRule` adında ortak bir arayüz bulunur ve her bir kural (örn: 7 günlük giriş serisi, 50 beğeni yapılması) bu arayüzü uygulayarak kendi iş mantığını işletir. Yeni bir rozet veya kural eklemek, sadece yeni bir rule sınıfı/bean'i tanımlayarak sisteme enjekte etmeyi gerektirir.
- **Asenkron Olay Tüketimi (RabbitMQ)**: Platform içindeki etkileşimler (giriş, beğeni, yorum, kulübe katılım, bilet alma vs.) doğrudan veritabanına yazılmakla kalmaz; asenkron olarak `gamification.exchange` üzerine `gamification.event.#` kalıbıyla fırlatılır. Oyunlaştırma servisi bu olayları kuyruktan (`gamification.events.queue`) sırayla çeker, puan motorundan (`GamificationEngine`) geçirerek kullanıcının XP'sini, seviyesini ve rozetlerini asenkron olarak günceller.
- **Günlük Giriş Serisi ve Hile Koruması (Daily Streak)**: Kullanıcının günlük giriş serileri takip edilir. Aynı gün içerisinde birden fazla kez giriş yapıldığında mükerrer puan kazanılması veritabanı düzeyindeki tarihsel kontroller ile engellenir.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`gamification_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **user_points**: `id` (PK), `user_id` (UK, Kullanıcı Auth ID), `total_points` (Toplam XP), `level` (Kullanıcı Seviyesi), `updated_at`
* **badges**: `id` (PK), `name` (UK, Örn: STREAK_7, LIKE_100), `description`, `icon_url`, `xp_reward` (Kazanılan Rozet XP Ödülü)
* **user_badges**: `id` (PK), `user_id` (FK), `badge_id` (FK), `earned_at`
* **daily_streaks**: `id` (PK), `user_id` (UK), `current_streak` (Mevcut Giriş Serisi), `longest_streak` (Tüm Zamanlar En Uzun Seri), `last_login_date`

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Servis, sistemdeki tüm etkileşimleri tek bir merkezi kuyruk üzerinden asenkron dinler.

### 1. Tüketilen Olaylar (Consumed Events)
- **Oyunlaştırma Olayları (`GamificationEvent`)**:
  - **Exchange**: `gamification.exchange`
  - **Queue**: `gamification.events.queue`
  - **Routing Key Pattern**: `gamification.event.#`
  - **Kabul Edilen Olay Türleri**:
    - `gamification.event.user.login` -> Günlük giriş serisini ve XP artışını tetikler.
    - `gamification.event.post.created` -> Post paylaşım rozeti kurallarını tetikler.
    - `gamification.event.like.created` / `like.removed` -> Beğeni rozeti kurallarını tetikler.
    - `gamification.event.comment.created` -> Yorum rozeti kurallarını tetikler.
    - `gamification.event.ticket.created` -> Etkinlik katılım rozeti kurallarını tetikler.
    - `user.joined.club.key` -> Kulübe katılım olayını ödüllendirir.

---

## 🔌 Servis İletişimi (OpenFeign)
Servis, veri bütünlüğünü tamamlamak amacıyla diğer servislerle senkron FeignClient bağlantısı kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`ProfileServiceClient` (`user-profile-service` çağrılır)**:
  - **Uç Nokta (`GET /api/profiles/user/{authId}`)**: Liderlik tablosu (`leaderboard`) listelenirken, kullanıcıların ham UUID'lerini ad, soyad ve profil resmi bilgilerine dönüştürmek amacıyla kullanılır.

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9011` (Gateway yönlendirmesi: `/api/gamification/**`)
- **Veritabanı Şeması**: `gamification_schema`
- **Merkezi Yapılandırma (Config Repo)**: `gamification-service.yml`
- **RabbitMQ Abone Yapılandırması**:
  - `gamification.rabbitmq.exchange`: `gamification.exchange`
  - `gamification.rabbitmq.queue`: `gamification.events.queue`
  - `gamification.rabbitmq.routing-key`: `gamification.event.#`

---

## 🛣️ API Endpoint'leri (Yolları)

### 🏆 Oyunlaştırma ve Profil Bilgileri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/gamification/{userId}/points` | Giriş Yapmış Kullanıcı | Kullanıcının toplam XP puanını ve seviyesini getirir. |
| `GET` | `/api/gamification/{userId}/badges` | Giriş Yapmış Kullanıcı | Kullanıcının kazandığı tüm rozetlerin listesini döner. |
| `GET` | `/api/gamification/{userId}/streak` | Giriş Yapmış Kullanıcı | Kullanıcının giriş serisi bilgilerini döner. |
| `GET` | `/api/gamification/{userId}/streak-info` | Giriş Yapmış Kullanıcı | Kullanıcının güncel serisi, en uzun serisi ve son giriş tarihini formatlı döner. |
| `GET` | `/api/gamification/{userId}/summary` | Giriş Yapmış Kullanıcı | **Özet Ekranı**: Puan, rozet ve streak bilgilerini tek istekte birleştirerek döner. |
| `GET` | `/api/gamification/leaderboard` | Giriş Yapmış Kullanıcı | En yüksek puana sahip ilk 10 kullanıcının liderlik sıralamasını getirir (FeignClient ile kullanıcı detayları eklenir). |
