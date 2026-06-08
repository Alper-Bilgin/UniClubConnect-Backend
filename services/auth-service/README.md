# Auth Service (Kimlik Doğrulama Servisi)

`auth-service`, UniClubConnect platformunun kullanıcı kimlik doğrulama, kayıt, e-posta doğrulama, şifreleme ve rol yönetimi (Admin, Club Owner, User) süreçlerini yöneten temel Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Kullanıcı Kaydı & Giriş**: Yeni kullanıcı oluşturma (`/register`) ve JWT tabanlı kimlik doğrulama (`/login`).
- **E-posta Doğrulama**: Kayıt olan kullanıcılara doğrulama kodu gönderimi ve kontrolü (`/verify`).
- **Rol Talepleri (Role Upgrade Requests)**: Standart kullanıcıların (USER) kulüp sahibi (CLUB_OWNER) olmak için talep göndermesi (`/requests/club-owner-role`) ve durum takibi (`/requests/my-status`).
- **Admin Paneli**: Admin yetkisine sahip kullanıcıların bekleyen rol taleplerini listelemesi, onaylaması veya reddetmesi (`/api/admin/role-requests/...`).
- **Güvenlik**: Spring Security ve JWT (JSON Web Token) kullanımı.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Security & JWT** (Kimlik doğrulama ve yetkilendirme)
- **Spring Data JPA & PostgreSQL** (Kullanıcı ve rol verilerinin saklanması)
- **Eureka Client** (Servis keşfi ve API Gateway entegrasyonu)
- **Spring Cloud Config Client** (Merkezi yapılandırma sunucusuna bağlanma)
- **RabbitMQ** (Kullanıcı kaydında hoş geldin ve doğrulama e-postası tetiklemek için `welcome-email` kuyruğuna event fırlatır)

## 🛣️ API Endpoint'leri (Yolları)

### 🔑 Genel Kimlik Doğrulama (Public)
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/auth/test` | Servisin çalışıp çalışmadığını test eden uç nokta. |
| `POST` | `/api/auth/register` | Yeni bir kullanıcı hesabı oluşturur. |
| `POST` | `/api/auth/login` | Giriş yapar ve JWT Token döner. |
| `POST` | `/api/auth/verify` | E-postaya gönderilen doğrulama kodu ile hesabı aktifleştirir. |
| `POST` | `/api/auth/resend-code` | E-posta doğrulama kodunu tekrar gönderir. |

### 📋 Rol Yükseltme Talepleri (Kullanıcı)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/requests/club-owner-role` | `ROLE_USER` | Kulüp yöneticisi (CLUB_OWNER) olmak için başvuru yapar. |
| `GET` | `/api/requests/my-status` | Giriş Yapmış Kullanıcılar | Mevcut kullanıcının yaptığı rol talebinin durumunu sorgular. |

### 🛠️ Admin Yönetimi (Admin Only)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/role-requests/pending` | `ROLE_ADMIN` | Beklemede olan (PENDING) rol yükseltme taleplerini listeler. |
| `POST` | `/api/admin/role-requests/{requestId}/approve` | `ROLE_ADMIN` | Belirli bir rol talebini onaylar ve kullanıcı rolünü `CLUB_OWNER` yapar. |
| `POST` | `/api/admin/role-requests/{requestId}/reject` | `ROLE_ADMIN` | Belirli bir rol talebini reddeder. |
