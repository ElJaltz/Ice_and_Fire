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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;

public class RenderStoneStatue extends EntityRenderer<EntityStoneStatue> {

    protected static final ResourceLocation[] DESTROY_STAGES = new ResourceLocation[]{new ResourceLocation("textures/block/destroy_stage_0.png"), new ResourceLocation("textures/block/destroy_stage_1.png"), new ResourceLocation("textures/block/destroy_stage_2.png"), new ResourceLocation("textures/block/destroy_stage_3.png"), new ResourceLocation("textures/block/destroy_stage_4.png"), new ResourceLocation("textures/block/destroy_stage_5.png"), new ResourceLocation("textures/block/destroy_stage_6.png"), new ResourceLocation("textures/block/destroy_stage_7.png"), new ResourceLocation("textures/block/destroy_stage_8.png"), new ResourceLocation("textures/block/destroy_stage_9.png")};
    private final Map<String, EntityModel> modelMap = new HashMap();
    private final Map<String, Entity> hollowEntityMap = new HashMap();
    private final EntityRendererProvider.Context context;
    // Client-side rotation tracking per statue entity id
    private final Map<Integer, Float> rotationOffsets = new HashMap<>();
    private final Map<Integer, Integer> lastTickUpdated = new HashMap<>();

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

    private float getRotationOffset(EntityStoneStatue entity, boolean rotating) {
        int id = entity.getId();
        float current = rotationOffsets.getOrDefault(id, 0.0F);
        int tick = entity.tickCount;
        Integer last = lastTickUpdated.get(id);
        if (rotating) {
            int dt = (last == null) ? 1 : (tick - last);
            if (dt < 1) dt = 1;
            float ratePerTick = 4.0F; // degrees per tick for smooth rotation
            current = (current + ratePerTick * dt) % 360.0F;
            rotationOffsets.put(id, current);
            lastTickUpdated.put(id, tick);
        } else {
            // keep last tick updated to avoid multiple updates per tick
            lastTickUpdated.put(id, tick);
        }
        return current;
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
        Minecraft mc = Minecraft.getInstance();
        boolean rotating = false;
        if (mc.player != null && (mc.player.isCrouching() || mc.player.isShiftKeyDown()) && mc.options.keyUse.isDown()) {
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult ehr && ehr.getEntity().getId() == entityIn.getId()) {
                rotating = true;
            }
        }
        float baseYaw = entityIn.yRotO + (entityIn.getYRot() - entityIn.yRotO) * partialTicks;
        float rotOffset = getRotationOffset(entityIn, rotating);
        float yaw = baseYaw + rotOffset;
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
            VertexConsumer baseBuilder = bufferIn.getBuffer(crackTex);
            int crackW = 16, crackH = 16;
            try {
                int[] csz = getTextureSize(DESTROY_STAGES[i]);
                crackW = csz[0];
                crackH = csz[1];
            } catch (Throwable ignored) {
            }
            int trapW = 64, trapH = 64;
            if (fakeEntity != null) {
                try {
                    EntityRenderer<? super Entity> trappedRenderer2 = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fakeEntity);
                    if (trappedRenderer2 != null) {
                        ResourceLocation trappedRL = trappedRenderer2.getTextureLocation(fakeEntity);
                        if (trappedRL != null) {
                            int[] tsz = getTextureSize(trappedRL);
                            trapW = tsz[0];
                            trapH = tsz[1];
                        }
                    }
                } catch (Throwable ignored) {
                }
            } else {
                int[] modelSize = getModelTextureSize(model);
                trapW = modelSize[0];
                trapH = modelSize[1];
            }
            float tileU = crackW > 0 ? (float) trapW / (float) crackW : 1.0F;
            float tileV = crackH > 0 ? (float) trapH / (float) crackH : 1.0F;
            VertexConsumer ivertexbuilder2 = new TilingVertexConsumer(baseBuilder, tileU, tileV);
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

    private static int[] getModelTextureSize(EntityModel model) {
        int w = 64;
        int h = 64;
        try {
            Field fw = findFieldInHierarchy(model.getClass(), "texWidth");
            Field fh = findFieldInHierarchy(model.getClass(), "texHeight");
            if (fw != null) {
                fw.setAccessible(true);
                Object val = fw.get(model);
                if (val instanceof Integer) w = (Integer) val;
            }
            if (fh != null) {
                fh.setAccessible(true);
                Object val = fh.get(model);
                if (val instanceof Integer) h = (Integer) val;
            }
        } catch (Throwable ignored) {
        }
        if (w <= 0) w = 64;
        if (h <= 0) h = 64;
        return new int[]{w, h};
    }

    private static Field findFieldInHierarchy(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static final Map<ResourceLocation, int[]> TEX_SIZE_CACHE = new HashMap<>();

    private static int[] getTextureSize(ResourceLocation rl) {
        int[] cached = TEX_SIZE_CACHE.get(rl);
        if (cached != null) return cached;
        int w = 16, h = 16;
        try {
            Object optObj = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (optObj instanceof java.util.Optional) {
                java.util.Optional opt = (java.util.Optional) optObj;
                if (opt.isPresent()) {
                    net.minecraft.server.packs.resources.Resource res = (net.minecraft.server.packs.resources.Resource) opt.get();
                    try (java.io.InputStream is = res.open()) {
                        com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(is);
                        w = img.getWidth();
                        h = img.getHeight();
                        img.close();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        int[] size = new int[]{w, h};
        TEX_SIZE_CACHE.put(rl, size);
        return size;
    }

    private static class TilingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float uMul, vMul;

        private TilingVertexConsumer(VertexConsumer delegate, float uMul, float vMul) {
            this.delegate = delegate;
            this.uMul = uMul;
            this.vMul = vMul;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u * uMul, v * vMul);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}