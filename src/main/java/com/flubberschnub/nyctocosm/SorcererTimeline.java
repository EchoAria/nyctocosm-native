package com.flubberschnub.nyctocosm;

import java.util.*;

/**
 * Source-authored command keyframes from the two original Sorcerer rigs, expressed
 * as ordinary mod data. Keeping the timings here makes combat deterministic and
 * independent from animation rendering or command functions.
 */
public final class SorcererTimeline {
    public enum Action {
        CHARGE, HALO_SUMMON, HALO_ACTIVATE, HALO_FIRE, ICE_FOCUS, HAIL,
        VOID_FOCUS, SINGULARITY, BLADE_FOCUS, BLADE_SWING, FROZEN_BLADES,
        METEOR_TEAR, METEOR_RAIN, DASH_AIM, DASH_STEP, DASH_FINISH,
        VOIDWAVE_FOCUS, VOIDWAVE, JAVELIN, AIR_RISE, AIR_AIM, AIR_DASH,
        AIR_SLAM, VOIDBEAM_AIM, VOIDBEAM, SEEKING_ORBS, ICE_SPIKES
    }

    public record Event(int tick, Action action, String sound) {}
    private static final Map<String,List<Event>> EVENTS = build();

    public static List<Event> events(String animation) {
        return EVENTS.getOrDefault(animation,List.of());
    }

    public static List<Event> at(String animation,int tick) {
        List<Event> result=new ArrayList<>();
        for(Event event:events(animation))if(event.tick==tick)result.add(event);
        return result;
    }

    public static Set<String> attacks(){return EVENTS.keySet();}

    private static Map<String,List<Event>> build(){
        Map<String,List<Event>> map=new LinkedHashMap<>();
        add(map,"halobeam",0,Action.CHARGE,null);
        for(int tick=5;tick<=40;tick+=5)add(map,"halobeam",tick,Action.HALO_SUMMON,"sorcerer.halobeam.summon");
        add(map,"halobeam",67,Action.HALO_ACTIVATE,"sorcerer.halobeam.activate");
        for(int tick=75;tick<=110;tick+=5)add(map,"halobeam",tick,Action.HALO_FIRE,"sorcerer.halobeam.shoot");

        add(map,"shardhail",0,Action.CHARGE,"sorcerer.shardhail.charge");
        for(int tick:new int[]{20,23,25,26,27,28,29,30,31,32,33,34})add(map,"shardhail",tick,Action.ICE_FOCUS,null);
        add(map,"shardhail",35,Action.HAIL,"sorcerer.shardhail.transmutate");

        add(map,"singularitycharge",0,Action.CHARGE,"sorcerer.singularitycharge.charge");
        for(int tick=20;tick<=41;tick++)add(map,"singularitycharge",tick,Action.VOID_FOCUS,tick==41?"sorcerer.singularitycharge.throw":null);
        add(map,"singularitycharge",44,Action.SINGULARITY,null);

        add(map,"frozenblades",0,Action.CHARGE,"sorcerer.frozenblades.charge");
        for(int tick=1;tick<=28;tick++)add(map,"frozenblades",tick,Action.BLADE_FOCUS,null);
        add(map,"frozenblades",30,Action.CHARGE,"sorcerer.frozenblades.throw");
        add(map,"frozenblades",32,Action.BLADE_SWING,null);
        add(map,"frozenblades",34,Action.FROZEN_BLADES,null);

        add(map,"voidmeteor",0,Action.CHARGE,"sorcerer.voidmeteor.charge");
        add(map,"voidmeteor",10,Action.METEOR_TEAR,null);
        add(map,"voidmeteor",30,Action.METEOR_RAIN,"sorcerer.voidmeteor.tear");

        add(map,"dashstrike",0,Action.CHARGE,"sorcerer.dashstrike.charge");
        add(map,"dashstrike",37,Action.DASH_AIM,null);
        for(int tick=41;tick<=48;tick++)add(map,"dashstrike",tick,Action.DASH_STEP,tick==41?"sorcerer.dashstrike.dash":null);
        add(map,"dashstrike",49,Action.DASH_FINISH,"sorcerer.dashstrike.strike");
        add(map,"dashstrike",50,Action.DASH_STEP,null);

        add(map,"voidwave",0,Action.CHARGE,null);
        add(map,"voidwave",8,Action.CHARGE,"sorcerer.voidwave.charge");
        for(int tick:new int[]{13,15,17,19,21,23,25,27,29,31,33})add(map,"voidwave",tick,Action.VOIDWAVE_FOCUS,null);
        add(map,"voidwave",35,Action.CHARGE,"sorcerer.voidwave.discharge");
        add(map,"voidwave",38,Action.VOIDWAVE,null);

        add(map,"javelinswing",0,Action.CHARGE,"sorcerer.javelinswing.charge");
        add(map,"javelinswing",34,Action.BLADE_SWING,"sorcerer.javelinswing.swing");
        add(map,"javelinswing",35,Action.JAVELIN,null);

        add(map,"airdash",0,Action.CHARGE,"sorcerer.airdash.fly");
        for(int tick=1;tick<=19;tick++)add(map,"airdash",tick,Action.AIR_RISE,null);
        add(map,"airdash",20,Action.AIR_AIM,"sorcerer.airdash.aim");
        add(map,"airdash",25,Action.CHARGE,"sorcerer.airdash.dash");
        for(int tick=26;tick<=39;tick++)add(map,"airdash",tick,Action.AIR_DASH,null);
        add(map,"airdash",40,Action.AIR_SLAM,"sorcerer.airdash.slam");

        add(map,"voidbeam",0,Action.CHARGE,"sorcerer.voidbeam.charge");
        add(map,"voidbeam",20,Action.VOIDBEAM_AIM,"sorcerer.voidbeam.aim");
        add(map,"voidbeam",27,Action.CHARGE,"sorcerer.voidbeam.shoot");
        for(int tick=30;tick<=57;tick++)add(map,"voidbeam",tick,Action.VOIDBEAM,null);

        add(map,"seekingsingularities",0,Action.CHARGE,"sorcerer.seekingsingularities.charge");
        add(map,"seekingsingularities",30,Action.SEEKING_ORBS,"sorcerer.seekingsingularities.summon");

        add(map,"icespikes",0,Action.CHARGE,"sorcerer.staff.emitcharge");
        add(map,"icespikes",40,Action.ICE_SPIKES,"sorcerer.staff.emitwave");
        map.replaceAll((name,list)->List.copyOf(list));
        return Collections.unmodifiableMap(map);
    }

    private static void add(Map<String,List<Event>> map,String attack,int tick,Action action,String sound){
        map.computeIfAbsent(attack,key->new ArrayList<>()).add(new Event(tick,action,sound));
    }

    private SorcererTimeline(){}
}
