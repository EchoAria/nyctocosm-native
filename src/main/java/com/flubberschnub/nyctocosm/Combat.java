package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;

/** All hits go through Forge's ordinary LivingEntity damage pipeline, including other mods. */
public final class Combat {
    public static double distanceToBoxSqr(Vec3 point,AABB box){
        double x=Math.max(box.minX,Math.min(point.x,box.maxX)),y=Math.max(box.minY,Math.min(point.y,box.maxY)),z=Math.max(box.minZ,Math.min(point.z,box.maxZ));
        return point.distanceToSqr(x,y,z);
    }
    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target == owner || target instanceof ArmorStand || target.isSpectator()) return false;
        if (target instanceof Player player && player.isCreative()) return false;
        if (owner == null) return true;
        if (owner.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        return !(owner instanceof Player p && target instanceof Player other) || p.canHarmPlayer(other);
    }
    public static boolean hit(LivingEntity owner, LivingEntity target, float amount, Element element) {
        if (!canHit(owner,target) || amount <= 0) return false;
        return target.hurt(source(target.level(),owner,element),amount);
    }
    public static net.minecraft.world.damagesource.DamageSource source(net.minecraft.world.level.Level level,LivingEntity owner,Element element){
        var type = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
            new net.minecraft.resources.ResourceLocation(NativeMod.ID, element.name().toLowerCase(java.util.Locale.ROOT)));
        var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return new net.minecraft.world.damagesource.DamageSource(holder,owner);
    }
    public static void area(ServerLevel level, LivingEntity owner, Vec3 center, double radius, float amount, Element element, double push) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius))) {
            if (target.getBoundingBox().getCenter().distanceToSqr(center) > radius*radius || !canHit(owner,target)) continue;
            if (hit(owner,target,amount,element) && push != 0) {
                Vec3 motion = target.position().subtract(center).normalize().scale(push);
                target.push(motion.x,.2,motion.z);
                target.hurtMarked = true;
            }
        }
    }
    public static Vec3 endpoint(LivingEntity owner, double distance) {
        Vec3 start = owner.getEyePosition();
        return owner.level().clip(new ClipContext(start,start.add(owner.getLookAngle().scale(distance)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getLocation();
    }
    public static Vec3 beam(ServerLevel level, LivingEntity owner, double range, float amount, Element element, double width) {
        return beam(level,owner,range,amount,element,width,true);
    }
    public static Vec3 beam(ServerLevel level, LivingEntity owner, double range, float amount, Element element, double width,boolean defaultParticles) {
        Vec3 start = owner.getEyePosition(), end = endpoint(owner,range);
        if(element==Element.RESONANCE&&owner instanceof Player)NativePortalBlock.activate(level,net.minecraft.core.BlockPos.containing(end.add(owner.getLookAngle().scale(.05))));
        line(level,owner,start,end,amount,element,width,defaultParticles);
        return end;
    }
    public static void line(ServerLevel level,LivingEntity owner,Vec3 start,Vec3 end,float amount,Element element,double width,boolean defaultParticles){
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(width))) {
            if (target.getBoundingBox().inflate(width).clip(start,end).isPresent() || target.getBoundingBox().contains(start)) hit(owner,target,amount,element);
        }
        if(defaultParticles){
            Vec3 delta=end.subtract(start);
            for (double d=0; d<delta.length(); d+=.5) {
                Vec3 p=start.add(delta.normalize().scale(d));
                level.sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,1,0,0,0,0);
            }
        }
    }
    public enum Element { DREAM, RADIANCE, RESONANCE, NIGHTMARE, VOID, DISSONANCE }
    private Combat() {}
}
