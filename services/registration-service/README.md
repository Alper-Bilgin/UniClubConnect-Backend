# Registration Service (Etkinlik Kayıt ve Bilet Servisi)

`registration-service`, UniClubConnect platformundaki etkinliklere kayıt olma, bilet tanımlama, bilet iptalleri ve etkinlik girişlerinde bilet kodlarının/QR kodlarının doğrulanması süreçlerini yöneten Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Etkinliğe Kayıt Olma & Bilet Basımı**: Kullanıcıların bir etkinliğe kaydolması (`POST /{eventId}`).
  - Kontenjan (quota) kontrolü ve mükerrer kayıt engellemesi yapılır.
  - Kayıt onaylandığında benzersiz bir bilet kodu (ticket code) üretilir ve RabbitMQ üzerinden bilet e-postası tetiklemek için `ticket-email` kuyruğuna event fırlatılır.
- **Kişisel Biletler**: Kullanıcının geçmiş ve gelecek tüm biletlerini listelemesi (`/my-tickets`) ve gerektiğinde biletini iptal edebilmesi (`DELETE /{ticketCode}`).
- **Katılımcı Yönetimi**: Kulüp yöneticilerinin, kendi düzenledikleri etkinliklere kayıtlı olan katılımcı listelerini inceleyebilmesi (`/event/{eventId}`).
- **QR Kod / Bilet Doğrulama**: Etkinlik günü kapıda bilet kontrolü yapmak amacıyla mobil uygulama veya web paneli üzerinden bilet kodlarının geçerliliğinin anlık sorgulanması (`/validate/{ticketCode}`).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Bilet kayıtlarının, durumlarının ve kullanıcı e-posta eşleşmelerinin saklanması)
- **RabbitMQ** (Bilet onaylarında QR kod görseli oluşturmak ve PDF bilet yollamak üzere `notification-service`e event gönderimi)
- **Spring Security** (Rol bazlı yetkilendirme ve bilet iptal/görüntüleme sahiplik kontrolleri)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 🎫 Kullanıcı Bilet İşlemleri (User)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/registrations/{eventId}` | `ROLE_USER` | Belirtilen etkinliğe kayıt olur ve yeni bir bilet/bilet kodu oluşturur. |
| `GET` | `/api/registrations/my-tickets` | `ROLE_USER` | Giriş yapmış kullanıcının sahip olduğu tüm aktif biletleri listeler. |
| `DELETE` | `/api/registrations/{ticketCode}` | `ROLE_USER` | Kullanıcının kendi biletini iptal etmesini (etkinlik kaydını silmesini) sağlar. |

### 🔍 Kulüp Sahibi ve Bilet Doğrulama İşlemleri (Club Owner)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/registrations/event/{eventId}` | `ROLE_CLUB_OWNER` | Belirli bir etkinliğe kayıt olan tüm katılımcı listesini getirir. *(Sadece etkinliği düzenleyen kulüp sahibi erişebilir)* |
| `POST` | `/api/registrations/validate/{ticketCode}` | `ROLE_CLUB_OWNER` | Kapıda bilet kontrolü için bilet kodunun geçerli, kullanılmamış ve o etkinliğe ait olup olmadığını doğrular. |
