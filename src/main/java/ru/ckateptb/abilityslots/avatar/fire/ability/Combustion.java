package ru.ckateptb.abilityslots.avatar.fire.ability;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.fire.ability.elements.FireElement;
import ru.ckateptb.abilityslots.avatar.fire.ability.util.TemporaryChargeBar;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "Dreig_Michihi",
        name = "Combustion",
        displayName = "Воспламенение",
        instruction = "instr",
        description = "desc",
        cost = 0.2,
        cooldown = 3000,
        category = "combustion",
        activationMethods = {ActivationMethod.SNEAK}
)
@CollisionParticipant(destroyAbilities = {
        FireStrike.class
})

public class Combustion extends Ability {

    private enum CombustStage {CHARGING, LAUNCH, CURVE, EXPLOSION}

    //на 0 и 0.4 от прогресса CURVE проигрывать небольшой взрыв, а на прогрессе в 1 уже EXPLOSION!!1!1!
    @ConfigField
    private static double range = 60;
    @ConfigField
    private static double minAimRange = 25;
    @ConfigField
    private static long chargeTime = 2000;
    @ConfigField
    private static double speed = 2.0;
    @ConfigField
    private static double damage = 7.0;
    @ConfigField
    private static double explosionRadius = 3.5;
    @ConfigField
    private static double maxAngle = 0.42;
    @ConfigField
    private static double startCurveRange = 9;

    // если угол между direction и originDirection становится больше чем maxAngle, то происходит взрыв
    // чем ближе к нулю угол между direction и originDirection, тем ближе к currectChargeFactor будет усиление взрыва.

    private RemovalConditional removal;
    private RemovalConditional launchRemoval;
    private CombustStage currentStage;
    private ImmutableVector origin;
    private ImmutableVector originDirection;
    private ImmutableVector location;
    private ImmutableVector startCurveLocation;
    private ImmutableVector aimCurveLocation;
    private ImmutableVector endCurveLocation;
    private ImmutableVector direction;
    private TemporaryChargeBar chargeBar;
    private double particlesAngle = 3 * Math.PI;
    private Collider collider;
    private Set<Entity> affectedEntities;
    private double progress = 0;
    private double progressStep;
    private double aimAngle = 0;
    private boolean selfExplosion;
    private OnDamage onDamage;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.chargeBar = new TemporaryChargeBar(this.getInformation().getDisplayName(), BarColor.RED, BarStyle.SOLID, chargeTime, this.user);
        this.currentStage = CombustStage.CHARGING;
        updateCharging();
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> this.location.toLocation(world))
                .build();
        this.launchRemoval = new RemovalConditional.Builder()
                .range(() -> this.origin.toLocation(world), () -> this.location.toLocation(world), startCurveRange)
                .build();
        this.collider = new SphereCollider(world, this.location, 0.1);
        affectedEntities = new HashSet<>();
        this.onDamage = new OnDamage(this.user);
        Bukkit.getPluginManager().registerEvents(onDamage, AbilitySlots.getInstance());
        return ActivateResult.ACTIVATE;
    }

    private void render() {
        if (this.currentStage != CombustStage.EXPLOSION) {
            ImmutableVector x = new ImmutableVector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
            ImmutableVector y = this.direction.crossProduct(x).normalize();
            double r = 0.2 - 0.2 * this.origin.distance(this.location) / range;
            ImmutableVector left = this.location
                    .add(x.multiply(Math.cos(particlesAngle)).multiply(r))
                    .add(y.multiply(Math.sin(particlesAngle)).multiply(r));
            ImmutableVector right = this.location
                    .add(x.multiply(Math.cos(particlesAngle + Math.PI)).multiply(r))
                    .add(y.multiply(Math.sin(particlesAngle + Math.PI)).multiply(r));

            Particle.REDSTONE.display(location.toLocation(world), 3, 0.01, 0.01, 0.01,
                    new org.bukkit.Particle.DustOptions
                            (Color.WHITE,
                                    0.65f));
            Particle.REDSTONE.display(left.toLocation(world), 1, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions
                            (Color.GRAY,
                                    0.5f));
            Particle.REDSTONE.display(right.toLocation(world), 1, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions
                            (Color.GRAY,
                                    0.5f));
            particlesAngle += Math.PI / 20;
            if (particlesAngle >= 4 * Math.PI) {//каждые 3 оборота
                renderRing(this.direction, location);
                particlesAngle -= 4 * Math.PI;
                world.playSound(this.location.toLocation(world), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, (float) progress);//эксперимент с звуками
            }
        } else {
            Particle.EXPLOSION_HUGE.display(location.toLocation(world));
        }
    }

    private void renderRing(ImmutableVector direction, ImmutableVector location) {
        double randStep = ThreadLocalRandom.current().nextDouble(2 * Math.PI / 20);
        double randRingSize = ThreadLocalRandom.current().nextDouble(0.2, 0.35);
        ImmutableVector x = new ImmutableVector(this.direction.getZ(), 0, -this.direction.getX()).normalize();
        ImmutableVector y = direction.crossProduct(x).normalize();
        for (double angle = randStep; angle < 2 * Math.PI + randStep; angle += 2 * Math.PI / 20) {
            ImmutableVector side = location
                    .add(x.multiply(Math.cos(angle)).multiply(explosionRadius))
                    .add(y.multiply(Math.sin(angle)).multiply(explosionRadius));
            Vector extension = side.subtract(location).normalize().add(direction.multiply(3)).normalize();
            //CLOUD
            Particle.CLOUD.display(location.toLocation(world), 0, extension.getX(), extension.getY(), extension.getZ(), randRingSize);
        }
    }

    private void renderSmallExplosion(ImmutableVector location) {
        //FLASH.display(this.location.toLocation(world));
        FireElement.display(user, location.toLocation(world), 5, 0.025, 0.1f, 0.1f, 0.1f);
        FireElement.display(user, location.toLocation(world), 5, 0.05, 0.1f, 0.1f, 0.1f);
        FireElement.display(user, location.toLocation(world), 5, 0.1, 0.1f, 0.1f, 0.1f);
        FireElement.display(user, location.toLocation(world), 5, 0.15, 0.1f, 0.1f, 0.1f);
        FireElement.display(user, location.toLocation(world), 5, 0.2, 0.1f, 0.1f, 0.1f);
        world.playSound(location.toLocation(world), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5, 1.2f);
        world.playSound(location.toLocation(world), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5, 0.0001f);
    }

    @Override
    public UpdateResult update() {
        if (this.removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        switch (this.currentStage) {
            case CHARGING -> {
                if (!updateCharging())
                    return UpdateResult.REMOVE;//просто отменить способность, если она не была нормально дозаряжена
            }
            case LAUNCH -> {
                if (this.launchRemoval.shouldRemove(user, this)) {
                    renderSmallExplosion(this.location);
                    this.startCurveLocation = this.location;
                    ImmutableVector aimDirection = this.user.getDirection();
                    if (originDirection.angle(aimDirection) > aimAngle) {
                        aimDirection = originDirection.rotateAroundAxis(originDirection.crossProduct(aimDirection), aimAngle);
                    }
                    this.endCurveLocation = this.user.getEyeLocation();
                    while (endCurveLocation.distance(origin) < range && !endCurveLocation.toBlock(world).isSolid() && !new SphereCollider(world, endCurveLocation, endCurveLocation.distance(origin) < startCurveRange ? 0.1 : explosionRadius / 2).handleEntityCollision(livingEntity, entity -> {
                        endCurveLocation = new ImmutableVector(entity.getLocation().add(0, 0.5, 0));
                        return CollisionCallbackResult.END;
                    })) {//пока дистанция позволяет и блок в локации не цельный
                        endCurveLocation = endCurveLocation.add(aimDirection.multiply(0.5));
                    }
                    if (endCurveLocation.distance(origin) < minAimRange)
                        this.endCurveLocation = this.user.getEyeLocation().add(aimDirection.multiply(minAimRange));
                    this.aimCurveLocation = this.startCurveLocation.add(this.originDirection.multiply((startCurveLocation.distance(endCurveLocation)) * 0.66));
                    ImmutableVector middlePoint = getBezierLocation(0.5);
                    this.progressStep = speed / (startCurveLocation.distance(middlePoint) + middlePoint.distance(endCurveLocation)); //скорость/примерная длина кривой
                    this.currentStage = CombustStage.CURVE;
                } else if (!updateLaunch()) {
                    explode();
                }
            }
            case CURVE -> {
                if (!updateCurve()) {
                    explode();
                }
            }
            case EXPLOSION -> {
                if (!updateExplosion())
                    return UpdateResult.REMOVE;//updateExplosion вернёт false если вызрыв сделать невозможно(например из-за нахождения снаряда в регионе)
            }
            default -> {
                return UpdateResult.REMOVE;//на всякий пожарный
            }
        }
        return UpdateResult.CONTINUE;
    }

    private void shoot() {
        this.aimAngle = this.chargeBar.getBossBar().getProgress() * maxAngle;//чем больше "заряжена" способность, тем сильнее можно будет сменить ей направление.
        this.direction = this.originDirection;
        Particle.EXPLOSION_NORMAL.display(location.toLocation(world), 0, direction.getX(), direction.getY(), direction.getZ(), 0.01);
        world.playSound(this.location.toLocation(world), Sound.ENTITY_ZOMBIE_INFECT, 0.4f, 1.5f);
        world.playSound(this.location.toLocation(world), Sound.ENTITY_ZOMBIE_INFECT, 0.4f, 0f);
        this.chargeBar.revert();
        this.currentStage = CombustStage.LAUNCH;
    }

    private void explode() {
        this.particlesAngle = 0;//нужно для инь-янь анимации перед взрывом
        this.currentStage = CombustStage.EXPLOSION;//чтобы начал прогрессировать код взрыва
        if (!this.secondSmallExplode)
            renderSmallExplosion(this.location);
        Particle.FLASH.display(this.location.toLocation(world));
    }

    private boolean updateCharging() {
        this.originDirection = this.user.getDirection();
        this.direction = this.originDirection;
        this.origin = this.user.getEyeLocation().add(originDirection.crossProduct(
                                new ImmutableVector(originDirection.getZ(), 0, -originDirection.getX())).normalize()
                        .multiply(user.isSneaking() ? 0.3 : 0.4))
                .add(this.originDirection.multiply(0.8));
        this.location = this.origin;
        chargeBar.update();
        if (selfExplosion) {
            this.location = this.location.add(this.direction);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (this.progress >= 1 || !secondSmallExplode) {
                secondSmallExplode = true;
                this.progress = 0;
                ImmutableVector rLoc = this.location.add(new Vector(random.nextFloat(-1, 1), random.nextFloat(-1, 1), random.nextFloat(-1, 1)).multiply(1.5));
                renderSmallExplosion(rLoc);
                //FireElement.display(this.user, rLoc.toLocation(world), 1, 0.1, 0.5f, 0.5f, 0.5f);
                //Particle.ELECTRIC_SPARK.display(rLoc.toLocation(world), 3, 0.5f, 0.5f, 0.5f);
                //world.playSound(rLoc.toLocation(world), Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1, (float) (0.5 * this.chargeBar.getBossBar().getProgress()));
            }
            this.progress += 0.15;
        }
        double barProgress = chargeBar.getBossBar().getProgress();
        if (user.getSelectedAbility() != this.getInformation()//сменил слот?
                || !user.isSneaking()
                || !(barProgress < 1 && this.user.removeEnergy(this))) {//или зарядил полностью или не осталось энергии
            if (selfExplosion) {
                if (barProgress >= 1) {
                    explode();
                    this.particlesAngle=Math.PI*1.5;
                    this.user.setCooldown(this.getInformation(), getInformation().getCooldown() * 3);
                }
            } else {
                if (barProgress > 0.25) {
                    shoot();
                    this.user.setCooldown(this);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean updateLaunch() {
        double stepCount = Math.ceil(speed / 0.1);
        for (int i = 0; i <= stepCount; i++) {
            this.location = location.add(originDirection.multiply(speed / stepCount));
            render();
            if (checkCollisions()) {
                return false;//столкновение = взрыв
            }
        }
        return true;
    }

    private boolean secondSmallExplode = false;

    private boolean updateCurve() {
        double stepCount = Math.ceil(speed / 0.1);
        ImmutableVector oldLocation = this.location;
        for (int i = 0; i <= stepCount; i++) {
            progress += progressStep / stepCount;
            this.location = getBezierLocation(progress);
            render();
            if (checkCollisions()) {
                return false;//столкновение = взрыв
            }
        }
        if (!secondSmallExplode && progress > 0.6) {
            renderSmallExplosion(this.location);
            secondSmallExplode = true;
        }
        this.direction = this.location.subtract(oldLocation).normalize();
        if (progress >= 1)
            explode();
        return true;
    }

    private boolean updateExplosion() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (particlesAngle < Math.PI * 2) {
            world.playSound(this.location.toLocation(world), Sound.ENTITY_CREEPER_PRIMED, 6f, (float) particlesAngle);//эксперимент с звуками
            FireElement.display(user, this.location.toLocation(world), 10, -0.1, 0.1f, 0.1f, 0.1f);
            particlesAngle += Math.PI / 2.5;//за пол секунды полный круг
            if (particlesAngle >= Math.PI * 2) {
                Particle.EXPLOSION_HUGE.display(location.toLocation(world));
                world.playSound(this.location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 6, 0f);
                world.playSound(this.location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 6, 0.8f);
            }
            return true;
        } else {
            this.collider = new SphereCollider(world, this.location, explosionRadius);
            this.collider.handleEntityCollision(this.livingEntity, false, true, entity -> {
                if (!affectedEntities.contains(entity)) {
                    affectEntity(entity);
                    affectedEntities.add(entity);
                }
                return CollisionCallbackResult.CONTINUE;
            });
            collider.handleBlockCollisions(false, true, block -> {
                BlockData bd = block.getBlockData();
                    /*BlockData clown = (FireElement.isBlueFireBender(user)
                            || (FireElement.isColorfulFireBender(user) && random.nextBoolean()) ?
                            Material.SOUL_FIRE : Material.FIRE).createBlockData();*/
                new TemporaryBlock(block.getLocation(), Material.AIR.createBlockData(), 5000);
                if (random.nextBoolean()) {
                    TemporaryFallingBlock tmpBlock = new TemporaryFallingBlock(block.getLocation(),
                            bd,
                            20, false, true);
                    tmpBlock.getFallingBlock().setVelocity(direction.multiply(-1).add(
                            random.nextFloat(-0.5f, 0.5f),
                            random.nextFloat(-0.5f, 0.5f),
                            random.nextFloat(-0.5f, 0.5f)
                    ).normalize());
                }
                return CollisionCallbackResult.CONTINUE;
            });
            for (int i = 0; i < 30; i++) {
                ImmutableVector randLoc = this.location.add(new Vector(
                        random.nextFloat(-1, 1),
                        random.nextFloat(-1, 1),
                        random.nextFloat(-1, 1)
                ).multiply(random.nextDouble(1) * explosionRadius));
                FireElement.display(user, this.location.toLocation(world), 15, random.nextDouble(0.1, 0.3)
                        , random.nextFloat((float) (explosionRadius / 3))
                        , random.nextFloat((float) (explosionRadius / 3))
                        , random.nextFloat((float) (explosionRadius / 3)));
                Particle.EXPLOSION_LARGE.display(randLoc.toLocation(world));
                Particle.EXPLOSION_NORMAL.display(randLoc.toLocation(world), 1, 0, 0, 0, 0.4);
            }
            //world.playSound(this.location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 6, 0.2f);
            return false;
        }
    }

    private void affectEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            Vector vec = living.getEyeLocation().subtract(this.location)
                    .toVector().normalize();
            if (vec.getY() > 0.5)
                vec.setY(0.5);
            if (vec.getY() < -0.5)
                vec.setY(-0.5);
            AbilityTarget.of(living).setVelocity(vec.multiply(3), this);
            AbilityTarget.of(living).damage(damage, this, true);
            living.setFireTicks(30);
        } else {
            AbilityTarget.of(entity).setVelocity(entity.getLocation().add(0, 0.5, 0).subtract(this.location).toVector().normalize(), this);
            entity.setFireTicks(30);
        }
    }

    private boolean checkCollisions() {
        this.collider = new SphereCollider(world, this.location.add(direction), 0.1);
        return this.collider
                .handleBlockCollisions(true, false, block -> CollisionCallbackResult.END)
                || new SphereCollider(world, this.location.add(direction), explosionRadius / 3).handleEntityCollision(this.livingEntity);
    }

    private ImmutableVector getSegmentPoint(ImmutableVector from, ImmutableVector to, double progress) {
        return from.add(to.subtract(from).multiply(progress));
    }

    private ImmutableVector getBezierLocation(double progress) {
        return getSegmentPoint(
                getSegmentPoint(startCurveLocation, aimCurveLocation, progress),
                getSegmentPoint(aimCurveLocation, endCurveLocation, progress),
                progress);
    }

    @Override
    public void destroy() {
        this.chargeBar.revert();
        EntityDamageEvent.getHandlerList().unregister(this.onDamage);
    }

    private static class OnDamage implements Listener {
        private final AbilityUser user;

        public OnDamage(AbilityUser user) {
            this.user = user;
        }

        @EventHandler(ignoreCancelled = true)
        public void onAbilityUserDamage(EntityDamageEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity livingEntity) {
                EntityDamageEvent.DamageCause cause = event.getCause();
                if (livingEntity == user.getEntity()) {
                    user.getEntity().sendMessage("debug1");
                    for (Ability ability : user.getActiveAbilities(Combustion.class)) {
                        user.getEntity().sendMessage("debug2");
                        if (ability instanceof Combustion combustion) {
                            user.getEntity().sendMessage("debug3");
                            combustion.selfExplosion = true;
                        }
                    }
                }
            }
        }
    }
}
