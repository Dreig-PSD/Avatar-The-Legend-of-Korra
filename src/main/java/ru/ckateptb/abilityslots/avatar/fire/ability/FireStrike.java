package ru.ckateptb.abilityslots.avatar.fire.ability;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.fire.ability.elements.FireElement;
import ru.ckateptb.abilityslots.common.util.MaterialUtils;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;
import ru.ckateptb.tablecloth.temporary.fallingblock.TemporaryFallingBlock;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "Dreig_Michihi",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.SNEAK},
        category = "fire",
        cooldown = 1000,
        cost = 5,
        description = "desc",
        instruction = "instr",
        displayName = "Огненный выстрел",
        name = "FireStrike"
)

public class FireStrike extends Ability {

    private static final Map<AbilityUser, MainHand> lastPunch = new HashMap<>();

    @ConfigField
    private static double damage = 2.0;
    @ConfigField
    private static double maxRange = 25;
    @ConfigField
    private static double speed = 1.5;
    @ConfigField
    private static double radius = 0.5;
    @ConfigField
    private static double controlRange = 7;
    @ConfigField
    private static long chargeTime = 1000;
    @ConfigField
    private static double chargeFactor = 1.5;

    private RemovalConditional removal;//проверка на регион это тоже в removal можно?
    private RemovalConditional controlRemoval;//проверка на регион это тоже в removal можно?
    private ImmutableVector origin;
    private ImmutableVector location;
    private ImmutableVector direction;
    private Collider collider;
    private long startTime;
    private boolean launch;
    private ThreadLocalRandom random = ThreadLocalRandom.current();
    private boolean charged;
    private MainHand attackHand = null;
    private boolean directable = true;
    private double cooldownFactor = 1;
    private double factor = 1;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.startTime = System.currentTimeMillis();
        //ImmutableVector mainHandLocation = user.getEyeLocation().add(0, -0.3, 0).add(user.getDirection());
        this.removal = new RemovalConditional.Builder()//ты что-то говорил про способ дебага
                .offline()
                .dead()
                .world()
                //.slot()
                .range(() -> this.origin.toLocation(world), () -> this.location.toLocation(world), () -> maxRange * factor)
                .canUse(() -> location.toLocation(world))
                .build();
        this.controlRemoval = new RemovalConditional.Builder()
                .range(() -> this.origin.toLocation(world), () -> this.location.toLocation(world), () -> controlRange * factor)
                .build();
        /*if (method == ActivationMethod.LEFT_CLICK)
            launch();*/
        boolean oldMet;
        if (method == ActivationMethod.SNEAK) {//для удара из приседа выбирается неосновная рука
            attackHand = (livingEntity instanceof Player player ? player.getMainHand() == MainHand.LEFT ? MainHand.RIGHT : MainHand.LEFT : null);
            directable = false;
        } else {
            for (FireStrike strike : getAbilityInstanceService().getAbilityUserInstances(user, FireStrike.class)) {
                if (!strike.launch) {
                    if (strike.user.isSneaking()) {//клик сидя означает, что удар был совершён основной рукой
                        strike.attackHand = (livingEntity instanceof Player player ? player.getMainHand() : null);
                    }//а так уже выбрана неосновная перед этой проверкой
                    strike.updateCharging();//последний вызов этого метода установит origin и т.д. в центральное положение если способность заряжена
                    if (strike.charged) {//если способность заряжена, будет проигран звук заряженного выстрела
                        world.playSound(this.user.getHandLocation(strike.attackHand).toLocation(world), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.01f);
                    }//при любом выстреле звук огня
                    world.playSound(this.user.getHandLocation(strike.attackHand).toLocation(world), Sound.ENTITY_ZOMBIE_INFECT, 0.5f, 0.5f);
                    strike.launch = true;
                    if (lastPunch.containsKey(this.user))
                        if (lastPunch.get(this.user) != strike.attackHand)
                            strike.cooldownFactor = 0.5;
                    lastPunch.put(this.user, strike.attackHand);
                    strike.user.setCooldown(this.getInformation(), (long) (getInformation().getCooldown() * strike.cooldownFactor));
                    return ActivateResult.NOT_ACTIVATE;//способность уже активирована
                }
            }
            livingEntity.sendMessage("cooldown: " + getInformation().getCooldown() * cooldownFactor);
            user.setCooldown(this.getInformation(), (long) (getInformation().getCooldown() * cooldownFactor));
            attackHand = (livingEntity instanceof Player player ? player.getMainHand() : null);
            world.playSound(this.user.getHandLocation(this.attackHand).toLocation(world), Sound.ENTITY_ZOMBIE_INFECT, 0.5f, 0.5f);
            this.launch = true;
            //return ActivateResult.ACTIVATE;
        }
        if (!updateCharging())
            return ActivateResult.NOT_ACTIVATE;
        return ActivateResult.ACTIVATE;
    }

    private boolean updateCharging() {
        if (!this.charged && System.currentTimeMillis() > (this.startTime + chargeTime)) {
            this.charged = true;
            world.playSound(this.user.getEyeLocation().toLocation(world), Sound.ENTITY_ZOMBIE_INFECT, 0.6f, 0.2f);
            factor = chargeFactor;
        }
        if (this.charged) {
            this.origin = getHandLocation(null, 0, 0, 0.4);
        } else
            this.origin = getHandLocation(attackHand, 0.5, 1.2, 0.4);
        if (origin.toBlock(world).isLiquid() ||
                !user.canUse(origin.toLocation(world))
                || !user.removeEnergy(this)
                || user.getSelectedAbility() != this.getInformation()) {//
            return false;
        }
        this.location = this.origin;
        this.direction = user.getDirection();
        return true;
    }

    private ImmutableVector getHandLocation(MainHand hand, double sideways, double up, double forward) {
        ImmutableVector direction = user.getDirection();
        if (hand == null) return user.getEyeLocation().add(direction.multiply(forward));
        double angle = Math.toRadians(user.getYaw());
        ImmutableVector location = user.getLocation();
        ImmutableVector offset = direction.multiply(forward).add(0, up, 0);
        ImmutableVector vector = new ImmutableVector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(sideways);
        return (hand == MainHand.LEFT ? location.add(vector) : location.subtract(vector)).add(offset);
    }

    private void displayCharge() {
        FireElement.display(user, getHandLocation(MainHand.LEFT, 0.4, 0.9, 0.5).toLocation(world),
                1, 0.01, 0.01f, 0.01f, 0.01f);
        FireElement.display(user, getHandLocation(MainHand.RIGHT, 0.4, 0.9, 0.5).toLocation(world),
                1, 0.01, 0.01f, 0.01f, 0.01f);
    }

    private void renderDirectFlame(ImmutableVector location, ImmutableVector direction, double size) {
        FireElement.display(user, location.toLocation(world), (int) Math.ceil(radius * radius * radius), -0.01, (float) (size / 3), (float) (size / 3), (float) (size / 3));//красивск
        for (int i = 0; i < (int) Math.ceil(size * size * size); i++) {
            FireElement.display(user,
                    location.add(random.nextDouble(-size, size), random.nextDouble(-size, size), random.nextDouble(-size, size)).toLocation(world)
                    , 0, random.nextFloat(0.5f, 0.7f),//мб и extra можно чуть чуть рандомизировать
                    (float) -direction.getX() + random.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getY() + random.nextFloat(-0.1f, 0.1f),
                    (float) -direction.getZ() + random.nextFloat(-0.1f, 0.1f));
        }
    }

    private double angle = 0;

    private void render() {
        if (this.charged) {
            ImmutableVector x = new ImmutableVector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
            ImmutableVector y = this.direction.crossProduct(x).normalize();
            ImmutableVector left = this.location
                    .add(x.multiply(Math.cos(angle)).multiply(radius * 0.7))
                    .add(y.multiply(Math.sin(angle)).multiply(radius * 0.7));
            ImmutableVector right = this.location
                    .add(x.multiply(Math.cos(angle + Math.PI)).multiply(radius * 0.7))
                    .add(y.multiply(Math.sin(angle + Math.PI)).multiply(radius * 0.7));
            //FireElement.display(user, location.toLocation(world), (int)Math.ceil(radius*radius*radius)/3, 0.2, (float) (radius*factor), (float) (radius*factor), (float) (radius*factor));//красивск
            renderDirectFlame(left, this.direction.add(this.location.subtract(left).normalize().multiply(random.nextFloat(0.4f))).normalize(), radius * 0.7);
            renderDirectFlame(right, this.direction.add(this.location.subtract(right).normalize().multiply(0.1)).normalize(), radius * 0.7);
            angle += ((attackHand == MainHand.RIGHT) ? 1 : -1) * Math.PI / 10;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
            if (angle < 0) angle += 2 * Math.PI;
        } else {
            renderDirectFlame(this.location, this.direction.multiply(-1), radius * 0.7);
        }
    }


    private void explode() {
        FireElement.display(user, location.toLocation(world), 5 * (int) Math.ceil(radius * radius * radius * factor * factor * factor),
                0.2, (float) (radius * factor), (float) (radius * factor), (float) (radius * factor));
        Particle.EXPLOSION_HUGE.display(location.toLocation(world), 1, 0f, 0f, 0f);
        world.playSound(this.user.getEyeLocation().toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.75f);
        Set<Entity> affectedEntities = new HashSet<>();
        SphereCollider collider = new SphereCollider(world, this.location, radius * factor * 1.5);
        collider.handleEntityCollision(
                livingEntity, entity -> {
                    if (!affectedEntities.contains(entity)) {
                        affectedEntities.add(entity);
                        if (entity instanceof LivingEntity targetLiving) {
                            affectLiving(targetLiving);
                            FireElement.display(user, targetLiving.getEyeLocation(), 5, 0.2, 0.3f, 0.3f, 0.3f);
                        } else {
                            entity.setFireTicks((int) (3000 * factor) / 50);
                            AbilityTarget.of(entity).setVelocity(entity.getLocation().subtract(this.location).multiply(factor).toVector().setY(0.2), this);
                        }
                    }
                    return CollisionCallbackResult.CONTINUE;
                }
        );
        collider.handleBlockCollisions(false, true, block -> {
            BlockData bd = block.getBlockData();
            new TemporaryBlock(block.getLocation(), Material.AIR.createBlockData(), 5000);
            TemporaryFallingBlock tmpBlock = new TemporaryFallingBlock(block.getLocation(), bd, 20, false, true);
            tmpBlock.getFallingBlock().setVelocity(direction.multiply(-1).add(
                    random.nextFloat(-0.5f, 0.5f),
                    random.nextFloat(-0.5f, 0.5f),
                    random.nextFloat(-0.5f, 0.5f)
            ).normalize());
            return CollisionCallbackResult.CONTINUE;
        });
        FireElement.igniteBlocks(user, this.location.add(this.direction).toLocation(world), radius * factor * 1.5);
    }

    private void affectLiving(LivingEntity targetLiving) {
        if (targetLiving.getFireTicks() > 0)
            targetLiving.setNoDamageTicks(0);
        AbilityTarget.of(targetLiving).setVelocity(targetLiving.getEyeLocation().subtract(this.location).multiply(factor).toVector().setY(0), this);
        AbilityTarget.of(targetLiving).damage(damage * factor, this);
        targetLiving.setFireTicks((int) (3000 * factor) / 50);
    }

    @Override
    public UpdateResult update() {
        if (this.removal.shouldRemove(user, this)) {
            return UpdateResult.REMOVE;
        }
        if (this.launch) {
            double speedFactor = Math.min(chargeFactor, 0.75 + (this.origin.distance(this.location) / maxRange) * (chargeFactor - 0.75));
            //renderDirectFlame(this.location, this.direction);
            double stepCount = Math.ceil(speed * speedFactor / radius * factor);
            ImmutableVector accurate = user.getDirection().subtract(direction).multiply(1 / stepCount);
            this.collider = new SphereCollider(world, this.location, radius * factor);
            for (int i = 0; i <= stepCount; i++) {
                render();
                if (
                        this.collider.handleEntityCollision(livingEntity, false, entity -> {
                            if (entity instanceof LivingEntity targetLiving) {
                                //LivingEntity targetLiving = (LivingEntity) entity;
                                if (!charged) {
                                    affectLiving(targetLiving);
                                } else {
                                    explode();
                                }
                                FireElement.display(user, targetLiving.getEyeLocation(), 15, 0.2, 0.3f, 0.3f, 0.3f);
                                return CollisionCallbackResult.END;
                            } else {
                                entity.setFireTicks((int) (3000 * factor) / 50);
                                entity.setVelocity(direction.multiply(factor).setY(0));
                                return CollisionCallbackResult.CONTINUE;
                            }
                        })
                                || new SphereCollider(world, this.location, 0.1)
                                .handleBlockCollisions(true, false, block -> {
                                    if (MaterialUtils.isWater(block)) {
                                        Particle.CLOUD.display(this.location.add(direction.multiply(-1)).toLocation(world), 7, radius * factor, radius * factor, radius * factor, 0.1);
                                    }
                                    if (block.isSolid()) {
                                        if (charged)
                                            explode();
                                        else
                                            FireElement.igniteBlocks(user, this.getLocation().toLocation(world), radius * factor);
                                    }
                                    FireElement.display(user, block.getLocation().toCenterLocation(), 15, 0.2, 0.3f, 0.3f, 0.3f);
                                    return CollisionCallbackResult.END;
                                }, block -> MaterialUtils.isWater(block) || block.isSolid())
                ) {
                    return UpdateResult.REMOVE;
                }
                if (!this.controlRemoval.shouldRemove(user, this) && directable) {
                    direction = direction.add(accurate).normalize();
                }
                this.location = location.add(direction.multiply(speed * speedFactor / stepCount));
                //Particle.END_ROD.display(location.toLocation(world), i,0f,0f,0f,0f); //debug
            }
        } else {
            if (!updateCharging())
                return UpdateResult.REMOVE;
            if (this.charged) {
                displayCharge();
            }
            if (!user.isSneaking()) {
                livingEntity.swingOffHand();
            }
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
    }
}
