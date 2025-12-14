#  İşletim Sistemleri: İşlemci Zamanlama Simülatörü (CPU Scheduling)

Bu repository, **İşletim Sistemleri** dersi projesi olarak **Java** ile geliştirilmiştir. Proje, işletim sistemlerinin çekirdeğinde yer alan süreç yönetimi (process management) algoritmalarını simüle eder ve farklı senaryolar altındaki performanslarını analiz eder.

Amaç; FCFS, SJF, Priority ve Round Robin gibi temel tekniklerin **bekleme süresi**, **bağlam değiştirme maliyeti** ve **işlemci verimliliği** üzerindeki etkilerini gözlemlemektir.

---

##  Geliştirici Bilgileri

* **Ad Soyad:** Gürkan Özdemir
* **Öğrenci No:** 20232013047
* **Bölüm:** Bilgisayar Mühendisliği
* **Ders:** EBLM341 - İşletim Sistemleri
* **Üniversite:** İstanbul Nişantaşı Üniversitesi

---

##  Proje Konfigürasyonu ve Kurallar

Simülasyon, verilen veri setlerini (`.txt`) işlerken aşağıdaki **sabit parametreleri** baz almaktadır:

* **Platform:** Java (JDK gerektirir)
* **Quantum Süresi (RR için):** `10` birim zaman.
* **Context Switch (Bağlam Değiştirme) Maliyeti:** `0.001` ms.
* **Öncelik Sıralaması:** Düşük sayı = Yüksek Öncelik.
    * *High (1)* > *Normal (2)* > *Low (3)*

###  Simüle Edilen Algoritmalar
Aşağıdaki 6 farklı senaryo tek bir çalışma döngüsünde test edilir:

| Algoritma | Tür | Açıklama |
| :--- | :--- | :--- |
| **FCFS** | Non-Preemptive | İlk gelen işlem ilk çalıştırılır. |
| **SJF** | Non-Preemptive | En kısa işlem süresine sahip olan seçilir. |
| **SRTF (SJF)** | **Preemptive** | Kalan süresi en az olan işlem araya girer. |
| **Priority** | Non-Preemptive | Öncelik değeri en yüksek (sayısal olarak en küçük) olan seçilir. |
| **Priority** | **Preemptive** | Daha yüksek öncelikli iş gelirse mevcut iş kesilir. |
| **Round Robin** | Preemptive | Her işlem `q=10` süre kadar çalışır, süre biterse kuyruğun sonuna atılır. |

---

##  Dosya Mimarisi

```text
/
├── Odev1.java           # Main sınıf ve algoritma mantıklarını içeren kaynak kod
├── odev1_case1.txt      # Senaryo 1: Düşük yoğunluklu veri seti
├── odev1_case2.txt      # Senaryo 2: Yüksek yoğunluklu veri seti
├── README.md            # Proje dökümantasyonu
└── sonuclar/            # (Auto-Generated) Raporların kaydedildiği dizin
