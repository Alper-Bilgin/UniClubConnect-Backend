# Event Service (Etkinlik Servisi)

`event-service`, UniClubConnect platformundaki kulüpler tarafından düzenlenen etkinliklerin (seminerler, festivaller, toplantılar vb.) oluşturulması, güncellenmesi, görsellerinin yüklenmesi ve detaylarının listelenmesini yöneten Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Etkinlik Keşfi**: Tüm etkinlikleri listeleme (`GET /api/events`), detay sorgulama (`/{id}`) ve belirli bir kulübe ait etkinlikleri getirme (`/club/{clubId}`).
- **Etkinlik Oluşturma**: `ROLE_CLUB_OWNER` olan kullanıcıların kendi kulüpleri adına etkinlik tanımlayabilmesi.
  - *Güvenlik Kontrolü*: Etkinlik oluşturulurken `club-service` aranarak kullanıcının o kulübün sahibi olup olmadığı kontrol edilir.
- **Etkinlik Görseli Yükleme**: Etkinliğe ait afiş/kapak resminin MinIO nesne depolama sunucusuna yüklenmesi.
- **Düzenleme & Silme**: Etkinlik bilgilerini güncelleme ve iptal/silme süreçleri (sadece etkinlik sahibi kulüp yöneticisi tarafından yapılabilir).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Etkinlik bilgilerinin veritabanında saklanması)
- **MinIO S3 API SDK** (Etkinlik afişlerinin/resimlerinin depolanması)
- **Feign Client** (`club-service` ile entegrasyon kurarak kulüp sahipliği doğrulamasını gerçekleştirir)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 📢 Genel Etkinlik İşlemleri (Public)
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/events` | Sistemdeki tüm aktif etkinlikleri listeler. |
| `GET` | `/api/events/{id}` | Belirtilen etkinliğin detay bilgilerini döner. |
| `GET` | `/api/events/club/{clubId}` | Sadece belirli bir kulübün düzenlediği etkinlikleri listeler. |

### 🛠️ Etkinlik Yönetim İşlemleri (Club Owner Only)
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/events` | `ROLE_CLUB_OWNER` | Yeni bir etkinlik kaydı oluşturur. |
| `POST` | `/api/events/{id}/image` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinliğe kapak görseli/afiş yükler (Multipart File -> MinIO). |
| `PUT` | `/api/events/{id}` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinlik bilgilerini (başlık, açıklama, tarih, konum, kota vb.) günceller. |
| `DELETE` | `/api/events/{id}` | `ROLE_CLUB_OWNER` *(Etkinlik Sahibi)* | Etkinliği tamamen siler. |
