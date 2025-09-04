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
    public  AdvancedModelBox Abdomen;
    public  AdvancedModelBox L_Antenna;
    public  AdvancedModelBox R_Antenna;
    public  AdvancedModelBox R_jaw;
    public  AdvancedModelBox L_jaw;
    public  AdvancedModelBox R_leg_1;
    public  AdvancedModelBox R_leg_2;
    public  AdvancedModelBox R_leg_3;
    public  AdvancedModelBox L_leg_1;
    public  AdvancedModelBox L_leg_2;
    public  AdvancedModelBox L_leg_3;





    public ModelMyrmexWorker() {
        this.texWidth = 64;
        this.texHeight = 64;

        Torso = new AdvancedModelBox(this);
        Torso.setRotationPoint(0.0F, 17.0F, 0.0F);
        Torso.setTextureOffset(28, 18).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 9.0F, 0.0F, false);
        Torso.setTextureOffset(16, 31).addBox(0.0F, -2.0F, -3.0F, 0.0F, 2.0F, 10.0F, 0.0F, false);
        Torso.setTextureOffset(8, 46).addBox(-1.0F, 1.0F, 6.0F, 2.0F, 2.0F, 1.0F, 0.0F, false);

        R_leg_1 = new AdvancedModelBox(this);
        R_leg_1.setRotationPoint(2.0F, 0.0F, 1.0F);
        R_leg_1.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        setRotateAngle(R_leg_1, 0.0F, -0.523599F, 0.0F);
        Torso.addChild(R_leg_1);

        R_leg_2 = new AdvancedModelBox(this);
        R_leg_2.setRotationPoint(2.0F, 0.0F, 1.0F);
        R_leg_2.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        Torso.addChild(R_leg_2);

        R_leg_3 = new AdvancedModelBox(this);
        R_leg_3.setRotationPoint(2.0F, 0.0F, 1.0F);
        R_leg_3.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        setRotateAngle(R_leg_3, 0.0F, 0.523599F, 0.0F);
        Torso.addChild(R_leg_3);


        L_leg_1 = new AdvancedModelBox(this);
        L_leg_1.setRotationPoint(2.0F, 0.0F, 1.0F);
        L_leg_1.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        setRotateAngle(L_leg_1, 0.0F, -0.523599F, 0.0F);
        Torso.addChild(L_leg_1);

        L_leg_2 = new AdvancedModelBox(this);
        L_leg_2.setRotationPoint(2.0F, 0.0F, 1.0F);
        L_leg_2.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        Torso.addChild(L_leg_2);

        L_leg_3 = new AdvancedModelBox(this);
        L_leg_3.setRotationPoint(2.0F, 0.0F, 1.0F);
        L_leg_3.setTextureOffset(36, 31).addBox(-2.0F, 0.0F, -1.0F, 8.0F, 8.0F, 0.0F, 0.0F, false);
        setRotateAngle(L_leg_3, 0.0F, 0.523599F, 0.0F);
        Torso.addChild(L_leg_3);

        Head = new AdvancedModelBox(this);
        Head.setRotationPoint(0.0F, -2.0F, -3.0F);
        Torso.addChild(Head);
        Head.setTextureOffset(0, 18).addBox(-3.0F, -2.0F, -8.5F, 6.0F, 4.0F, 8.0F, 0.0F, false);
        setRotateAngle(Head, 0.436332F, 0.0F, 0.0F);

        L_eye = new AdvancedModelBox(this);
        L_eye.setTextureOffset(36, 39).addBox(3.0F, -2.5F, -5.5F, 1.0F, 3.0F, 5.0F, 0.0F, false);
        setRotateAngle(L_eye, 0.2181662F, 0.0F, 0.0F);
        Head.addChild(L_eye);

        R_eye = new AdvancedModelBox(this);
        R_eye.setTextureOffset(36, 39).addBox(-4.0F, -2.5F, -5.5F, 1.0F, 3.0F, 5.0F, 0.0F, false);
        setRotateAngle(R_eye, 0.2181662F, 0.0F, 0.0F);
        Head.addChild(R_eye);

        L_Antenna = new AdvancedModelBox(this);
        L_Antenna.setTextureOffset(0, 30).addBox(0.5F, -10.0F, -9.5F, 0.0F, 8.0F, 8.0F, 0.0F, false);
        setRotateAngle(L_Antenna, 0.0F, -0.523599F, 0.0F);
        Head.addChild(L_Antenna);

        R_Antenna = new AdvancedModelBox(this);
       R_Antenna.setTextureOffset(0, 30).addBox(-0.5F, -10.0F, -9.5F, 0.0F, 8.0F, 8.0F, 0.0F, false);
        setRotateAngle(R_Antenna, 0.0F, 0.523599F, 0.0F);
        Head.addChild(R_Antenna);

        R_jaw = new AdvancedModelBox(this);
        R_jaw.setRotationPoint(3.0F, 0.0F, -10.0F);
        R_jaw.setTextureOffset(16, 43).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 3.0F, 0.0F, false);
        R_jaw.setTextureOffset(0, 46).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F, 0.0F, true);
        Head.addChild(R_jaw);

        L_jaw = new AdvancedModelBox(this);
        L_jaw.setRotationPoint(-4.0F, 0.0F, -10.0F);
        L_jaw.setTextureOffset(16, 43).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 3.0F, 0.0F, true);
        L_jaw.setTextureOffset(0, 46).addBox(1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);
        Head.addChild(L_jaw);


        Abdomen = new AdvancedModelBox(this);
        Abdomen.setRotationPoint(0.0F, 1.0F, 7.0F);
        setRotateAngle(Abdomen, -0.1309F, 0.0F, 0.0F);
        Torso.addChild(Abdomen);
        Abdomen.setTextureOffset(0, 0).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 8.0F, 10.0F, 0.0F, false);
        Abdomen.setTextureOffset(37, 0).addBox(0.0F, -7.0F, 0.0F, 0.0F, 2.0F, 8.0F, 0.0F, false);

        Abdomen.setTextureOffset(36, 10).addBox(-3.0F, -4.0F, 10.0F, 6.0F, 6.0F, 2.0F, 0.0F, false);
        Abdomen.setTextureOffset(32, 34).addBox(0.0F, -2.0F, 18.0F, 0.0F, 5.0F, 2.0F, 0.0F, false);



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
        return ImmutableList.of(Torso, Head, L_eye, R_eye, Abdomen, R_Antenna, L_Antenna, R_jaw, L_jaw, R_leg_1, R_leg_2, R_leg_3 ,L_leg_1, L_leg_2, L_leg_3 );
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
