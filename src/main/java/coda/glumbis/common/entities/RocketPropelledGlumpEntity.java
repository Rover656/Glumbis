package coda.glumbis.common.entities;

import coda.glumbis.common.registry.GlumbisEntities;
import coda.glumbis.common.registry.GlumbisParticles;
import coda.glumbis.common.registry.GlumbisSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;
import java.util.function.Predicate;

public class RocketPropelledGlumpEntity extends AbstractHurtingProjectile implements IAnimatable, IAnimationTickable {
    public static final Predicate<LivingEntity> NOT_CATS = (p_20436_) -> !(p_20436_ instanceof GlumpEntity) && !(p_20436_ instanceof GlumbossEntity) && !(p_20436_ instanceof Cat);
    private final AnimationFactory factory = new AnimationFactory(this);

    public RocketPropelledGlumpEntity(EntityType<? extends AbstractHurtingProjectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    public RocketPropelledGlumpEntity(Level p_36861_, double p_36862_, double p_36863_, double p_36864_) {
        this(GlumbisEntities.ROCKET_PROPELLED_GLUMP.get(), p_36861_);
        this.setPos(p_36862_, p_36863_, p_36864_);
    }

    public RocketPropelledGlumpEntity(LivingEntity p_36718_, Level p_36719_) {
        this(p_36719_, p_36718_.getX(), p_36718_.getEyeY() - (double)0.1F, p_36718_.getZ());
        this.setOwner(p_36718_);
        this.xPower = 0.5F;
        this.yPower = 0.5F;
        this.zPower = 0.5F;
    }

    @Override
    protected float getInertia() {
        return super.getInertia();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        for (LivingEntity entity : getNearbyEntities()) {
            explode(entity);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult p_36755_) {
        super.onHitBlock(p_36755_);
        Vec3 vec3 = p_36755_.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3);
        Vec3 vec31 = vec3.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);
        this.onGround = true;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {

        // Explode when in ground
        if (onGround && tickCount % 30 == 0) {

            for (LivingEntity entity : getNearbyEntities()) {
                explode(entity);
            }
        }

        // Forget the target if it loses line of sight
        /*if (getTarget() != null && !getTarget().hasLineOfSight(this)) {

        }*/

        // Movement
        // TODO - make it continue straight if it loses its target so it doesnt find a new target
        if (!isOnGround() && getBlockStateOn().isAir()) {
            if (getTarget() != null) {
                Vec3 entityToTarget = getTarget().position().subtract(position());
                Vec3 direction = entityToTarget.normalize();
                setDeltaMovement(direction);

                System.out.println(getTarget());
            }
        }
        setDeltaMovement(getDeltaMovement().multiply(0.35, 0.35, 0.35));

        Vec3 direction = getDeltaMovement();
        double angle = Mth.atan2(direction.z, direction.x);
        setYRot((float) Math.toDegrees(angle));

        // Explode after 15 seconds
        if (tickCount > 300) {
            for (LivingEntity entity : getNearbyEntities()) {
                explode(entity);
            }
        }

        super.tick();
    }

    private List<LivingEntity> getNearbyEntities() {
        return level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10));
    }

    private void explode(LivingEntity entity) {
        playSound(GlumbisSounds.GLUMP_EXPLODE.get(),2.0F, 1.0F);

        for(int i = 0; i < 20; i++) {
            this.level.addParticle(GlumbisParticles.STATIC_LIGHTNING.get(), this.getRandomX(3.5D), (this.getPosition(1.0f).y() - 0.5) , this.getRandomZ(3.5D), 0, this.getRandomY() * 2, 0);
        }
        tryHurtEntity(entity);
        discard();
    }

    protected void tryHurtEntity(LivingEntity entity) {
        double distanceTo = distanceToSqr(entity);
        float damage = 1 - Mth.sqrt((float) distanceTo) / 10;
        entity.hurt(DamageSource.explosion(entity), (0.5F * damage + 0.5F) * 2);

        double x = (0.5F * (entity.getX() - getX()) + 0.5F) * 2.0;
        double z = (0.5F * (entity.getZ() - getZ()) + 0.5F) * 2.0;

        System.out.println("entity = " + entity.getType() + ", x = " + x + ", z = " + z);
        entity.setDeltaMovement(entity.getDeltaMovement().add(x, 0.15, z));
    }

    protected ParticleOptions getTrailParticle() {
        return GlumbisParticles.STATIC_LIGHTNING.get();
    }

    public LivingEntity getTarget() {
        return level.getNearestEntity(LivingEntity.class, TargetingConditions.forCombat(), null, getX(), getY(), getZ(), getBoundingBox().inflate(50));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    @Override
    public int tickTimer() {
        return tickCount;
    }
}
