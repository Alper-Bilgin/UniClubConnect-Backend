# User Profile Service (Kullanıcı Profil Servisi)

`user-profile-service`, UniClubConnect platformundaki kullanıcıların kişisel profillerini (ad, soyad, biyografi, profil resmi vb.) oluşturmasını, güncellemesini ve diğer servisler tarafından sorgulanabilmesini sağlayan Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Kişisel Profil Sorgulama**: Giriş yapmış kullanıcının kendi profil bilgilerini görüntülemesi (`GET /me`).
- **Profil Güncelleme**: Kullanıcının adı, soyadı, biyografisi gibi kişisel alanları düzenleyebilmesi (`PUT /me`).
- **Profil Resmi Yükleme**: Kullanıcının profil resmini güncelleyebilmesi (`POST /me/image`).
  - *Resim Kontrolü*: Resmin boş olup olmadığı ve sadece izin verilen dosya uzantılarına (`jpeg`, `png`, `gif`) sahip olup olmadığı kontrol edilir.
  - *Depolama*: Yüklenen resim dosyası MinIO nesne depolama sunucusunda saklanır.
- **Profil Arama ve Çözümleme**: Diğer mikroservislerin (örn. `post-service`, `chat-service`, `notification-service`) kullanıcı ID'lerini kullanıcı adı, soyadı ve e-posta bilgilerine dönüştürebilmesi için profil detaylarını sorgulayabilmesi (`GET /user/{authId}`).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Profil verilerinin ve profil resim yollarının saklanması)
- **MinIO S3 API SDK** (Kullanıcı profil resimlerinin saklanması)
- **Spring Security** (Kullanıcı kimliğinin token'dan okunması ve doğrulanması)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 👤 Kendi Profil İşlemleri (Giriş Yapmış Kullanıcılar)
| Yöntem | Endpoint | Parametreler (Form-Data / Body) | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profiles/me` | - | Mevcut kullanıcının kendi profil bilgilerini döner. |
| `PUT` | `/api/profiles/me` | `UpdateProfileRequest` (JSON) | Kullanıcının adı, soyadı ve biyografisini günceller. |
| `POST` | `/api/profiles/me/image` | `file` (Multipart File) | Profil resmini günceller ve MinIO sunucusuna kaydeder. |

### 🔍 Diğer Profil Sorgulama İşlemleri (Public & Internal)
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/profiles/user/{authId}` | ID'si verilen herhangi bir kullanıcının profil bilgilerini döner. *(Hem web/mobil görünüm hem de iç servis Feign Client'lar tarafından kullanılır)* |
