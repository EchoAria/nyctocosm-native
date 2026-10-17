package com.flubberschnub.nyctocosm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.entity.PartEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import java.util.function.IntUnaryOperator;

/** All hits go through Forge's ordinary LivingEntity damage pipeline, including other mods. */
public final class Combat {
    static final TagKey<EntityType<?>> LIVING_THINGS=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(NativeMod.ID,"livingthings"));
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
        return hit(owner,target,amount,element,bound->owner==null?target.level().getRandom().nextInt(bound):owner.getRandom().nextInt(bound));
    }
    static boolean hitGeneric(Entity direct,LivingEntity attacker,LivingEntity target,float amount){
        if(target==null||!target.isAlive()||target instanceof ArmorStand||target.isSpectator()||amount<=0)return false;
        if(attacker!=null&&!canHit(attacker,target))return false;
        var type=net.minecraft.world.damagesource.DamageTypes.GENERIC;
        var holder=target.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return target.hurt(new net.minecraft.world.damagesource.DamageSource(holder,direct,attacker),amount);
    }
    static boolean hit(LivingEntity owner, LivingEntity target, float amount, Element element,IntUnaryOperator traceRoll) {
        if (!canHit(owner,target) || amount <= 0) return false;
        boolean applied=target.hurt(source(target.level(),owner,element),amount);
        if(owner instanceof Player attacker && target.level() instanceof ServerLevel server)
            HypertraceService.onSpellHit(server,attacker,target,element,traceRoll);
        return applied;
    }
    public static LivingEntity damageTarget(Entity entity){
        if(entity instanceof LivingEntity living)return living;
        if(entity instanceof PartEntity<?> part&&part.getParent() instanceof LivingEntity living)return living;
        return null;
    }
    public static double distanceToDamageBoxSqr(Vec3 point,LivingEntity target){
        double distance=distanceToBoxSqr(point,target.getBoundingBox());
        if(target instanceof NativeBoss boss&&boss.isMultipartEntity())distance=Math.min(distance,boss.damagePartsDistanceToSqr(point));
        return distance;
    }
    private static void addMultipartTargets(ServerLevel level,AABB query,java.util.Set<LivingEntity> targets){
        // Forge parts are stored on their parent, not in Level's spatial entity sections. Scan the
        // loaded multipart bosses and reject by each boss's live part bounds.
        for(Entity entity:level.getAllEntities())
            if(entity instanceof NativeBoss boss&&boss.isMultipartEntity()&&boss.intersectsDamageParts(query))targets.add(boss);
    }
    public static net.minecraft.world.damagesource.DamageSource source(net.minecraft.world.level.Level level,LivingEntity owner,Element element){
        var type = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
            new net.minecraft.resources.ResourceLocation(NativeMod.ID, element.name().toLowerCase(java.util.Locale.ROOT)));
        var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return new net.minecraft.world.damagesource.DamageSource(holder,owner);
    }
    public static void area(ServerLevel level, LivingEntity owner, Vec3 center, double radius, float amount, Element element, double push) {
        AABB search=new AABB(center,center).inflate(radius);
        java.util.Set<LivingEntity> targets=new java.util.LinkedHashSet<>(level.getEntitiesOfClass(LivingEntity.class,search));
        addMultipartTargets(level,search,targets);
        for (LivingEntity target : targets) {
            if (distanceToDamageBoxSqr(center,target) > radius*radius || !canHit(owner,target)) continue;
            if (hit(owner,target,amount,element) && push != 0) {
                Vec3 motion = target.position().subtract(center).normalize().scale(push);
                target.push(motion.x,.2,motion.z);
                target.hurtMarked = true;
            }
        }
    }
    /** Source Expulsion selector: livingthings within distance=..radius, measured from each entity's feet. */
    static void livingFeetSphere(ServerLevel level,LivingEntity owner,Vec3 center,double radius,float amount,Element element){
        if(radius<0||amount<=0)return;
        double radiusSqr=radius*radius;
        AABB search=new AABB(center,center).inflate(radius+1.0e-4);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,search))
            if(target.getType().is(LIVING_THINGS)&&!target.getTags().contains("damagetemp")
                &&target.position().distanceToSqr(center)<=radiusSqr+1.0e-7&&canHit(owner,target))
                hit(owner,target,amount,element);
    }
    /** Source @a[distance=..r]: compare each player's feet to the selector origin, then use the normal damage pipeline. */
    static void playerFeetSphere(ServerLevel level,LivingEntity owner,Vec3 center,double radius,float amount,Element element){
        if(radius<0||amount<=0)return;
        AABB search=new AABB(center,center).inflate(radius+1.0e-4);
        double radiusSqr=radius*radius;
        for(Player player:level.getEntitiesOfClass(Player.class,search))
            if(player.position().distanceToSqr(center)<=radiusSqr+1.0e-7&&canHit(owner,player))
                hit(owner,player,amount,element);
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
        line(level,owner,start,end,amount,element,width,defaultParticles);
        return end;
    }
    public static void line(ServerLevel level,LivingEntity owner,Vec3 start,Vec3 end,float amount,Element element,double width,boolean defaultParticles){
        AABB search=new AABB(start,end).inflate(width);
        java.util.Set<LivingEntity> targets=new java.util.LinkedHashSet<>(level.getEntitiesOfClass(LivingEntity.class,search));
        addMultipartTargets(level,search,targets);
        for (LivingEntity target : targets) {
            boolean intersects=target.getBoundingBox().inflate(width).clip(start,end).isPresent()||target.getBoundingBox().contains(start);
            if(!intersects&&target instanceof NativeBoss boss&&boss.isMultipartEntity())intersects=boss.damagePartsOnLine(start,end,width);
            if(intersects)hit(owner,target,amount,element);
        }
        if(defaultParticles){
            Vec3 delta=end.subtract(start);
            for (double d=0; d<delta.length(); d+=.5) {
                Vec3 p=start.add(delta.normalize().scale(d));
                level.sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,1,0,0,0,0);
            }
        }
    }
    /** Source Serpent Laser: one player-feet sphere query at each whole-block ray sample. */
    static void serpentLaser(ServerLevel level,LivingEntity owner,Vec3 start,Vec3 end,float amount){
        Vec3 delta=end.subtract(start);double length=delta.length();
        if(length<=0||amount<=0)return;
        Vec3 direction=delta.scale(1/length);
        int samples=Math.min(150,(int)Math.floor(length+1.0e-6));
        java.util.Set<java.util.UUID> attempted=new java.util.HashSet<>();
        for(int sampleIndex=1;sampleIndex<=samples;sampleIndex++){
            Vec3 sample=start.add(direction.scale(sampleIndex));
            AABB query=new AABB(sample,sample).inflate(3.001);
            for(Player player:level.getEntitiesOfClass(Player.class,query,candidate->!candidate.isSpectator())){
                if(player.hurtTime!=0||player.position().distanceToSqr(sample)>9.0+1.0e-7
                    ||!attempted.add(player.getUUID())||!canHit(owner,player))continue;
                hit(owner,player,amount,Element.NIGHTMARE);
            }
        }
    }
    public enum Element { DREAM, RADIANCE, RESONANCE, NIGHTMARE, VOID, DISSONANCE }
    private Combat() {}
}
