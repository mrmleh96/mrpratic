# MrPratic

Paper 1.21.11 ve Java 21 icin Turkce Practice/PvP sunucu cekirdegi. Arayuz ve varsayilan mesaj temasi zümrüt yesili, siyah ve deepslate renklerindedir.

## Gereksinimler
- Paper 1.21.11
- Java 21
- Gradle 8.14 (GitHub Actions otomatik kurar)

## Derleme
`main` dalina push yapildiginda GitHub Actions `gradle clean build` calistirir. Basarili derleme sonunda Actions > ilgili workflow > Artifacts bolumunden `MrPratic` ZIP artefaktini indir; icindeki JAR'i sunucunun `plugins` klasorune koy.

Yerel derleme:
```bash
gradle clean build
```

## Bu surumde bulunanlar
- Otomatik olusturulan `mrpratic_lobi` dunyasi ve zümrüt/deepslate temali spawn
- Lobi korumasi: blok kirma/koyma, yere esya atma/toplama, hasar ve aclik engeli
- Giriste lobiye isinlanma
- Türkçe komutlar ve mesajlar
- Rank prefixleri ve varsayilan Çaylak ranki
- TAB baslik/altlik ve temel scoreboard
- FFA mod secim menusu ve mod basina ayri test platformu
- Hazir kit dagitimi
- Oyuncu davetli 1v1 CPvP, rövans ve maç izleme
- Basit eslestirme kuyrugu
- Parti kurma/davet/kabul/ayrilma/liste
- Arkadas listesi
- Öldürme/ölüm istatistikleri ve liderlik komutu
- Verilerin config.yml icinde saklanmasi
- GitHub Actions Java 21 derlemesi

## Komutlar
- `/spawn` veya `/lobi`
- `/ffa`, `/ffa <mod>`, `/ffa çık`
- `/kit`, `/kit <kit>`
- `/duello <oyuncu>`, `/duello kabul <oyuncu>`, `/duello red <oyuncu>`
- `/sıra`, `/sıra çık`
- `/rematch <oyuncu>`, `/rematch kabul`
- `/izle <oyuncu>`, `/izle çık`
- `/parti kur|davet <oyuncu>|kabul|ayrıl|liste`
- `/arkadaş ekle|sil <oyuncu>`, `/arkadaş liste`
- `/rank`, `/rank ayarla <oyuncu> <rank>` (admin)
- `/istatistik`, `/liderlik [öldürme|ölüm]`
- `/arena`, `/mrpratic kaydet`

## Bilinen eksikler - tamamlanmis gibi kabul etmeyin
Bu repo su an tam bir buyuk Practice agi degildir. Su ozellikler henuz gercek oyun akisina sahip degildir veya bu cekirdekte bulunmaz:
- Ranked/Unranked ayri kuyruklar ve ELO/MMR
- Gercek 2v2/3v3/4v4 takim maclari
- Party ile takim eslestirme ve party FFA
- Gercek BedWars ve The Bridge round/goal/bed mekanikleri (su an yalnizca FFA menusu/kit/platform kaydi var)
- Kit editoru ve oyuncu bazli kit kaydetme
- Turnuva, ekonomi, kozmetik ve kill efektleri
- Harici NPC/hologram entegrasyonu
- Harita dosyalarinin internetten otomatik indirilmesi veya mevcut dunya dosyasina yerlestirilmesi

Bu ozellikler sonraki gelistirme adimlaridir. Her yeni ozellik gercek oyun mantigi, veri kaydi ve test ile eklenmelidir.
