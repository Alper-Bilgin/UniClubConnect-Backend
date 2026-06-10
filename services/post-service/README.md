# Post Service (Gönderi/Paylaşım Servisi)

`post-service`, UniClubConnect platformundaki kullanıcıların ve kulüplerin metin ve görsel içerikli gönderiler (post) paylaşmasını, güncellemesini, silmesini ve listelemesini yöneten Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, medya saklama yetenekleri ve asenkron olay yayını içeren **Katmanlı Mimari (Layered Architecture)** prensiplerine göre tasarlanmıştır.
- **Güvenlik ve Sahiplik Kontrolü**: Gönderiler oluşturulurken yazarın authId bilgisi JWT token'ından alınır. Gönderi güncelleme (`PUT`) ve silme (`DELETE`) işlemlerinde, işlemi yapan kullanıcının gönderinin asıl yazarı olup olmadığı veritabanı seviyesinde doğrulanır (`organizerAuthId` veya `authorId` kontrolü).
- **Medya Depolama (Object Storage)**: Gönderilere eklenen görseller S3 uyumlu **MinIO** nesne deposunda (`uniclubposts` bucket) saklanır. Dosya boyutları sunucu seviyesinde maksimum 10MB olarak sınırlandırılmıştır.
- **Asenkron Olay Yayını (RabbitMQ)**:
  - Yeni bir post oluşturulduğunda:
    1. `post.exchange` -> `post.created` routing key'i ile `feed-service` akış dağıtımını tetikler.
    2. `gamification.exchange` -> `gamification.event.post.created` routing key'i ile `gamification-service` ödül motorunu tetikler.
  - Bir post silindiğinde:
    1. `post.exchange` -> `post.deleted` routing key'i ile olay fırlatılır.
- **Toplu Sorgulama Arayüzü (Batch REST API)**: `feed-service` gibi akış servislerinin performansını artırmak amacıyla, gönderilen ID listesine karşılık gelen tüm gönderi detaylarını tek bir seferde dönen `/batch` uç noktası sunulmaktadır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`post_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **posts**: `id` (PK), `content`, `image_url` (MinIO dosya adı), `author_id` (User Auth ID), `created_at`, `updated_at`

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Gönderi durumlarını sisteme bildirmek amacıyla asenkron olaylar yayınlanır.

### 1. Yayınlanan Olaylar (Published Events)
- **Gönderi Oluşturuldu / Silindi (`PostEvent`)**:
  - **Exchange**: `post.exchange`
  - **Routing Keys**: `post.created` (Oluşturuldu), `post.deleted` (Silindi)
  - **Payload DTO**: `eventId`, `eventType`, `postId`, `authorId`
  - **Tüketen Servisler**: 
    - `feed-service`: Kullanıcıların akışlarını (timeline) güncellemek için.

- **Post Paylaşımı Ödülü (`GamificationEvent`)**:
  - **Exchange**: `gamification.exchange`
  - **Routing Key**: `gamification.event.post.created`
  - **Payload DTO**: `userId`, `eventType` (`POST_CREATED`), `referenceId` (Post ID), `timestamp`
  - **Tüketen Servisler**:
    - `gamification-service`: Kullanıcıya post paylaşmasından dolayı XP ödülü vermek ve ilgili rozetleri kontrol etmek için.

---

## 🔌 Servis İletişimi (OpenFeign)
Servis, yazar bilgilerini zenginleştirmek ve diğer servislere veri sağlamak için senkron FeignClient bağlantıları kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`ProfileServiceClient` (`user-profile-service` çağrılır)**:
  - **Uç Nokta (`GET /api/profiles/user/{authId}`)**: Gönderi listeleri dönerken, her gönderinin yazarının adını, soyadını ve profil resmini eklemek amacıyla çağrılır.

### 2. Sağlanan Endpoint'ler (Exposed to Feign)
- **Toplu Sorgulama API'si (`POST /api/posts/batch`)**:
  - **Tüketen Servisler**: `feed-service` (Kullanıcının Redis akışındaki post ID listesini tek tek sorgulamak yerine toplu halde çekmek için bu uç noktayı çağırır).

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9007` (Gateway yönlendirmesi: `/api/posts/**`)
- **Veritabanı Şeması**: `post_schema`
- **Merkezi Yapılandırma (Config Repo)**: `post-service.yml`
- **Dosya Boyutu Sınırları**:
  - `spring.servlet.multipart.max-file-size`: `10MB`
  - `spring.servlet.multipart.max-request-size`: `10MB`
- **Depolama S3/MinIO Yapılandırması**:
  - `minio.url`: `http://localhost:9000`
  - `minio.bucketName`: `uniclubposts`
  - `minio.accessKey` / `secretKey`: `minioadmin`

---

## 🛣️ API Endpoint'leri (Yolları)

### 📢 Genel Gönderi İşlemleri (Public & Internal)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/posts` | Herkese Açık | Sistemdeki tüm gönderileri en yeniye doğru listeler. |
| `GET` | `/api/posts/{postId}` | Herkese Açık | Belirtilen gönderinin detay bilgilerini getirir. |
| `GET` | `/api/posts/user/{userId}` | Herkese Açık | Sadece belirli bir kullanıcının veya kulübün paylaştığı gönderileri listeler. |
| `POST` | `/api/posts/batch` | **İç Servis (Feign)** | Gönderilen ID listesine ait tüm gönderi detaylarını toplu döner. |

### 📝 Gönderi Yazma ve Düzenleme (Giriş Yapmış Kullanıcılar)
*Not: Medya dosyası içerebilme durumundan dolayı oluşturma ve güncelleme işlemleri `multipart/form-data` formatındadır.*

| Yöntem | Endpoint | Parametreler (Form-Data) | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/posts` | `content` (Metin), `image` (Dosya, Opsiyonel) | Yeni bir gönderi paylaşır. Varsa görseli MinIO'ya kaydeder. RabbitMQ olaylarını fırlatır. |
| `PUT` | `/api/posts/{postId}` | `content` (Metin, Opsiyonel), `image` (Dosya, Opsiyonel) | *(Yalnızca Yazar)* Gönderinin içeriğini veya görselini günceller. |
| `DELETE` | `/api/posts/{postId}` | - | *(Yalnızca Yazar)* Gönderiyi tamamen siler, MinIO'dan görselini kaldırır ve RabbitMQ olayı fırlatır. |
