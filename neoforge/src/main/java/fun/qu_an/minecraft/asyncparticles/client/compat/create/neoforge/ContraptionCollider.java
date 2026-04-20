////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by Fernflower decompiler)
////
//
//package com.simibubi.create.content.contraptions;
//
//import com.simibubi.create.AllBlocks;
//import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
//import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
//import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
//import com.simibubi.create.content.contraptions.sync.ClientMotionPacket;
//import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
//import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
//import com.simibubi.create.foundation.advancement.AllAdvancements;
//import com.simibubi.create.foundation.advancement.CreateAdvancement;
//import com.simibubi.create.foundation.collision.CollisionList;
//import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
//import com.simibubi.create.foundation.collision.Matrix3d;
//import com.simibubi.create.foundation.collision.OrientedBB;
//import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
//import com.simibubi.create.foundation.utility.BlockHelper;
//import com.simibubi.create.infrastructure.config.AllConfigs;
//import java.lang.ref.WeakReference;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.WeakHashMap;
//import net.createmod.catnip.math.VecHelper;
//import net.createmod.catnip.platform.CatnipServices;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.multiplayer.ClientPacketListener;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.client.player.RemotePlayer;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.Direction.Axis;
//import net.minecraft.core.Direction.AxisDirection;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.tags.BlockTags;
//import net.minecraft.util.Mth;
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraft.world.entity.item.ItemEntity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.CocoaBlock;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.BlockHitResult;
//import net.minecraft.world.phys.Vec3;
//import net.minecraft.world.phys.shapes.Shapes;
//import net.minecraft.world.phys.shapes.VoxelShape;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//import org.apache.commons.lang3.mutable.MutableBoolean;
//import org.apache.commons.lang3.tuple.MutablePair;
//
//public class ContraptionCollider {
//	private static MutablePair<WeakReference<AbstractContraptionEntity>, Double> safetyLock = new MutablePair();
//	private static Map<AbstractContraptionEntity, Map<Player, Double>> remoteSafetyLocks = new WeakHashMap();
//	private static int packetCooldown = 0;
//
//	public ContraptionCollider() {
//	}
//
//	static void collideEntities(AbstractContraptionEntity contraptionEntity) {
//		Level world = contraptionEntity.getCommandSenderWorld();
//		Contraption contraption = contraptionEntity.getContraption();
//		AABB bounds = contraptionEntity.getBoundingBox();
//		if (contraption != null) {
//			if (bounds != null) {
//				Vec3 contraptionPosition = contraptionEntity.position();
//				Vec3 contraptionMotion = contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
//				Vec3 anchorVec = contraptionEntity.getAnchorVec();
//				AbstractContraptionEntity.ContraptionRotationState rotation = null;
//				if (world.isClientSide() && safetyLock.left != null && ((WeakReference)safetyLock.left).get() == contraptionEntity) {
//					CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> saveClientPlayerFromClipping(contraptionEntity, contraptionMotion));
//				}
//
//				boolean skipClientPlayer = false;
//				CollisionList denseViableColliders = new CollisionList();
//				AABB var10002 = bounds.inflate((double)2.0F).expandTowards((double)0.0F, (double)32.0F, (double)0.0F);
//				Objects.requireNonNull(contraptionEntity);
//
//				for(Entity entity : world.getEntitiesOfClass(Entity.class, var10002, contraptionEntity::canCollideWith)) {
//					if (entity.isAlive() && !world.tickRateManager().isEntityFrozen(entity)) {
//						PlayerType playerType = getPlayerType(entity);
//						if (playerType == ContraptionCollider.PlayerType.REMOTE) {
//							if (contraption instanceof TranslatingContraption) {
//								CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> saveRemotePlayerFromClipping((Player)entity, contraptionEntity, contraptionMotion));
//							}
//						} else {
//							entity.getSelfAndPassengers().forEach((e) -> {
//								if (e instanceof ServerPlayer) {
//									((ServerPlayer)e).connection.aboveGroundTickCount = 0;
//								}
//
//							});
//							if (playerType != ContraptionCollider.PlayerType.SERVER) {
//								if (playerType == ContraptionCollider.PlayerType.CLIENT) {
//									if (skipClientPlayer) {
//										continue;
//									}
//
//									skipClientPlayer = true;
//								}
//
//								if (rotation == null) {
//									rotation = contraptionEntity.getRotationState();
//								}
//
//								Matrix3d rotationMatrix = rotation.asMatrix();
//								Vec3 entityPosition = entity.position();
//								AABB entityBounds = entity.getBoundingBox();
//								Vec3 motion = entity.getDeltaMovement();
//								float yawOffset = rotation.getYawOffset();
//								Vec3 position = getWorldToLocalTranslation(entity, anchorVec, rotationMatrix, yawOffset);
//								if (playerType == ContraptionCollider.PlayerType.CLIENT && entityBounds.getYsize() > (double)1.0F) {
//									entityBounds = entityBounds.contract((double)0.0F, (double)0.125F, (double)0.0F);
//								}
//
//								motion = motion.subtract(contraptionMotion);
//								motion = rotationMatrix.transform(motion);
//								AABB localBB = entityBounds.move(position).inflate(1.0E-7);
//								OrientedBB obb = new OrientedBB(localBB);
//								obb.setRotation(rotationMatrix);
//								CollisionList collidableBBs = contraption.getSimplifiedEntityColliders();
//								if (collidableBBs == null) {
//									collidableBBs = new CollisionList();
//									getPotentiallyCollidedShapes(world, contraption, localBB.expandTowards(motion), new CollisionList.Populate(collidableBBs));
//								}
//
//								ContinuousOBBCollider.CollisionResponse collisionResult = ContinuousOBBCollider.collideMany(collidableBBs, denseViableColliders, obb, motion, entity.maxUpStep(), !rotation.hasVerticalRotation());
//								Vec3 entityMotion = entity.getDeltaMovement();
//								Vec3 entityMotionNoTemporal = entityMotion;
//								Vec3 collisionNormal = collisionResult.normal;
//								Vec3 collisionLocation = collisionResult.location;
//								Vec3 totalResponse = collisionResult.collisionResponse;
//								boolean surfaceCollision = collisionResult.surfaceCollision;
//								boolean hardCollision = !totalResponse.equals(Vec3.ZERO);
//								boolean temporalCollision = collisionResult.temporalResponse != (double)1.0F;
//								Vec3 motionResponse = !temporalCollision ? motion : motion.normalize().scale(motion.length() * collisionResult.temporalResponse);
//								motionResponse = rotationMatrix.transformTransposed(motionResponse).add(contraptionMotion);
//								totalResponse = rotationMatrix.transformTransposed(totalResponse);
//								totalResponse = VecHelper.rotate(totalResponse, (double)yawOffset, Axis.Y);
//								collisionNormal = rotationMatrix.transformTransposed(collisionNormal);
//								collisionNormal = VecHelper.rotate(collisionNormal, (double)yawOffset, Axis.Y);
//								collisionNormal = collisionNormal.normalize();
//								collisionLocation = rotationMatrix.transformTransposed(collisionLocation);
//								collisionLocation = VecHelper.rotate(collisionLocation, (double)yawOffset, Axis.Y);
//								double bounce = (double)0.0F;
//								double slide = (double)0.0F;
//								if (!collisionLocation.equals(Vec3.ZERO)) {
//									collisionLocation = collisionLocation.add(entity.position().add(entity.getBoundingBox().getCenter()).scale((double)0.5F));
//									if (temporalCollision) {
//										collisionLocation = collisionLocation.add((double)0.0F, motionResponse.y, (double)0.0F);
//									}
//
//									BlockPos pos = BlockPos.containing(contraptionEntity.toLocalVector(entity.position(), 0.0F));
//									if (contraption.getBlocks().containsKey(pos)) {
//										BlockState blockState = ((StructureTemplate.StructureBlockInfo)contraption.getBlocks().get(pos)).state();
//										if (blockState.is(BlockTags.CLIMBABLE)) {
//											surfaceCollision = true;
//											totalResponse = totalResponse.add((double)0.0F, (double)0.1F, (double)0.0F);
//										}
//									}
//
//									pos = BlockPos.containing(contraptionEntity.toLocalVector(collisionLocation, 0.0F));
//									if (contraption.getBlocks().containsKey(pos)) {
//										BlockState blockState = ((StructureTemplate.StructureBlockInfo)contraption.getBlocks().get(pos)).state();
//										MovingInteractionBehaviour movingInteractionBehaviour = (MovingInteractionBehaviour)contraption.interactors.get(pos);
//										if (movingInteractionBehaviour != null) {
//											movingInteractionBehaviour.handleEntityCollision(entity, pos, contraptionEntity);
//										}
//
//										bounce = BlockHelper.getBounceMultiplier(blockState.getBlock());
//										slide = (double)Math.max(0.0F, blockState.getFriction(contraption.collisionLevel, pos, entity) - 0.6F);
//									}
//								}
//
//								boolean hasNormal = !collisionNormal.equals(Vec3.ZERO);
//								boolean anyCollision = hardCollision || temporalCollision;
//								if (bounce > (double)0.0F && hasNormal && anyCollision && bounceEntity(entity, collisionNormal, contraptionEntity, bounce)) {
//									entity.level().playSound(playerType == ContraptionCollider.PlayerType.CLIENT ? (Player)entity : null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 0.5F, 1.0F);
//								} else {
//									if (temporalCollision) {
//										double idealVerticalMotion = motionResponse.y;
//										if (idealVerticalMotion != entityMotion.y) {
//											entity.setDeltaMovement(entityMotion.multiply((double)1.0F, (double)0.0F, (double)1.0F).add((double)0.0F, idealVerticalMotion, (double)0.0F));
//											entityMotion = entity.getDeltaMovement();
//										}
//									}
//
//									if (hardCollision) {
//										double motionX = entityMotion.x();
//										double motionY = entityMotion.y();
//										double motionZ = entityMotion.z();
//										double intersectX = totalResponse.x();
//										double intersectY = totalResponse.y();
//										double intersectZ = totalResponse.z();
//										double horizonalEpsilon = (double)0.0078125F;
//										if (motionX != (double)0.0F && Math.abs(intersectX) > horizonalEpsilon && motionX > (double)0.0F == intersectX < (double)0.0F) {
//											entityMotion = entityMotion.multiply((double)0.0F, (double)1.0F, (double)1.0F);
//										}
//
//										if (motionY != (double)0.0F && intersectY != (double)0.0F && motionY > (double)0.0F == intersectY < (double)0.0F) {
//											entityMotion = entityMotion.multiply((double)1.0F, (double)0.0F, (double)1.0F).add((double)0.0F, contraptionMotion.y, (double)0.0F);
//										}
//
//										if (motionZ != (double)0.0F && Math.abs(intersectZ) > horizonalEpsilon && motionZ > (double)0.0F == intersectZ < (double)0.0F) {
//											entityMotion = entityMotion.multiply((double)1.0F, (double)1.0F, (double)0.0F);
//										}
//									}
//
//									if (bounce == (double)0.0F && slide > (double)0.0F && hasNormal && anyCollision && rotation.hasVerticalRotation()) {
//										double slideFactor = collisionNormal.multiply((double)1.0F, (double)0.0F, (double)1.0F).length() * (double)1.25F;
//										Vec3 motionIn = entityMotionNoTemporal.multiply((double)0.0F, 0.9, (double)0.0F).add((double)0.0F, (double)-0.01F, (double)0.0F);
//										Vec3 slideNormal = collisionNormal.cross(motionIn.cross(collisionNormal)).normalize();
//										Vec3 newMotion = entityMotion.multiply(0.85, (double)0.0F, 0.85).add(slideNormal.scale(((double)0.2F + slide) * motionIn.length() * slideFactor).add((double)0.0F, (double)-0.1F - collisionNormal.y * (double)0.125F, (double)0.0F));
//										entity.setDeltaMovement(newMotion);
//										entityMotion = entity.getDeltaMovement();
//									}
//
//									if (hardCollision || surfaceCollision) {
//										Vec3 allowedMovement = collide(totalResponse, entity);
//										entity.setPos(entityPosition.x + allowedMovement.x, entityPosition.y + allowedMovement.y, entityPosition.z + allowedMovement.z);
//										entityPosition = entity.position();
//										entityMotion = handleDamageFromTrain(world, contraptionEntity, contraptionMotion, entity, entityMotion, playerType);
//										entity.hurtMarked = true;
//										Vec3 contactPointMotion = Vec3.ZERO;
//										if (surfaceCollision) {
//											contraptionEntity.registerColliding(entity);
//											entity.fallDistance = 0.0F;
//
//											for(Entity rider : entity.getIndirectPassengers()) {
//												if (getPlayerType(rider) == ContraptionCollider.PlayerType.CLIENT) {
//													CatnipServices.NETWORK.sendToServer(new ClientMotionPacket(rider.getDeltaMovement(), true, 0.0F));
//												}
//											}
//
//											boolean canWalk = bounce != (double)0.0F || slide == (double)0.0F;
//											if (canWalk || !rotation.hasVerticalRotation()) {
//												if (canWalk) {
//													entity.setOnGround(true);
//												}
//
//												if (entity instanceof ItemEntity) {
//													entityMotion = entityMotion.multiply((double)0.5F, (double)1.0F, (double)0.5F);
//												}
//											}
//
//											contactPointMotion = contraptionEntity.getContactPointMotion(entityPosition);
//											allowedMovement = collide(contactPointMotion, entity);
//											entity.setPos(entityPosition.x + allowedMovement.x, entityPosition.y, entityPosition.z + allowedMovement.z);
//										}
//
//										entity.setDeltaMovement(entityMotion);
//										if (playerType == ContraptionCollider.PlayerType.CLIENT) {
//											double d0 = entity.getX() - entity.xo - contactPointMotion.x;
//											double d1 = entity.getZ() - entity.zo - contactPointMotion.z;
//											float limbSwing = Mth.sqrt((float)(d0 * d0 + d1 * d1)) * 4.0F;
//											if (limbSwing > 1.0F) {
//												limbSwing = 1.0F;
//											}
//
//											CatnipServices.NETWORK.sendToServer(new ClientMotionPacket(entityMotion, true, limbSwing));
//											if (entity.onGround() && contraption instanceof TranslatingContraption) {
//												safetyLock.setLeft(new WeakReference(contraptionEntity));
//												safetyLock.setRight(entity.getY() - contraptionEntity.getY());
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//
//			}
//		}
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	private static void saveClientPlayerFromClipping(AbstractContraptionEntity contraptionEntity, Vec3 contraptionMotion) {
//		LocalPlayer entity = Minecraft.getInstance().player;
//		if (!entity.isPassenger()) {
//			double prevDiff = (Double)safetyLock.right;
//			double currentDiff = entity.getY() - contraptionEntity.getY();
//			double motion = contraptionMotion.subtract(entity.getDeltaMovement()).y;
//			double trend = Math.signum(currentDiff - prevDiff);
//			ClientPacketListener handler = entity.connection;
//			if (handler.getOnlinePlayers().size() > 1) {
//				if (packetCooldown > 0) {
//					--packetCooldown;
//				}
//
//				if (packetCooldown == 0) {
//					CatnipServices.NETWORK.sendToServer(new ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest(contraptionEntity.getId(), currentDiff));
//					packetCooldown = 3;
//				}
//			}
//
//			if (trend != (double)0.0F) {
//				if (trend != Math.signum(motion)) {
//					double speed = contraptionMotion.multiply((double)0.0F, (double)1.0F, (double)0.0F).lengthSqr();
//					if (!(trend > (double)0.0F) || !(speed < 0.1)) {
//						if (!(speed < 0.05)) {
//							if (!savePlayerFromClipping(entity, contraptionEntity, contraptionMotion, prevDiff)) {
//								safetyLock.setLeft((Object)null);
//							}
//
//						}
//					}
//				}
//			}
//		}
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	public static void lockPacketReceived(int contraptionId, int remotePlayerId, double suggestedOffset) {
//		ClientLevel level = Minecraft.getInstance().level;
//		Entity var6 = level.getEntity(contraptionId);
//		if (var6 instanceof ControlledContraptionEntity contraptionEntity) {
//			Entity var7 = level.getEntity(remotePlayerId);
//			if (var7 instanceof RemotePlayer player) {
//				((Map)remoteSafetyLocks.computeIfAbsent(contraptionEntity, ($) -> new WeakHashMap())).put(player, suggestedOffset);
//			}
//		}
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	private static void saveRemotePlayerFromClipping(Player entity, AbstractContraptionEntity contraptionEntity, Vec3 contraptionMotion) {
//		if (!entity.isPassenger()) {
//			Map<Player, Double> locksOnThisContraption = (Map)remoteSafetyLocks.getOrDefault(contraptionEntity, Collections.emptyMap());
//			double prevDiff = (Double)locksOnThisContraption.getOrDefault(entity, entity.getY() - contraptionEntity.getY());
//			if (!savePlayerFromClipping(entity, contraptionEntity, contraptionMotion, prevDiff) && locksOnThisContraption.containsKey(entity)) {
//				locksOnThisContraption.remove(entity);
//			}
//
//		}
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	private static boolean savePlayerFromClipping(Player entity, AbstractContraptionEntity contraptionEntity, Vec3 contraptionMotion, double yStartOffset) {
//		AABB bb = entity.getBoundingBox().deflate((double)0.25F, (double)0.0F, (double)0.25F);
//		double shortestDistance = Double.MAX_VALUE;
//		double yStart = (double)entity.maxUpStep() + contraptionEntity.getY() + yStartOffset;
//		double rayLength = Math.max((double)5.0F, Math.abs(entity.getY() - yStart));
//
//		for(int rayIndex = 0; rayIndex < 4; ++rayIndex) {
//			Vec3 start = new Vec3(rayIndex / 2 == 0 ? bb.minX : bb.maxX, yStart, rayIndex % 2 == 0 ? bb.minZ : bb.maxZ);
//			Vec3 end = start.add((double)0.0F, -rayLength, (double)0.0F);
//			BlockHitResult hitResult = ContraptionHandlerClient.rayTraceContraption(start, end, contraptionEntity);
//			if (hitResult != null) {
//				Vec3 hit = contraptionEntity.toGlobalVector(hitResult.getLocation(), 1.0F);
//				double hitDiff = start.y - hit.y;
//				if (shortestDistance > hitDiff) {
//					shortestDistance = hitDiff;
//				}
//			}
//		}
//
//		if (shortestDistance > rayLength) {
//			return false;
//		} else {
//			entity.setPos(entity.getX(), yStart - shortestDistance, entity.getZ());
//			return true;
//		}
//	}
//
//	private static Vec3 handleDamageFromTrain(Level world, AbstractContraptionEntity contraptionEntity, Vec3 contraptionMotion, Entity entity, Vec3 entityMotion, PlayerType playerType) {
//		if (!(contraptionEntity instanceof CarriageContraptionEntity cce)) {
//			return entityMotion;
//		} else if (!entity.onGround()) {
//			return entityMotion;
//		} else {
//			CompoundTag persistentData = entity.getPersistentData();
//			if (persistentData.contains("ContraptionGrounded")) {
//				persistentData.remove("ContraptionGrounded");
//				return entityMotion;
//			} else if (cce.collidingEntities.containsKey(entity)) {
//				return entityMotion;
//			} else if (entity instanceof ItemEntity) {
//				return entityMotion;
//			} else if (cce.nonDamageTicks != 0) {
//				return entityMotion;
//			} else if (!(Boolean)AllConfigs.server().trains.trainsCauseDamage.get()) {
//				return entityMotion;
//			} else {
//				Vec3 diffMotion = contraptionMotion.subtract(entity.getDeltaMovement());
//				if (!(diffMotion.length() <= (double)0.35F) && !(contraptionMotion.length() <= (double)0.35F)) {
//					DamageSource source = CreateDamageSources.runOver(world, contraptionEntity);
//					double damage = diffMotion.length();
//					if (entity.getClassification(false) == MobCategory.MONSTER) {
//						damage *= (double)2.0F;
//					}
//
//					if (entity instanceof Player) {
//						Player p = (Player)entity;
//						if (p.isCreative() || p.isSpectator()) {
//							return entityMotion;
//						}
//					}
//
//					if (playerType == ContraptionCollider.PlayerType.CLIENT) {
//						CatnipServices.NETWORK.sendToServer(new TrainCollisionPacket((int)(damage * (double)16.0F), contraptionEntity.getId()));
//						world.playSound((Player)entity, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 1.0F, 0.75F);
//					} else {
//						entity.hurt(source, (float)((int)(damage * (double)16.0F)));
//						world.playSound((Player)null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 1.0F, 0.75F);
//						if (!entity.isAlive()) {
//							Optional var10000 = contraptionEntity.getControllingPlayer();
//							Objects.requireNonNull(world);
//							var10000 = var10000.map(world::getPlayerByUUID);
//							CreateAdvancement var10001 = AllAdvancements.TRAIN_ROADKILL;
//							Objects.requireNonNull(var10001);
//							var10000.ifPresent(var10001::awardTo);
//						}
//					}
//
//					Vec3 added = entityMotion.add(contraptionMotion.multiply((double)1.0F, (double)0.0F, (double)1.0F).normalize().add((double)0.0F, (double)0.25F, (double)0.0F).scale(damage * (double)4.0F)).add(diffMotion);
//					return VecHelper.clamp(added, 3.0F);
//				} else {
//					return entityMotion;
//				}
//			}
//		}
//	}
//
//	static boolean bounceEntity(Entity entity, Vec3 normal, AbstractContraptionEntity contraption, double factor) {
//		if (factor == (double)0.0F) {
//			return false;
//		} else if (entity.isSuppressingBounce()) {
//			return false;
//		} else {
//			Vec3 contactPointMotion = contraption.getContactPointMotion(entity.position());
//			Vec3 motion = entity.getDeltaMovement().subtract(contactPointMotion);
//			Vec3 deltav = normal.scale(factor * (double)2.0F * motion.dot(normal));
//			if (deltav.dot(deltav) < (double)0.1F) {
//				return false;
//			} else {
//				entity.setDeltaMovement(entity.getDeltaMovement().subtract(deltav));
//				return true;
//			}
//		}
//	}
//
//	public static Vec3 getWorldToLocalTranslation(Entity entity, Vec3 anchorVec, Matrix3d rotationMatrix, float yawOffset) {
//		Vec3 entityPosition = entity.position();
//		Vec3 centerY = new Vec3((double)0.0F, entity.getBoundingBox().getYsize() / (double)2.0F, (double)0.0F);
//		Vec3 position = entityPosition.add(centerY);
//		position = worldToLocalPos(position, anchorVec, rotationMatrix, yawOffset);
//		position = position.subtract(centerY);
//		position = position.subtract(entityPosition);
//		return position;
//	}
//
//	public static Vec3 worldToLocalPos(Vec3 worldPos, AbstractContraptionEntity contraptionEntity) {
//		return worldToLocalPos(worldPos, contraptionEntity.getAnchorVec(), contraptionEntity.getRotationState());
//	}
//
//	public static Vec3 worldToLocalPos(Vec3 worldPos, Vec3 anchorVec, AbstractContraptionEntity.ContraptionRotationState rotation) {
//		return worldToLocalPos(worldPos, anchorVec, rotation.asMatrix(), rotation.getYawOffset());
//	}
//
//	public static Vec3 worldToLocalPos(Vec3 worldPos, Vec3 anchorVec, Matrix3d rotationMatrix, float yawOffset) {
//		Vec3 localPos = worldPos.subtract(anchorVec);
//		localPos = localPos.subtract(VecHelper.CENTER_OF_ORIGIN);
//		localPos = VecHelper.rotate(localPos, (double)(-yawOffset), Axis.Y);
//		localPos = rotationMatrix.transform(localPos);
//		localPos = localPos.add(VecHelper.CENTER_OF_ORIGIN);
//		return localPos;
//	}
//
//	static Vec3 collide(Vec3 p_20273_, Entity e) {
//		AABB aabb = e.getBoundingBox();
//		List<VoxelShape> list = e.level().getEntityCollisions(e, aabb.expandTowards(p_20273_));
//		Vec3 vec3 = p_20273_.lengthSqr() == (double)0.0F ? p_20273_ : Entity.collideBoundingBox(e, p_20273_, aabb, e.level(), list);
//		boolean flag = p_20273_.x != vec3.x;
//		boolean flag1 = p_20273_.y != vec3.y;
//		boolean flag2 = p_20273_.z != vec3.z;
//		boolean flag3 = flag1 && p_20273_.y < (double)0.0F;
//		if (e.maxUpStep() > 0.0F && flag3 && (flag || flag2)) {
//			Vec3 vec31 = Entity.collideBoundingBox(e, new Vec3(p_20273_.x, (double)e.maxUpStep(), p_20273_.z), aabb, e.level(), list);
//			Vec3 vec32 = Entity.collideBoundingBox(e, new Vec3((double)0.0F, (double)e.maxUpStep(), (double)0.0F), aabb.expandTowards(p_20273_.x, (double)0.0F, p_20273_.z), e.level(), list);
//			if (vec32.y < (double)e.maxUpStep()) {
//				Vec3 vec33 = Entity.collideBoundingBox(e, new Vec3(p_20273_.x, (double)0.0F, p_20273_.z), aabb.move(vec32), e.level(), list).add(vec32);
//				if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
//					vec31 = vec33;
//				}
//			}
//
//			if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
//				return vec31.add(Entity.collideBoundingBox(e, new Vec3((double)0.0F, -vec31.y + p_20273_.y, (double)0.0F), aabb.move(vec31), e.level(), list));
//			}
//		}
//
//		return vec3;
//	}
//
//	private static PlayerType getPlayerType(Entity entity) {
//		if (!(entity instanceof Player)) {
//			return ContraptionCollider.PlayerType.NONE;
//		} else if (!entity.level().isClientSide) {
//			return ContraptionCollider.PlayerType.SERVER;
//		} else {
//			MutableBoolean isClient = new MutableBoolean(false);
//			CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> isClient.setValue(isClientPlayerEntity(entity)));
//			return isClient.booleanValue() ? ContraptionCollider.PlayerType.CLIENT : ContraptionCollider.PlayerType.REMOTE;
//		}
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	private static boolean isClientPlayerEntity(Entity entity) {
//		return entity instanceof LocalPlayer;
//	}
//
//	private static void getPotentiallyCollidedShapes(Level world, Contraption contraption, AABB localBB, Shapes.DoubleLineConsumer out) {
//		double height = localBB.getYsize();
//		double width = localBB.getXsize();
//		double horizontalFactor = height > width && width != (double)0.0F ? height / width : (double)1.0F;
//		double verticalFactor = width > height && height != (double)0.0F ? width / height : (double)1.0F;
//		AABB blockScanBB = localBB.inflate((double)0.5F);
//		blockScanBB = blockScanBB.inflate(horizontalFactor, verticalFactor, horizontalFactor);
//		BlockPos min = BlockPos.containing(blockScanBB.minX, blockScanBB.minY, blockScanBB.minZ);
//		BlockPos max = BlockPos.containing(blockScanBB.maxX, blockScanBB.maxY, blockScanBB.maxZ);
//
//		for(BlockPos p : BlockPos.betweenClosed(min, max)) {
//			if (contraption.blocks.containsKey(p) && !contraption.isHiddenInPortal(p)) {
//				StructureTemplate.StructureBlockInfo info = (StructureTemplate.StructureBlockInfo)contraption.getBlocks().get(p);
//				BlockState blockState = info.state();
//				BlockPos pos = info.pos();
//				VoxelShape collisionShape = blockState.getCollisionShape(world, p).move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
//				if (!collisionShape.isEmpty()) {
//					collisionShape.forAllBoxes(out);
//				}
//			}
//		}
//
//	}
//
//	public static boolean collideBlocks(AbstractContraptionEntity contraptionEntity) {
//		if (!contraptionEntity.supportsTerrainCollision()) {
//			return false;
//		} else {
//			Level world = contraptionEntity.getCommandSenderWorld();
//			Vec3 motion = contraptionEntity.getDeltaMovement();
//			TranslatingContraption contraption = (TranslatingContraption)contraptionEntity.getContraption();
//			AABB bounds = contraptionEntity.getBoundingBox();
//			Vec3 position = contraptionEntity.position();
//			BlockPos gridPos = BlockPos.containing(position);
//			if (contraption == null) {
//				return false;
//			} else if (bounds == null) {
//				return false;
//			} else if (motion.equals(Vec3.ZERO)) {
//				return false;
//			} else {
//				Direction movementDirection = Direction.getNearest(motion.x, motion.y, motion.z);
//				if (movementDirection.getAxisDirection() == AxisDirection.POSITIVE) {
//					gridPos = gridPos.relative(movementDirection);
//				}
//
//				if (isCollidingWithWorld(world, contraption, gridPos, movementDirection)) {
//					return true;
//				} else {
//					for(ControlledContraptionEntity otherContraptionEntity : world.getEntitiesOfClass(ControlledContraptionEntity.class, bounds.inflate((double)1.0F), (e) -> !e.equals(contraptionEntity))) {
//						if (otherContraptionEntity.supportsTerrainCollision()) {
//							Vec3 otherMotion = otherContraptionEntity.getDeltaMovement();
//							TranslatingContraption otherContraption = (TranslatingContraption)otherContraptionEntity.getContraption();
//							AABB otherBounds = otherContraptionEntity.getBoundingBox();
//							Vec3 otherPosition = otherContraptionEntity.position();
//							if (otherContraption == null) {
//								return false;
//							}
//
//							if (otherBounds == null) {
//								return false;
//							}
//
//							if (bounds.move(motion).intersects(otherBounds.move(otherMotion))) {
//								for(BlockPos colliderPos : contraption.getOrCreateColliders(world, movementDirection)) {
//									colliderPos = colliderPos.offset(gridPos).subtract(BlockPos.containing(otherPosition));
//									if (otherContraption.getBlocks().containsKey(colliderPos)) {
//										return true;
//									}
//								}
//							}
//						}
//					}
//
//					return false;
//				}
//			}
//		}
//	}
//
//	public static boolean isCollidingWithWorld(Level world, TranslatingContraption contraption, BlockPos anchor, Direction movementDirection) {
//		for(BlockPos pos : contraption.getOrCreateColliders(world, movementDirection)) {
//			BlockPos colliderPos = pos.offset(anchor);
//			if (!world.isLoaded(colliderPos)) {
//				return true;
//			}
//
//			BlockState collidedState = world.getBlockState(colliderPos);
//			StructureTemplate.StructureBlockInfo blockInfo = (StructureTemplate.StructureBlockInfo)contraption.getBlocks().get(pos);
//			boolean emptyCollider = collidedState.getCollisionShape(world, pos).isEmpty();
//			if (!(collidedState.getBlock() instanceof CocoaBlock)) {
//				MovementBehaviour movementBehaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(blockInfo.state());
//				if (movementBehaviour != null) {
//					if (movementBehaviour instanceof BlockBreakingMovementBehaviour) {
//						BlockBreakingMovementBehaviour behaviour = (BlockBreakingMovementBehaviour)movementBehaviour;
//						if (!behaviour.canBreak(world, colliderPos, collidedState) && !emptyCollider) {
//							return true;
//						}
//						continue;
//					}
//
//					if (movementBehaviour instanceof HarvesterMovementBehaviour) {
//						HarvesterMovementBehaviour harvesterMovementBehaviour = (HarvesterMovementBehaviour)movementBehaviour;
//						if (!harvesterMovementBehaviour.isValidCrop(world, colliderPos, collidedState) && !harvesterMovementBehaviour.isValidOther(world, colliderPos, collidedState) && !emptyCollider) {
//							return true;
//						}
//						continue;
//					}
//				}
//
//				if ((!AllBlocks.PULLEY_MAGNET.has(collidedState) || !pos.equals(BlockPos.ZERO) || movementDirection != Direction.UP) && !collidedState.canBeReplaced() && !emptyCollider) {
//					return true;
//				}
//			}
//		}
//
//		return false;
//	}
//
//	static enum PlayerType {
//		NONE,
//		CLIENT,
//		REMOTE,
//		SERVER;
//
//		private PlayerType() {
//		}
//	}
//}
