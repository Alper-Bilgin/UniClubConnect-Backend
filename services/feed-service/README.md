# Feed Service (Akış/Timeline Servisi)

`feed-service`, platformdaki kullanıcıların takip ettikleri kulüp veya kişilerin paylaştığı gönderilerden oluşan kişiselleştirilmiş ana sayfa akışını hazırlayan ve sunan yüksek performanslı Spring Boot mikroservisidir.

---

## 🏗️ Geliştirme ve Yazılım Mimarisi
Servis, mikroservis dünyasındaki en popüler tasarım kalıplarından olan **Fan-out-on-Write (Yazma anında dağıtma)** ve **Lazy Evaluation / Lazy Delete (Gecikmeli Silme)** yaklaşımlarını birleştirir.
- **Fan-out-on-Write (Push Modeli)**: Bir kullanıcı yeni bir gönderi paylaştığında (`post-service` üzerinden), bu olay RabbitMQ aracılığıyla asenkron olarak `feed-service`e iletilir. Servis, yazarı takip eden kişilerin listesini `follow-service`den çeker ve her bir takipçinin Redis'teki akış kuyruğuna (`feed:<followerId>`) gönderi ID'sini anında ekler.
- **Redis Akış Önbelleği**: Kullanıcıların akışları veritabanında saklanmaz. Redis üzerinde list (`opsForList`) olarak tutulur.
  - Akış listesi maksimum **500 gönderi** ile sınırlandırılır (`trim`).
  - Akışın geçerlilik süresi (TTL) **7 gün** olarak ayarlanır.
- **Lazy Evaluation & Lazy Delete (Read-Path Caching)**: Bir gönderi silindiğinde (`POST_DELETED` olayı alındığında), tüm takipçilerin Redis listelerinden bu gönderiyi silmek yerine, veriyi okuma anında temizleme tercih edilir. Kullanıcı akışını talep ettiğinde (`GET /api/feed`):
  1. Redis'ten sayfalanmış gönderi ID listesi çekilir.
  2. `post-service` FeignClient ile toplu (`/batch`) sorgulanır.
  3. Silinmiş olan gönderiler `post-service`den dönmeyeceği için null dönen kayıtlar filtre edilerek (`filter(Objects::nonNull)`) kullanıcıya temiz veri sunulur. Bu sayede yazma performansında devasa kazanç sağlanır.

---

## 💾 Veritabanı Şeması ve Tablolar
Bu servis, yüksek performans ve düşük gecikme süresi hedeflediği için **ilişkisel veritabanı (JPA/PostgreSQL) barındırmaz**. Tüm veriler tamamen bellek içi anahtar-değer deposu olan **Redis** üzerinde tutulur.

### Veri Yapısı (Redis Bellek Haritası)
- **Anahtar Tasarımı**: `feed:<userId>`
- **Veri Türü**: `List` (Sıralı Post ID'leri barındırır. Yeni eklenen gönderiler sol taraftan `leftPush` ile eklenir).
- **Bellek Yönetimi**: Maksimum 500 eleman (`ltrim feed:<userId> 0 500`), TTL 7 gün.

---

## ✉️ RabbitMQ Olay Akışları (Event-Driven)
Servis, asenkron olarak gönderi olaylarını dinlemek için RabbitMQ abonesidir (subscriber).

### 1. Tüketilen Olaylar (Consumed Events)
- **Gönderi Olayları (`PostEvent`)**:
  - **Exchange**: `post.exchange`
  - **Queue**: `feed_post_event_queue`
  - **Routing Keys**: `post.created`, `post.deleted`
  - **Payload DTO**: `eventId`, `eventType` (`POST_CREATED`, `POST_DELETED`), `postId`, `authorId`
  - **İşlem**: Yeni gönderilerde yazarın takipçilerinin Redis akış listelerine post ID'si eklenir.

---

## 🔌 Servis İletişimi (OpenFeign)
`feed-service` akışı oluştururken diğer servislerle senkron FeignClient bağlantıları kurar.

### 1. Tüketilen Dış Servisler (Feign Clients)
- **`FollowClient` (`follow-service` çağrılır)**:
  - **Uç Nokta (`GET /api/follows/{userId}/followers`)**: Gönderiyi paylaşan kişinin takipçi listesini çekerek akış dağıtımı yapılacak hedef kullanıcıları belirler.
- **`PostClient` (`post-service` çağrılır)**:
  - **Uç Nokta (`POST /api/posts/batch`)**: Redis'ten çekilen post ID'lerinin detaylarını (içerik, resim URL'si, yazar adı vb.) toplu olarak tek bir HTTP isteğiyle getirmek için çağrılır.

---

## ⚙️ Yapılandırma ve Çalışma Parametreleri
- **Çalışma Portu**: `9010` (Gateway yönlendirmesi: `/api/feed/**`)
- **Veritabanı Şeması**: Kalıcı veritabanı yoktur.
- **Merkezi Yapılandırma (Config Repo)**: `feed-service.yml`
- **Önemli Property Tanımları**:
  - `feed.rabbitmq.exchange`: `post.exchange` (Post servisi exchange adı)
  - `feed.rabbitmq.queue.post-event-queue`: `feed_post_event_queue`
  - `feign.client.config.default.readTimeout`: `5000` (5 saniye timeout koruması)

---

## 🛣️ API Endpoint'leri (Yolları)

### 📱 Akış API'si
| Yöntem | Endpoint | Erişim Yetkisi | Açıklama |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/feed` | Giriş Yapmış Kullanıcı | Oturum açan kullanıcının takip ettiği kulüp ve kişilere ait gönderi akışını getirir (`?page=0&size=10`). |
