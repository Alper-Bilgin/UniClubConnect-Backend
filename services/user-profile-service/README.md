# User Profile Service (Kullanıcı Profil Servisi)

`user-profile-service`, UniClubConnect platformundaki kullanıcıların kişisel profillerini (ad, soyad, biyografi, bölüm, profil resmi vb.) oluşturmasını, güncellemesini ve diğer servisler tarafından sorgulanabilmesini sağlayan Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, mikroservisler arası asenkron tetikleme ve medya yönetimi barındıran **Katmanlı Mimari (Layered Architecture)** prensiplerine göre tasarlanmıştır.
- **Asenkron Profil Başlatma (Event Subscriber)**: Bir kullanıcı platforma kaydolduğunda (`auth-service` üzerinden), bu olay RabbitMQ (`user_exchange` -> `profile_user_created_queue`) aracılığıyla asenkron olarak bu servise iletilir. Tüketici sınıfı (`UserCreatedListener`), olaydaki verileri alarak veritabanında (`profile_schema` -> `user_profiles`) boş bir profil kaydı başlatır. Bu sayede kullanıcı kayıt süreci profil servisinin ayakta olma durumundan bağımsız olarak kesintisiz çalışır (asynchronous decoupling).
- **Medya Yönetimi (Object Storage)**: Kullanıcıların profil resimleri S3 uyumlu **MinIO** nesne deposunda (`uniclub-profiles` bucket) saklanır. Yüklenen dosyaların formatı (`jpeg`, `png`, `gif`) ve boş olup olmadığı servis içinde sıkı şekilde kontrol edilir.
- **Ortak Profil Sağlayıcı (Information Provider)**: Sistemdeki diğer mikroservisler (örn. `post-service`, `follow-service`, `notification-service`, `gamification-service`, `interaction-service`) kullanıcıların ham UUID bilgilerini ad/soyad ve profil resmine dönüştürmek için bu servisin `/user/{authId}` API uç noktasına bağımlıdır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, ortak PostgreSQL veritabanındaki isolated **`profile_schema`** şemasını kullanmaktadır.

### Tablolar ve Alanları
* **user_profiles**: `user_id` (PK, User Auth ID [UUID]), `first_name`, `last_name`, `bio`, `profile_picture_url` (MinIO dosya adı), `student_id`, `academic_email`, `department`, `phone_number`, `gender`, `is_private`, `updated_at`

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Servis, yeni profillerin asenkron oluşturulması için RabbitMQ olaylarını tüketir.

### 1. Tüketilen Olaylar (Consumed Events)
- **Kullanıcı Kayıt Olayı (`UserCreatedEvent`)**:
  - **Exchange**: `user_exchange`
  - **Queue**: `profile_user_created_queue`
  - **Routing Key**: `user.created.key`
  - **Payload DTO**: `id` (userId), `email`, `firstName`, `lastName`
  - **İşlem**: Kullanıcı için veritabanında ilk boş profil kaydını oluşturur.

---

## 🔌 Servis İletişimi (OpenFeign)
`user-profile-service` diğer mikroservislerin profil detaylarını çekebilmesi için senkron FeignClient arayüzü sağlar.

### 1. Sağlanan Endpoint'ler (Exposed to Feign)
- **Kullanıcı Bilgisi Sorgulama API'si (`GET /api/profiles/user/{authId}`)**:
  - **Tüketen Servisler**: `notification-service`, `post-service`, `interaction-service`, `follow-service`, `gamification-service` (Kullanıcı isimleri, resimleri ve bölüm detaylarını ham UUID'lerden çözmek için).

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9002` (Gateway yönlendirmesi: `/api/profiles/**`)
- **Veritabanı Şeması**: `profile_schema`
- **Merkezi Yapılandırma (Config Repo)**: `user-profile-service.yml`
- **Depolama S3/MinIO Yapılandırması**:
  - `minio.url`: `http://localhost:9000`
  - `minio.bucketName`: `uniclub-profiles`
  - `minio.accessKey` / `secretKey`: `minioadmin`

---

## 🛣️ API Endpoint'leri (Yolları)

### 👤 Kendi Profil İşlemleri (Giriş Yapmış Kullanıcılar)
| Yöntem | Endpoint | Parametreler (Form-Data / Body) | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profiles/me` | - | Oturum açan kullanıcının kendi detaylı profil bilgilerini döner. |
| `PUT` | `/api/profiles/me` | `UpdateProfileRequest` (JSON) | Kullanıcının adı, soyadı, biyografisi, telefon numarası vb. alanlarını günceller. |
| `POST` | `/api/profiles/me/image` | `file` (Multipart File) | Kullanıcının profil resmini günceller ve MinIO deposuna yükler. |

### 🔍 Diğer Profil Sorgulama İşlemleri (Public & Internal)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profiles/user/{authId}` | Herkese Açık / İç Feign | ID'si verilen herhangi bir kullanıcının profil bilgilerini döner. Hem web/mobil arayüz hem de iç FeignClient'lar tarafından ortak kullanılır. |
