package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.client.model.ICustomStatueModel;
import com.github.alexthe666.iceandfire.client.model.ModelHydraBody;
import com.github.alexthe666.iceandfire.client.model.ModelStonePlayer;
import com.github.alexthe666.iceandfire.client.render.IafRenderType;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerHydraHead;
import com.github.alexthe666.iceandfire.entity.EntityHydra;
import com.github.alexthe666.iceandfire.entity.EntityStoneStatue;
import com.github.alexthe666.iceandfire.entity.EntityTroll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class RenderStoneStatue extends EntityRenderer<EntityStoneStatue> {

    protected static final ResourceLocation[] DESTROY_STAGES = new ResourceLocation[]{new ResourceLocation("textures/block/destroy_stage_0.png"), new ResourceLocation("textures/block/destroy_stage_1.png"), new ResourceLocation("textures/block/destroy_stage_2.png"), new ResourceLocation("textures/block/destroy_stage_3.png"), new ResourceLocation("textures/block/destroy_stage_4.png"), new ResourceLocation("textures/block/destroy_stage_5.png"), new ResourceLocation("textures/block/destroy_stage_6.png"), new ResourceLocation("textures/block/destroy_stage_7.png"), new ResourceLocation("textures/block/destroy_stage_8.png"), new ResourceLocation("textures/block/destroy_stage_9.png")};
    private final Map<String, EntityModel> modelMap = new HashMap();
    private final Map<String, Entity> hollowEntityMap = new HashMap();
    private final EntityRendererProvider.Context context;

    public RenderStoneStatue(EntityRendererProvider.Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityStoneStatue entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    protected void preRenderCallback(EntityStoneStatue entity, Entity fakeEntity, PoseStack matrixStackIn, float partialTickTime) {
        float scale = entity.getScale() < 0.01F ? 1F : entity.getScale();
        // If the trapped entity is a baby and its renderer will apply a 0.5x baby scale,
        // neutralize duplicate scaling by dividing out the baby factor from the statue scale
        if (entity.isTrappedBaby() && fakeEntity instanceof AgeableMob) {
            scale /= 0.5F;
        }
        matrixStackIn.scale(scale, scale, scale);
    }

    private void applyRendererScale(@NotNull EntityStoneStatue statue, Entity fakeEntity, PoseStack poseStack, float partialTicks) {
        if (fakeEntity == null) return;
        try {
            EntityRenderer<? super Entity> trappedRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fakeEntity);
            if (trappedRenderer == null) return;
            Class<?> cls = trappedRenderer.getClass();
            Method scaleMethod = null;
            while (cls != null && scaleMethod == null) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals("scale")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 3 && PoseStack.class.isAssignableFrom(params[1]) && params[2] == float.class) {
                            if (params[0].isInstance(fakeEntity) || params[0].isAssignableFrom(fakeEntity.getClass())) {
                                scaleMethod = m;
                                break;
                            }
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
            if (scaleMethod != null) {
                scaleMethod.setAccessible(true);
                scaleMethod.invoke(trappedRenderer, fakeEntity, poseStack, partialTicks);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void render(EntityStoneStatue entityIn, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        EntityModel model = new PigModel(context.bakeLayer(ModelLayers.PIG));

        // Get the correct model
        if (modelMap.get(entityIn.getTrappedEntityTypeString()) != null) {
            model = modelMap.get(entityIn.getTrappedEntityTypeString());
        } else {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(entityIn.getTrappedEntityType());

            if (renderer instanceof RenderLayerParent) {
                model = ((RenderLayerParent<?, ?>) renderer).getModel();
            } else if (entityIn.getTrappedEntityType() == EntityType.PLAYER) {
                model = new ModelStonePlayer(context.bakeLayer(ModelLayers.PLAYER));
            }
            modelMap.put(entityIn.getTrappedEntityTypeString(), model);
        }
        if (model == null)
            return;

        Entity fakeEntity = null;
        if (this.hollowEntityMap.get(entityIn.getTrappedEntityTypeString()) == null) {
            Entity build = entityIn.getTrappedEntityType().create(Minecraft.getInstance().level);
            if (build != null) {
                try {
                    build.load(entityIn.getTrappedTag());
                } catch (Exception e) {
                    IceAndFire.LOGGER.warn("Mob " + entityIn.getTrappedEntityTypeString() + " could not build statue NBT");
                }
                fakeEntity = this.hollowEntityMap.putIfAbsent(entityIn.getTrappedEntityTypeString(), build);
            }
        } else {
            fakeEntity = this.hollowEntityMap.get(entityIn.getTrappedEntityTypeString());
        }
        RenderType tex = IafRenderType.getStoneMobRenderType(200, 200);
        // Use trapped entity's own texture for non-player entities when available
        if (fakeEntity != null && entityIn.getTrappedEntityType() != EntityType.PLAYER) {
            try {
                EntityRenderer<? super Entity> trappedRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fakeEntity);
                if (trappedRenderer != null) {
                    ResourceLocation textureRL = trappedRenderer.getTextureLocation(fakeEntity);
                    if (textureRL != null) {
                        tex = model.renderType(textureRL);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // Keep troll special-case stone texture override
        if (fakeEntity instanceof EntityTroll) {
            tex = RenderType.entityCutout(((EntityTroll) fakeEntity).getTrollType().TEXTURE_STONE);
        }

        VertexConsumer ivertexbuilder = bufferIn.getBuffer(tex);


        matrixStackIn.pushPose();
        float yaw = entityIn.yRotO + (entityIn.getYRot() - entityIn.yRotO) * partialTicks;
        boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit());
        // Use persisted baby flag from statue to decide young scaling
        boolean trappedIsBaby = entityIn.isTrappedBaby();
        model.young = trappedIsBaby;
        // Optionally align fakeEntity baby state for layers that depend on it
        if (fakeEntity instanceof AgeableMob ageable) {
            try {
                ageable.setAge(trappedIsBaby ? -24000 : 0);
            } catch (Throwable ignored) {
            }
        }
        model.riding = shouldSit;
        model.attackTime = entityIn.getAttackAnim(partialTicks);
        if (model instanceof AdvancedEntityModel) {
            ((AdvancedEntityModel) model).resetToDefaultPose();
        } else if (fakeEntity != null) {
            model.setupAnim(fakeEntity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F);
        }
        if (fakeEntity != null) {
            applyRendererScale(entityIn, fakeEntity, matrixStackIn, partialTicks);
        }
        preRenderCallback(entityIn, fakeEntity, matrixStackIn, partialTicks);
        matrixStackIn.translate(0, 1.5F, 0);
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(180.0F));
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(yaw));
        if (model instanceof ICustomStatueModel && fakeEntity != null) {
            ((ICustomStatueModel) model).renderStatue(matrixStackIn, ivertexbuilder, packedLightIn, fakeEntity);
            if (model instanceof ModelHydraBody && fakeEntity instanceof EntityHydra) {
                LayerHydraHead.renderHydraHeads((ModelHydraBody) model, true, matrixStackIn, bufferIn, packedLightIn, (EntityHydra) fakeEntity, 0, 0, partialTicks, 0, 0, 0);
            }
        } else {
            model.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            // Render the trapped entity's layers (e.g., Enderman eyes, drowned outer layer)
            if (fakeEntity != null) {
                renderTrappedLayers(fakeEntity, matrixStackIn, bufferIn, packedLightIn, partialTicks);
            }
        }

        matrixStackIn.popPose();

        if (entityIn.getCrackAmount() >= 1) {
            int i = Mth.clamp(entityIn.getCrackAmount() - 1, 0, DESTROY_STAGES.length - 1);
            RenderType crackTex = IafRenderType.getStoneCrackRenderType(DESTROY_STAGES[i]);
            VertexConsumer ivertexbuilder2 = bufferIn.getBuffer(crackTex);
            matrixStackIn.pushPose();
            matrixStackIn.pushPose();
            if (fakeEntity != null) {
                applyRendererScale(entityIn, fakeEntity, matrixStackIn, partialTicks);
            }
            preRenderCallback(entityIn, fakeEntity, matrixStackIn, partialTicks);
            matrixStackIn.translate(0, 1.5F, 0);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(180.0F));
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(yaw));
            if (model instanceof ICustomStatueModel) {
                ((ICustomStatueModel) model).renderStatue(matrixStackIn, ivertexbuilder2, packedLightIn, fakeEntity);
            } else {
                model.renderToBuffer(matrixStackIn, ivertexbuilder2, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            matrixStackIn.popPose();
            matrixStackIn.popPose();
        }
        //super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private void renderTrappedLayers(Entity fakeEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
        try {
            EntityRenderer<? super Entity> trappedRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fakeEntity);
            if (!(trappedRenderer instanceof RenderLayerParent)) return;
            // Access protected 'layers' list from LivingEntityRenderer/RenderLayerParent via reflection
            Class<?> cls = trappedRenderer.getClass();
            Field layersField = null;
            while (cls != null && layersField == null) {
                try {
                    layersField = cls.getDeclaredField("layers");
                } catch (NoSuchFieldException ignored) {
                    cls = cls.getSuperclass();
                }
            }
            if (layersField == null) return;
            layersField.setAccessible(true);
            Object layersObj = layersField.get(trappedRenderer);
            if (!(layersObj instanceof List<?> layers)) return;

            float limbSwing = 0.0F;
            float limbSwingAmount = 0.0F;
            float ageInTicks = fakeEntity.tickCount + partialTicks;
            float netHeadYaw = 0.0F;
            float headPitch = 0.0F;

            for (Object layer : layers) {
                Method renderMethod = null;
                Class<?> layerCls = layer.getClass();
                // Find a 'render' method matching (PoseStack, MultiBufferSource, int, Entity, float, float, float, float, float, float)
                for (Method m : layerCls.getMethods()) {
                    if (m.getName().equals("render")) {
                        Class<?>[] p = m.getParameterTypes();
                        if (p.length == 10 && PoseStack.class.isAssignableFrom(p[0]) && MultiBufferSource.class.isAssignableFrom(p[1]) && p[2] == int.class && Entity.class.isAssignableFrom(p[3])) {
                            renderMethod = m;
                            break;
                        }
                    }
                }
                if (renderMethod != null) {
                    try {
                        renderMethod.invoke(layer, poseStack, buffer, packedLight, fakeEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}