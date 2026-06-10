# Follow Service (Takip ve İlişki Yönetim Servisi)

`follow-service`, UniClubConnect platformundaki kullanıcıların birbirlerini takip etmesini, takipçi/takip edilen listelerini ve gizli profil onay süreçlerini yöneten Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, ilişkisel veritabanı (PostgreSQL) ve hızlı okuma önbelleği (Redis) katmanlarının entegre olduğu **Katmanlı Mimari (Layered Architecture)** prensiplerine göre tasarlanmıştır.
- **Gizlilik ve İstek Yönetimi**: Kullanıcılar kendi gizlilik durumlarını (`isPrivate`) ayarlayabilir. Profil gizli ise takip işlemi `PENDING` durumunda bir talep olarak kaydedilir ve onay bekler. Profil açık ise doğrudan `ACCEPTED` durumunda takip ilişkisi kurulur.
- **Redis İstatistik Önbelleği (Caching)**: Kullanıcıların takipçi ve takip edilen sayıları gibi sıkça sorgulanan istatistik verileri Redis üzerinde sırasıyla `"user:follower_count:<userId>"` ve `"user:following_count:<userId>"` anahtarlarında saklanır. Bir takip ilişkisi değiştiğinde (takip etme, bırakma, kabul edilme veya çıkarılma), bu Redis sayaç anahtarları otomatik olarak silinir (cache invalidation) ve bir sonraki okumada güncellenir.
- **Ortak Takipçi Tabanlı Keşif/Öneri (Recommendation Engine)**: Kullanıcılara henüz takip etmedikleri ancak ortak takipçilerinin bulunduğu popüler kişileri önermek için SQL düzeyinde bir öneri sorgusu (`getRecommendations`) çalıştırılır ve sonuçlar FeignClient ile profillere dönüştürülür.
- **Asenkron Bildirim Tetikleme**: Takip istekleri, kabulleri veya silinmelerinde RabbitMQ üzerinden olaylar fırlatılarak e-posta servisinin asenkron çalışması sağlanır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`follow_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **follows**: `id` (PK), `follower_id` (User Id who follows), `following_id` (User Id being followed), `status` (`ACCEPTED`, `PENDING`), `created_at`
* **follow_settings**: `user_id` (PK), `is_private`, `updated_at`

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Takip olaylarını bildirmek amacıyla RabbitMQ üzerinden asenkron olaylar yayınlanır.

### 1. Yayınlanan Olaylar (Published Events)
- **Takip Olayı (`FollowEvent`)**:
  - **Exchange**: `follow.exchange`
  - **Routing Key**: `follow.created`
  - **Payload DTO**: `eventId`, `followerId`, `followingId`, `type` (`FOLLOW_CREATED`, `FOLLOW_REQUESTED`, `FOLLOW_ACCEPTED`, `FOLLOW_REJECTED`, `FOLLOW_REMOVED`), `timestamp`
  - **Tüketen Servisler**:
    - `notification-service`: Takip isteklerinde ve onaylarında kullanıcılara bilgilendirme e-postası göndermek için.

---

## 🔌 Servis İletişimi (OpenFeign)
Servis, DTO nesnelerindeki isim ve profil resmi gibi eksik alanları tamamlamak için diğer servislerle senkron FeignClient bağlantıları kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`ProfileServiceClient` (`user-profile-service` çağrılır)**:
  - **Uç Nokta (`GET /api/profiles/user/{authId}`)**: Takipçi veya takip edilenlerin listesi ya da önerilen kullanıcılar dönerken, authId'leri kullanıcıların adı, soyadı, profil resmi ve bölüm adı (department) gibi profil bilgilerine dönüştürmek amacıyla çağrılır.

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9009` (Gateway yönlendirmesi: `/api/follows/**`)
- **Veritabanı Şeması**: `follow_schema`
- **Merkezi Yapılandırma (Config Repo)**: `follow-service.yml`
- **RabbitMQ Yapılandırması**:
  - `follow.exchange`: `follow.exchange`
  - `follow.routing-key`: `follow.created`
- **Önbellek (Redis)**: İstatistik sayaçları için `localhost:6379` adresi üzerinden Redis bağlantısı kurulur.

---

## 🛣️ API Endpoint'leri (Yolları)

### 👥 Takip İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/follows/{targetId}` | Giriş Yapmış Kullanıcı | Belirtilen kullanıcıyı takip eder (hedef profil gizliyse takip isteği gönderir). |
| `DELETE` | `/api/follows/{targetId}` | Giriş Yapmış Kullanıcı | Belirtilen kullanıcıyı takipten çıkarır. |
| `DELETE` | `/api/follows/followers/{followerId}` | Giriş Yapmış Kullanıcı | Kendi takipçilerinden birini takipçi listesinden çıkartır. |
| `GET` | `/api/follows/status/{targetId}` | Giriş Yapmış Kullanıcı | Hedef kullanıcı ile olan güncel takip ilişkisini döner (`FOLLOWING`, `PENDING`, `NONE`). |
| `GET` | `/api/follows/{userId}/counts` | Herkese Açık | Belirtilen kullanıcının takipçi ve takip edilen toplam sayılarını döner (Redis'ten veya DB'den). |
| `GET` | `/api/follows/{userId}/followers` | Herkese Açık | Belirtilen kullanıcının onaylı takipçilerini sayfalanmış listeler (`?page=0&size=20`). |
| `GET` | `/api/follows/{userId}/following` | Herkese Açık | Belirtilen kullanıcının takip ettiği kişileri sayfalanmış listeler (`?page=0&size=20`). |

### 🔒 Gizli Profil ve Takip İstekleri Yönetimi
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/follows/requests` | Giriş Yapmış Kullanıcı | Kendi profiline gelen ve onay bekleyen beklemedeki takip isteklerini listeler. |
| `PUT` | `/api/follows/requests/{followerId}/accept` | Giriş Yapmış Kullanıcı | Gelen bir takip isteğini onaylar ve Redis sayaçlarını temizler. |
| `DELETE` | `/api/follows/requests/{followerId}/reject` | Giriş Yapmış Kullanıcı | Gelen bir takip isteğini reddeder ve siler. |
| `PUT` | `/api/follows/settings/privacy` | Giriş Yapmış Kullanıcı | Profil gizlilik durumunu günceller (`?isPrivate=true/false`). |
| `GET` | `/api/follows/settings/privacy` | Giriş Yapmış Kullanıcı | Giriş yapmış kullanıcının profil gizlilik durumunu döner. |
| `GET` | `/api/follows/{userId}/privacy-status` | Herkese Açık | Belirtilen kullanıcının profilinin gizli olup olmadığını kontrol eder. |

### 💡 Keşif ve Öneriler
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/follows/recommendations` | Giriş Yapmış Kullanıcı | Ortak arkadaş sayısına göre kullanıcı için takip edebileceği diğer kişileri önerir (`?limit=5`). |
