package gg.mrpratic;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MrPratic extends JavaPlugin implements Listener, CommandExecutor {
    private enum Mode {
        CPVP("CPvP", Material.TOTEM_OF_UNDYING), MACE("Mace", Material.MACE),
        MIZRAK("Mızrak", Material.TRIDENT), KILIC("Kılıç", Material.NETHERITE_SWORD),
        BALTA("Balta", Material.NETHERITE_AXE), COMBO("Combo", Material.STICK),
        NODEBUFF("NoDebuff", Material.POTION), BUILDUHC("BuildUHC", Material.GOLDEN_APPLE),
        SUMO("Sumo", Material.SLIME_BLOCK), BOXING("Boxing", Material.LEATHER_CHESTPLATE),
        BEDWARS("BedWars", Material.RED_BED), THEBRIDGE("The Bridge", Material.BRICKS);
        final String display; final Material icon;
        Mode(String display, Material icon) { this.display = display; this.icon = icon; }
        static Mode parse(String input) {
            String s = input.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
            for (Mode m : values()) if (m.name().replace("_", "").equalsIgnoreCase(s)
                    || m.display.replace(" ", "").replace("ı","i").replace("İ","i").equalsIgnoreCase(s.replace("ı","i").replace("İ","i"))) return m;
            return null;
        }
    }

    private final Map<UUID, String> ranks = new ConcurrentHashMap<>();
    private final Map<UUID, Mode> ffaPlayers = new ConcurrentHashMap<>();
    private final Map<Mode, Location> arenaSpawns = new EnumMap<>(Mode.class);
    private final Map<UUID, UUID> duelOpponents = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> duelInvites = new ConcurrentHashMap<>();
    private final Map<UUID, Long> inviteTimes = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastOpponents = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> rematchRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyOf = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyInvites = new ConcurrentHashMap<>();
    private final Set<UUID> duelQueue = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> spectatorReturns = new ConcurrentHashMap<>();
    private World world;
    private Location lobbySpawn;

    @Override public void onEnable() {
        saveDefaultConfig();
        world = Bukkit.getWorld(getConfig().getString("server.lobby-world", "mrpratic_lobi"));
        if (world == null) world = new WorldCreator(getConfig().getString("server.lobby-world", "mrpratic_lobi")).createWorld();
        if (world == null) { getLogger().severe("Lobi dünyası oluşturulamadı!"); getServer().getPluginManager().disablePlugin(this); return; }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        createLobby();
        createArenas();
        loadRanks();
        getServer().getPluginManager().registerEvents(this, this);
        for (String c : List.of("spawn","kit","ffa","duello","rank","istatistik","arena","mrpratic","parti","arkadaş","liderlik","sıra","rematch","izle")) {
            PluginCommand pc = getCommand(c);
            if (pc != null) pc.setExecutor(this);
        }
        new BukkitRunnable() { @Override public void run() {
            for (Player p : Bukkit.getOnlinePlayers()) updateUi(p);
            long now = System.currentTimeMillis();
            duelInvites.entrySet().removeIf(e -> {
                Long t = inviteTimes.get(e.getKey());
                if (t != null && now - t > 60_000) { inviteTimes.remove(e.getKey()); Player p = Bukkit.getPlayer(e.getKey()); if (p != null) p.sendMessage(msg("&cDüello davetinin süresi doldu.")); return true; }
                return false;
            });
        }}.runTaskTimer(this, 20L, 40L);
        getLogger().info("MrPratic çekirdek sistemleri etkin.");
    }

    @Override public void onDisable() { saveRanks(); }
    private String c(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    private String msg(String s) { return c("&8[&aMrPratic&8] ") + c(s); }
    private String rank(Player p) { return ranks.getOrDefault(p.getUniqueId(), "Çaylak"); }
    private String rankPrefix(Player p) { return c(getConfig().getString("ranks.prefixes." + rank(p), "&7[Çaylak]")); }
    private void loadRanks() {
        if (getConfig().isConfigurationSection("player-ranks")) for (String id : getConfig().getConfigurationSection("player-ranks").getKeys(false)) {
            try { ranks.put(UUID.fromString(id), getConfig().getString("player-ranks." + id, "Çaylak")); } catch (IllegalArgumentException ignored) {}
        }
    }
    private void saveRanks() {
        for (Map.Entry<UUID,String> e : ranks.entrySet()) getConfig().set("player-ranks." + e.getKey(), e.getValue());
        saveConfig();
    }
    private void createLobby() {
        int y = 100; lobbySpawn = new Location(world, 0.5, y, 0.5);
        for (int x=-24;x<=24;x++) for(int z=-24;z<=24;z++) {
            Material floor = Math.abs(x)==24 || Math.abs(z)==24 ? Material.EMERALD_BLOCK : Material.POLISHED_DEEPSLATE;
            world.getBlockAt(x,y-1,z).setType(floor,false);
            world.getBlockAt(x,y,z).setType(Material.AIR,false);
            world.getBlockAt(x,y+1,z).setType(Material.AIR,false);
        }
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) world.getBlockAt(x,y-1,z).setType(Material.EMERALD_BLOCK,false);
        world.setSpawnLocation(lobbySpawn);
    }
    private void createArenas() {
        int i=0;
        for (Mode mode : Mode.values()) {
            int cx=150+(i%4)*100, cz=(i/4)*100; int y=100;
            for(int x=-18;x<=18;x++) for(int z=-18;z<=18;z++) {
                world.getBlockAt(cx+x,y-1,cz+z).setType(Math.abs(x)==18||Math.abs(z)==18?Material.OBSIDIAN:Material.SMOOTH_STONE,false);
                world.getBlockAt(cx+x,y,cz+z).setType(Material.AIR,false);
                world.getBlockAt(cx+x,y+1,cz+z).setType(Material.AIR,false);
            }
            arenaSpawns.put(mode,new Location(world,cx+0.5,y,cz+0.5));
            i++;
        }
        int y=100;
        for(int x=-18;x<=18;x++) for(int z=-18;z<=18;z++) {
            world.getBlockAt(2000+x,y-1,z).setType(Math.abs(x)==18||Math.abs(z)==18?Material.EMERALD_BLOCK:Material.DEEPSLATE_BRICKS,false);
            world.getBlockAt(2000+x,y,z).setType(Material.AIR,false);
            world.getBlockAt(2000+x,y+1,z).setType(Material.AIR,false);
        }
    }
    private void updateUi(Player p) {
        p.setPlayerListHeaderFooter(c("&8&m----------------\n&a&lMRPRATIC\n&7Practice Sunucusu"),
                c("\n&7Rank: ") + rankPrefix(p) + c("\n&7Online: &a" + Bukkit.getOnlinePlayers().size() + "\n&amrpratic.mcsh.gg\n&8&m----------------"));
        p.setPlayerListName(rankPrefix(p) + " &f" + p.getName());
        Scoreboard b=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o=b.registerNewObjective("mrpratic","dummy",c("&a&lMRPRATIC")); o.setDisplaySlot(DisplaySlot.SIDEBAR);
        Mode m=ffaPlayers.get(p.getUniqueId());
        o.getScore(c("&8&m------------------")).setScore(6);
        o.getScore(c("&7Oyuncu: &f"+p.getName())).setScore(5);
        o.getScore(c("&7Rank: "+rank(p))).setScore(4);
        o.getScore(c("&7Mod: &a"+(m==null?(duelOpponents.containsKey(p.getUniqueId())?"1v1":"Lobi"):m.display))).setScore(3);
        o.getScore(c("&7Online: &a"+Bukkit.getOnlinePlayers().size())).setScore(2);
        o.getScore(c("&amrpratic.mcsh.gg")).setScore(1);
        p.setScoreboard(b);
    }
    private void giveKit(Player p, Mode mode) {
        p.getInventory().clear(); p.getInventory().setArmorContents(null);
        p.setHealth(20); p.setFoodLevel(20); p.setFireTicks(0);
        for(PotionEffect effect:p.getActivePotionEffects()) p.removePotionEffect(effect.getType());
        switch(mode) {
            case CPVP -> { armor(p,Material.NETHERITE); p.getInventory().addItem(item(Material.NETHERITE_SWORD),item(Material.END_CRYSTAL,64),item(Material.OBSIDIAN,64),item(Material.END_CRYSTAL,64),item(Material.TOTEM_OF_UNDYING,2),item(Material.GOLDEN_APPLE,16),item(Material.EXPERIENCE_BOTTLE,32)); p.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING)); }
            case MACE -> { armor(p,Material.NETHERITE); p.getInventory().addItem(item(Material.MACE),item(Material.WIND_CHARGE,32),item(Material.ENDER_PEARL,16)); }
            case MIZRAK -> { armor(p,Material.IRON); p.getInventory().addItem(item(Material.TRIDENT),item(Material.SHIELD),item(Material.COOKED_BEEF,16)); }
            case KILIC, BALTA -> { armor(p,Material.DIAMOND); p.getInventory().addItem(item(mode==Mode.KILIC?Material.DIAMOND_SWORD:Material.DIAMOND_AXE),item(Material.GOLDEN_APPLE,8),item(Material.COBWEB,8)); }
            case COMBO, BOXING -> { armor(p,Material.IRON); p.getInventory().addItem(item(mode==Mode.BOXING?Material.STICK:Material.DIAMOND_SWORD)); }
            case SUMO -> { p.getInventory().addItem(item(Material.STICK)); }
            case BUILDUHC -> { armor(p,Material.IRON); p.getInventory().addItem(item(Material.DIAMOND_SWORD),item(Material.GOLDEN_APPLE, goldenCount()),item(Material.WATER_BUCKET),item(Material.LAVA_BUCKET),item(Material.OAK_PLANKS,64),item(Material.COBBLESTONE,64),item(Material.BOW),item(Material.ARROW,32)); }
            case NODEBUFF -> { armor(p,Material.DIAMOND); p.getInventory().addItem(item(Material.DIAMOND_SWORD),item(Material.ENDER_PEARL,16),item(Material.SPLASH_POTION,16),item(Material.GOLDEN_CARROT,16)); }
            case BEDWARS, THEBRIDGE -> { armor(p,Material.IRON); p.getInventory().addItem(item(Material.IRON_SWORD),item(Material.WHITE_WOOL,64),item(Material.GOLDEN_APPLE,4)); }
        }
    }
    private int goldenCount(){return 6;}
    private ItemStack item(Material m){return new ItemStack(m);}
    private ItemStack item(Material m,int amount){return new ItemStack(m,amount);}
    private void armor(Player p,Material m) {
        Material helmet=m==Material.NETHERITE?Material.NETHERITE_HELMET:m==Material.DIAMOND?Material.DIAMOND_HELMET:Material.IRON_HELMET;
        Material chest=m==Material.NETHERITE?Material.NETHERITE_CHESTPLATE:m==Material.DIAMOND?Material.DIAMOND_CHESTPLATE:Material.IRON_CHESTPLATE;
        Material legs=m==Material.NETHERITE?Material.NETHERITE_LEGGINGS:m==Material.DIAMOND?Material.DIAMOND_LEGGINGS:Material.IRON_LEGGINGS;
        Material boots=m==Material.NETHERITE?Material.NETHERITE_BOOTS:m==Material.DIAMOND?Material.DIAMOND_BOOTS:Material.IRON_BOOTS;
        p.getInventory().setArmorContents(new ItemStack[]{item(boots),item(legs),item(chest),item(helmet)});
    }
    private void joinFfa(Player p,Mode mode) {
        leaveModes(p); ffaPlayers.put(p.getUniqueId(),mode); giveKit(p,mode);
        p.teleport(arenaSpawns.get(mode)); p.sendMessage(msg("&a"+mode.display+" FFA arenasına katıldın!"));
    }
    private void leaveModes(Player p) {
        ffaPlayers.remove(p.getUniqueId());
        UUID opponent=duelOpponents.remove(p.getUniqueId());
        if(opponent!=null) duelOpponents.remove(opponent);
        duelQueue.remove(p.getUniqueId());
        leaveParty(p, false);
        spectatorReturns.remove(p.getUniqueId());
        p.getInventory().clear(); p.getInventory().setArmorContents(null);
    }
    private void openFfaMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,c("&2&lMrPratic &8» &aFFA Modları"));
        int slot=10; for(Mode m:Mode.values()) { ItemStack it=item(m.icon); ItemMeta meta=it.getItemMeta(); meta.setDisplayName(c("&a"+m.display)); meta.setLore(List.of(c("&7Katılmak için tıkla"))); it.setItemMeta(meta); inv.setItem(slot,it); slot++; if(slot==17) slot=19; }
        p.openInventory(inv);
    }
    private void startDuel(Player a,Player b) {
        leaveModes(a); leaveModes(b);
        duelOpponents.put(a.getUniqueId(),b.getUniqueId()); duelOpponents.put(b.getUniqueId(),a.getUniqueId());
        lastOpponents.put(a.getUniqueId(),b.getUniqueId()); lastOpponents.put(b.getUniqueId(),a.getUniqueId());
        duelQueue.remove(a.getUniqueId()); duelQueue.remove(b.getUniqueId());
        a.setGameMode(GameMode.SURVIVAL); b.setGameMode(GameMode.SURVIVAL);
        giveKit(a,Mode.CPVP); giveKit(b,Mode.CPVP);
        a.teleport(new Location(world,1994.5,100,0.5)); b.teleport(new Location(world,2005.5,100,0.5));
        a.sendMessage(msg("&a"+b.getName()+" ile düello başladı!")); b.sendMessage(msg("&a"+a.getName()+" ile düello başladı!"));
    }
    private void endDuel(Player loser,Player winner) {
        duelOpponents.remove(loser.getUniqueId()); duelOpponents.remove(winner.getUniqueId());
        lastOpponents.put(loser.getUniqueId(),winner.getUniqueId()); lastOpponents.put(winner.getUniqueId(),loser.getUniqueId());
        addStat(winner,"kills",1); addStat(loser,"deaths",1);
        loser.sendMessage(msg("&cDüelloyu kaybettin.")); winner.sendMessage(msg("&aDüelloyu kazandın!"));
        Bukkit.getScheduler().runTask(this,()->{ loser.teleport(lobbySpawn); winner.teleport(lobbySpawn); loser.getInventory().clear(); winner.getInventory().clear(); });
    }
    private void addStat(Player p,String key,int amount) {
        String path="stats."+p.getUniqueId()+"."+key; getConfig().set(path,getConfig().getInt(path)+amount); saveConfig();
    }
    private void leaveParty(Player p,boolean notify) {
        UUID id=p.getUniqueId();UUID leader=partyOf.remove(id);if(leader==null)return;
        Set<UUID> members=parties.get(leader);if(members==null)return;members.remove(id);
        if(members.isEmpty()){parties.remove(leader);return;}
        if(leader.equals(id)){UUID next=members.iterator().next();Set<UUID> updated=ConcurrentHashMap.newKeySet();updated.addAll(members);parties.remove(leader);parties.put(next,updated);for(UUID member:updated)partyOf.put(member,next);leader=next;}
        if(notify){Player owner=Bukkit.getPlayer(leader);if(owner!=null)owner.sendMessage(msg("&e"+p.getName()+" partiden ayrıldı."));p.sendMessage(msg("&aPartiden ayrıldın."));}
    }
    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p=e.getPlayer(); ranks.putIfAbsent(p.getUniqueId(),"Çaylak");
        e.setJoinMessage(c("&8[&a+&8] ")+rankPrefix(p)+" &f"+p.getName());
        Bukkit.getScheduler().runTaskLater(this,()->{ if(p.isOnline()){ if(getConfig().getBoolean("server.teleport-on-join",true)) p.teleport(lobbySpawn); updateUi(p); p.sendMessage(msg("&aHoş geldin! &7/ffa yazarak oyun seçebilirsin.")); }},2L);
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        Player p=e.getPlayer(); UUID opponent=duelOpponents.remove(p.getUniqueId());
        if(opponent!=null){duelOpponents.remove(opponent); Player other=Bukkit.getPlayer(opponent); if(other!=null){other.sendMessage(msg("&cRakibin oyundan çıktı, düelloyu kazandın.")); other.teleport(lobbySpawn);}}
        ffaPlayers.remove(p.getUniqueId()); duelInvites.remove(p.getUniqueId()); inviteTimes.remove(p.getUniqueId());
        duelQueue.remove(p.getUniqueId());
        leaveParty(p,false);
        partyInvites.remove(p.getUniqueId());
        rematchRequests.remove(p.getUniqueId());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        Player p=e.getEntity(); Player killer=p.getKiller();
        if(duelOpponents.containsKey(p.getUniqueId())) {
            Player opponent=Bukkit.getPlayer(duelOpponents.get(p.getUniqueId()));
            if(opponent!=null) endDuel(p,opponent);
            e.setKeepInventory(true); e.getDrops().clear(); e.setDroppedExp(0);
            Bukkit.getScheduler().runTask(this,()->p.spigot().respawn());
            return;
        }
        Mode mode=ffaPlayers.get(p.getUniqueId());
        if(mode!=null) {
            addStat(p,"deaths",1); if(killer!=null){addStat(killer,"kills",1); killer.sendMessage(msg("&a"+p.getName()+" oyuncusunu yendin."));}
            e.setKeepInventory(true); e.getDrops().clear(); e.setDroppedExp(0);
            Bukkit.getScheduler().runTask(this,()->{ if(p.isOnline()){p.spigot().respawn(); Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()){giveKit(p,mode);p.teleport(arenaSpawns.get(mode));}},2L);}});
        } else if(killer!=null) addStat(killer,"kills",1);
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) {
        Player p=e.getPlayer(); Mode mode=ffaPlayers.get(p.getUniqueId());
        if(mode!=null && arenaSpawns.containsKey(mode)) e.setRespawnLocation(arenaSpawns.get(mode));
        else e.setRespawnLocation(lobbySpawn);
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p)) return;
        if(e.getView().getTitle().equals(c("&2&lMrPratic &8» &aFFA Modları"))) {
            e.setCancelled(true); ItemStack it=e.getCurrentItem(); if(it==null||it.getType()==Material.AIR)return;
            String name=ChatColor.stripColor(it.getItemMeta().getDisplayName()); if(name!=null){Mode m=Mode.parse(name);if(m!=null){p.closeInventory();joinFfa(p,m);}}
        }
    }
    @EventHandler public void onChat(AsyncPlayerChatEvent e) { e.setFormat(rankPrefix(e.getPlayer())+" &f"+e.getPlayer().getName()+" &8» &7%2$s"); e.setFormat(c(e.getFormat())); }

    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a) {
        String n=cmd.getName().toLowerCase(Locale.ROOT);
        if(n.equals("spawn")) { if(s instanceof Player p){leaveModes(p);spectatorReturns.remove(p.getUniqueId());p.setGameMode(GameMode.SURVIVAL);p.teleport(lobbySpawn);p.sendMessage(msg("&aLobiye döndün."));} else s.sendMessage("Oyuncu komutu."); return true; }
        if(n.equals("ffa")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){openFfaMenu(p);return true;}
            if(a[0].equalsIgnoreCase("çık")||a[0].equalsIgnoreCase("leave")){leaveModes(p);p.teleport(lobbySpawn);p.sendMessage(msg("&aFFA'dan çıktın."));return true;}
            Mode m=Mode.parse(a[0]); if(m==null){p.sendMessage(msg("&cMod bulunamadı. &7/ffa"));return true;} joinFfa(p,m);return true;
        }
        if(n.equals("kit")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){p.sendMessage(msg("&aKitler: &fCPvP, Mace, Mızrak, Kılıç, Balta, Combo, NoDebuff, BuildUHC, Sumo, Boxing"));return true;}
            Mode m=Mode.parse(a[0]); if(m==null){p.sendMessage(msg("&cBilinmeyen kit."));return true;} giveKit(p,m);p.sendMessage(msg("&a"+m.display+" kiti verildi."));return true;
        }
        if(n.equals("duello")||n.equals("duel")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){p.sendMessage(msg("&eKullanım: /duello <oyuncu> | /duello kabul <oyuncu> | /duello red <oyuncu>"));return true;}
            if(a[0].equalsIgnoreCase("kabul")&&a.length>1){Player target=Bukkit.getPlayerExact(a[1]);if(target==null||!p.getUniqueId().equals(duelInvites.get(target.getUniqueId()))){p.sendMessage(msg("&cBu oyuncudan bekleyen davet yok."));return true;}duelInvites.remove(target.getUniqueId());inviteTimes.remove(target.getUniqueId());startDuel(p,target);return true;}
            if(a[0].equalsIgnoreCase("red")&&a.length>1){Player target=Bukkit.getPlayerExact(a[1]);if(target!=null){duelInvites.remove(target.getUniqueId());inviteTimes.remove(target.getUniqueId());target.sendMessage(msg("&cDüello davetin reddedildi."));}return true;}
            Player target=Bukkit.getPlayerExact(a[0]);if(target==null||target.equals(p)){p.sendMessage(msg("&cOyuncu bulunamadı."));return true;}
            duelInvites.put(target.getUniqueId(),p.getUniqueId());inviteTimes.put(target.getUniqueId(),System.currentTimeMillis());
            p.sendMessage(msg("&a"+target.getName()+" oyuncusuna davet gönderildi."));target.sendMessage(msg("&e"+p.getName()+" sana düello attı! &a/duello kabul "+p.getName()));return true;
        }
        if(n.equals("istatistik")) { if(!(s instanceof Player p))return true;String id=p.getUniqueId().toString();s.sendMessage(msg("&aİstatistiklerin: &fÖldürme "+getConfig().getInt("stats."+id+".kills")+" &8| &cÖlüm "+getConfig().getInt("stats."+id+".deaths")));return true; }
        if(n.equals("rank")) {
            if(a.length==0){if(s instanceof Player p)s.sendMessage(msg("&7Rankin: ")+rankPrefix(p));return true;}
            if(a.length>=3&&s.hasPermission("mrpratic.admin")&&a[0].equalsIgnoreCase("ayarla")){Player t=Bukkit.getPlayerExact(a[1]);if(t==null){s.sendMessage(msg("&cOyuncu çevrimiçi değil."));return true;}ranks.put(t.getUniqueId(),a[2]);saveRanks();t.sendMessage(msg("&aRankin güncellendi: "+a[2]));return true;}
            s.sendMessage(msg("&cKullanım: /rank veya /rank ayarla <oyuncu> <rank>"));return true;
        }
        if(n.equals("arena")) {s.sendMessage(msg("&aOluşturulan FFA arenaları:"));for(Mode m:Mode.values())s.sendMessage(c("&7- &f"+m.display));return true;}
        if(n.equals("liderlik")) {
            String stat=a.length>0&&a[0].equalsIgnoreCase("ölüm")?"deaths":"kills";
            if(a.length>0&&!a[0].equalsIgnoreCase("ölüm")&&!a[0].equalsIgnoreCase("öldürme")){s.sendMessage(msg("&cKullanım: /liderlik [öldürme|ölüm]"));return true;}
            if(!getConfig().isConfigurationSection("stats")){s.sendMessage(msg("&eHenüz istatistik yok."));return true;}
            List<String> ids=new ArrayList<>(getConfig().getConfigurationSection("stats").getKeys(false));
            ids.sort((x,y)->Integer.compare(getConfig().getInt("stats."+y+"."+stat),getConfig().getInt("stats."+x+"."+stat)));
            s.sendMessage(msg("&a&l"+(stat.equals("kills")?"EN ÇOK ÖLDÜRME":"EN ÇOK ÖLÜM")));
            int place=0;for(String id:ids){if(place>=10)break;int value=getConfig().getInt("stats."+id+"."+stat);if(value<=0)continue;String playerName;try{playerName=Optional.ofNullable(Bukkit.getOfflinePlayer(UUID.fromString(id)).getName()).orElse("Bilinmeyen");}catch(IllegalArgumentException ex){playerName="Bilinmeyen";}place++;s.sendMessage(c("&e"+place+". &f"+playerName+" &8» &a"+value));}
            if(place==0)s.sendMessage(msg("&7Henüz kayıtlı sonuç yok."));return true;
        }
        if(n.equals("parti")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){UUID leader=partyOf.getOrDefault(p.getUniqueId(),p.getUniqueId());Set<UUID> members=parties.get(leader);p.sendMessage(msg("&aParti komutları: &f/parti kur, /parti davet <oyuncu>, /parti kabul, /parti ayrıl, /parti liste"));if(members!=null){StringJoiner names=new StringJoiner(", ");for(UUID id:members){Player member=Bukkit.getPlayer(id);names.add(member==null?id.toString():member.getName());}p.sendMessage(msg("&7Partin: &f"+names));}return true;}
            String sub=a[0].toLowerCase(Locale.ROOT);
            if(sub.equals("kur")){if(partyOf.containsKey(p.getUniqueId())){p.sendMessage(msg("&cZaten bir partidesin."));return true;}Set<UUID> members=ConcurrentHashMap.newKeySet();members.add(p.getUniqueId());parties.put(p.getUniqueId(),members);partyOf.put(p.getUniqueId(),p.getUniqueId());p.sendMessage(msg("&aParti kuruldu."));return true;}
            if(sub.equals("davet")&&a.length>1){Player target=Bukkit.getPlayerExact(a[1]);if(target==null||target.equals(p)){p.sendMessage(msg("&cOyuncu bulunamadı."));return true;}UUID leader=partyOf.getOrDefault(p.getUniqueId(),p.getUniqueId());Set<UUID> members=parties.get(leader);if(members==null||!leader.equals(p.getUniqueId())){p.sendMessage(msg("&cParti lideri değilsin. Önce /parti kur."));return true;}partyInvites.put(target.getUniqueId(),leader);target.sendMessage(msg("&e"+p.getName()+" seni partisine davet etti. &a/parti kabul"));p.sendMessage(msg("&aParti daveti gönderildi."));return true;}
            if(sub.equals("kabul")){UUID leader=partyInvites.remove(p.getUniqueId());Set<UUID> members=leader==null?null:parties.get(leader);if(members==null){p.sendMessage(msg("&cBekleyen parti davetin yok."));return true;}leaveParty(p,false);members.add(p.getUniqueId());partyOf.put(p.getUniqueId(),leader);for(UUID id:members){Player member=Bukkit.getPlayer(id);if(member!=null)member.sendMessage(msg("&a"+p.getName()+" partiye katıldı."));}return true;}
            if(sub.equals("ayrıl")||sub.equals("leave")){leaveParty(p,true);return true;}
            if(sub.equals("liste")){UUID leader=partyOf.getOrDefault(p.getUniqueId(),p.getUniqueId());Set<UUID> members=parties.get(leader);if(members==null){p.sendMessage(msg("&cBir partide değilsin."));return true;}StringJoiner names=new StringJoiner(", ");for(UUID id:members){Player member=Bukkit.getPlayer(id);names.add(member==null?id.toString():member.getName());}p.sendMessage(msg("&aParti üyeleri: &f"+names));return true;}
            p.sendMessage(msg("&cKullanım: /parti kur|davet <oyuncu>|kabul|ayrıl|liste"));return true;
        }
        if(n.equals("arkadaş")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){p.sendMessage(msg("&aKullanım: /arkadaş ekle <oyuncu>, /arkadaş sil <oyuncu>, /arkadaş liste"));return true;}
            String path="friends."+p.getUniqueId();String sub=a[0].toLowerCase(Locale.ROOT);
            if(sub.equals("liste")){List<String> names=getConfig().getStringList(path);p.sendMessage(msg("&aArkadaşların: &f"+(names.isEmpty()?"Yok":String.join(", ",names))));return true;}
            if(a.length<2){p.sendMessage(msg("&cOyuncu adı yazmalısın."));return true;}
            List<String> names=new ArrayList<>(getConfig().getStringList(path));String target=a[1];
            if(sub.equals("ekle")){if(target.equalsIgnoreCase(p.getName())){p.sendMessage(msg("&cKendini ekleyemezsin."));return true;}if(names.stream().anyMatch(x->x.equalsIgnoreCase(target))){p.sendMessage(msg("&eBu oyuncu zaten arkadaş listende."));return true;}names.add(target);getConfig().set(path,names);saveConfig();p.sendMessage(msg("&a"+target+" arkadaş listene eklendi."));return true;}
            if(sub.equals("sil")){boolean removed=names.removeIf(x->x.equalsIgnoreCase(target));getConfig().set(path,names);saveConfig();p.sendMessage(msg(removed?"&aArkadaş silindi.":"&cBu oyuncu listende yok."));return true;}
            p.sendMessage(msg("&cKullanım: /arkadaş ekle|sil <oyuncu> veya liste"));return true;
        }
        if(n.equals("sıra")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length>0&&(a[0].equalsIgnoreCase("çık")||a[0].equalsIgnoreCase("leave"))){duelQueue.remove(p.getUniqueId());p.sendMessage(msg("&eEşleştirme kuyruğundan çıktın."));return true;}
            if(duelOpponents.containsKey(p.getUniqueId())||ffaPlayers.containsKey(p.getUniqueId())){p.sendMessage(msg("&cÖnce mevcut maçından çıkmalısın."));return true;}
            if(!duelQueue.add(p.getUniqueId())){p.sendMessage(msg("&eZaten kuyruktasın."));return true;}
            p.sendMessage(msg("&aCPvP eşleştirme kuyruğuna girdin. &7/ sıra çık"));
            List<UUID> waiting=new ArrayList<>(duelQueue);waiting.remove(p.getUniqueId());
            for(UUID id:waiting){Player other=Bukkit.getPlayer(id);if(other!=null&&other.isOnline()){duelQueue.remove(id);duelQueue.remove(p.getUniqueId());startDuel(p,other);return true;}}
            p.sendMessage(msg("&7Rakip bekleniyor..."));return true;
        }
        if(n.equals("rematch")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){p.sendMessage(msg("&eRövanş için: /rematch <oyuncu> veya /rematch kabul"));return true;}
            if(a[0].equalsIgnoreCase("kabul")){UUID requester=rematchRequests.remove(p.getUniqueId());if(requester==null||!p.getUniqueId().equals(lastOpponents.get(requester))){p.sendMessage(msg("&cBekleyen rövanş isteği yok."));return true;}Player other=Bukkit.getPlayer(requester);if(other==null){p.sendMessage(msg("&cRakibin çevrimiçi değil."));return true;}startDuel(p,other);return true;}
            Player target=Bukkit.getPlayerExact(a[0]);if(target==null||target.equals(p)||!target.getUniqueId().equals(lastOpponents.get(p.getUniqueId()))){p.sendMessage(msg("&cSon rakibin çevrimiçi değil veya bu oyuncu son rakibin değil."));return true;}rematchRequests.put(target.getUniqueId(),p.getUniqueId());target.sendMessage(msg("&e"+p.getName()+" rövanş istiyor. &a/rematch kabul"));p.sendMessage(msg("&aRövanş isteği gönderildi."));return true;
        }
        if(n.equals("izle")) {
            if(!(s instanceof Player p)){s.sendMessage("Oyuncu komutu.");return true;}
            if(a.length==0){p.sendMessage(msg("&eKullanım: /izle <oyuncu> veya /izle çık"));return true;}
            if(a[0].equalsIgnoreCase("çık")||a[0].equalsIgnoreCase("leave")){Location back=spectatorReturns.remove(p.getUniqueId());p.setGameMode(GameMode.SURVIVAL);p.teleport(back==null?lobbySpawn:back);p.sendMessage(msg("&aİzleme modundan çıktın."));return true;}
            Player target=Bukkit.getPlayerExact(a[0]);if(target==null||target.equals(p)||!duelOpponents.containsKey(target.getUniqueId())){p.sendMessage(msg("&cİzlenecek aktif düello oyuncusu bulunamadı."));return true;}spectatorReturns.putIfAbsent(p.getUniqueId(),p.getLocation());p.setGameMode(GameMode.SPECTATOR);p.teleport(target.getLocation());p.sendMessage(msg("&a"+target.getName()+" izleniyor. Çıkmak için /izle çık"));return true;
        }

        if(n.equals("mrpratic")) {if(!s.hasPermission("mrpratic.admin")){s.sendMessage(msg("&cYetkin yok."));return true;}if(a.length>0&&a[0].equalsIgnoreCase("kaydet")){saveRanks();s.sendMessage(msg("&aVeriler kaydedildi."));}else s.sendMessage(msg("&aMrPratic aktif. &7/ffa, /duello, /kit, /istatistik"));return true;}
        return false;
    }
}
