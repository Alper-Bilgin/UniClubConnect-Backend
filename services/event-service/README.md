# Event Service (Etkinlik Servisi)

`event-service`, UniClubConnect platformundaki kulüpler tarafından düzenlenen etkinliklerin (seminerler, festivaller, toplantılar vb.) oluşturulması, güncellenmesi, görsellerinin yüklenmesi ve detaylarının listelenmesini yöneten Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, mikroservisler arası senkron iletişim ve hızlı veri doğrulama mekanizmaları içeren **Katmanlı Mimari (Layered Architecture)** prensiplerine göre tasarlanmıştır.
- **Güvenlik Mimarisi**: Oturum açmış kullanıcının kimliği JWT ile doğrulanır. Etkinlik oluştururken kulüp sahipliği, güncellerken/silerken ise etkinlik sahipliği kontrolleri yapılır.
- **Redis ile Kontenjan Yönetimi (Caching)**: Etkinlik oluşturulduğu veya güncellendiği anda, etkinliğin maksimum bilet kontenjanı (quota) Redis sunucusunda `"event:<eventId>:quota"` anahtarı ile saklanır. Bilet alım işlemleri (`registration-service`), PostgreSQL'e gitmeden önce bu Redis anahtarı üzerinden atomik olarak kontenjanı sorgular ve düşürür.
- **Medya Yönetimi (Object Storage)**: Etkinlik afişleri ve kapak resimleri S3 uyumlu **MinIO** nesne deposunda saklanır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`event_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **events**: `id` (PK), `title`, `description`, `location`, `event_link`, `event_date_time`, `image_url` (MinIO resim adı), `total_quota`, `club_id`, `organizer_auth_id` (ROLE_CLUB_OWNER Id), `created_at`

- **Not**: Tabloda kulüp adı (`clubName`) saklanmaz, bunun yerine ilişkisel bütünlük amacıyla `club_id` tutulur. Get isteklerinde veriler birleştirilerek döner.

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
`event-service` doğrudan RabbitMQ üzerinden olay yayınlamaz veya tüketmez. Ancak, dolaylı olarak veriler `registration-service` gibi biletleme servisleri tarafından okunur.

---

## 🔌 Servis İletişimi (OpenFeign)
Servis, veri bütünlüğünü sağlamak amacıyla diğer servislerle senkron FeignClient bağlantıları kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`ClubServiceClient` (`club-service` çağrılır)**:
  - **Sahiplik Kontrolü (`GET /api/clubs/{clubId}/is-owner/{authId}`)**: Etkinlik oluşturulurken, oluşturan kulüp yöneticisinin gerçekten o kulübün sahibi olup olmadığını doğrulamak için çağrılır.
  - **Detay Getirme (`GET /api/clubs/{clubId}`)**: Etkinlik detayları listelenirken, response içerisine kulübün adını (`clubName`) dinamik olarak eklemek amacıyla çağrılır.

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9004` (Gateway yönlendirmesi: `/api/events/**`)
- **Veritabanı Şeması**: `event_schema`
- **Merkezi Yapılandırma (Config Repo)**: `event-service.yml`
- **Depolama S3/MinIO Yapılandırması**:
  - `minio.url`: `http://localhost:9000`
  - `minio.bucketName`: `uniclub-events`
  - `minio.accessKey` / `secretKey`: `minioadmin`
- **Önbellek (Redis)**: Kontenjanların saklanması için `localhost:6379` adresi üzerinden Redis bağlantısı kurulur.

---

## 🛣️ API Endpoint'leri (Yolları)

### 📢 Genel Etkinlik İşlemleri (Public)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/events` | Herkese Açık | Sistemdeki tüm aktif etkinlikleri listeler. |
| `GET` | `/api/events/{id}` | Herkese Açık | Belirtilen etkinliğin detay bilgilerini döner. (FeignClient ile kulüp adı eklenir). |
| `GET` | `/api/events/club/{clubId}` | Herkese Açık | Sadece belirli bir kulübün düzenlediği etkinlikleri listeler. |

### 🛠️ Etkinlik Yönetim İşlemleri (Club Owner Only)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/events` | `ROLE_CLUB_OWNER` | Yeni bir etkinlik kaydı oluşturur. `club-service` doğrulaması yapılır ve Redis kontenjanı set edilir. |
| `POST` | `/api/events/{id}/image` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinliğe kapak görseli/afiş yükler (Multipart File -> MinIO). |
| `PUT` | `/api/events/{id}` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinlik bilgilerini günceller. Kontenjan değişirse Redis'i günceller. |
| `DELETE` | `/api/events/{id}` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinliği tamamen siler ve Redis'teki kontenjan anahtarını temizler. |
