package com.imitatorgame.event;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.game.WinConditionChecker;
import com.imitatorgame.role.Faction;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FloodingEvent implements TimedGameEvent {

    private int remainingTicks;
    private final int durationTicks;
    private boolean valve1Fixed;
    private boolean valve2Fixed;
    private boolean resolved;
    private BossBar bossBar;

    public FloodingEvent(int durationSeconds) {
        this.durationTicks = durationSeconds * Constants.TICKS_PER_SECOND;
        this.remainingTicks = this.durationTicks;
    }

    @Override
    public void onActivate(GameSession session) {
        session.broadcastMessage(Constants.PREFIX + "§1§l水淹警报！管道破裂！");
        session.broadcastMessage(Constants.PREFIX + "§7前往东沐浴间和盥洗室关闭阀门！");
        session.broadcastMessage(Constants.PREFIX + "§c如果在 " + (durationTicks / 20) + " 秒内未修复，模仿者获胜！");

        bossBar = BossBar.bossBar(
                Component.text("水淹倒计时 - 关闭阀门！").color(TextColor.color(0x5555FF)),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );

        for (var uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showBossBar(bossBar);
                p.playSound(p.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.5f);
            }
        }
    }

    @Override
    public void onTick(GameSession session) {
        remainingTicks--;
        float progress = Math.max(0, (float) remainingTicks / durationTicks);
        if (bossBar != null) {
            bossBar.progress(progress);
            bossBar.name(Component.text("水淹倒计时: " + (remainingTicks / 20) + " 秒 - 关闭阀门！")
                    .color(remainingTicks < 200 ? TextColor.color(0xFF5555) : TextColor.color(0x5555FF)));
        }

        if (remainingTicks <= 0 && !resolved) {
            session.broadcastMessage(Constants.PREFIX + "§4§l水淹时间到！模仿者获胜！");
            WinConditionChecker checker = new WinConditionChecker(session);
            checker.checkFloodingImitatorWin();
        }
    }

    @Override
    public void onDeactivate(GameSession session) {
        if (bossBar != null) {
            for (var uuid : session.getAlivePlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.hideBossBar(bossBar);
            }
            bossBar = null;
        }
    }

    @Override
    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    public boolean tryFixValve(int valveNum, Player player, GameSession session) {
        if (valveNum == 1 && valve1Fixed) return false;
        if (valveNum == 2 && valve2Fixed) return false;

        Location fixLoc = valveNum == 1
                ? session.getPlugin().getConfigManager().getMapConfig().getFloodingFix1()
                : session.getPlugin().getConfigManager().getMapConfig().getFloodingFix2();

        if (fixLoc != null && player.getLocation().distance(fixLoc) > 5) {
            player.sendMessage(Constants.PREFIX + "§c你离阀门太远了");
            return false;
        }

        if (valveNum == 1) valve1Fixed = true;
        else valve2Fixed = true;

        player.sendMessage(Constants.PREFIX + "§a阀门 #" + valveNum + " 已关闭！");
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.0f);

        if (valve1Fixed && valve2Fixed) {
            resolved = true;
            session.broadcastMessage(Constants.PREFIX + "§a所有阀门已关闭！水淹危机解除！");
            session.broadcastMessage(Constants.PREFIX + "§a模仿者未能通过水淹获胜。");
            onDeactivate(session);
        }
        return true;
    }
}
