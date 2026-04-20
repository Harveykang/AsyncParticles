////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by Fernflower decompiler)
////
//
//package com.simibubi.create.content.contraptions;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.simibubi.create.AllItems;
//import com.simibubi.create.AllSoundEvents;
//import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
//import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
//import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
//import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
//import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
//import com.simibubi.create.content.contraptions.actors.trainControls.ControlsStopControllingPacket;
//import com.simibubi.create.content.contraptions.behaviour.MovementContext;
//import com.simibubi.create.content.contraptions.data.ContraptionSyncLimiting;
//import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
//import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
//import com.simibubi.create.content.contraptions.mounted.MountedContraption;
//import com.simibubi.create.content.contraptions.sync.ContraptionSeatMappingPacket;
//import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
//import com.simibubi.create.content.trains.entity.CarriageContraption;
//import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
//import com.simibubi.create.content.trains.entity.Train;
//import com.simibubi.create.foundation.advancement.AllAdvancements;
//import com.simibubi.create.foundation.collision.Matrix3d;
//import com.simibubi.create.foundation.mixin.accessor.ServerLevelAccessor;
//import io.netty.handler.codec.DecoderException;
//import java.util.Collection;
//import java.util.IdentityHashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.UUID;
//import net.createmod.catnip.math.AngleHelper;
//import net.createmod.catnip.math.BlockFace;
//import net.createmod.catnip.math.VecHelper;
//import net.createmod.catnip.platform.CatnipServices;
//import net.minecraft.client.Minecraft;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.NbtAccounter;
//import net.minecraft.nbt.Tag;
//import net.minecraft.network.RegistryFriendlyByteBuf;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.util.Mth;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.MoverType;
//import net.minecraft.world.entity.TamableAnimal;
//import net.minecraft.world.entity.Entity.RemovalReason;
//import net.minecraft.world.entity.decoration.ArmorStand;
//import net.minecraft.world.entity.decoration.HangingEntity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.entity.projectile.Projectile;
//import net.minecraft.world.entity.vehicle.AbstractMinecart;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import net.minecraft.world.level.material.PushReaction;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
//import org.apache.commons.lang3.mutable.MutableInt;
//import org.apache.commons.lang3.tuple.MutablePair;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//public abstract class AbstractContraptionEntity extends Entity implements IEntityWithComplexSpawn {
//	private static final EntityDataAccessor<Boolean> STALLED;
//	private static final EntityDataAccessor<Optional<UUID>> CONTROLLED_BY;
//	public final Map<Entity, MutableInt> collidingEntities = new IdentityHashMap();
//	protected Contraption contraption;
//	protected boolean initialized;
//	protected boolean prevPosInvalid = true;
//	private boolean skipActorStop;
//	public int staleTicks = 3;
//
//	public AbstractContraptionEntity(EntityType<?> entityTypeIn, Level worldIn) {
//		super(entityTypeIn, worldIn);
//	}
//
//	protected void setContraption(Contraption contraption) {
//		this.contraption = contraption;
//		if (contraption != null) {
//			if (!this.level().isClientSide) {
//				contraption.onEntityCreated(this);
//			}
//		}
//	}
//
//	public void move(MoverType pType, Vec3 pPos) {
//		if (pType != MoverType.SHULKER) {
//			if (pType != MoverType.SHULKER_BOX) {
//				if (pType != MoverType.PISTON) {
//					super.move(pType, pPos);
//				}
//			}
//		}
//	}
//
//	public boolean supportsTerrainCollision() {
//		return this.contraption instanceof TranslatingContraption && !(this.contraption instanceof ElevatorContraption);
//	}
//
//	protected void contraptionInitialize() {
//		this.contraption.onEntityInitialize(this.level(), this);
//		this.initialized = true;
//	}
//
//	public boolean collisionEnabled() {
//		return true;
//	}
//
//	public void registerColliding(Entity collidingEntity) {
//		this.collidingEntities.put(collidingEntity, new MutableInt());
//	}
//
//	public void addSittingPassenger(Entity passenger, int seatIndex) {
//		for(Entity entity : this.getPassengers()) {
//			BlockPos seatOf = this.contraption.getSeatOf(entity.getUUID());
//			if (seatOf != null && seatOf.equals(this.contraption.getSeats().get(seatIndex))) {
//				if (entity instanceof Player) {
//					return;
//				}
//
//				if (!(passenger instanceof Player)) {
//					return;
//				}
//
//				entity.stopRiding();
//			}
//		}
//
//		passenger.startRiding(this, true);
//		if (passenger instanceof TamableAnimal ta) {
//			ta.setInSittingPose(true);
//		}
//
//		if (!this.level().isClientSide) {
//			this.contraption.getSeatMapping().put(passenger.getUUID(), seatIndex);
//			CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new ContraptionSeatMappingPacket(this.getId(), this.contraption.getSeatMapping()));
//		}
//	}
//
//	protected void removePassenger(Entity passenger) {
//		Vec3 transformedVector = this.getPassengerPosition(passenger, 1.0F);
//		super.removePassenger(passenger);
//		if (passenger instanceof TamableAnimal ta) {
//			ta.setInSittingPose(false);
//		}
//
//		if (!this.level().isClientSide) {
//			if (transformedVector != null) {
//				passenger.getPersistentData().put("ContraptionDismountLocation", VecHelper.writeNBT(transformedVector));
//			}
//
//			this.contraption.getSeatMapping().remove(passenger.getUUID());
//			CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new ContraptionSeatMappingPacket(this.getId(), this.contraption.getSeatMapping(), passenger.getId()));
//		}
//	}
//
//	public Vec3 getDismountLocationForPassenger(LivingEntity entityLiving) {
//		Vec3 position = super.getDismountLocationForPassenger(entityLiving);
//		CompoundTag data = entityLiving.getPersistentData();
//		if (!data.contains("ContraptionDismountLocation")) {
//			return position;
//		} else {
//			position = VecHelper.readNBT(data.getList("ContraptionDismountLocation", 6));
//			data.remove("ContraptionDismountLocation");
//			entityLiving.setOnGround(false);
//			if (!data.contains("ContraptionMountLocation")) {
//				return position;
//			} else {
//				Vec3 prevPosition = VecHelper.readNBT(data.getList("ContraptionMountLocation", 6));
//				data.remove("ContraptionMountLocation");
//				if (entityLiving instanceof Player) {
//					Player player = (Player)entityLiving;
//					if (!prevPosition.closerThan(position, (double)5000.0F)) {
//						AllAdvancements.LONG_TRAVEL.awardTo(player);
//					}
//				}
//
//				return position;
//			}
//		}
//	}
//
//	public void positionRider(Entity passenger, Entity.MoveFunction callback) {
//		if (this.hasPassenger(passenger)) {
//			Vec3 transformedVector = this.getPassengerPosition(passenger, 1.0F);
//			if (transformedVector != null) {
//				float offset = -0.125F;
//				if (passenger instanceof AbstractContraptionEntity) {
//					offset = 0.0F;
//				}
//
//				callback.accept(passenger, transformedVector.x, transformedVector.y + SeatEntity.getCustomEntitySeatOffset(passenger) + (double)offset, transformedVector.z);
//			}
//		}
//	}
//
//	public Vec3 getPassengerPosition(Entity passenger, float partialTicks) {
//		if (this.contraption == null) {
//			return null;
//		} else {
//			UUID id = passenger.getUUID();
//			if (passenger instanceof OrientedContraptionEntity) {
//				BlockPos localPos = this.contraption.getBearingPosOf(id);
//				if (localPos != null) {
//					return this.toGlobalVector(VecHelper.getCenterOf(localPos), partialTicks).add(VecHelper.getCenterOf(BlockPos.ZERO)).subtract((double)0.5F, (double)1.0F, (double)0.5F);
//				}
//			}
//
//			AABB bb = passenger.getBoundingBox();
//			double ySize = bb.getYsize();
//			BlockPos seat = this.contraption.getSeatOf(id);
//			if (seat == null) {
//				return null;
//			} else {
//				Vec3 transformedVector = this.toGlobalVector(Vec3.atLowerCornerOf(seat).add((double)0.5F, -passenger.getVehicleAttachmentPoint(this).y + ySize + (double)0.125F - SeatEntity.getCustomEntitySeatOffset(passenger), (double)0.5F), partialTicks).add(VecHelper.getCenterOf(BlockPos.ZERO)).subtract((double)0.5F, ySize, (double)0.5F);
//				return transformedVector;
//			}
//		}
//	}
//
//	protected boolean canAddPassenger(@NotNull Entity pPassenger) {
//		if (pPassenger instanceof OrientedContraptionEntity) {
//			return true;
//		} else {
//			return this.contraption.getSeatMapping().size() < this.contraption.getSeats().size();
//		}
//	}
//
//	public Component getContraptionName() {
//		return this.getName();
//	}
//
//	public Optional<UUID> getControllingPlayer() {
//		return (Optional)this.entityData.get(CONTROLLED_BY);
//	}
//
//	public void setControllingPlayer(@Nullable UUID playerId) {
//		this.entityData.set(CONTROLLED_BY, Optional.ofNullable(playerId));
//	}
//
//	public boolean startControlling(BlockPos controlsLocalPos, Player player) {
//		return false;
//	}
//
//	public boolean control(BlockPos controlsLocalPos, Collection<Integer> heldControls, Player player) {
//		return true;
//	}
//
//	public void stopControlling(BlockPos controlsLocalPos) {
//		Optional var10000 = this.getControllingPlayer();
//		Level var10001 = this.level();
//		Objects.requireNonNull(var10001);
//		var10000.map(var10001::getPlayerByUUID).map((p) -> p instanceof ServerPlayer ? (ServerPlayer)p : null).ifPresent((p) -> CatnipServices.NETWORK.sendToClient(p, ControlsStopControllingPacket.INSTANCE));
//		this.setControllingPlayer((UUID)null);
//	}
//
//	public boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand interactionHand) {
//		int indexOfSeat = this.contraption.getSeats().indexOf(localPos);
//		if (indexOfSeat != -1 && !AllItems.WRENCH.isIn(player.getItemInHand(interactionHand))) {
//			if (player.isPassenger()) {
//				return false;
//			} else {
//				Entity toDismount = null;
//
//				for(Map.Entry<UUID, Integer> entry : this.contraption.getSeatMapping().entrySet()) {
//					if ((Integer)entry.getValue() == indexOfSeat) {
//						for(Entity entity : this.getPassengers()) {
//							if (((UUID)entry.getKey()).equals(entity.getUUID())) {
//								if (entity instanceof Player) {
//									return false;
//								}
//
//								toDismount = entity;
//							}
//						}
//					}
//				}
//
//				if (toDismount != null && !this.level().isClientSide) {
//					Vec3 transformedVector = this.getPassengerPosition(toDismount, 1.0F);
//					toDismount.stopRiding();
//					if (transformedVector != null) {
//						toDismount.teleportTo(transformedVector.x, transformedVector.y, transformedVector.z);
//					}
//				}
//
//				if (this.level().isClientSide) {
//					return true;
//				} else {
//					this.addSittingPassenger((Entity)SeatBlock.getLeashed(this.level(), player).or(player), indexOfSeat);
//					return true;
//				}
//			}
//		} else {
//			return this.contraption.interactors.containsKey(localPos) ? ((MovingInteractionBehaviour)this.contraption.interactors.get(localPos)).handlePlayerInteraction(player, interactionHand, localPos, this) : this.contraption.storage.handlePlayerStorageInteraction(this.contraption, player, localPos);
//		}
//	}
//
//	public boolean canInteractWithBlock(Player player, BlockPos localPos, double distance) {
//		return this.canInteractWithBlock(player, Vec3.atCenterOf(localPos), distance);
//	}
//
//	public boolean canInteractWithBlock(Player player, Vec3 localPos, double distance) {
//		BlockPos pos = BlockPos.containing(this.toGlobalVector(localPos, 0.0F));
//		return player.canInteractWithBlock(pos, distance);
//	}
//
//	public Vec3 toGlobalVector(Vec3 localVec, float partialTicks) {
//		return this.toGlobalVector(localVec, partialTicks, false);
//	}
//
//	public Vec3 toGlobalVector(Vec3 localVec, float partialTicks, boolean prevAnchor) {
//		Vec3 anchor = prevAnchor ? this.getPrevAnchorVec() : this.getAnchorVec();
//		Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
//		localVec = localVec.subtract(rotationOffset);
//		localVec = this.applyRotation(localVec, partialTicks);
//		localVec = localVec.add(rotationOffset).add(anchor);
//		return localVec;
//	}
//
//	public Vec3 toLocalVector(Vec3 localVec, float partialTicks) {
//		return this.toLocalVector(localVec, partialTicks, false);
//	}
//
//	public Vec3 toLocalVector(Vec3 globalVec, float partialTicks, boolean prevAnchor) {
//		Vec3 anchor = prevAnchor ? this.getPrevAnchorVec() : this.getAnchorVec();
//		Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
//		globalVec = globalVec.subtract(anchor).subtract(rotationOffset);
//		globalVec = this.reverseRotation(globalVec, partialTicks);
//		globalVec = globalVec.add(rotationOffset);
//		return globalVec;
//	}
//
//	public void tick() {
//		if (this.contraption == null) {
//			this.discard();
//		} else {
//			this.collidingEntities.entrySet().removeIf((e) -> ((MutableInt)e.getValue()).incrementAndGet() > 3);
//			this.xo = this.getX();
//			this.yo = this.getY();
//			this.zo = this.getZ();
//			this.prevPosInvalid = false;
//			if (!this.initialized) {
//				this.contraptionInitialize();
//			}
//
//			this.contraption.tickStorage(this);
//			this.tickContraption();
//			super.tick();
//			Level var2 = this.level();
//			if (var2 instanceof ServerLevelAccessor) {
//				ServerLevelAccessor sl = (ServerLevelAccessor)var2;
//
//				for(Entity entity : this.getPassengers()) {
//					if (!(entity instanceof Player) && !entity.isAlwaysTicking() && !sl.create$getEntityTickList().contains(entity)) {
//						this.positionRider(entity);
//					}
//				}
//
//			}
//		}
//	}
//
//	public void alignPassenger(Entity passenger) {
//		Vec3 motion = this.getContactPointMotion(passenger.getEyePosition());
//		if (!Mth.equal(motion.length(), (double)0.0F)) {
//			if (!(passenger instanceof ArmorStand)) {
//				if (passenger instanceof LivingEntity) {
//					LivingEntity living = (LivingEntity)passenger;
//					float prevAngle = living.getYRot();
//					float angle = AngleHelper.deg(-Mth.atan2(motion.x, motion.z));
//					angle = AngleHelper.angleLerp((double)0.4F, (double)prevAngle, (double)angle);
//					if (this.level().isClientSide) {
//						living.lerpTo((double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 0.0F, 0);
//						living.lerpHeadTo(0.0F, 0);
//						living.setYRot(angle);
//						living.setXRot(0.0F);
//						living.yBodyRot = angle;
//						living.yHeadRot = angle;
//					} else {
//						living.setYRot(angle);
//					}
//
//				}
//			}
//		}
//	}
//
//	public void setBlock(BlockPos localPos, StructureTemplate.StructureBlockInfo newInfo) {
//		this.contraption.blocks.put(localPos, newInfo);
//		CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new ContraptionBlockChangedPacket(this.getId(), localPos, newInfo.state()));
//	}
//
//	protected abstract void tickContraption();
//
//	public abstract Vec3 applyRotation(Vec3 var1, float var2);
//
//	public abstract Vec3 reverseRotation(Vec3 var1, float var2);
//
//	public void tickActors() {
//		boolean stalledPreviously = this.contraption.stalled;
//		if (!this.level().isClientSide) {
//			this.contraption.stalled = false;
//		}
//
//		this.skipActorStop = true;
//
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.contraption.getActors()) {
//			MovementContext context = (MovementContext)pair.right;
//			StructureTemplate.StructureBlockInfo blockInfo = (StructureTemplate.StructureBlockInfo)pair.left;
//			MovementBehaviour actor = (MovementBehaviour)MovementBehaviour.REGISTRY.get(blockInfo.state());
//			if (actor != null) {
//				Vec3 oldMotion = context.motion;
//				Vec3 actorPosition = this.toGlobalVector(VecHelper.getCenterOf(blockInfo.pos()).add(actor.getActiveAreaOffset(context)), 1.0F);
//				BlockPos gridPosition = BlockPos.containing(actorPosition);
//				boolean newPosVisited = !context.stall && this.shouldActorTrigger(context, blockInfo, actor, actorPosition, gridPosition);
//				context.rotation = (v) -> this.applyRotation(v, 1.0F);
//				context.position = actorPosition;
//				if (this.isActorActive(context, actor) || actor.mustTickWhileDisabled()) {
//					if (newPosVisited && !context.stall) {
//						actor.visitNewPosition(context, gridPosition);
//						if (!this.isAlive()) {
//							break;
//						}
//
//						context.firstMovement = false;
//					}
//
//					if (!oldMotion.equals(context.motion)) {
//						actor.onSpeedChanged(context, oldMotion, context.motion);
//						if (!this.isAlive()) {
//							break;
//						}
//					}
//
//					actor.tick(context);
//					if (!this.isAlive()) {
//						break;
//					}
//
//					Contraption var10000 = this.contraption;
//					var10000.stalled |= context.stall;
//				}
//			}
//		}
//
//		if (!this.isAlive()) {
//			this.contraption.stop(this.level());
//		} else {
//			this.skipActorStop = false;
//
//			for(Entity entity : this.getPassengers()) {
//				if (entity instanceof OrientedContraptionEntity) {
//					OrientedContraptionEntity orientedCE = (OrientedContraptionEntity)entity;
//					if (this.contraption.stabilizedSubContraptions.containsKey(entity.getUUID()) && orientedCE.contraption != null && orientedCE.contraption.stalled) {
//						this.contraption.stalled = true;
//						break;
//					}
//				}
//			}
//
//			if (!this.level().isClientSide) {
//				if (!stalledPreviously && this.contraption.stalled) {
//					this.onContraptionStalled();
//				}
//
//				this.entityData.set(STALLED, this.contraption.stalled);
//			} else {
//				this.contraption.stalled = this.isStalled();
//			}
//		}
//	}
//
//	public void refreshPSIs() {
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.contraption.getActors()) {
//			MovementContext context = (MovementContext)pair.right;
//			StructureTemplate.StructureBlockInfo blockInfo = (StructureTemplate.StructureBlockInfo)pair.left;
//			MovementBehaviour actor = (MovementBehaviour)MovementBehaviour.REGISTRY.get(blockInfo.state());
//			if (actor instanceof PortableStorageInterfaceMovement && this.isActorActive(context, actor) && context.position != null) {
//				actor.visitNewPosition(context, BlockPos.containing(context.position));
//			}
//		}
//
//	}
//
//	protected boolean isActorActive(MovementContext context, MovementBehaviour actor) {
//		return actor.isActive(context);
//	}
//
//	protected void onContraptionStalled() {
//		CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new ContraptionStallPacket(this.getId(), this.getX(), this.getY(), this.getZ(), this.getStalledAngle()));
//	}
//
//	protected boolean shouldActorTrigger(MovementContext context, StructureTemplate.StructureBlockInfo blockInfo, MovementBehaviour actor, Vec3 actorPosition, BlockPos gridPosition) {
//		Vec3 previousPosition = context.position;
//		if (previousPosition == null) {
//			return false;
//		} else {
//			context.motion = actorPosition.subtract(previousPosition);
//			if (!this.level().isClientSide()) {
//				AbstractContraptionEntity var8 = context.contraption.entity;
//				if (var8 instanceof CarriageContraptionEntity) {
//					CarriageContraptionEntity cce = (CarriageContraptionEntity)var8;
//					if (cce.getCarriage() != null) {
//						Train train = cce.getCarriage().train;
//						double actualSpeed = train.speedBeforeStall != null ? train.speedBeforeStall : train.speed;
//						context.motion = context.motion.normalize().scale(Math.abs(actualSpeed));
//					}
//				}
//			}
//
//			Vec3 relativeMotion = context.motion;
//			relativeMotion = this.reverseRotation(relativeMotion, 1.0F);
//			context.relativeMotion = relativeMotion;
//			boolean ignoreMotionForFirstMovement = context.contraption instanceof CarriageContraption || actor instanceof PortableStorageInterfaceMovement;
//			return !BlockPos.containing(previousPosition).equals(gridPosition) || (context.relativeMotion.length() > (double)0.0F || ignoreMotionForFirstMovement) && context.firstMovement;
//		}
//	}
//
//	public void move(double x, double y, double z) {
//		this.setPos(this.getX() + x, this.getY() + y, this.getZ() + z);
//	}
//
//	public Vec3 getAnchorVec() {
//		return this.position();
//	}
//
//	public Vec3 getPrevAnchorVec() {
//		return this.getPrevPositionVec();
//	}
//
//	public float getYawOffset() {
//		return 0.0F;
//	}
//
//	public void setPos(double x, double y, double z) {
//		super.setPos(x, y, z);
//		if (this.contraption != null) {
//			AABB cbox = this.contraption.bounds;
//			if (cbox != null) {
//				Vec3 actualVec = this.getAnchorVec();
//				this.setBoundingBox(cbox.move(actualVec));
//			}
//		}
//	}
//
//	public static float yawFromVector(Vec3 vec) {
//		return (float)(((Math.PI * 1.5D) + Math.atan2(vec.z, vec.x)) / Math.PI * (double)180.0F);
//	}
//
//	public static float pitchFromVector(Vec3 vec) {
//		return (float)(Math.acos(vec.y) / Math.PI * (double)180.0F);
//	}
//
//	public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
//		return builder.sized(1.0F, 1.0F);
//	}
//
//	protected void defineSynchedData(SynchedEntityData.Builder builder) {
//		builder.define(STALLED, false);
//		builder.define(CONTROLLED_BY, Optional.empty());
//	}
//
//	public void writeSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
//		CompoundTag compound = new CompoundTag();
//		this.writeAdditional(compound, registryFriendlyByteBuf.registryAccess(), true);
//		ContraptionSyncLimiting.writeSafe(compound, registryFriendlyByteBuf);
//	}
//
//	protected final void addAdditionalSaveData(CompoundTag compound) {
//		this.writeAdditional(compound, this.registryAccess(), false);
//	}
//
//	protected void writeAdditional(CompoundTag compound, HolderLookup.Provider registries, boolean spawnPacket) {
//		if (this.contraption != null) {
//			compound.put("Contraption", this.contraption.writeNBT(registries, spawnPacket));
//		}
//
//		compound.putBoolean("Stalled", this.isStalled());
//		compound.putBoolean("Initialized", this.initialized);
//	}
//
//	public void readSpawnData(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
//		CompoundTag nbt = readAnySizeNbt(registryFriendlyByteBuf);
//		if (nbt != null) {
//			this.readAdditional(nbt, true);
//		}
//
//	}
//
//	protected final void readAdditionalSaveData(CompoundTag compound) {
//		this.readAdditional(compound, false);
//	}
//
//	private static @Nullable CompoundTag readAnySizeNbt(RegistryFriendlyByteBuf buf) {
//		Tag tag = buf.readNbt(NbtAccounter.unlimitedHeap());
//		if (tag != null && !(tag instanceof CompoundTag)) {
//			throw new DecoderException("Not a compound tag: " + String.valueOf(tag));
//		} else {
//			return (CompoundTag)tag;
//		}
//	}
//
//	protected void readAdditional(CompoundTag compound, boolean spawnData) {
//		if (!compound.isEmpty()) {
//			this.initialized = compound.getBoolean("Initialized");
//			this.contraption = Contraption.fromNBT(this.level(), compound.getCompound("Contraption"), spawnData);
//			this.contraption.entity = this;
//			this.entityData.set(STALLED, compound.getBoolean("Stalled"));
//		}
//	}
//
//	public void disassemble() {
//		if (this.isAlive()) {
//			if (this.contraption != null) {
//				StructureTransform transform = this.makeStructureTransform();
//				this.contraption.stop(this.level());
//				CatnipServices.NETWORK.sendToClientsTrackingEntity(this, new ContraptionDisassemblyPacket(this.getId(), transform));
//				this.contraption.addBlocksToWorld(this.level(), transform);
//				this.contraption.addPassengersToWorld(this.level(), transform, this.getPassengers());
//
//				for(Entity entity : this.getPassengers()) {
//					if (entity instanceof OrientedContraptionEntity) {
//						UUID id = entity.getUUID();
//						if (this.contraption.stabilizedSubContraptions.containsKey(id)) {
//							BlockPos transformed = transform.apply(((BlockFace)this.contraption.stabilizedSubContraptions.get(id)).getConnectedPos());
//							entity.setPos((double)transformed.getX(), (double)transformed.getY(), (double)transformed.getZ());
//							((AbstractContraptionEntity)entity).disassemble();
//						}
//					}
//				}
//
//				this.skipActorStop = true;
//				this.discard();
//				this.ejectPassengers();
//				this.moveCollidedEntitiesOnDisassembly(transform);
//				AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(this.level(), this.blockPosition());
//			}
//		}
//	}
//
//	private void moveCollidedEntitiesOnDisassembly(StructureTransform transform) {
//		for(Entity entity : this.collidingEntities.keySet()) {
//			Vec3 localVec = this.toLocalVector(entity.position(), 0.0F);
//			Vec3 transformed = transform.apply(localVec);
//			if (this.level().isClientSide) {
//				entity.setPos(transformed.x, transformed.y + (double)0.0625F, transformed.z);
//			} else {
//				entity.teleportTo(transformed.x, transformed.y + (double)0.0625F, transformed.z);
//			}
//		}
//
//	}
//
//	public void remove(Entity.RemovalReason p_146834_) {
//		if (!this.level().isClientSide && !this.isRemoved() && this.contraption != null && !this.skipActorStop) {
//			this.contraption.stop(this.level());
//		}
//
//		super.remove(p_146834_);
//	}
//
//	protected abstract StructureTransform makeStructureTransform();
//
//	public void kill() {
//		this.ejectPassengers();
//		super.kill();
//	}
//
//	protected void onBelowWorld() {
//		this.ejectPassengers();
//		super.onBelowWorld();
//	}
//
//	public void onRemovedFromLevel() {
//		super.onRemovedFromLevel();
//	}
//
//	protected void doWaterSplashEffect() {
//	}
//
//	public Contraption getContraption() {
//		return this.contraption;
//	}
//
//	public boolean isStalled() {
//		return (Boolean)this.entityData.get(STALLED);
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	static void handleStallPacket(ContraptionStallPacket packet) {
//		Entity var2 = Minecraft.getInstance().level.getEntity(packet.entityId());
//		if (var2 instanceof AbstractContraptionEntity ce) {
//			ce.handleStallInformation(packet.x(), packet.y(), packet.z(), packet.angle());
//		}
//
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	static void handleBlockChangedPacket(ContraptionBlockChangedPacket packet) {
//		Entity var2 = Minecraft.getInstance().level.getEntity(packet.entityId());
//		if (var2 instanceof AbstractContraptionEntity ce) {
//			ce.handleBlockChange(packet.localPos(), packet.newState());
//		}
//
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	static void handleDisassemblyPacket(ContraptionDisassemblyPacket packet) {
//		Entity var2 = Minecraft.getInstance().level.getEntity(packet.entityId());
//		if (var2 instanceof AbstractContraptionEntity ce) {
//			ce.moveCollidedEntitiesOnDisassembly(packet.transform());
//		}
//
//	}
//
//	protected abstract float getStalledAngle();
//
//	protected abstract void handleStallInformation(double var1, double var3, double var5, float var7);
//
//	@OnlyIn(Dist.CLIENT)
//	protected void handleBlockChange(BlockPos localPos, BlockState newState) {
//		if (this.contraption != null && this.contraption.blocks.containsKey(localPos)) {
//			StructureTemplate.StructureBlockInfo info = (StructureTemplate.StructureBlockInfo)this.contraption.blocks.get(localPos);
//			this.contraption.blocks.put(localPos, new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt()));
//			if (info.state() != newState && !(newState.getBlock() instanceof SlidingDoorBlock)) {
//				this.contraption.resetClientContraption();
//			}
//
//			this.contraption.invalidateColliders();
//		}
//	}
//
//	public CompoundTag saveWithoutId(CompoundTag nbt) {
//		Vec3 vec = this.position();
//
//		for(Entity entity : this.getPassengers()) {
//			entity.removalReason = RemovalReason.UNLOADED_TO_CHUNK;
//			Vec3 prevVec = entity.position();
//			entity.setPosRaw(vec.x, prevVec.y, vec.z);
//			entity.removalReason = null;
//		}
//
//		CompoundTag tag = super.saveWithoutId(nbt);
//		return tag;
//	}
//
//	public void setDeltaMovement(Vec3 motionIn) {
//	}
//
//	public PushReaction getPistonPushReaction() {
//		return PushReaction.IGNORE;
//	}
//
//	public void setContraptionMotion(Vec3 vec) {
//		super.setDeltaMovement(vec);
//	}
//
//	public boolean isPickable() {
//		return false;
//	}
//
//	public boolean hurt(DamageSource source, float amount) {
//		return false;
//	}
//
//	public Vec3 getPrevPositionVec() {
//		return this.prevPosInvalid ? this.position() : new Vec3(this.xo, this.yo, this.zo);
//	}
//
//	public abstract ContraptionRotationState getRotationState();
//
//	public Vec3 getContactPointMotion(Vec3 globalContactPoint) {
//		if (this.prevPosInvalid) {
//			return Vec3.ZERO;
//		} else {
//			Vec3 contactPoint = this.toGlobalVector(this.toLocalVector(globalContactPoint, 0.0F, true), 1.0F, true);
//			Vec3 contraptionLocalMovement = contactPoint.subtract(globalContactPoint);
//			Vec3 contraptionAnchorMovement = this.position().subtract(this.getPrevPositionVec());
//			return contraptionLocalMovement.add(contraptionAnchorMovement);
//		}
//	}
//
//	public boolean canCollideWith(Entity e) {
//		if (e instanceof Player && e.isSpectator()) {
//			return false;
//		} else if (e.noPhysics) {
//			return false;
//		} else if (e instanceof HangingEntity) {
//			return false;
//		} else if (e instanceof AbstractMinecart) {
//			return !(this.contraption instanceof MountedContraption);
//		} else if (e instanceof SuperGlueEntity) {
//			return false;
//		} else if (e instanceof SeatEntity) {
//			return false;
//		} else if (e instanceof Projectile) {
//			return false;
//		} else if (e.getVehicle() != null) {
//			return false;
//		} else {
//			for(Entity riding = this.getVehicle(); riding != null; riding = riding.getVehicle()) {
//				if (riding == e) {
//					return false;
//				}
//			}
//
//			return e.getPistonPushReaction() == PushReaction.NORMAL;
//		}
//	}
//
//	public boolean hasExactlyOnePlayerPassenger() {
//		return false;
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	public abstract void applyLocalTransforms(PoseStack var1, float var2);
//
//	protected boolean updateInWaterStateAndDoFluidPushing() {
//		return false;
//	}
//
//	public void igniteForTicks(int ticks) {
//	}
//
//	public boolean fireImmune() {
//		return true;
//	}
//
//	public boolean isIgnoringBlockTriggers() {
//		return true;
//	}
//
//	public boolean isReadyForRender() {
//		return this.initialized;
//	}
//
//	public boolean isAliveOrStale() {
//		return !this.isAlive() && !this.level().isClientSide() ? false : this.staleTicks > 0;
//	}
//
//	public boolean isPrevPosInvalid() {
//		return this.prevPosInvalid;
//	}
//
//	static {
//		STALLED = SynchedEntityData.defineId(AbstractContraptionEntity.class, EntityDataSerializers.BOOLEAN);
//		CONTROLLED_BY = SynchedEntityData.defineId(AbstractContraptionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
//	}
//
//	public static class ContraptionRotationState {
//		public static final ContraptionRotationState NONE = new ContraptionRotationState();
//		public float xRotation = 0.0F;
//		public float yRotation = 0.0F;
//		public float zRotation = 0.0F;
//		public float secondYRotation = 0.0F;
//		Matrix3d matrix;
//
//		public ContraptionRotationState() {
//		}
//
//		public Matrix3d asMatrix() {
//			if (this.matrix != null) {
//				return this.matrix;
//			} else {
//				this.matrix = (new Matrix3d()).asIdentity();
//				if (this.xRotation != 0.0F) {
//					this.matrix.multiply((new Matrix3d()).asXRotation(AngleHelper.rad((double)(-this.xRotation))));
//				}
//
//				if (this.yRotation != 0.0F) {
//					this.matrix.multiply((new Matrix3d()).asYRotation(AngleHelper.rad((double)(-this.yRotation))));
//				}
//
//				if (this.zRotation != 0.0F) {
//					this.matrix.multiply((new Matrix3d()).asZRotation(AngleHelper.rad((double)(-this.zRotation))));
//				}
//
//				return this.matrix;
//			}
//		}
//
//		public boolean hasVerticalRotation() {
//			return this.xRotation != 0.0F || this.zRotation != 0.0F;
//		}
//
//		public float getYawOffset() {
//			return this.secondYRotation;
//		}
//	}
//}
