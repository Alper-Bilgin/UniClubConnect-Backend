# Club Service (Kulüp Yönetim Servisi)

`club-service`, UniClubConnect platformundaki öğrenci kulüplerinin oluşturulması, güncellenmesi, üyelik başvurularının yönetilmesi ve üye listelerinin idare edilmesini sağlayan Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Kulüp Keşfi ve Arama**: Platformdaki tüm kulüpleri listeleme (`GET /api/clubs`) ve kulüp isimlerine göre arama yapma (`/search`).
- **Kulüp Oluşturma**: `ROLE_CLUB_OWNER` yetkisine sahip kullanıcıların kendi kulüplerini oluşturabilmesi.
- **Profil Resmi/Logo Yükleme**: Kulüplere ait logoların MinIO nesne depolama (Object Storage) sunucusuna yüklenmesi.
- **Üyelik Başvuruları**: Kullanıcıların kulüplere katılım isteği göndermesi (`/join`), kulüp sahiplerinin bu başvuruları listelemesi, onaylaması (`/approve`) veya reddetmesi (`/reject`).
- **Üye Yönetimi**: Kulüp üyelerini listeleme, üyelikten kendi isteğiyle ayrılma (`/leave`) ve kulüp yöneticisi tarafından üyenin kulüpten çıkarılması (`/members/{memberAuthId}`).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Kulüpler, üyeler ve isteklerin ilişkisel olarak saklanması)
- **MinIO S3 API SDK** (Kulüp logolarının güvenli, CDN uyumlu ve ölçeklenebilir şekilde saklanması)
- **Spring Security** (Rol ve sahiplik bazlı yetkilendirme kontrolleri)
- **Eureka Client & Spring Cloud Config Client**
- **Feign Clients** (Diğer servislerle güvenli iletişim kurmak amacıyla iç servis endpoint'leri barındırır)

## 🛣️ API Endpoint'leri (Yolları)

### 📢 Kulüp Bilgi ve Keşif İşlemleri (Public)
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/clubs` | Platformdaki tüm kulüpleri listeler. |
| `GET` | `/api/clubs/{clubId}` | ID'si verilen kulübün detay bilgilerini getirir. |
| `GET` | `/api/clubs/search` | Kulüpler arasında arama yapar (`?query=...`). |
| `GET` | `/api/clubs/{clubId}/is-owner/{authId}` | **[İç Servis/Feign]** Belirtilen kullanıcının kulübün sahibi olup olmadığını sorgular. |

### 🛠️ Kulüp Yönetim İşlemleri (Club Owner)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/clubs` | `ROLE_CLUB_OWNER` | Yeni bir kulüp oluşturur. |
| `GET` | `/api/clubs/my-club` | `ROLE_CLUB_OWNER` | Oturum açmış kulüp sahibinin kendi kulüp detaylarını getirir. |
| `PUT` | `/api/clubs/{clubId}` | `ROLE_CLUB_OWNER` *(Kulüp Sahibi)* | Kulüp bilgilerini (ad, açıklama vb.) günceller. |
| `POST` | `/api/clubs/{clubId}/logo` | `ROLE_CLUB_OWNER` *(Kulüp Sahibi)* | Kulübe yeni bir logo yükler (Multipart File -> MinIO). |

### 👥 Kulüp Üyelik ve Başvuru İşlemleri (User & Club Owner)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/clubs/{clubId}/join` | `ROLE_USER` | Kulübe üye olmak için başvuru (katılım isteği) gönderir. |
| `DELETE` | `/api/clubs/{clubId}/leave` | `ROLE_USER` | Kullanıcının üye olduğu kulüpten kendi isteğiyle ayrılmasını sağlar. |
| `GET` | `/api/clubs/{clubId}/requests` | `ROLE_CLUB_OWNER` *(Kulüp Sahibi)* | Kulübe yapılmış beklemedeki (PENDING) üyelik başvurularını listeler. |
| `POST` | `/api/clubs/requests/{requestId}/approve` | `ROLE_CLUB_OWNER` | Üyelik başvurusunu onaylar ve kullanıcıyı kulübe üye olarak ekler. |
| `POST` | `/api/clubs/requests/{requestId}/reject` | `ROLE_CLUB_OWNER` | Üyelik başvurusunu reddeder. |
| `GET` | `/api/clubs/{clubId}/members` | `ROLE_CLUB_OWNER` *(Kulüp Sahibi)* | Kulübe kayıtlı mevcut üyelerin listesini getirir. |
| `DELETE` | `/api/clubs/{clubId}/members/{memberAuthId}` | `ROLE_CLUB_OWNER` *(Kulüp Sahibi)* | Belirtilen üyeyi kulüpten çıkartır (Kicking). |
