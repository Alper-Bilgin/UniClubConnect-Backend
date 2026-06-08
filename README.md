# UniClubConnect - Mikroservis Mimarisi

UniClubConnect, üniversite kulüpleri ve öğrencileri arasında sosyal etkileşim, etkinlik yönetimi ve gerçek zamanlı iletişimi sağlamak amacıyla geliştirilmiş **Spring Cloud** tabanlı, olay-odaklı (event-driven) bir mikroservis projesidir.

---

## 🏗️ Sistem Mimarisi ve Teknoloji Yığını

Proje, bağımsız olarak ölçeklenebilen mikroservislerden ve bunları koordine eden altyapı bileşenlerinden oluşmaktadır.

### 🛠️ Altyapı Bileşenleri (Infrastructure)
1. **[Eureka Server](file:///infrastructure/eureka-server)**: Mikroservislerin birbirlerini dinamik olarak bulmasını sağlayan Servis Keşfi (Service Discovery) sunucusu.
2. **[Config Server](file:///infrastructure/config-server)**: Tüm servislerin yapılandırma (configuration) parametrelerini tek bir Git deposundan (Centralized Config) yöneten yapılandırma sunucusu.
3. **[API Gateway](file:///infrastructure/api-gateway)**: Güvenlik, CORS yönetimi ve gelen isteklerin ilgili servislere yönlendirilmesini sağlayan ana giriş kapısı (Port: `8080`).

### 📦 Mikroservisler ve İşlevleri
Her bir servisin kök dizininde ne işe yaradığı ve hangi API uç noktalarına (yollarına) sahip olduğunu açıklayan detaylı dokümanlar oluşturulmuştur:

| Servis Adı | Açıklama | Dokümantasyon Linki |
| :--- | :--- | :--- |
| **Auth Service** | JWT tabanlı Kimlik Doğrulama, Kayıt ve Rol Yönetimi (Admin, Kulüp Sahibi, Üye). | 📄 [README.md](file:///services/auth-service/README.md) |
| **Chat Service** | WebSocket ve STOMP destekli anlık birebir mesajlaşma, inbox yönetimi ve arama. | 📄 [README.md](file:///services/chat-service/README.md) |
| **Club Service** | Kulüp oluşturma, bilgi güncelleme, logoları MinIO'ya yükleme ve üyelik onayları. | 📄 [README.md](file:///services/club-service/README.md) |
| **Event Service** | Kulüplerin etkinlik tanımlaması, afiş yükleme ve etkinlik listeleme süreçleri. | 📄 [README.md](file:///services/event-service/README.md) |
| **Feed Service** | Kullanıcıların takip ettikleri kişilerin gönderilerini Redis listelerinde tutarak hazırlanan akış. | 📄 [README.md](file:///services/feed-service/README.md) |
| **Follow Service** | Kullanıcılar arası takip etme, takipten çıkarma, gizlilik ayarları ve takip önerileri. | 📄 [README.md](file:///services/follow-service/README.md) |
| **Gamification Service** | XP kazanma, seviye atlama, daily streak (günlük seri) ve rozet (badge) ödül sistemi. | 📄 [README.md](file:///services/gamification-service/README.md) |
| **Interaction Service** | Gönderi ve etkinliklere yapılan yorum ve beğenilerin (like) yönetimi. | 📄 [README.md](file:///services/interaction-service/README.md) |
| **Notification Service** | RabbitMQ kuyruklarını dinleyerek e-posta (hoş geldin, bilet, takip, yeni mesaj) gönderimi. | 📄 [README.md](file:///services/notification-service/README.md) |
| **Post Service** | Metin ve görsel içerikli gönderi (post) paylaşma, güncelleme ve silme. | 📄 [README.md](file:///services/post-service/README.md) |
| **Registration Service** | Etkinlik bilet alımı, bilet iptalleri ve kapıda QR kod doğrulama kontrolü. | 📄 [README.md](file:///services/registration-service/README.md) |
| **User Profile Service** | Kullanıcı profil bilgilerinin (ad, soyad, biyografi, resim) güncellenmesi ve sorgulanması. | 📄 [README.md](file:///services/user-profile-service/README.md) |

---

## ⚙️ Ortak Kullanılan Teknolojiler
- **Java 17 & Spring Boot 3.x**
- **Spring Cloud (Eureka, Gateway, Config, OpenFeign)**
- **Spring Data JPA & PostgreSQL** (Kalıcı ilişkisel veriler için)
- **Redis Cache & Key-Value Store** (Kullanıcı akışları ve online durum yönetimi için)
- **RabbitMQ Message Broker** (Asenkron, gevşek bağlı olay iletimi için)
- **MinIO Object Storage** (Görsel, logo ve afiş dosyalarının S3 uyumlu depolanması için)
- **Docker & Docker Compose** (Altyapı servislerinin kolayca ayağa kaldırılması için)
