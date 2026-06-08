# Post Service (Gönderi/Paylaşım Servisi)

`post-service`, UniClubConnect platformundaki kullanıcıların ve kulüplerin metin ve görsel içerikli gönderiler (post) paylaşmasını, güncellemesini, silmesini ve listelemesini yöneten Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Gönderi Paylaşma**: Metin ve opsiyonel olarak görsel içeren gönderilerin oluşturulması (`POST /api/posts`).
- **Medya Depolama (Object Storage)**: Gönderi görsellerinin MinIO nesne depolama sunucusunda güvenli şekilde saklanması.
- **Gönderi Yönetimi**: Kullanıcıların paylaştıkları gönderileri güncelleyebilmesi (`PUT`) veya silebilmesi (`DELETE`).
  - *Güvenlik Kontrolü*: Silme ve güncelleme işlemleri sadece gönderinin sahibi olan kullanıcılara kapatılmıştır.
- **Toplu Sorgulama (Batch API)**: `feed-service` gibi dış servislerin, kullanıcının akışındaki gönderileri tek tek sorgulamak yerine liste halindeki gönderi ID'lerini toplu olarak çekebilmesi (`POST /batch`).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Gönderi metinlerinin, görsellerinin yollarının ve yazar bilgilerinin saklanması)
- **MinIO S3 API SDK** (Gönderi resimlerinin nesne deposunda saklanması)
- **Spring Security** (Oturum sahibi kimliği doğrulaması ve veri sahipliği kontrolleri)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 📢 Genel Gönderi İşlemleri (Public & Internal)
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/posts` | Platformdaki tüm gönderileri kronolojik listeler. |
| `GET` | `/api/posts/{postId}` | Belirtilen gönderinin detay bilgilerini getirir. |
| `GET` | `/api/posts/user/{userId}` | Sadece belirli bir kullanıcının veya kulübün paylaştığı gönderileri listeler. |
| `POST` | `/api/posts/batch` | **[İç Servis/Feign]** Gönderilen ID listesine ait tüm gönderi detaylarını toplu döner. |

### 📝 Gönderi Yazma ve Düzenleme (Giriş Yapmış Kullanıcılar)
*Not: Gönderi oluşturma ve güncelleme işlemleri medya dosyası içerebileceği için `multipart/form-data` formatında kabul edilir.*

| Yöntem | Endpoint | Parametreler (Form-Data) | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/posts` | `content` (text), `image` (file, opsiyonel) | Yeni bir gönderi paylaşır. Görseli varsa MinIO'ya yükler. |
| `PUT` | `/api/posts/{postId}` | `content` (text, opsiyonel), `image` (file, opsiyonel) | *(Yalnızca Yazar)* Gönderinin metnini veya görselini günceller. |
| `DELETE` | `/api/posts/{postId}` | - | *(Yalnızca Yazar)* Belirtilen gönderiyi sistemden tamamen siler. |
