package com.flubberschnub.nyctocosm;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Per-player cooldowns for weapon abilities that may be used from either hand. */
public final class WeaponAbilityCooldowns {
    private static final String DATA_KEY="NyctocosmWeaponAbilityCooldowns";
    private static final String[] FIELDS={"NullfissureSwing","NullfissureCast","SemblanceSwing","SemblanceCast"};

    public static boolean supportsHand(int kind,InteractionHand hand){
        return hand==InteractionHand.MAIN_HAND||kind>=2;
    }

    public static int duration(int kind,InteractionHand hand){
        if(kind==2)return hand==InteractionHand.MAIN_HAND?20:25;
        if(kind==3)return 30;
        return 0;
    }

    public static int remaining(Player player,int kind,InteractionHand hand){
        int index=index(kind,hand);
        if(index<0)return 0;
        return Math.max(0,player.getPersistentData().getCompound(DATA_KEY).getInt(FIELDS[index]));
    }

    /** Starts an ability timer only on the server; timers belong to the ability, not a stack. */
    public static boolean start(Player player,int kind,InteractionHand hand){
        int index=index(kind,hand);
        if(index<0||remaining(player,kind,hand)>0)return false;
        CompoundTag timers=player.getPersistentData().getCompound(DATA_KEY).copy();
        timers.putInt(FIELDS[index],duration(kind,hand));
        player.getPersistentData().put(DATA_KEY,timers);
        syncIfConnected(player);
        return true;
    }

    /** Mirrors the source playertick counters: logged-in player ticks only. */
    public static void tick(Player player){
        CompoundTag timers=player.getPersistentData().getCompound(DATA_KEY).copy();
        boolean changed=false,active=false;
        for(String field:FIELDS){
            int value=timers.getInt(field);
            if(value>0){
                value--;
                changed=true;
                if(value>0){timers.putInt(field,value);active=true;}
                else timers.remove(field);
            }
        }
        if(!changed)return;
        if(active)player.getPersistentData().put(DATA_KEY,timers);
        else player.getPersistentData().remove(DATA_KEY);
        syncIfConnected(player);
    }

    public static void sync(ServerPlayer player){
        int[] values=new int[FIELDS.length];
        CompoundTag timers=player.getPersistentData().getCompound(DATA_KEY);
        for(int i=0;i<FIELDS.length;i++)values[i]=Math.max(0,timers.getInt(FIELDS[i]));
        NativeNetwork.weaponCooldowns(player,values[0],values[1],values[2],values[3]);
    }

    private static void syncIfConnected(Player player){if(player instanceof ServerPlayer serverPlayer)sync(serverPlayer);}

    private static int index(int kind,InteractionHand hand){
        if(kind==2)return hand==InteractionHand.MAIN_HAND?0:1;
        if(kind==3)return hand==InteractionHand.MAIN_HAND?2:3;
        return -1;
    }

    private WeaponAbilityCooldowns(){}
}
