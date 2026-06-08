# Feed Service (Akış/Timeline Servisi)

`feed-service`, UniClubConnect platformundaki kullanıcıların takip ettikleri kulüp veya kişilerin paylaştığı gönderilerden (post) oluşan kişiselleştirilmiş ana sayfa akışını (timeline) hazırlayan ve sunan yüksek performanslı Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Kişiselleştirilmiş Akış Hazırlama**: Fan-out on write (yazma anında dağıtım) mimarisi kullanarak gönderileri anlık olarak takipçilerin akışlarına işler.
- **Olay Dinleme (Event Listener)**: RabbitMQ üzerinden `POST_CREATED` ve `POST_DELETED` olaylarını dinler:
  - Yeni bir post paylaşıldığında, `follow-service` üzerinden yazarın tüm takipçilerini çeker (`FollowClient`).
  - Her bir takipçinin Redis'teki akış listesine (`feed:<followerId>`) gönderinin ID'sini ekler.
  - Akış listesini maksimum 500 gönderide sınırlandırır (trim) ve 7 gün süreli (TTL) olarak tutar.
- **Lazy Delete (Gecikmeli Silme)**: Akış çekildiğinde silinen postlar filtrelenir, böylece silme anında tüm takipçilerin Redis listelerini güncellemek yerine veri getirme anında performans kazancı sağlanır.
- **Performans Optimizasyonu**: Kullanıcı akışındaki gönderileri veritabanından tek tek sorgulamak yerine Redis'te ID listesi olarak tutar ve `post-service` üzerinden toplu (batch) olarak sorgular.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Redis (StringRedisTemplate)** (Kullanıcı akışlarının ID bazlı listeler olarak hızlıca saklanması ve çekilmesi)
- **RabbitMQ Listener** (Post oluşturma ve silme olaylarını asenkron dinlemek için)
- **Feign Clients** (`follow-service` ile takipçileri bulmak, `post-service` ile gönderi detaylarını toplu çekmek için)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API Endpoint'leri (Yolları)

### 📱 Akış API'si
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/feed` | Giriş Yapmış Kullanıcılar | Kullanıcının kişiselleştirilmiş ana sayfa akışını sayfa sayfa (pagination) getirir (`?page=0&size=10`). |
