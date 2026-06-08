# Chat Service (Mesajlaşma Servisi)

`chat-service`, UniClubConnect platformundaki kullanıcılar arasında birebir, gerçek zamanlı (WebSocket) ve geçmişe dönük (REST) mesajlaşmayı sağlayan gelişmiş Spring Boot mikroservisidir.

## 📌 Genel Bakış ve Görevleri
- **Gerçek Zamanlı Mesajlaşma**: WebSocket (STOMP) protokolü üzerinden anlık mesaj iletimi.
- **Kullanıcı Çevrimiçi (Online) Kontrolü**: Redis üzerinden alıcının online olup olmadığının tespiti.
  - Alıcı online ise mesaj doğrudan WebSocket ile iletilir.
  - Alıcı offline ise RabbitMQ (`notification_exchange`) üzerinden bildirim kuyruğuna event fırlatılır.
- **Yazıyor... (Typing Indicator)**: Kullanıcıların sohbet sırasında karşı tarafa anlık olarak yazma durumunu iletmesi.
- **Mesaj Durumları (ACK)**: Mesajların gönderildi (`SENT`), iletildi (`DELIVERED`) veya okundu (`READ`) durumlarının anlık takibi.
- **Mesaj Düzenleme & Silme**: Mesajların hem REST API hem de WebSocket üzerinden silinebilmesi ve güncellenebilmesi.
- **Gelen Kutusu (Conversations/Inbox)**: Kullanıcı bazlı sohbet listeleri, okunmamış mesaj adetleri, sohbet arşivleme ve arşivden çıkarma.
- **Mesaj Arama**: Belli bir sohbet içinde veya tüm sohbetlerde (Global Search) kelime bazlı arama yapılması.

## ⚙️ Teknolojiler ve Bağımlılıklar
- **Java 17 & Spring Boot**
- **Spring WebSocket / STOMP** (Gerçek zamanlı iki yönlü iletişim)
- **Spring Data JPA & PostgreSQL** (Mesaj ve sohbet geçmişinin kalıcı olarak saklanması)
- **Redis** (Kullanıcıların çevrimiçi/çevrimdışı durumlarının anlık tutulması)
- **RabbitMQ** (Çevrimdışı kullanıcılara anlık e-posta veya anlık bildirim göndermek üzere olay iletimi)
- **Eureka Client & Spring Cloud Config Client**

## 🛣️ API ve WebSocket Protokol Yolları

### 🔌 WebSocket Mesaj Kanalları (STOMP)
*Not: İstemcinin `/app` prefixi ile istek gönderdiği, sunucunun ise `/user` prefixiyle kullanıcıya özel kanallardan bildirim döndüğü mimaridir.*

| Hedef Kanal (Destination) | Payload (Veri Yapısı) | Açıklama |
| :--- | :--- | :--- |
| `/app/chat.send` | `ChatMessageRequest` | Karşı tarafa yeni bir mesaj gönderir. |
| `/app/chat.status` | `{"messageId", "senderId", "status"}` | Mesajın iletildi/okundu bilgisini günceller ve göndericiye bildirir. |
| `/app/chat.typing` | `{"recipientId", "isTyping"}` | "Yazıyor..." animasyonu durumunu tetikler. |
| `/app/chat.delete` | `{"messageId"}` | Belli bir mesajı gerçek zamanlı olarak siler. |
| `/app/chat.edit` | `EditMessageRequest` | Gönderilen bir mesajı gerçek zamanlı olarak günceller. |
| `/app/chat.mark-read` | `{"messageId", "senderId"}` | Gelen tekil bir mesajı okundu işaretler ve sayacı sıfırlar. |

---

### 📂 REST API Endpoint'leri

#### 💬 Sohbet Geçmişi ve Arama
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/chat/history/{recipientId}` | Belirli bir kullanıcıyla olan mesajlaşma geçmişini sayfa sayfa (pagination) getirir. |
| `GET` | `/api/chat/search` | Belirli bir odadaki mesajlarda arama yapar (`?recipientId=...&q=...`). |
| `GET` | `/api/chat/search-all` | Kullanıcının tüm sohbetlerindeki mesajlarda global arama yapar (`?q=...`). |
| `GET` | `/api/chat/search/{partnerId}` | Belirli bir kullanıcı ile olan sohbette arama yapar (`?q=...`). |

#### 📥 Gelen Kutusu (Conversations) Yönetimi
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/chat/conversations` | Kullanıcının aktif sohbet (inbox) listesini son mesaj detaylarıyla getirir. |
| `GET` | `/api/chat/conversations/archived` | Arşivlenmiş sohbetleri listeler. |
| `GET` | `/api/chat/conversations/unread` | Okunmamış mesaja sahip sohbetleri getirir. |
| `POST` | `/api/chat/conversations/{conversationId}/mark-read` | Bütün bir sohbeti/konuşmayı okundu olarak işaretler. |
| `POST` | `/api/chat/conversations/{conversationId}/archive` | Sohbeti arşivler. |
| `POST` | `/api/chat/conversations/{conversationId}/unarchive` | Sohbeti arşivden çıkartır. |

#### 🔢 Okunmamış Sayacı & Mesaj İşlemleri
| Yöntem | Endpoint | Açıklama |
| :--- | :--- | :--- |
| `GET` | `/api/chat/unread-count` | Kullanıcının toplam okunmamış mesaj sayısını döner. |
| `GET` | `/api/chat/unread-count/{recipientId}` | Sadece belirli bir kullanıcıdan gelen okunmamış mesaj sayısını döner. |
| `POST` | `/api/chat/mark-read/{recipientId}` | Belirli bir kullanıcı ile olan sohbeti okundu işaretler ve okunmamış sayacını sıfırlar. |
| `POST` | `/api/chat/mark-all-read` | Tüm mesajları okundu olarak işaretler. |
| `PUT` | `/api/chat/messages/{messageId}` | Gönderilen mesaj içeriğini REST API üzerinden günceller. |
| `DELETE` | `/api/chat/messages/{messageId}` | Gönderilen mesajı REST API üzerinden siler. |
