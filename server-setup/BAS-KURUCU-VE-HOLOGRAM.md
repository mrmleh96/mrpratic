# MRPRATIC — Baş Kurucu rankı ve DecentHolograms başlığı

Bu kurulum Paper 1.21.11 sunucusu ve mevcut MrPratic rank sistemi içindir.

## Baş Kurucu rankı

Yeni sürümde `Baş Kurucu` rankı şu yerlerde görünür:
- Sohbet prefixi
- TAB oyuncu listesi
- MrPratic scoreboard rank satırı

Rank, blok/piksel hissi veren `▰` karakterleri ve koyu/parlak yeşil renklerle tasarlanmıştır.

Yetkili hesabından (OP veya `mrpratic.admin` izni olan hesap) şu komutu kullan:

```
/rank ayarla OYUNCU_ADI Baş_Kurucu
```

Komutta boşluk yerine alt çizgi kullanılır; eklenti bunu otomatik olarak `Baş Kurucu` adına çevirir. Rankın prefix tanımı `plugins/MrPratic/config.yml` içindeki `ranks.prefixes` bölümündedir.

## DecentHolograms — MRPRATIC spawn başlığı

DecentHolograms eklentisini Paper 1.21.11 sunucusuna kurup sunucuyu bir kez başlattıktan sonra konsoldan şu komutları sırayla çalıştır:

```
/dh h create mrpratic -l:mrpratic_spawn:0.5:107:0.5 <#00FF88>&l✦ MRPRATIC ✦</#006B35>
/dh l add mrpratic &8&m━━━━━━━━━━━━━━━━━━━━━━━━
/dh l add mrpratic &2&lP R A C T I C E   N E T W O R K
/dh l add mrpratic &7Java 1.21.11 &8• &amrpratic.mcsh.gg
```

Başlık, MrPratic'in otomatik oluşturduğu `mrpratic_spawn` dünyasında merkez spawnın üstüne yerleştirilir (X=0.5, Y=107, Z=0.5). Başka bir yere taşımak istersen `-l:...` koordinatlarını değiştir.

Mevcut hologramı düzenlemek için `/dh l set mrpratic 2 YENİ_METİN`; tamamen kaldırmak için `/dh h delete mrpratic` kullan.

**Boyut notu:** Klasik DecentHolograms metin satırlarının yazı tipi ve punto boyutu değiştirilemez. Bu tasarım kalın yazı, renk geçişi ve dekoratif blok çizgileriyle daha büyük görünüm verir. Gerçek ölçek büyütme, kurulu DecentHolograms sürümünün Display hologram desteğine bağlıdır.

