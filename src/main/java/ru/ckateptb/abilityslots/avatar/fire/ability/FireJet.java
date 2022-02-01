package ru.ckateptb.abilityslots.avatar.fire.ability;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.MainHand;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.fire.ability.elements.FireElement;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.concurrent.ThreadLocalRandom;

@Getter//создаёт get методы для всех переменных, объявленных в этом объекте
@AbilityInfo(
        author = "Dreig_Michihi",
        name = "FireJet",
        displayName = "Реактивный огонь",
        activationMethods = {/*ActivationMethod.SNEAK, */ActivationMethod.LEFT_CLICK},
        category = "fire",
        description = "desc",
        instruction = "instr",
        cooldown = 5000,
        cost = 10
)
public class FireJet extends Ability {

    @ConfigField(comment = "Длительность полёта. Устновите 0, чтобы выключить.")
    private static long duration = 1500;
    @ConfigField
    private static long energyCostInterval = 1000;
    @ConfigField
    private static double speed = 1.0;

    private FireBlockDamage listener;
    private RemovalConditional removal;
    private ImmutableVector direction;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        if (getAbilityInstanceService().destroyInstanceType(user, FireJet.class)) {//способность активировалась при получении урона
            FireElement.display(user, livingEntity.getLocation(), 15, -0.2, 0.3f, 0.3f, 0.3f);
            return ActivateResult.NOT_ACTIVATE;
        }

        Location location = livingEntity.getLocation();//жесть... а в IntelliJ просто Ctrl+F6 :D
        Block block = location.getBlock();
        direction = user.getDirection();
        //user.canUse(user.getLocation.toLocation(world))
        if (!user.canUse(location) || block.isLiquid()) {
            return ActivateResult.NOT_ACTIVATE;// Ctrl+Shift+F - reformat code
        }

        this.removal = new RemovalConditional.Builder()
                .offline()//аналог проверок bPlayer в конструкторах у PK
                .dead()
                .world()
                .passableNoLiquid(true)
                .duration(duration)
                .costInterval(energyCostInterval)
                .canUse(() -> livingEntity.getLocation())
                .build();
        this.listener = new FireBlockDamage(user);
        Bukkit.getPluginManager().registerEvents(listener, AbilitySlots.getInstance());
        if (FireElement.igniteBlock(user, block))
            FireElement.display(user, block.getLocation().toCenterLocation(), 15, -0.2, 0.3f, 0.3f, 0.3f);
        livingEntity.setFireTicks(0);
        return ActivateResult.ACTIVATE;
    }
    private Location getLegLocation(MainHand hand) {
        ImmutableVector direction = getDirection();
        if (hand == null) return user.getEyeLocation().add(direction.multiply(0.4)).toLocation(world);
        double angle = Math.toRadians(user.getYaw());
        ImmutableVector location = user.getLocation();
        ImmutableVector vector = new ImmutableVector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(0.2);
        return (hand == MainHand.LEFT ? location.add(vector) : location.subtract(vector)).toLocation(world);
    }
    @Override
    public UpdateResult update() {//это как progress()
        if (this.removal.shouldRemove(user, this)) {
            return UpdateResult.REMOVE;//если что-то из removal не совпало, то 'remove()'

        }
        Location leftLeg = getLegLocation(MainHand.LEFT);
        Location rightLeg = getLegLocation(MainHand.RIGHT);
        /*Location leftLeg = user.getHandLocation(MainHand.LEFT).add(0, -1.2, 0).toLocation(world);
        leftLeg.add(user.getLocation().subtract(leftLeg.toVector()).multiply(0.1));
        Location rightLeg = user.getHandLocation(MainHand.RIGHT).add(0, -1.2, 0).toLocation(world);
        rightLeg.add(user.getLocation().subtract(rightLeg.toVector()).multiply(0.1));*/
        ThreadLocalRandom random = ThreadLocalRandom.current();
        //FireElement.display(user, leftLeg, 2, 0.08, 0.2f, 0.2f, 0.2f);
        //FireElement.display(user, rightLeg, 2, 0.08, 0.2f, 0.2f, 0.2f);
        FireElement.display(user, leftLeg, 3, -0.05, 0.1f, 0.1f, 0.1f);
        FireElement.display(user, rightLeg, 3, -0.05, 0.1f, 0.1f, 0.1f);
        for (int i = 0; i < 5; i++) {
            FireElement.display(user,
                    leftLeg.add(random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1))
                    , 0, random.nextFloat((float) 0.5),//мб и extra можно чуть чуть рандомизировать
                    (float) -direction.getX() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f),
                    (float) -direction.getY() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f),
                    (float) -direction.getZ() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f));//тут случайно нету метода, чтобы добавить к location рандомный вектор?
        }
        for (int i = 0; i < 5; i++) {
            FireElement.display(user,
                    rightLeg.add(random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1))
                    , 0, random.nextFloat((float) 0.5),//мб и extra можно чуть чуть рандомизировать
                    (float) -direction.getX() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f),
                    (float) -direction.getY() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f),
                    (float) -direction.getZ() + random.nextFloat(-i * 0.1f - 0.01f, i * 0.1f + 0.01f));//тут случайно нету метода, чтобы добавить к location рандомный вектор?
        }
        /*for (int i = 0; i < 3; i++) {
            float[] randomXYZ = {random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f)};
            FireElement.display(user, leftLeg.add(randomXYZ[0],randomXYZ[1],randomXYZ[2]*//*random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f)*//*)
                    , 0, random.nextFloat(0.5f, 0.8f),
                    (float) -direction.getX() + randomXYZ[0],//random.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getY() + randomXYZ[1],//.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getZ() + randomXYZ[2]);//random.nextFloat(-0.1f, 0.1f));
            FireElement.display(user, rightLeg.add(randomXYZ[0],randomXYZ[1],randomXYZ[2]*//*random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f), random.nextFloat(-0.1f, 0.1f)*//*)
                    , 0, random.nextFloat(0.5f, 0.8f),
                    (float) -direction.getX() + randomXYZ[0],//random.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getY() + randomXYZ[1],//random.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getZ() + randomXYZ[2]);//random.nextFloat(-0.1f, 0.1f));
        }*/
        /*FireElement.display(user, livingEntity.getLocation(),
                7, 0.1, 0.5f, 0.5f, 0.5f, true);*/
        if (user.getSelectedAbility() == this.getInformation()) {
            direction = user.getDirection();
        }
        user.setVelocity(direction.multiply(speed), this);//
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
        EntityDamageEvent.getHandlerList().unregister(listener);
    }

    public static class FireBlockDamage implements Listener {
        private final AbilityUser user;

        public FireBlockDamage(AbilityUser user) {
            this.user = user;
        }

        @EventHandler(ignoreCancelled = true)
        public void onAbilityUserDamage(EntityDamageEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity livingEntity) {
                EntityDamageEvent.DamageCause cause = event.getCause();
                if (livingEntity == user.getEntity()) {
                    if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {//FIRE - урон от FireTick
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
