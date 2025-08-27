package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.util.IBlacklistedFromStatues;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class EntityStoneStatue extends LivingEntity implements IBlacklistedFromStatues {

    private static final EntityDataAccessor<String> TRAPPED_ENTITY_TYPE = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> TRAPPED_ENTITY_DATA = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_WIDTH = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_HEIGHT = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRAPPED_ENTITY_SCALE = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CRACK_AMOUNT = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRAPPED_IS_BABY = SynchedEntityData.defineId(EntityStoneStatue.class, EntityDataSerializers.BOOLEAN);
    private EntityDimensions stoneStatueSize = EntityDimensions.fixed(0.5F, 0.5F);
    private boolean playedBreakEffect = false;

    public EntityStoneStatue(EntityType<? extends LivingEntity> t, Level worldIn) {
        super(t, worldIn);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 20)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            //KNOCKBACK
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public static EntityStoneStatue buildStatueEntity(LivingEntity parent) {
        EntityStoneStatue statue = IafEntityRegistry.STONE_STATUE.get().create(parent.level());
        CompoundTag entityTag = new CompoundTag();
        try {
            if (!(parent instanceof Player)) {
                parent.saveWithoutId(entityTag);
            }
        } catch (Exception e) {
            IceAndFire.LOGGER.debug("Encountered issue creating stone statue from {}", parent);
        }
        statue.setTrappedTag(entityTag);
        statue.setTrappedEntityTypeString(ForgeRegistries.ENTITY_TYPES.getKey(parent.getType()).toString());
        statue.setTrappedEntityWidth(parent.getBbWidth());
        statue.setTrappedHeight(parent.getBbHeight());
        statue.setTrappedScale(parent.getScale());
        // Persist if the original entity was a baby/young for correct model scaling later
        try {
            statue.setTrappedBaby(parent.isBaby());
        } catch (Throwable ignored) {
            statue.setTrappedBaby(false);
        }

        return statue;
    }

    @Override
    public void push(@NotNull Entity entityIn) {
    }

    @Override
    public boolean canBeCollidedWith() {
        // Allow entities/players to collide and stand on this statue
        return true;
    }

    @Override
    public boolean isPickable() {
        // Allow ray picking/selection if needed
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // Prevent external motion application
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // Ignore knockback from attacks/explosions
    }

    @Override
    public void baseTick() {

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRAPPED_ENTITY_TYPE, "minecraft:pig");
        this.entityData.define(TRAPPED_ENTITY_DATA, new CompoundTag());
        this.entityData.define(TRAPPED_ENTITY_WIDTH, 0.5F);
        this.entityData.define(TRAPPED_ENTITY_HEIGHT, 0.5F);
        this.entityData.define(TRAPPED_ENTITY_SCALE, 1F);
        this.entityData.define(CRACK_AMOUNT, 0);
        this.entityData.define(TRAPPED_IS_BABY, Boolean.FALSE);
    }

    public EntityType getTrappedEntityType() {
        String str = getTrappedEntityTypeString();
        return EntityType.byString(str).orElse(EntityType.PIG);
    }

    public String getTrappedEntityTypeString() {
        return this.entityData.get(TRAPPED_ENTITY_TYPE);
    }

    public void setTrappedEntityTypeString(String string) {
        this.entityData.set(TRAPPED_ENTITY_TYPE, string);
    }

    public CompoundTag getTrappedTag() {
        return this.entityData.get(TRAPPED_ENTITY_DATA);
    }

    public void setTrappedTag(CompoundTag tag) {
        this.entityData.set(TRAPPED_ENTITY_DATA, tag);
    }

    public float getTrappedWidth() {
        return this.entityData.get(TRAPPED_ENTITY_WIDTH);
    }

    public void setTrappedEntityWidth(float size) {
        this.entityData.set(TRAPPED_ENTITY_WIDTH, size);
    }

    public float getTrappedHeight() {
        return this.entityData.get(TRAPPED_ENTITY_HEIGHT);
    }

    public void setTrappedHeight(float size) {
        this.entityData.set(TRAPPED_ENTITY_HEIGHT, size);
    }

    public float getTrappedScale() {
        return this.entityData.get(TRAPPED_ENTITY_SCALE);
    }

    public boolean isTrappedBaby() {
        return this.entityData.get(TRAPPED_IS_BABY);
    }

    public void setTrappedBaby(boolean baby) {
        this.entityData.set(TRAPPED_IS_BABY, baby);
    }

    public void setTrappedScale(float size) {
        this.entityData.set(TRAPPED_ENTITY_SCALE, size);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CrackAmount", this.getCrackAmount());
        tag.putFloat("StatueWidth", this.getTrappedWidth());
        tag.putFloat("StatueHeight", this.getTrappedHeight());
        tag.putFloat("StatueScale", this.getTrappedScale());
        tag.putString("StatueEntityType", this.getTrappedEntityTypeString());
        tag.put("StatueEntityTag", this.getTrappedTag());
        tag.putBoolean("StatueIsBaby", this.isTrappedBaby());
    }

    @Override
    public float getScale() {
        return this.getTrappedScale();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCrackAmount(tag.getByte("CrackAmount"));
        this.setTrappedEntityWidth(tag.getFloat("StatueWidth"));
        this.setTrappedHeight(tag.getFloat("StatueHeight"));
        this.setTrappedScale(tag.getFloat("StatueScale"));
        this.setTrappedEntityTypeString(tag.getString("StatueEntityType"));
        if (tag.contains("StatueEntityTag")) {
            this.setTrappedTag(tag.getCompound("StatueEntityTag"));

        }
        if (tag.contains("StatueIsBaby")) {
            this.setTrappedBaby(tag.getBoolean("StatueIsBaby"));
        }
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose poseIn) {
        return stoneStatueSize;
    }

    @Override
    public void tick() {
        super.tick();
        this.setYRot(this.yBodyRot);
        this.yHeadRot = this.getYRot();
        if (Math.abs(this.getBbWidth() - getTrappedWidth()) > 0.01 || Math.abs(this.getBbHeight() - getTrappedHeight()) > 0.01) {
            double prevX = this.getX();
            double prevZ = this.getZ();
            this.stoneStatueSize = EntityDimensions.scalable(getTrappedWidth(), getTrappedHeight());
            refreshDimensions();
            this.setPos(prevX, this.getY(), prevZ);
        }
    }

    @Override
    public void kill() {
        this.playBreakEffect();
        super.remove(RemovalReason.KILLED);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
            this.playBreakEffect();
        }
        super.remove(reason);
    }

    private void playBreakEffect() {
        if (!this.level().isClientSide && !this.playedBreakEffect) {
            BlockState state = Blocks.STONE.defaultBlockState();
            // Vanilla block break event (plays sound and central particles)
            this.level().levelEvent(null, 2001, this.blockPosition(), Block.getId(state));

            // Scale particle area to the statue's hitbox
            AABB box = this.getBoundingBox();
            double w = box.getXsize();
            double h = box.getYsize();
            double d = box.getZsize();
            double cx = (box.minX + box.maxX) * 0.5D;
            double cy = (box.minY + box.maxY) * 0.5D;
            double cz = (box.minZ + box.maxZ) * 0.5D;

            // Particle count proportional to surface area; normalized to a 1x2x1 box (area=10)
            double area = 2.0D * (w * h + w * d + h * d);
            double baselineArea = 10.0D; // 2*(1*2 + 1*1 + 2*1)
            int count = Mth.clamp((int) Math.round(30.0D * (area / baselineArea)), 4, 200);

            if (this.level() instanceof ServerLevel server) {
                BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
                // Spread across the full hitbox for a proper distributed effect
                server.sendParticles(particle, cx, cy, cz, count, w * 0.5D, h * 0.5D, d * 0.5D, 0.15D);
            }

            this.playedBreakEffect = true;
        }
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return ImmutableList.of();
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slotIn, @NotNull ItemStack stack) {

    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    public int getCrackAmount() {
        return this.entityData.get(CRACK_AMOUNT);
    }

    public void setCrackAmount(int crackAmount) {
        this.entityData.set(CRACK_AMOUNT, crackAmount);
    }


    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBeTurnedToStone() {
        return false;
    }
}
