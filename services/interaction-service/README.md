# Interaction Service (Etkileşim Servisi)

`interaction-service`, UniClubConnect platformundaki gönderiler (post) ve etkinlikler (event) gibi çeşitli hedefler üzerindeki beğenme (like) ve yorum yapma (comment) gibi sosyal etkileşimleri yöneten Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Yorum Yönetimi**: Gönderilere veya etkinliklere yorum yapma (`POST /comments`), silme (`DELETE /comments/{commentId}`) ve hedeflere ait tüm yorumları listeleme (`GET /comments/{targetType}/{targetId}`).
- **Beğeni Yönetimi**: Kullanıcıların bir hedefi beğenmesi veya beğeniyi geri çekmesi (Toggle Like - `/likes`).
- **İlişkisel Hedef Yapısı**: Etkileşimler `ETargetType` enum yapısı (örn. POST, EVENT) ve `targetId` (hedef ID'si) ikilisiyle dinamik olarak eşleştirilir. Bu sayede servis, yeni bir hedef türü geldiğinde kolayca genişletilebilir.
- **Durum Kontrolü**: Kullanıcının belirli bir gönderiyi/etkinliği beğenip beğenmediğini sorgulayabilmesi (`/status`).

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Data JPA & PostgreSQL** (Yorumların, beğenilerin ve hedeflerin veritabanında saklanması)
- **Spring Security** (Oturum açmış kullanıcıların doğrulanması ve yorum silme işlemlerinde sahiplik kontrolü)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 💬 Yorum İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/interactions/comments` | Giriş Yapmış Kullanıcılar | Belirtilen hedefe yeni bir yorum yazar. (Body: `content`, `targetId`, `targetType`). |
| `GET` | `/api/interactions/comments/{targetType}/{targetId}` | Public | Belirtilen hedefin tüm yorumlarını listeler (örn. `POST` veya `EVENT` tipinde). |
| `DELETE` | `/api/interactions/comments/{commentId}` | Giriş Yapmış Kullanıcılar | Yazarı olunan bir yorumu sistemden siler. |

### 💖 Beğeni İşlemleri
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/interactions/likes` | Giriş Yapmış Kullanıcılar | Hedefi beğenir. Zaten beğenildiyse beğeniyi kaldırır (Toggle). (Body: `targetId`, `targetType`). |
| `GET` | `/api/interactions/likes/{targetType}/{targetId}/count` | Public | Belirtilen hedefin toplam beğeni sayısını döner. |
| `GET` | `/api/interactions/likes/{targetType}/{targetId}/status` | Giriş Yapmış Kullanıcılar | Oturum açan kullanıcının o hedefi beğenip beğenmediğini döner (`true/false`). |
