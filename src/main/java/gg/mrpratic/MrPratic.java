package gg.mrpratic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MrPratic extends JavaPlugin implements Listener {
    private final Map<UUID, String> ranks = new ConcurrentHashMap<>();
    private World lobby;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("server.auto-create-lobby", true)) {
            createLobby();
        }
        new BukkitRunnable() {
            @Override public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) updatePlayerUi(player);
            }
        }.runTaskTimer(this, 20L, 40L);
        getLogger().info("MrPratic etkinlestirildi. Turkce Practice altyapisi hazir.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MrPratic kapatildi.");
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String prefix() {
        return color(getConfig().getString("theme.prefix", "&8[&aMrPratic&8] "));
    }

    private String rank(Player player) {
        return ranks.getOrDefault(player.getUniqueId(), getConfig().getString("ranks.default", "Çaylak"));
    }

    private String rankPrefix(Player player) {
        return color(getConfig().getString("ranks.prefixes." + rank(player), "&7[Çaylak]"));
    }

    private void createLobby() {
        String worldName = getConfig().getString("server.lobby-world", "mrpratic_lobi");
        lobby = Bukkit.getWorld(worldName);
        if (lobby == null) lobby = new WorldCreator(worldName).environment(World.Environment.NORMAL).createWorld();
        if (lobby == null) {
            getLogger().warning("Lobi dunyasi olusturulamadi.");
            return;
        }
        lobby.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        lobby.setTime(1000L);
        int y = Math.max(lobby.getMinHeight() + 5, 100);
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                Material floor = (Math.abs(x) == 20 || Math.abs(z) == 20)
                        ? Material.EMERALD_BLOCK : Material.POLISHED_DEEPSLATE;
                Block block = lobby.getBlockAt(x, y - 1, z);
                block.setType(floor, false);
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    lobby.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                lobby.getBlockAt(x, y - 1, z).setType(Material.EMERALD_BLOCK, false);
            }
        }
        lobby.setSpawnLocation(0, y, 0);
        getLogger().info("MrPratic lobisi hazir: " + worldName);
    }

    private void updatePlayerUi(Player player) {
        String title = color(getConfig().getString("scoreboard.title", "&a&lMRPRATIC"));
        player.setPlayerListHeaderFooter(
                color("&8&m--------------------\n&a&lMRPRATIC\n&7Practice Sunucusu\n"),
                color("\n&7Oyuncu: &f" + player.getName() + "\n&7Rank: ") + rankPrefix(player)
                        + color("\n&7Online: &a" + Bukkit.getOnlinePlayers().size()
                        + "\n&7Adres: &amrpratic.mcsh.gg\n&8&m--------------------"));

        player.setPlayerListName(rankPrefix(player) + " &f" + player.getName());
        if (!getConfig().getBoolean("scoreboard.enabled", true)) return;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("mrpratic", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore(color("&8&m----------------")).setScore(6);
        objective.getScore(color("&7Oyuncu: &f" + player.getName())).setScore(5);
        objective.getScore(color("&7Rank: ") + rankPrefix(player)).setScore(4);
        objective.getScore(color("&7Mod: &aLobi")).setScore(3);
        objective.getScore(color("&7Online: &a" + Bukkit.getOnlinePlayers().size())).setScore(2);
        objective.getScore(color("&amrpratic.mcsh.gg")).setScore(1);
        player.setScoreboard(board);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ranks.putIfAbsent(player.getUniqueId(), getConfig().getString("ranks.default", "Çaylak"));
        event.setJoinMessage(color("&8[&a+&8] ") + rankPrefix(player) + " &f" + player.getName());
        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) return;
                if (getConfig().getBoolean("server.teleport-on-join", true) && lobby != null) {
                    player.teleport(new Location(lobby, 0.5, lobby.getSpawnLocation().getY(), 0.5));
                }
                updatePlayerUi(player);
                player.sendMessage(prefix() + color("&aMrPratic'e hos geldin!"));
            }
        }.runTaskLater(this, 2L);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String format = rankPrefix(player) + " &f" + player.getName() + " &8» &7%2$s";
        event.setFormat(color(format).replace("%2$s", "%2$s"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("spawn") || cmd.equals("lobi")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Bu komut oyuncular icindir."); return true; }
            if (lobby == null) { player.sendMessage(prefix() + color("&cLobi dunyasi bulunamadi.")); return true; }
            player.teleport(lobby.getSpawnLocation().add(0.5, 0, 0.5));
            player.sendMessage(prefix() + color("&aLobiye isinlandin."));
            return true;
        }
        if (cmd.equals("rank") || cmd.equals("rutbe") || cmd.equals("rütbe")) {
            if (sender instanceof Player player) sender.sendMessage(prefix() + color("&7Rankin: ") + rankPrefix(player));
            else sender.sendMessage("Oyuncu komutu.");
            return true;
        }
        if (cmd.equals("kit")) {
            sender.sendMessage(prefix() + color("&aKit sistemi: &7KitEditor ve kit secimi sonraki gelistirme asamasinda."));
            return true;
        }
        if (cmd.equals("ffa")) {
            sender.sendMessage(prefix() + color("&aFFA modlari: &fCPvP, Mace, Mizrak, Kilic, Balta, Combo, NoDebuff, BuildUHC, Sumo, Boxing."));
            sender.sendMessage(prefix() + color("&7Arena eslestirme altyapisi sonraki asamada eklenecek."));
            return true;
        }
        if (cmd.equals("duello")) {
            sender.sendMessage(prefix() + color("&aKullanim: &f/duello <oyuncu>"));
            sender.sendMessage(prefix() + color("&7Kit secimli 1v1 sistemi sonraki asamada eklenecek."));
            return true;
        }
        if (cmd.equals("istatistik")) {
            sender.sendMessage(prefix() + color("&7Istatistik kaydi sonraki gelistirme asamasinda eklenecek."));
            return true;
        }
        if (cmd.equals("arena")) {
            sender.sendMessage(prefix() + color("&7Arena yonetimi sonraki gelistirme asamasinda eklenecek."));
            return true;
        }
        if (cmd.equals("mrpratic")) {
            if (!sender.hasPermission("mrpratic.admin")) { sender.sendMessage(prefix() + color("&cYetkin yok.")); return true; }
            sender.sendMessage(prefix() + color("&aMrPratic temel altyapisi aktif."));
            sender.sendMessage(color("&7Komutlar: /spawn, /kit, /ffa, /duello, /rank, /istatistik, /arena"));
            return true;
        }
        return false;
    }
}
