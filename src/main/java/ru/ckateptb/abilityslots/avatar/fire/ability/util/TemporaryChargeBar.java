package ru.ckateptb.abilityslots.avatar.fire.ability.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.temporary.AbstractTemporary;
import ru.ckateptb.tablecloth.temporary.TemporaryUpdateState;

@Getter
@Setter
public class TemporaryChargeBar extends AbstractTemporary {
    private BossBar bossBar;
    private AbilityUser user;
    private long duration;
    private long startTime;

    public TemporaryChargeBar(String title, long duration, AbilityUser user) {
        this(title, BarColor.BLUE, BarStyle.SOLID, duration, user);
    }

    public TemporaryChargeBar(String title, BarColor color, BarStyle style, long duration, AbilityUser user) {
        this.bossBar = Bukkit.createBossBar(title, color, style);
        this.duration = duration;
        this.user = user;
        this.startTime = System.currentTimeMillis();
        this.register();
    }

    @Override
    public void init() {
        Validate.notNull(user);
        this.bossBar.setProgress(0);
        this.bossBar.addPlayer((Player) user.getEntity());
    }

    @Override
    public TemporaryUpdateState update() {
        if (user.isDead()||!user.isOnline()) return TemporaryUpdateState.REVERT;
        if (duration > 0) {
            double spendMs = System.currentTimeMillis() - this.startTime;
            double subtract = spendMs / this.duration;
            double progress = Math.min(1, subtract);
            this.bossBar.setProgress(progress);
        }
        return TemporaryUpdateState.CONTINUE;
    }

    @Override
    public void revert() {
        this.bossBar.removePlayer((Player)user.getEntity());
    }
}