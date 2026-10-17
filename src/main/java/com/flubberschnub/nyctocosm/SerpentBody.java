package com.flubberschnub.nyctocosm;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The original 40 independently chasing segments, followed by a separate tail. */
final class SerpentBody {
    static final int LINKS=41;
    private final Vec3[] points=new Vec3[LINKS], previous=new Vec3[LINKS];
    private final float[] yaw=new float[LINKS], pitch=new float[LINKS], oldYaw=new float[LINKS], oldPitch=new float[LINKS];
    private Vec3 head, oldHead;
    private float headYaw,headPitch,oldHeadYaw,oldHeadPitch;

    void initialize(Vec3 at){if(head==null){head=oldHead=at;Arrays.fill(points,at);Arrays.fill(previous,at);}}
    void tick(Vec3 at,int tier){
        Vec3 movement=head==null?Vec3.ZERO:at.subtract(head);
        tick(at,tier,movement.lengthSqr()<1e-12?headYaw:SerpentMotion.facingYaw(movement),movement.lengthSqr()<1e-12?headPitch:SerpentMotion.facingPitch(movement));
    }
    void tick(Vec3 at,int tier,float facingYaw,float facingPitch){
        initialize(at);remember();head=at;
        headYaw=facingYaw;headPitch=facingPitch;
        for(int i=0;i<LINKS;i++){
            Vec3 leader=i==0?head:points[i-1], delta=leader.subtract(points[i]);
            double gate=i==40?.8:i==0?(tier==1?4.5:tier==2?4.2:4):4.5;
            if(delta.lengthSqr()<gate*gate)continue; // No teleport also means no rotation update.
            yaw[i]=SerpentMotion.facingYaw(delta);pitch[i]=SerpentMotion.facingPitch(delta);
            points[i]=points[i].add(SerpentMotion.localForward(yaw[i],pitch[i]).scale(tier+.05));
        }
    }
    private void remember(){oldHead=head;oldHeadYaw=headYaw;oldHeadPitch=headPitch;System.arraycopy(points,0,previous,0,LINKS);System.arraycopy(yaw,0,oldYaw,0,LINKS);System.arraycopy(pitch,0,oldPitch,0,LINKS);}
    Vec3 position(int index,float partial,Vec3 renderedHead){
        if(head==null)return renderedHead;
        if(index==0)return oldHead.lerp(head,partial);
        int i=Math.min(LINKS,index)-1;
        // Entity position packets and body metadata interpolate on different clocks.
        // The whole chain must use its own coherent snapshot, including the head.
        return previous[i].lerp(points[i],partial);
    }
    void finishInterpolation(){if(head!=null)remember();}
    Vec3 currentPosition(int index){return points[index-1];}
    float yaw(int index,float partial){return index==0?Mth.rotLerp(partial,oldHeadYaw,headYaw):Mth.rotLerp(partial,oldYaw[index-1],yaw[index-1]);}
    float pitch(int index,float partial){return index==0?Mth.rotLerp(partial,oldHeadPitch,headPitch):Mth.rotLerp(partial,oldPitch[index-1],pitch[index-1]);}
    CompoundTag save(){
        CompoundTag tag=new CompoundTag();if(head==null)return tag;
        tag.putDouble("X",head.x);tag.putDouble("Y",head.y);tag.putDouble("Z",head.z);
        tag.putFloat("HeadYaw",headYaw);tag.putFloat("HeadPitch",headPitch);
        ListTag links=new ListTag();
        for(int i=0;i<LINKS;i++){CompoundTag link=new CompoundTag();link.putDouble("X",points[i].x);link.putDouble("Y",points[i].y);link.putDouble("Z",points[i].z);link.putFloat("Yaw",yaw[i]);link.putFloat("Pitch",pitch[i]);links.add(link);}
        tag.put("Links",links);return tag;
    }
    void load(CompoundTag tag){
        ListTag links=tag.getList("Links",10);if(links.size()!=LINKS)return;
        head=new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"));
        headYaw=tag.getFloat("HeadYaw");headPitch=tag.getFloat("HeadPitch");
        for(int i=0;i<LINKS;i++){CompoundTag link=links.getCompound(i);points[i]=new Vec3(link.getDouble("X"),link.getDouble("Y"),link.getDouble("Z"));yaw[i]=link.getFloat("Yaw");pitch[i]=link.getFloat("Pitch");}remember();
    }
    // 820 bytes of relative floats per snapshot, instead of 41 separately tracked entities.
    CompoundTag snapshot(){
        CompoundTag tag=new CompoundTag();if(head==null)return tag;
        tag.putDouble("X",head.x);tag.putDouble("Y",head.y);tag.putDouble("Z",head.z);int[] values=new int[LINKS*5];
        tag.putFloat("HeadYaw",headYaw);tag.putFloat("HeadPitch",headPitch);
        for(int i=0;i<LINKS;i++){Vec3 delta=points[i].subtract(head);values[i*5]=Float.floatToIntBits((float)delta.x);values[i*5+1]=Float.floatToIntBits((float)delta.y);values[i*5+2]=Float.floatToIntBits((float)delta.z);values[i*5+3]=Float.floatToIntBits(yaw[i]);values[i*5+4]=Float.floatToIntBits(pitch[i]);}
        tag.putIntArray("Links",values);return tag;
    }
    void receive(CompoundTag tag,boolean interpolate){
        int[] values=tag.getIntArray("Links");if(values.length!=LINKS*5)return;
        boolean fresh=head==null;if(!fresh)remember();
        head=new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"));
        headYaw=tag.getFloat("HeadYaw");headPitch=tag.getFloat("HeadPitch");
        for(int i=0;i<LINKS;i++){points[i]=head.add(Float.intBitsToFloat(values[i*5]),Float.intBitsToFloat(values[i*5+1]),Float.intBitsToFloat(values[i*5+2]));yaw[i]=Float.intBitsToFloat(values[i*5+3]);pitch[i]=Float.intBitsToFloat(values[i*5+4]);}
        if(fresh||!interpolate)remember();
    }
}
