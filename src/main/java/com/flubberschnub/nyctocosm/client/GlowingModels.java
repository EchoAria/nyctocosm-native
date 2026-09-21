package com.flubberschnub.nyctocosm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;

/** Original OptiFine _e masks rendered natively, with no OptiFine dependency. */
public final class GlowingModels {
    private static final Map<BakedModel,BakedModel> MODELS=new IdentityHashMap<>();
    public static void clear(){MODELS.clear();}
    public static BakedModel wrap(BakedModel model){
        if(model instanceof GlowModel)return model;
        return MODELS.computeIfAbsent(model,GlowModel::new);
    }
    private static final class GlowModel extends BakedModelWrapper<BakedModel>{
        private final BakedModel overlay;
        GlowModel(BakedModel base){super(base);overlay=new EmissionPass(base);}
        @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left){originalModel.applyTransform(context,pose,left);return this;}
        @Override public List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous){return List.of(originalModel,overlay);}
        @Override public ItemOverrides getOverrides(){return new ItemOverrides(){
            @Override public BakedModel resolve(BakedModel model,ItemStack stack,ClientLevel level,LivingEntity entity,int seed){
                BakedModel resolved=originalModel.getOverrides().resolve(originalModel,stack,level,entity,seed);
                return resolved==null?GlowModel.this:wrap(resolved);
            }
        };}
    }
    private static final class EmissionPass extends BakedModelWrapper<BakedModel>{
        private final Map<BakedQuad,Optional<BakedQuad>> quads=new IdentityHashMap<>();
        EmissionPass(BakedModel base){super(base);}
        @Override public List<RenderType> getRenderTypes(ItemStack stack,boolean fabulous){return List.of(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction direction,RandomSource random){
            List<BakedQuad> result=new ArrayList<>();
            for(BakedQuad quad:originalModel.getQuads(state,direction,random))quads.computeIfAbsent(quad,this::emission).ifPresent(result::add);
            return result;
        }
        private Optional<BakedQuad> emission(BakedQuad quad){
            TextureAtlasSprite original=quad.getSprite();ResourceLocation texture=original.contents().name();
            ResourceLocation glow=new ResourceLocation(texture.getNamespace(),texture.getPath()+"_e");
            Minecraft mc=Minecraft.getInstance();
            if(mc.getResourceManager().getResource(new ResourceLocation(glow.getNamespace(),"textures/"+glow.getPath()+".png")).isEmpty())return Optional.empty();
            TextureAtlasSprite sprite=mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(glow);
            if(sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()))return Optional.empty();
            int[] vertices=quad.getVertices().clone();
            for(int offset=0;offset<vertices.length;offset+=8){
                float u=(Float.intBitsToFloat(vertices[offset+4])-original.getU0())/(original.getU1()-original.getU0());
                float v=(Float.intBitsToFloat(vertices[offset+5])-original.getV0())/(original.getV1()-original.getV0());
                vertices[offset+4]=Float.floatToRawIntBits(sprite.getU0()+u*(sprite.getU1()-sprite.getU0()));
                vertices[offset+5]=Float.floatToRawIntBits(sprite.getV0()+v*(sprite.getV1()-sprite.getV0()));
                vertices[offset+6]=LightTexture.FULL_BRIGHT;
            }
            return Optional.of(new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),sprite,false));
        }
    }
    private GlowingModels(){}
}
