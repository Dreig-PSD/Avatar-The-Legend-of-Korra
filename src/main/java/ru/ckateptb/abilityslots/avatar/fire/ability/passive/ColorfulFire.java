package ru.ckateptb.abilityslots.avatar.fire.ability.passive;

import org.bukkit.Color;
import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.fire.ability.elements.FireElement;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.particle.Particle;

import java.util.concurrent.ThreadLocalRandom;

@AbilityInfo(
        author = "Dreig_Michihi",
        name = "ColorfulFire",
        displayName = "ColorfulFire",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "fire",
        description = "This passive ability allows FireBender to manipulate the colorful fire shown by Ran and Shaw dragons.\nThis is original form of firebending, which could possibly give even more powerful bending than based on anger and rage.",
        instruction = "Passive Ability, that increases attack range and reduces their energy cost and cooldown.",
        canBindToSlot = false
)

public class ColorfulFire extends Ability {
    public static Color[] fireColors = {
            hexColor("E7FE0E"),//яркий жёлтый
            hexColor("0AF200"),//зелёный
            hexColor("00FF80"),//бирюзоватый
            hexColor("9447E1"),//фиолетовый
            hexColor("F394FF"),//пурпурный
            hexColor("C90100"),//красный
    };

    public static Color hexColor(String hexVal) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (hexVal.startsWith("#")) {
            hexVal = hexVal.substring(1);
        }

        if (hexVal.length() <= 6) {
            r = Integer.valueOf(hexVal.substring(0, 2), 16);
            g = Integer.valueOf(hexVal.substring(2, 4), 16);
            b = Integer.valueOf(hexVal.substring(4, 6), 16);
        }
        return Color.fromRGB(r, g, b);
    }

    public static void display(AbilityUser user, Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if(random.nextFloat()<0.3) {
            Particle.REDSTONE.display(location, amount, offsetX, offsetY, offsetZ, 0,
                    new org.bukkit.Particle.DustOptions
                            (fireColors[random.nextInt(fireColors.length)],
                                    random.nextFloat(0.9f, 1.8f)));
            if (FireElement.isBlueFireBender(user)) {
                Particle.FLAME.display(location, amount, offsetX, offsetY, offsetZ, extra);
            } else {
                Particle.SOUL_FIRE_FLAME.display(location, amount, offsetX, offsetY, offsetZ, extra);
            }
        }
    }

    @Override
    public ActivateResult activate(ActivationMethod method) {
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }
}
