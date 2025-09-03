package com.github.alexthe666.iceandfire.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.github.alexthe666.iceandfire.entity.EntityMyrmexWorker;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;

public class ModelMyrmexWorker extends ModelMyrmexBase {
    public AdvancedModelBox Torso;
    public AdvancedModelBox Head;
    public  AdvancedModelBox L_eye;
    public  AdvancedModelBox R_eye;


    public ModelMyrmexWorker() {
        this.texWidth = 64;
        this.texHeight = 64;

        Torso = new AdvancedModelBox(this);
        Torso.setRotationPoint(0.0F, 17.0F, 0.0F);
        Torso.setTextureOffset(28, 18).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 9.0F, 0.0F, false);
        Torso.setTextureOffset(16, 31).addBox(0.0F, -2.0F, -3.0F, 0.0F, 2.0F, 10.0F, 0.0F, false);
        Torso.setTextureOffset(8, 46).addBox(-1.0F, 1.0F, 6.0F, 2.0F, 2.0F, 1.0F, 0.0F, false);


        Head = new AdvancedModelBox(this);
        Head.setRotationPoint(0.0F, 0.0F, 0.0F);
        Torso.addChild(Head);
        Head.setTextureOffset(0, 18).addBox(-3.0F, -4.0F, -11.0F, 6.0F, 4.0F, 8.0F, 0.0F, false);
        setRotateAngle(Head, 0.436332F, 0.0F, 0.0F);

        L_eye = new AdvancedModelBox(this);
        L_eye.setTextureOffset(36, 39).addBox(3.0F, -5.0F, -7.5F, 1.0F, 3.0F, 5.0F, 0.0F, false);
        setRotateAngle(L_eye, 0.2181662F, 0.0F, 0.0F);
        Head.addChild(L_eye);

        R_eye = new AdvancedModelBox(this);
        R_eye.setTextureOffset(36, 39).addBox(-4.0F, -5.0F, -7.5F, 1.0F, 3.0F, 5.0F, 0.0F, false);
        setRotateAngle(R_eye, 0.2181662F, 0.0F, 0.0F);
        Head.addChild(R_eye);



        this.updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(Torso);
    }

    @Override
    public void setupAnim(Entity entity, float v, float v1, float v2,  float netHeadYaw, float headPitch) {
        this.faceTarget(netHeadYaw, headPitch, 2, Head);


    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(Torso, Head, L_eye, R_eye);
    }








    @Override
    public BasicModelPart[] getHeadParts() {
        return new BasicModelPart[]{Head};
    }

    @Override
    public void renderStatue(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, Entity living) {
        this.renderToBuffer(matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
