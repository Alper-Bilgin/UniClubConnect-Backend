# Interaction Service (Etkileşim Servisi)

`interaction-service`, platformdaki gönderiler (post) ve etkinlikler (event) üzerindeki beğenme (like) ve yorum yapma (comment) gibi sosyal etkileşimleri yöneten Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, dinamik hedef yapısı ve asenkron ödüllendirme tetikleri barındıran **Katmanlı Mimari (Layered Architecture)** prensiplerine göre tasarlanmıştır.
- **Dinamik Polimorfik Hedef Yapısı**: Beğeniler ve yorumlar tekil olarak doğrudan tablolara bağlanmaz. `ETargetType` enum yapısı (örn: `POST`, `EVENT`) ve `targetId` ikilisi kullanılarak dinamik olarak eşleştirilir. Bu polimorfik tasarım sayesinde, ilerleyen aşamalarda sisteme yeni bir etkileşim hedefi (örn. `CLUB_PAGE` veya `STORY`) eklendiğinde veri yapısını bozmadan kolayca genişletilebilirlik sağlanır.
- **Senkron İç Hata Doğrulama (Feign Barrier)**: Bir beğeni veya yorum eklenmeden önce, hedef içeriğin sistemde var olup olmadığı `PostServiceClient` veya `EventServiceClient` üzerinden senkron olarak sorgulanır (`validateTargetExists`). Eğer hedef bulunamazsa etkileşim işlemi iptal edilerek veritabanı tutarlılığı korunur.
- **Asenkron Oyunlaştırma Tetikleyicisi**: Kullanıcılar her yorum yaptığında veya beğeni eklediğinde (beğeni geri çekildiğinde hariç), RabbitMQ üzerinden asenkron olarak `GamificationEvent` fırlatılarak kullanıcının anında XP kazanması ve rozet kurallarının çalıştırılması sağlanır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`interaction_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **likes**: `id` (PK), `user_id` (Liker Auth ID), `target_id` (Post or Event ID), `target_type` (`POST`, `EVENT`), `liked_at`
* **comments**: `id` (PK), `user_id` (Author Auth ID), `content`, `target_id` (Post or Event ID), `target_type` (`POST`, `EVENT`), `created_at`

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Kullanıcı etkileşimlerini ödüllendirmek amacıyla asenkron olaylar yayınlanır.

### 1. Yayınlanan Olaylar (Published Events)
- **Etkileşim Olayı (`GamificationEvent`)**:
  - **Exchange**: `gamification.exchange` (Varsayılan)
  - **Routing Key**: `gamification.event.interaction` (Varsayılan)
  - **Payload DTO**: `userId`, `eventType` (`COMMENT_ADDED`, `POST_LIKED`), `referenceId` (Yorum ID veya Post ID), `timestamp`
  - **Tüketen Servisler**:
    - `gamification-service`: Kullanıcıya yaptığı etkileşimlerden dolayı XP ödülü vermek ve rozet kurallarını işletmek için.

---

## 🔌 Servis İletişimi (OpenFeign)
Servis, veri doğruluğu ve DTO zenginleştirme amacıyla diğer servislerle senkron FeignClient bağlantıları kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`PostServiceClient` (`post-service` çağrılır)**:
  - **Uç Nokta (`GET /api/posts/{postId}`)**: Gönderilen etkileşim hedefi `POST` olduğunda, postun silinmediğini doğrulamak için çağrılır.
- **`EventServiceClient` (`event-service` çağrılır)**:
  - **Uç Nokta (`GET /api/events/{id}`)**: Gönderilen etkileşim hedefi `EVENT` olduğunda, etkinliğin aktif olduğunu doğrulamak için çağrılır.
- **`ProfileServiceClient` (`user-profile-service` çağrılır)**:
  - **Uç Nokta (`GET /api/profiles/user/{authId}`)**: Yorumlar listelenirken, yorumu yazan kullanıcının ham authId bilgisini ad, soyad ve profil resmi gibi görünüm detaylarına dönüştürmek amacıyla çağrılır.

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9008` (Gateway yönlendirmesi: `/api/interactions/**`)
- **Veritabanı Şeması**: `interaction_schema`
- **Merkezi Yapılandırma (Config Repo)**: `interaction-service.yml`
- **Olay Yayınlama Parametreleri**:
  - `gamification.rabbitmq.exchange`: `gamification.exchange`
  - `gamification.rabbitmq.routing-key`: `gamification.event.interaction`

---

## 🛣️ API Endpoint'leri (Yolları)

### 💬 Yorum İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/interactions/comments` | Giriş Yapmış Kullanıcı | Hedef içerik için yeni bir yorum oluşturur ve `GamificationEvent` tetikler. (Body: `content`, `targetId`, `targetType` [`POST` / `EVENT`]). |
| `GET` | `/api/interactions/comments/{targetType}/{targetId}` | Herkese Açık | Belirtilen hedefe ait tüm yorumları kronolojik listeler. `user-profile-service` FeignClient ile yazar bilgileri eklenir. |
| `DELETE` | `/api/interactions/comments/{commentId}` | Giriş Yapmış Kullanıcı | Yalnızca yorumun sahibi tarafından çağrılabilir, yorumu siler. |

### 💖 Beğeni İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/interactions/likes` | Giriş Yapmış Kullanıcı | **Toggle Beğeni**: Gönderilen hedef içerik beğenilmemişse beğenir ve `GamificationEvent` fırlatır. Zaten beğenilmişse beğeniyi kaldırır. (Body: `targetId`, `targetType`). |
| `GET` | `/api/interactions/likes/{targetType}/{targetId}/count` | Herkese Açık | Belirtilen hedefin toplam beğeni sayısını döner. |
| `GET` | `/api/interactions/likes/{targetType}/{targetId}/status` | Giriş Yapmış Kullanıcı | Oturum açan kullanıcının o içeriği beğenip beğenmediğini döner (`true` / `false`). |
