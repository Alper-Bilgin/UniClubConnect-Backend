# Follow Service (Takip ve İlişki Yönetim Servisi)

`follow-service`, UniClubConnect platformundaki kullanıcıların birbirlerini takip etmesini, takipçi/takip edilen listelerini ve gizli profil onay süreçlerini yöneten Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Takip Etme / Takipten Çıkma**: Kullanıcıların birbirlerini takip etmesi (`POST /{targetId}`) veya takibi bırakması (`DELETE /{targetId}`).
- **Gizli Profil (Privacy Settings)**: Kullanıcıların profillerini gizliye alabilmesi (`isPrivate`).
  - Profil gizli ise, takip etme isteği doğrudan onaylanmaz; beklemedeki istekler (`PENDING`) arasına girer.
  - Profil sahibi bu istekleri listeler, kabul edebilir (`/accept`) veya reddedebilir (`/reject`).
- **Takipçi Listeleri & Sayıları**: Kullanıcıların takipçi ve takip ettiklerinin listelenmesi (sayfalanmış olarak) ve bunların toplam adetlerinin verilmesi.
- **Kullanıcı Önerileri (Recommendations)**: Kullanıcının henüz takip etmediği ama ortak takipçileri olan veya popüler kullanıcıları önerme sistemi (`/recommendations`).
- **Olay Gönderimi (Event Producer)**: Takip istekleri ve kabul olaylarında RabbitMQ üzerinden `follow-email` kuyruğuna event fırlatır, böylece `notification-service` kullanıcılara e-posta bildirimi gönderir.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Takip ilişkilerinin ve gizlilik ayarlarının saklanması)
- **RabbitMQ** (Takip olaylarını bildirmek amacıyla `notification_exchange` ve ilgili kuyruklara event gönderme)
- **Spring Security** (Oturum açmış kullanıcı doğrulama ve veri sahipliği kontrolleri)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 👥 Takip İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/follows/{targetId}` | Giriş Yapmış Kullanıcılar | Belirtilen kullanıcıyı takip eder (veya gizliyse istek gönderir). |
| `DELETE` | `/api/follows/{targetId}` | Giriş Yapmış Kullanıcılar | Belirtilen kullanıcıyı takibi bırakır. |
| `DELETE` | `/api/follows/followers/{followerId}` | Giriş Yapmış Kullanıcılar | Kendi takipçilerinden birini takipçi listesinden çıkartır. |
| `GET` | `/api/follows/status/{targetId}` | Giriş Yapmış Kullanıcılar | Hedef kullanıcı ile olan takip durumunu döner (`FOLLOWING`, `PENDING`, `NONE`). |
| `GET` | `/api/follows/{userId}/counts` | Public | Belirtilen kullanıcının takipçi ve takip edilen toplam sayılarını döner. |
| `GET` | `/api/follows/{userId}/followers` | Public | Belirtilen kullanıcının takipçilerini listeler (`?page=0&size=20`). |
| `GET` | `/api/follows/{userId}/following` | Public | Belirtilen kullanıcının takip ettiklerini listeler (`?page=0&size=20`). |

### 🔒 Gizli Profil ve Takip İstekleri Yönetimi
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/follows/requests` | Giriş Yapmış Kullanıcılar | Kendi profiline gelen beklemedeki takip isteklerini listeler. |
| `PUT` | `/api/follows/requests/{followerId}/accept` | Giriş Yapmış Kullanıcılar | Gelen takip isteğini onaylar. |
| `DELETE` | `/api/follows/requests/{followerId}/reject` | Giriş Yapmış Kullanıcılar | Gelen takip isteğini reddeder. |
| `PUT` | `/api/follows/settings/privacy` | Giriş Yapmış Kullanıcılar | Hesabı gizliye alır veya herkese açık yapar (`?isPrivate=true/false`). |
| `GET` | `/api/follows/settings/privacy` | Giriş Yapmış Kullanıcılar | Oturum açan kullanıcının gizlilik durumunu sorgular. |
| `GET` | `/api/follows/{userId}/privacy-status` | Public | Belirtilen kullanıcının profilinin gizli olup olmadığını kontrol eder. |

### 💡 Keşif ve Öneriler
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/follows/recommendations` | Giriş Yapmış Kullanıcılar | Kullanıcı için takip edebileceği diğer kullanıcıları önerir (`?limit=5`). |
