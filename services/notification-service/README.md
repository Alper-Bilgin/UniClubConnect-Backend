# Notification Service (Bildirim ve E-posta Servisi)

`notification-service`, UniClubConnect platformundaki asenkron bildirimleri ve e-posta gönderim süreçlerini yöneten Spring Boot mikroservisidir. REST API barındırmaz, tamamen event-driven (olay odaklı) çalışarak RabbitMQ üzerinden gelen mesajları tüketir (consume).

## 📌 Genel Bakış ve Görevleri
- **Kullanıcı Karşılama Mailleri**: Yeni üye kaydolduğunda gelen e-posta doğrulama kodunu içeren hoş geldin e-postası gönderir (`welcome-template`).
- **Etkinlik Bilet Mailleri**: Etkinliğe bilet alan kullanıcılara özel bilet kodunu ve QR kod görselini içeren bilet onay e-postası yollar (`ticket-template`).
- **Sosyal Bildirim Mailleri**:
  - Profil gizliyken gelen yeni takip isteklerini bildirir (`follow-request-template`).
  - Takip isteği kabul edildiğinde göndericiye onay haberi verir (`follow-accepted-template`).
- **Çevrimdışı Mesaj Mailleri**: Kullanıcı çevrimdışıyken (offline) gelen sohbet mesajları için okunmamış mesaj bildirimi yollar (`chat-notification-template`).
- **Loglama ve Kayıt**: Gönderilen tüm e-postaların geçmişini veritabanında (`SentEmail` tablosu) loglar.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring Boot Starter Mail & JavaMailSender** (E-posta gönderimi için SMTP entegrasyonu)
- **Thymeleaf Template Engine** (Zengin içerikli, dinamik HTML şablonları hazırlamak için)
- **Spring Data JPA & PostgreSQL** (Gönderilen bildirim ve e-postaların geçmişinin kalıcı loglanması)
- **RabbitMQ Listener** (Sistem genelindeki olayları dinleyip asenkron e-posta tetiklemek için)
- **Feign Client** (`user-profile-service` ile entegrasyon kurarak kullanıcı ID'lerinden ad/soyad ve e-posta adreslerini dinamik olarak çözer)
- **Eureka Client & Spring Cloud Config Client**

## ✉️ RabbitMQ Dinleyicileri (Listeners) & Olaylar
*Servis, RabbitMQ kuyruklarını dinleyerek gelen event nesnelerine göre ilgili şablonlar üzerinden e-posta gönderir.*

| Kuyruk (Queue) | Event Tipi | Şablon Adı | Açıklama |
| :--- | :--- | :--- | :--- |
| `welcome-email` | `UserCreatedEvent` | `welcome-template` | Hesap doğrulama kodu ve hoş geldin mesajı içerir. |
| `ticket-email` | `TicketCreatedEvent` | `ticket-template` | Etkinlik bilet kodu ve QR kodu içerir. |
| `follow-email` | `FollowEvent` (`FOLLOW_REQUESTED`) | `follow-request-template` | Profilinizi takip etmek isteyen kişinin bilgisini içerir. |
| `follow-email` | `FollowEvent` (`FOLLOW_ACCEPTED`) | `follow-accepted-template` | Takip isteğinizin kabul edildiğini bildirir. |
| `chat-notification` | `UnreadMessageEvent` | `chat-notification-template` | Çevrimdışıyken alınan mesajın önizlemesini içerir. |
