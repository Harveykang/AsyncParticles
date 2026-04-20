////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by Fernflower decompiler)
////
//
//package com.simibubi.create.content.contraptions;
//
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.Multimap;
//import com.simibubi.create.AllBlockEntityTypes;
//import com.simibubi.create.AllBlocks;
//import com.simibubi.create.AllTags.AllBlockTags;
//import com.simibubi.create.AllTags.AllContraptionTypeTags;
//import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
//import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
//import com.simibubi.create.api.contraption.BlockMovementChecks;
//import com.simibubi.create.api.contraption.ContraptionType;
//import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
//import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
//import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
//import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
//import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
//import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
//import com.simibubi.create.content.contraptions.bearing.StabilizedContraption;
//import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlock;
//import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
//import com.simibubi.create.content.contraptions.behaviour.MovementContext;
//import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
//import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
//import com.simibubi.create.content.contraptions.chassis.StickerBlock;
//import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
//import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
//import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
//import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
//import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
//import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
//import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
//import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
//import com.simibubi.create.content.contraptions.render.ClientContraption;
//import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
//import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
//import com.simibubi.create.content.kinetics.base.IRotate;
//import com.simibubi.create.content.kinetics.belt.BeltBlock;
//import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
//import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
//import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
//import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
//import com.simibubi.create.content.logistics.crate.CreativeCrateBlockEntity;
//import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
//import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
//import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
//import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
//import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
//import com.simibubi.create.foundation.collision.CollisionList;
//import com.simibubi.create.foundation.utility.BlockHelper;
//import com.simibubi.create.infrastructure.config.AllConfigs;
//import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
//import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Queue;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.BiConsumer;
//import net.createmod.catnip.data.Iterate;
//import net.createmod.catnip.data.UniqueLinkedList;
//import net.createmod.catnip.math.BBHelper;
//import net.createmod.catnip.math.BlockFace;
//import net.createmod.catnip.nbt.NBTHelper;
//import net.createmod.catnip.nbt.NBTProcessors;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.HolderGetter;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.core.Vec3i;
//import net.minecraft.core.Direction.Axis;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.ListTag;
//import net.minecraft.nbt.NbtUtils;
//import net.minecraft.nbt.Tag;
//import net.minecraft.network.protocol.game.DebugPackets;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.ai.village.poi.PoiTypes;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.LevelAccessor;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraft.world.level.block.ButtonBlock;
//import net.minecraft.world.level.block.ChestBlock;
//import net.minecraft.world.level.block.DoorBlock;
//import net.minecraft.world.level.block.PressurePlateBlock;
//import net.minecraft.world.level.block.Rotation;
//import net.minecraft.world.level.block.SimpleWaterloggedBlock;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.block.state.properties.ChestType;
//import net.minecraft.world.level.block.state.properties.PistonType;
//import net.minecraft.world.level.chunk.HashMapPalette;
//import net.minecraft.world.level.levelgen.structure.BoundingBox;
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//import net.minecraft.world.level.material.FluidState;
//import net.minecraft.world.level.material.Fluids;
//import net.minecraft.world.level.material.PushReaction;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.minecraft.world.phys.shapes.CollisionContext;
//import net.minecraft.world.phys.shapes.VoxelShape;
//import net.neoforged.neoforge.registries.GameData;
//import org.apache.commons.lang3.tuple.MutablePair;
//import org.apache.commons.lang3.tuple.Pair;
//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.Nullable;
//
//public abstract class Contraption {
//	public final CollisionList simplifiedEntityColliders = new CollisionList();
//	public AbstractContraptionEntity entity;
//	public AABB bounds;
//	public BlockPos anchor;
//	public boolean stalled;
//	public boolean hasUniversalCreativeCrate;
//	public boolean disassembled;
//	protected Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks = new HashMap();
//	protected Map<BlockPos, CompoundTag> updateTags = new HashMap();
//	public Object2BooleanMap<BlockPos> isLegacy = new Object2BooleanArrayMap();
//	protected List<MutablePair<StructureTemplate.StructureBlockInfo, MovementContext>> actors = new ArrayList();
//	protected Map<BlockPos, MovingInteractionBehaviour> interactors = new HashMap();
//	protected List<ItemStack> disabledActors = new ArrayList();
//	protected List<AABB> superglue = new ArrayList();
//	protected List<BlockPos> seats = new ArrayList();
//	protected Map<UUID, Integer> seatMapping = new HashMap();
//	protected Map<UUID, BlockFace> stabilizedSubContraptions = new HashMap();
//	protected MountedStorageManager storage = new MountedStorageManager();
//	protected Multimap<BlockPos, StructureTemplate.StructureBlockInfo> capturedMultiblocks = ArrayListMultimap.create();
//	private Set<SuperGlueEntity> glueToRemove = new HashSet();
//	private Map<BlockPos, Entity> initialPassengers = new HashMap();
//	private List<BlockFace> pendingSubContraptions = new ArrayList();
//	private final AtomicReference<ClientContraption> clientContraption = new AtomicReference();
//	protected ContraptionWorld collisionLevel;
//
//	public Contraption() {
//	}
//
//	public ContraptionWorld getContraptionWorld() {
//		if (this.collisionLevel == null) {
//			this.collisionLevel = new ContraptionWorld(this.entity.level(), this);
//		}
//
//		return this.collisionLevel;
//	}
//
//	public abstract boolean assemble(Level var1, BlockPos var2) throws AssemblyException;
//
//	public abstract boolean canBeStabilized(Direction var1, BlockPos var2);
//
//	public abstract ContraptionType getType();
//
//	protected boolean customBlockPlacement(LevelAccessor world, BlockPos pos, BlockState state) {
//		return false;
//	}
//
//	protected boolean customBlockRemoval(LevelAccessor world, BlockPos pos, BlockState state) {
//		return false;
//	}
//
//	protected boolean addToInitialFrontier(Level world, BlockPos pos, Direction forcedDirection, Queue<BlockPos> frontier) throws AssemblyException {
//		return true;
//	}
//
//	public static Contraption fromNBT(Level world, CompoundTag nbt, boolean spawnData) {
//		String type = nbt.getString("Type");
//		Contraption contraption = ContraptionType.fromType(type);
//		contraption.readNBT(world, nbt, spawnData);
//		contraption.collisionLevel = new ContraptionWorld(world, contraption);
//		contraption.invalidateColliders();
//		return contraption;
//	}
//
//	public boolean searchMovedStructure(Level world, BlockPos pos, @Nullable Direction forcedDirection) throws AssemblyException {
//		this.initialPassengers.clear();
//		Queue<BlockPos> frontier = new UniqueLinkedList();
//		Set<BlockPos> visited = new HashSet();
//		this.anchor = pos;
//		if (this.bounds == null) {
//			this.bounds = new AABB(BlockPos.ZERO);
//		}
//
//		if (!BlockMovementChecks.isBrittle(world.getBlockState(pos))) {
//			frontier.add(pos);
//		}
//
//		if (!this.addToInitialFrontier(world, pos, forcedDirection, frontier)) {
//			return false;
//		} else {
//			for(int limit = 100000; limit > 0; --limit) {
//				if (frontier.isEmpty()) {
//					return true;
//				}
//
//				if (!this.moveBlock(world, forcedDirection, frontier, visited)) {
//					return false;
//				}
//			}
//
//			throw AssemblyException.structureTooLarge();
//		}
//	}
//
//	public void onEntityCreated(AbstractContraptionEntity entity) {
//		this.entity = entity;
//
//		for(BlockFace blockFace : this.pendingSubContraptions) {
//			Direction face = blockFace.getFace();
//			StabilizedContraption subContraption = new StabilizedContraption(face);
//			Level world = entity.level();
//			BlockPos pos = blockFace.getPos();
//
//			try {
//				if (!subContraption.assemble(world, pos)) {
//					continue;
//				}
//			} catch (AssemblyException var10) {
//				continue;
//			}
//
//			subContraption.removeBlocksFromWorld(world, BlockPos.ZERO);
//			OrientedContraptionEntity movedContraption = OrientedContraptionEntity.create(world, subContraption, face);
//			BlockPos anchor = blockFace.getConnectedPos();
//			movedContraption.setPos((double)((float)anchor.getX() + 0.5F), (double)anchor.getY(), (double)((float)anchor.getZ() + 0.5F));
//			world.addFreshEntity(movedContraption);
//			this.stabilizedSubContraptions.put(movedContraption.getUUID(), new BlockFace(this.toLocalPos(pos), face));
//		}
//
//		this.storage.initialize();
//		this.invalidateColliders();
//	}
//
//	public void onEntityInitialize(Level world, AbstractContraptionEntity contraptionEntity) {
//		if (!world.isClientSide) {
//			for(OrientedContraptionEntity orientedCE : world.getEntitiesOfClass(OrientedContraptionEntity.class, contraptionEntity.getBoundingBox().inflate((double)1.0F))) {
//				if (this.stabilizedSubContraptions.containsKey(orientedCE.getUUID())) {
//					orientedCE.startRiding(contraptionEntity);
//				}
//			}
//
//			for(BlockPos seatPos : this.getSeats()) {
//				Entity passenger = (Entity)this.initialPassengers.get(seatPos);
//				if (passenger != null) {
//					int seatIndex = this.getSeats().indexOf(seatPos);
//					if (seatIndex != -1) {
//						contraptionEntity.addSittingPassenger(passenger, seatIndex);
//					}
//				}
//			}
//
//		}
//	}
//
//	protected boolean moveBlock(Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited) throws AssemblyException {
//		BlockPos pos = (BlockPos)frontier.poll();
//		if (pos == null) {
//			return false;
//		} else {
//			visited.add(pos);
//			if (world.isOutsideBuildHeight(pos)) {
//				return true;
//			} else if (!world.isLoaded(pos)) {
//				throw AssemblyException.unloadedChunk(pos);
//			} else if (this.isAnchoringBlockAt(pos)) {
//				return true;
//			} else {
//				BlockState state = world.getBlockState(pos);
//				if (!BlockMovementChecks.isMovementNecessary(state, world, pos)) {
//					return true;
//				} else if (!this.movementAllowed(state, world, pos)) {
//					throw AssemblyException.unmovableBlock(pos, state);
//				} else if (state.getBlock() instanceof AbstractChassisBlock && !this.moveChassis(world, pos, forcedDirection, frontier, visited)) {
//					return false;
//				} else {
//					if (AllBlocks.BELT.has(state)) {
//						this.moveBelt(pos, frontier, visited, state);
//					}
//
//					if (AllBlocks.WINDMILL_BEARING.has(state)) {
//						BlockEntity var8 = world.getBlockEntity(pos);
//						if (var8 instanceof WindmillBearingBlockEntity) {
//							WindmillBearingBlockEntity wbbe = (WindmillBearingBlockEntity)var8;
//							wbbe.disassembleForMovement();
//						}
//					}
//
//					if (AllBlocks.GANTRY_CARRIAGE.has(state)) {
//						this.moveGantryPinion(world, pos, frontier, visited, state);
//					}
//
//					if (AllBlocks.GANTRY_SHAFT.has(state)) {
//						this.moveGantryShaft(world, pos, frontier, visited, state);
//					}
//
//					if (AllBlocks.STICKER.has(state) && (Boolean)state.getValue(StickerBlock.EXTENDED)) {
//						Direction offset = (Direction)state.getValue(StickerBlock.FACING);
//						BlockPos attached = pos.relative(offset);
//						if (!visited.contains(attached) && !BlockMovementChecks.isNotSupportive(world.getBlockState(attached), offset.getOpposite())) {
//							frontier.add(attached);
//						}
//					}
//
//					BlockEntity stateBelow = world.getBlockEntity(pos);
//					if (stateBelow instanceof ChainConveyorBlockEntity) {
//						ChainConveyorBlockEntity ccbe = (ChainConveyorBlockEntity)stateBelow;
//						ccbe.notifyConnectedToValidate();
//					}
//
//					if (state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
//						Direction offset = ChestBlock.getConnectedDirection(state);
//						BlockPos attached = pos.relative(offset);
//						if (!visited.contains(attached)) {
//							frontier.add(attached);
//						}
//					}
//
//					Block var28 = state.getBlock();
//					if (var28 instanceof AbstractBogeyBlock) {
//						AbstractBogeyBlock<?> bogey = (AbstractBogeyBlock)var28;
//
//						for(Direction d : bogey.getStickySurfaces(world, pos, state)) {
//							if (!visited.contains(pos.relative(d))) {
//								frontier.add(pos.relative(d));
//							}
//						}
//					}
//
//					if (AllBlocks.MECHANICAL_BEARING.has(state)) {
//						this.moveBearing(pos, frontier, visited, state);
//					}
//
//					if (AllBlocks.WINDMILL_BEARING.has(state)) {
//						this.moveWindmillBearing(pos, frontier, visited, state);
//					}
//
//					if (AllBlockTags.SEATS.matches(state)) {
//						this.moveSeat(world, pos);
//					}
//
//					if (state.getBlock() instanceof PulleyBlock) {
//						this.movePulley(world, pos, frontier, visited);
//					}
//
//					if (state.getBlock() instanceof MechanicalPistonBlock && !this.moveMechanicalPiston(world, pos, frontier, visited, state)) {
//						return false;
//					} else {
//						if (MechanicalPistonBlock.isExtensionPole(state)) {
//							this.movePistonPole(world, pos, frontier, visited, state);
//						}
//
//						if (MechanicalPistonBlock.isPistonHead(state)) {
//							this.movePistonHead(world, pos, frontier, visited, state);
//						}
//
//						BlockPos posDown = pos.below();
//						BlockState stateBelow = world.getBlockState(posDown);
//						if (!visited.contains(posDown) && AllBlocks.CART_ASSEMBLER.has(stateBelow)) {
//							frontier.add(posDown);
//						}
//
//						for(Direction offset : Iterate.directions) {
//							BlockPos offsetPos = pos.relative(offset);
//							BlockState blockState = world.getBlockState(offsetPos);
//							if (!this.isAnchoringBlockAt(offsetPos)) {
//								if (!this.movementAllowed(blockState, world, offsetPos)) {
//									if (offset == forcedDirection) {
//										throw AssemblyException.unmovableBlock(pos, state);
//									}
//								} else {
//									boolean wasVisited = visited.contains(offsetPos);
//									boolean faceHasGlue = SuperGlueEntity.isGlued(world, pos, offset, this.glueToRemove);
//									boolean blockAttachedTowardsFace = BlockMovementChecks.isBlockAttachedTowards(blockState, world, offsetPos, offset.getOpposite());
//									boolean brittle = BlockMovementChecks.isBrittle(blockState);
//									boolean canStick = !brittle && state.canStickTo(blockState) && blockState.canStickTo(state);
//									if (canStick) {
//										if (state.getPistonPushReaction() == PushReaction.PUSH_ONLY || blockState.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
//											canStick = false;
//										}
//
//										if (BlockMovementChecks.isNotSupportive(state, offset)) {
//											canStick = false;
//										}
//
//										if (BlockMovementChecks.isNotSupportive(blockState, offset.getOpposite())) {
//											canStick = false;
//										}
//									}
//
//									if (!wasVisited && (canStick || blockAttachedTowardsFace || faceHasGlue || offset == forcedDirection && !BlockMovementChecks.isNotSupportive(state, forcedDirection))) {
//										frontier.add(offsetPos);
//									}
//								}
//							}
//						}
//
//						this.addBlock(world, pos, this.capture(world, pos));
//						if (this.blocks.size() <= (Integer)AllConfigs.server().kinetics.maxBlocksMoved.get()) {
//							return true;
//						} else {
//							throw AssemblyException.structureTooLarge();
//						}
//					}
//				}
//			}
//		}
//	}
//
//	protected void movePistonHead(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		Direction direction = (Direction)state.getValue(MechanicalPistonHeadBlock.FACING);
//		BlockPos offset = pos.relative(direction.getOpposite());
//		if (!visited.contains(offset)) {
//			BlockState blockState = world.getBlockState(offset);
//			if (MechanicalPistonBlock.isExtensionPole(blockState) && ((Direction)blockState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
//				frontier.add(offset);
//			}
//
//			if (blockState.getBlock() instanceof MechanicalPistonBlock) {
//				Direction pistonFacing = (Direction)blockState.getValue(MechanicalPistonBlock.FACING);
//				if (pistonFacing == direction && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
//					frontier.add(offset);
//				}
//			}
//		}
//
//		if (state.getValue(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
//			BlockPos attached = pos.relative(direction);
//			if (!visited.contains(attached)) {
//				frontier.add(attached);
//			}
//		}
//
//	}
//
//	protected void movePistonPole(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		for(Direction d : Iterate.directionsInAxis(((Direction)state.getValue(PistonExtensionPoleBlock.FACING)).getAxis())) {
//			BlockPos offset = pos.relative(d);
//			if (!visited.contains(offset)) {
//				BlockState blockState = world.getBlockState(offset);
//				if (MechanicalPistonBlock.isExtensionPole(blockState) && ((Direction)blockState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == d.getAxis()) {
//					frontier.add(offset);
//				}
//
//				if (MechanicalPistonBlock.isPistonHead(blockState) && ((Direction)blockState.getValue(MechanicalPistonHeadBlock.FACING)).getAxis() == d.getAxis()) {
//					frontier.add(offset);
//				}
//
//				if (blockState.getBlock() instanceof MechanicalPistonBlock) {
//					Direction pistonFacing = (Direction)blockState.getValue(MechanicalPistonBlock.FACING);
//					if (pistonFacing == d || pistonFacing == d.getOpposite() && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
//						frontier.add(offset);
//					}
//				}
//			}
//		}
//
//	}
//
//	protected void moveGantryPinion(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		BlockPos offset = pos.relative((Direction)state.getValue(GantryCarriageBlock.FACING));
//		if (!visited.contains(offset)) {
//			frontier.add(offset);
//		}
//
//		Direction.Axis rotationAxis = ((IRotate)state.getBlock()).getRotationAxis(state);
//
//		for(Direction d : Iterate.directionsInAxis(rotationAxis)) {
//			offset = pos.relative(d);
//			BlockState offsetState = world.getBlockState(offset);
//			if (AllBlocks.GANTRY_SHAFT.has(offsetState) && ((Direction)offsetState.getValue(GantryShaftBlock.FACING)).getAxis() == d.getAxis() && !visited.contains(offset)) {
//				frontier.add(offset);
//			}
//		}
//
//	}
//
//	protected void moveGantryShaft(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		for(Direction d : Iterate.directions) {
//			BlockPos offset = pos.relative(d);
//			if (!visited.contains(offset)) {
//				BlockState offsetState = world.getBlockState(offset);
//				Direction facing = (Direction)state.getValue(GantryShaftBlock.FACING);
//				if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.getValue(GantryShaftBlock.FACING) == facing) {
//					frontier.add(offset);
//				} else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState) && offsetState.getValue(GantryCarriageBlock.FACING) == d) {
//					frontier.add(offset);
//				}
//			}
//		}
//
//	}
//
//	private void moveWindmillBearing(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		Direction facing = (Direction)state.getValue(WindmillBearingBlock.FACING);
//		BlockPos offset = pos.relative(facing);
//		if (!visited.contains(offset)) {
//			frontier.add(offset);
//		}
//
//	}
//
//	private void moveBearing(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		Direction facing = (Direction)state.getValue(MechanicalBearingBlock.FACING);
//		if (!this.canBeStabilized(facing, pos.subtract(this.anchor))) {
//			BlockPos offset = pos.relative(facing);
//			if (!visited.contains(offset)) {
//				frontier.add(offset);
//			}
//
//		} else {
//			this.pendingSubContraptions.add(new BlockFace(pos, facing));
//		}
//	}
//
//	private void moveBelt(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
//		BlockPos nextPos = BeltBlock.nextSegmentPosition(state, pos, true);
//		BlockPos prevPos = BeltBlock.nextSegmentPosition(state, pos, false);
//		if (nextPos != null && !visited.contains(nextPos)) {
//			frontier.add(nextPos);
//		}
//
//		if (prevPos != null && !visited.contains(prevPos)) {
//			frontier.add(prevPos);
//		}
//
//	}
//
//	private void moveSeat(Level world, BlockPos pos) {
//		BlockPos local = this.toLocalPos(pos);
//		this.getSeats().add(local);
//		List<SeatEntity> seatsEntities = world.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
//		if (!seatsEntities.isEmpty()) {
//			SeatEntity seat = (SeatEntity)seatsEntities.get(0);
//			List<Entity> passengers = seat.getPassengers();
//			if (!passengers.isEmpty()) {
//				this.initialPassengers.put(local, (Entity)passengers.get(0));
//			}
//		}
//
//	}
//
//	private void movePulley(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited) {
//		int limit = (Integer)AllConfigs.server().kinetics.maxRopeLength.get();
//		BlockPos ropePos = pos;
//
//		while(limit-- >= 0) {
//			ropePos = ropePos.below();
//			if (!world.isLoaded(ropePos)) {
//				break;
//			}
//
//			BlockState ropeState = world.getBlockState(ropePos);
//			Block block = ropeState.getBlock();
//			if (!(block instanceof PulleyBlock.RopeBlock) && !(block instanceof PulleyBlock.MagnetBlock)) {
//				if (!visited.contains(ropePos)) {
//					frontier.add(ropePos);
//				}
//				break;
//			}
//
//			this.addBlock(world, ropePos, this.capture(world, ropePos));
//		}
//
//	}
//
//	private boolean moveMechanicalPiston(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) throws AssemblyException {
//		Direction direction = (Direction)state.getValue(MechanicalPistonBlock.FACING);
//		MechanicalPistonBlock.PistonState pistonState = (MechanicalPistonBlock.PistonState)state.getValue(MechanicalPistonBlock.STATE);
//		if (pistonState == PistonState.MOVING) {
//			return false;
//		} else {
//			BlockPos offset = pos.relative(direction.getOpposite());
//			if (!visited.contains(offset)) {
//				BlockState poleState = world.getBlockState(offset);
//				if (AllBlocks.PISTON_EXTENSION_POLE.has(poleState) && ((Direction)poleState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
//					frontier.add(offset);
//				}
//			}
//
//			if (pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
//				offset = pos.relative(direction);
//				if (!visited.contains(offset)) {
//					frontier.add(offset);
//				}
//			}
//
//			return true;
//		}
//	}
//
//	private boolean moveChassis(Level world, BlockPos pos, Direction movementDirection, Queue<BlockPos> frontier, Set<BlockPos> visited) {
//		BlockEntity be = world.getBlockEntity(pos);
//		if (be instanceof ChassisBlockEntity chassis) {
//			chassis.addAttachedChasses(frontier, visited);
//			List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
//			if (includedBlockPositions == null) {
//				return false;
//			} else {
//				for(BlockPos blockPos : includedBlockPositions) {
//					if (!visited.contains(blockPos)) {
//						frontier.add(blockPos);
//					}
//				}
//
//				return true;
//			}
//		} else {
//			return false;
//		}
//	}
//
//	protected Pair<StructureTemplate.StructureBlockInfo, BlockEntity> capture(Level world, BlockPos pos) {
//		BlockState blockstate = world.getBlockState(pos);
//		if (AllBlocks.REDSTONE_CONTACT.has(blockstate)) {
//			blockstate = (BlockState)blockstate.setValue(RedstoneContactBlock.POWERED, true);
//		}
//
//		if (AllBlocks.POWERED_SHAFT.has(blockstate)) {
//			blockstate = BlockHelper.copyProperties(blockstate, AllBlocks.SHAFT.getDefaultState());
//		}
//
//		if (blockstate.getBlock() instanceof ControlsBlock && AllContraptionTypeTags.OPENS_CONTROLS.matches(this.getType())) {
//			blockstate = (BlockState)blockstate.setValue(ControlsBlock.OPEN, true);
//		}
//
//		if (blockstate.hasProperty(SlidingDoorBlock.VISIBLE)) {
//			blockstate = (BlockState)blockstate.setValue(SlidingDoorBlock.VISIBLE, false);
//		}
//
//		if (blockstate.getBlock() instanceof ButtonBlock) {
//			blockstate = (BlockState)blockstate.setValue(ButtonBlock.POWERED, false);
//			world.scheduleTick(pos, blockstate.getBlock(), -1);
//		}
//
//		if (blockstate.getBlock() instanceof PressurePlateBlock) {
//			blockstate = (BlockState)blockstate.setValue(PressurePlateBlock.POWERED, false);
//			world.scheduleTick(pos, blockstate.getBlock(), -1);
//		}
//
//		CompoundTag compoundnbt = this.getBlockEntityNBT(world, pos);
//		BlockEntity blockEntity = world.getBlockEntity(pos);
//		if (blockEntity instanceof PoweredShaftBlockEntity) {
//			blockEntity = AllBlockEntityTypes.BRACKETED_KINETIC.create(pos, blockstate);
//		}
//
//		if (blockEntity instanceof FactoryPanelBlockEntity fpbe) {
//			fpbe.writeSafe(compoundnbt, world.registryAccess());
//		}
//
//		return Pair.of(new StructureTemplate.StructureBlockInfo(pos, blockstate, compoundnbt), blockEntity);
//	}
//
//	protected void addBlock(Level level, BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> pair) {
//		StructureTemplate.StructureBlockInfo captured = (StructureTemplate.StructureBlockInfo)pair.getKey();
//		BlockPos localPos = pos.subtract(this.anchor);
//		BlockState state = captured.state();
//		StructureTemplate.StructureBlockInfo structureBlockInfo = new StructureTemplate.StructureBlockInfo(localPos, state, captured.nbt());
//		if (this.blocks.put(localPos, structureBlockInfo) == null) {
//			this.bounds = this.bounds.minmax(new AABB(localPos));
//			BlockEntity be = (BlockEntity)pair.getValue();
//			if (be != null) {
//				CompoundTag updateTag = be.getUpdateTag(level.registryAccess());
//				this.updateTags.put(localPos, updateTag);
//			}
//
//			this.storage.addBlock(level, state, pos, localPos, be);
//			this.captureMultiblock(localPos, structureBlockInfo, be);
//			if (MovementBehaviour.REGISTRY.get(state) != null) {
//				this.actors.add(MutablePair.of(structureBlockInfo, (Object)null));
//			}
//
//			MovingInteractionBehaviour interactionBehaviour = (MovingInteractionBehaviour)MovingInteractionBehaviour.REGISTRY.get(state);
//			if (interactionBehaviour != null) {
//				this.interactors.put(localPos, interactionBehaviour);
//			}
//
//			if (be instanceof CreativeCrateBlockEntity && ((FilteringBehaviour)((CreativeCrateBlockEntity)be).getBehaviour(FilteringBehaviour.TYPE)).getFilter().isEmpty()) {
//				this.hasUniversalCreativeCrate = true;
//			}
//
//		}
//	}
//
//	protected void captureMultiblock(BlockPos localPos, StructureTemplate.StructureBlockInfo structureBlockInfo, BlockEntity be) {
//		if (be instanceof IMultiBlockEntityContainer multiBlockBE) {
//			CompoundTag nbt = structureBlockInfo.nbt();
//			BlockPos controllerPos = localPos;
//			if (nbt.contains("Controller")) {
//				controllerPos = this.toLocalPos(NBTHelper.readBlockPos(nbt, "Controller"));
//			}
//
//			nbt.put("Controller", NbtUtils.writeBlockPos(controllerPos));
//			if (this.updateTags.containsKey(localPos)) {
//				((CompoundTag)this.updateTags.get(localPos)).put("Controller", NbtUtils.writeBlockPos(controllerPos));
//			}
//
//			if (multiBlockBE.isController() && multiBlockBE.getHeight() <= 1 && multiBlockBE.getWidth() <= 1) {
//				nbt.put("LastKnownPos", NbtUtils.writeBlockPos(BlockPos.ZERO.below(2147483646)));
//			} else {
//				nbt.remove("LastKnownPos");
//				this.capturedMultiblocks.put(controllerPos, structureBlockInfo);
//			}
//		}
//	}
//
//	protected @Nullable CompoundTag getBlockEntityNBT(Level world, BlockPos pos) {
//		BlockEntity blockEntity = world.getBlockEntity(pos);
//		if (blockEntity == null) {
//			return null;
//		} else {
//			CompoundTag nbt = blockEntity.saveWithFullMetadata(world.registryAccess());
//			nbt.remove("x");
//			nbt.remove("y");
//			nbt.remove("z");
//			return nbt;
//		}
//	}
//
//	protected BlockPos toLocalPos(BlockPos globalPos) {
//		return globalPos.subtract(this.anchor);
//	}
//
//	protected boolean movementAllowed(BlockState state, Level world, BlockPos pos) {
//		return BlockMovementChecks.isMovementAllowed(state, world, pos);
//	}
//
//	protected boolean isAnchoringBlockAt(BlockPos pos) {
//		return pos.equals(this.anchor);
//	}
//
//	public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
//		Tag blocks = nbt.get("Blocks");
//		boolean usePalettedDeserialization = blocks != null && blocks.getId() == 10 && ((CompoundTag)blocks).contains("Palette");
//		this.readBlocksCompound(blocks, world, usePalettedDeserialization);
//		this.capturedMultiblocks.clear();
//		nbt.getList("CapturedMultiblocks", 10).forEach((c) -> {
//			CompoundTag tag = (CompoundTag)c;
//			if (tag.contains("Controller", 10) || tag.contains("Parts", 9)) {
//				BlockPos controllerPos = NBTHelper.readBlockPos(tag, "Controller");
//				tag.getList("Parts", 10).forEach((part) -> {
//					CompoundTag cPart = (CompoundTag)part;
//					BlockPos partPos = cPart.contains("Pos") ? NBTHelper.readBlockPos(cPart, "Pos") : new BlockPos(cPart.getInt("X"), cPart.getInt("Y"), cPart.getInt("Z"));
//					StructureTemplate.StructureBlockInfo partInfo = (StructureTemplate.StructureBlockInfo)this.blocks.get(partPos);
//					this.capturedMultiblocks.put(controllerPos, partInfo);
//				});
//			}
//		});
//		this.storage.read(nbt, world.registryAccess(), spawnData, this);
//		this.actors.clear();
//		nbt.getList("Actors", 10).forEach((c) -> {
//			CompoundTag comp = (CompoundTag)c;
//			StructureTemplate.StructureBlockInfo info = (StructureTemplate.StructureBlockInfo)this.blocks.get(NBTHelper.readBlockPos(comp, "Pos"));
//			if (info != null) {
//				MovementContext context = MovementContext.readNBT(world, info, comp, this);
//				this.getActors().add(MutablePair.of(info, context));
//			}
//		});
//		this.disabledActors = NBTHelper.readItemList(nbt.getList("DisabledActors", 10), world.registryAccess());
//
//		for(ItemStack stack : this.disabledActors) {
//			this.setActorsActive(stack, false);
//		}
//
//		this.superglue.clear();
//		NBTHelper.iterateCompoundList(nbt.getList("Superglue", 10), (c) -> this.superglue.add(SuperGlueEntity.readBoundingBox(c)));
//		this.seats.clear();
//		NBTHelper.iterateCompoundList(nbt.getList("Seats", 10), (c) -> this.seats.add(c.contains("Pos") ? NBTHelper.readBlockPos(c, "Pos") : new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z"))));
//		this.seatMapping.clear();
//		NBTHelper.iterateCompoundList(nbt.getList("Passengers", 10), (c) -> this.seatMapping.put(NbtUtils.loadUUID(NBTHelper.getINBT(c, "Id")), c.getInt("Seat")));
//		this.stabilizedSubContraptions.clear();
//		NBTHelper.iterateCompoundList(nbt.getList("SubContraptions", 10), (c) -> this.stabilizedSubContraptions.put(c.getUUID("Id"), BlockFace.fromNBT(c.getCompound("Location"))));
//		this.interactors.clear();
//		NBTHelper.iterateCompoundList(nbt.getList("Interactors", 10), (c) -> {
//			BlockPos pos = NBTHelper.readBlockPos(c, "Pos");
//			StructureTemplate.StructureBlockInfo structureBlockInfo = (StructureTemplate.StructureBlockInfo)this.getBlocks().get(pos);
//			if (structureBlockInfo != null) {
//				MovingInteractionBehaviour behaviour = (MovingInteractionBehaviour)MovingInteractionBehaviour.REGISTRY.get(structureBlockInfo.state());
//				if (behaviour != null) {
//					this.interactors.put(pos, behaviour);
//				}
//
//			}
//		});
//		if (nbt.contains("BoundsFront")) {
//			this.bounds = NBTHelper.readAABB(nbt.getList("BoundsFront", 5));
//		}
//
//		this.stalled = nbt.getBoolean("Stalled");
//		this.hasUniversalCreativeCrate = nbt.getBoolean("BottomlessSupply");
//		this.anchor = NBTHelper.readBlockPos(nbt, "Anchor");
//	}
//
//	public CompoundTag writeNBT(HolderLookup.Provider registries, boolean spawnPacket) {
//		CompoundTag nbt = new CompoundTag();
//		ResourceLocation typeId = this.getType().holder.key().location();
//		nbt.putString("Type", typeId.toString());
//		CompoundTag blocksNBT = this.writeBlocksCompound(spawnPacket);
//		ListTag multiblocksNBT = new ListTag();
//		this.capturedMultiblocks.keySet().forEach((controllerPos) -> {
//			CompoundTag tag = new CompoundTag();
//			tag.put("Controller", NbtUtils.writeBlockPos(controllerPos));
//			Collection<StructureTemplate.StructureBlockInfo> multiblockParts = this.capturedMultiblocks.get(controllerPos);
//			ListTag partsNBT = new ListTag();
//			multiblockParts.forEach((info) -> {
//				CompoundTag c = new CompoundTag();
//				c.put("Pos", NbtUtils.writeBlockPos(info.pos()));
//				partsNBT.add(c);
//			});
//			tag.put("Parts", partsNBT);
//			multiblocksNBT.add(tag);
//		});
//		ListTag actorsNBT = new ListTag();
//
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : this.getActors()) {
//			MovementBehaviour behaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(((StructureTemplate.StructureBlockInfo)actor.left).state());
//			if (behaviour != null) {
//				CompoundTag compound = new CompoundTag();
//				compound.put("Pos", NbtUtils.writeBlockPos(((StructureTemplate.StructureBlockInfo)actor.left).pos()));
//				behaviour.writeExtraData((MovementContext)actor.right);
//				((MovementContext)actor.right).writeToNBT(compound);
//				actorsNBT.add(compound);
//			}
//		}
//
//		ListTag disabledActorsNBT = NBTHelper.writeItemList(this.disabledActors, registries);
//		ListTag superglueNBT = new ListTag();
//		if (!spawnPacket) {
//			for(AABB glueEntry : this.superglue) {
//				CompoundTag c = new CompoundTag();
//				SuperGlueEntity.writeBoundingBox(c, glueEntry);
//				superglueNBT.add(c);
//			}
//		}
//
//		this.writeStorage(nbt, registries, spawnPacket);
//		ListTag interactorNBT = new ListTag();
//
//		for(BlockPos pos : this.interactors.keySet()) {
//			CompoundTag c = new CompoundTag();
//			c.put("Pos", NbtUtils.writeBlockPos(pos));
//			interactorNBT.add(c);
//		}
//
//		nbt.put("Seats", NBTHelper.writeCompoundList(this.getSeats(), (posx) -> {
//			CompoundTag c = new CompoundTag();
//			c.put("Pos", NbtUtils.writeBlockPos(posx));
//			return c;
//		}));
//		nbt.put("Passengers", NBTHelper.writeCompoundList(this.getSeatMapping().entrySet(), (e) -> {
//			CompoundTag tag = new CompoundTag();
//			tag.put("Id", NbtUtils.createUUID((UUID)e.getKey()));
//			tag.putInt("Seat", (Integer)e.getValue());
//			return tag;
//		}));
//		nbt.put("SubContraptions", NBTHelper.writeCompoundList(this.stabilizedSubContraptions.entrySet(), (e) -> {
//			CompoundTag tag = new CompoundTag();
//			tag.putUUID("Id", (UUID)e.getKey());
//			tag.put("Location", ((BlockFace)e.getValue()).serializeNBT());
//			return tag;
//		}));
//		nbt.put("Blocks", blocksNBT);
//		nbt.put("Actors", actorsNBT);
//		nbt.put("CapturedMultiblocks", multiblocksNBT);
//		nbt.put("DisabledActors", disabledActorsNBT);
//		nbt.put("Interactors", interactorNBT);
//		nbt.put("Superglue", superglueNBT);
//		nbt.put("Anchor", NbtUtils.writeBlockPos(this.anchor));
//		nbt.putBoolean("Stalled", this.stalled);
//		nbt.putBoolean("BottomlessSupply", this.hasUniversalCreativeCrate);
//		if (this.bounds != null) {
//			ListTag bb = NBTHelper.writeAABB(this.bounds);
//			nbt.put("BoundsFront", bb);
//		}
//
//		return nbt;
//	}
//
//	public void writeStorage(CompoundTag nbt, HolderLookup.Provider registries, boolean spawnPacket) {
//		this.storage.write(nbt, registries, spawnPacket);
//	}
//
//	private CompoundTag writeBlocksCompound(boolean spawnPacket) {
//		CompoundTag compound = new CompoundTag();
//		HashMapPalette<BlockState> palette = new HashMapPalette(GameData.getBlockStateIDMap(), 16, (ix, s) -> {
//			throw new IllegalStateException("Palette Map index exceeded maximum");
//		});
//		ListTag blockList = new ListTag();
//
//		for(StructureTemplate.StructureBlockInfo block : this.blocks.values()) {
//			int id = palette.idFor(block.state());
//			BlockPos pos = block.pos();
//			CompoundTag c = new CompoundTag();
//			c.putLong("Pos", pos.asLong());
//			c.putInt("State", id);
//			CompoundTag updateTag = (CompoundTag)this.updateTags.get(pos);
//			if (spawnPacket) {
//				if (updateTag != null) {
//					c.put("Data", updateTag);
//				} else if (block.nbt() != null) {
//					c.put("Data", block.nbt());
//					NBTHelper.putMarker(c, "Legacy");
//				}
//			} else {
//				if (block.nbt() != null) {
//					c.put("Data", block.nbt());
//				}
//
//				if (updateTag != null) {
//					c.put("UpdateTag", updateTag);
//				}
//			}
//
//			blockList.add(c);
//		}
//
//		ListTag paletteNBT = new ListTag();
//
//		for(int i = 0; i < palette.getSize(); ++i) {
//			paletteNBT.add(NbtUtils.writeBlockState((BlockState)palette.values.byId(i)));
//		}
//
//		compound.put("Palette", paletteNBT);
//		compound.put("BlockList", blockList);
//		return compound;
//	}
//
//	private void readBlocksCompound(Tag compound, Level world, boolean usePalettedDeserialization) {
//		this.blocks.clear();
//		this.updateTags.clear();
//		this.isLegacy.clear();
//		HolderGetter<Block> holderGetter = world.holderLookup(Registries.BLOCK);
//		HashMapPalette<BlockState> palette = null;
//		ListTag blockList;
//		if (usePalettedDeserialization) {
//			CompoundTag c = (CompoundTag)compound;
//			palette = new HashMapPalette(GameData.getBlockStateIDMap(), 16, (ix, s) -> {
//				throw new IllegalStateException("Palette Map index exceeded maximum");
//			});
//			ListTag list = c.getList("Palette", 10);
//			palette.values.clear();
//
//			for(int i = 0; i < list.size(); ++i) {
//				palette.values.add(NbtUtils.readBlockState(holderGetter, list.getCompound(i)));
//			}
//
//			blockList = c.getList("BlockList", 10);
//		} else {
//			blockList = (ListTag)compound;
//		}
//
//		for(Tag tag : blockList) {
//			CompoundTag c = (CompoundTag)tag;
//			StructureTemplate.StructureBlockInfo info = usePalettedDeserialization ? readStructureBlockInfo(c, palette) : legacyReadStructureBlockInfo(c, holderGetter);
//			this.blocks.put(info.pos(), info);
//			if (c.contains("UpdateTag", 10)) {
//				CompoundTag updateTag = c.getCompound("UpdateTag");
//				this.updateTags.put(info.pos(), updateTag);
//			}
//
//			this.isLegacy.put(info.pos(), c.contains("Legacy"));
//		}
//
//		this.resetClientContraption();
//	}
//
//	private static StructureTemplate.StructureBlockInfo readStructureBlockInfo(CompoundTag blockListEntry, HashMapPalette<BlockState> palette) {
//		return new StructureTemplate.StructureBlockInfo(BlockPos.of(blockListEntry.getLong("Pos")), (BlockState)Objects.requireNonNull((BlockState)palette.valueFor(blockListEntry.getInt("State"))), blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
//	}
//
//	private static StructureTemplate.StructureBlockInfo legacyReadStructureBlockInfo(CompoundTag blockListEntry, HolderGetter<Block> holderGetter) {
//		return new StructureTemplate.StructureBlockInfo(NBTHelper.readBlockPos(blockListEntry, "Pos"), NbtUtils.readBlockState(holderGetter, blockListEntry.getCompound("Block")), blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
//	}
//
//	public void removeBlocksFromWorld(Level world, BlockPos offset) {
//		this.glueToRemove.forEach((glue) -> {
//			this.superglue.add(glue.getBoundingBox().move(Vec3.atLowerCornerOf(offset.offset(this.anchor)).scale((double)-1.0F)));
//			glue.discard();
//		});
//		List<BoundingBox> minimisedGlue = new ArrayList();
//
//		for(int i = 0; i < this.superglue.size(); ++i) {
//			minimisedGlue.add((Object)null);
//		}
//
//		for(boolean brittles : Iterate.trueAndFalse) {
//			Iterator<StructureTemplate.StructureBlockInfo> iterator = this.blocks.values().iterator();
//
//			while(iterator.hasNext()) {
//				StructureTemplate.StructureBlockInfo block = (StructureTemplate.StructureBlockInfo)iterator.next();
//				if (brittles == BlockMovementChecks.isBrittle(block.state())) {
//					for(int i = 0; i < this.superglue.size(); ++i) {
//						AABB aabb = (AABB)this.superglue.get(i);
//						if (aabb != null && aabb.contains((double)block.pos().getX() + (double)0.5F, (double)block.pos().getY() + (double)0.5F, (double)block.pos().getZ() + (double)0.5F)) {
//							if (minimisedGlue.get(i) == null) {
//								minimisedGlue.set(i, new BoundingBox(block.pos()));
//							} else {
//								minimisedGlue.set(i, BBHelper.encapsulate((BoundingBox)minimisedGlue.get(i), block.pos()));
//							}
//						}
//					}
//
//					BlockPos add = block.pos().offset(this.anchor).offset(offset);
//					if (!this.customBlockRemoval(world, add, block.state())) {
//						BlockState oldState = world.getBlockState(add);
//						Block blockIn = oldState.getBlock();
//						boolean blockMismatch = block.state().getBlock() != blockIn;
//						blockMismatch &= !AllBlocks.POWERED_SHAFT.is(blockIn) || !AllBlocks.SHAFT.has(block.state());
//						if (blockMismatch) {
//							iterator.remove();
//						}
//
//						world.removeBlockEntity(add);
//						int flags = 122;
//						if (blockIn instanceof SimpleWaterloggedBlock && oldState.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean)oldState.getValue(BlockStateProperties.WATERLOGGED)) {
//							world.setBlock(add, Blocks.WATER.defaultBlockState(), flags);
//						} else {
//							world.setBlock(add, Blocks.AIR.defaultBlockState(), flags);
//						}
//					}
//				}
//			}
//		}
//
//		this.superglue.clear();
//
//		for(BoundingBox box : minimisedGlue) {
//			if (box != null) {
//				AABB bb = new AABB((double)box.minX(), (double)box.minY(), (double)box.minZ(), (double)(box.maxX() + 1), (double)(box.maxY() + 1), (double)(box.maxZ() + 1));
//				if (bb.getSize() > 1.01) {
//					this.superglue.add(bb);
//				}
//			}
//		}
//
//		for(StructureTemplate.StructureBlockInfo block : this.blocks.values()) {
//			BlockPos add = block.pos().offset(this.anchor).offset(offset);
//			int flags = 67;
//			world.sendBlockUpdated(add, block.state(), Blocks.AIR.defaultBlockState(), flags);
//			ServerLevel serverWorld = (ServerLevel)world;
//			PoiTypes.forState(block.state()).ifPresent((poiType) -> world.getServer().execute(() -> {
//				serverWorld.getPoiManager().add(add, poiType);
//				DebugPackets.sendPoiAddedPacket(serverWorld, add);
//			}));
//			world.markAndNotifyBlock(add, world.getChunkAt(add), block.state(), Blocks.AIR.defaultBlockState(), flags, 512);
//			block.state().updateIndirectNeighbourShapes(world, add, flags & -2);
//		}
//
//	}
//
//	public void addBlocksToWorld(Level world, StructureTransform transform) {
//		if (!this.disassembled) {
//			this.disassembled = true;
//			boolean shouldDropBlocks = !(Boolean)AllConfigs.server().kinetics.noDropWhenContraptionReplaceBlocks.get();
//			this.translateMultiblockControllers(transform);
//
//			for(boolean nonBrittles : Iterate.trueAndFalse) {
//				for(StructureTemplate.StructureBlockInfo block : this.blocks.values()) {
//					if (nonBrittles != BlockMovementChecks.isBrittle(block.state())) {
//						BlockPos targetPos = transform.apply(block.pos());
//						BlockState state = transform.apply(block.state());
//						if (!this.customBlockPlacement(world, targetPos, state)) {
//							if (nonBrittles) {
//								for(Direction face : Iterate.directions) {
//									state = state.updateShape(face, world.getBlockState(targetPos.relative(face)), world, targetPos, targetPos.relative(face));
//								}
//							}
//
//							BlockState blockState = world.getBlockState(targetPos);
//							if (blockState.getDestroySpeed(world, targetPos) != -1.0F && (!state.getCollisionShape(world, targetPos).isEmpty() || blockState.getCollisionShape(world, targetPos).isEmpty())) {
//								if (state.getBlock() instanceof SimpleWaterloggedBlock && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
//									FluidState fluidState = world.getFluidState(targetPos);
//									state = (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
//								}
//
//								world.destroyBlock(targetPos, shouldDropBlocks);
//								if (AllBlocks.SHAFT.has(state)) {
//									state = ShaftBlock.pickCorrectShaftType(state, world, targetPos);
//								}
//
//								if (state.hasProperty(SlidingDoorBlock.VISIBLE)) {
//									state = (BlockState)((BlockState)state.setValue(SlidingDoorBlock.VISIBLE, !(Boolean)state.getValue(SlidingDoorBlock.OPEN))).setValue(SlidingDoorBlock.POWERED, false);
//								}
//
//								if (state.is(Blocks.SCULK_SHRIEKER)) {
//									state = Blocks.SCULK_SHRIEKER.defaultBlockState();
//								}
//
//								world.setBlock(targetPos, state, 67);
//								boolean verticalRotation = transform.rotationAxis == null || transform.rotationAxis.isHorizontal();
//								verticalRotation = verticalRotation && transform.rotation != Rotation.NONE;
//								if (verticalRotation && (state.getBlock() instanceof PulleyBlock.RopeBlock || state.getBlock() instanceof PulleyBlock.MagnetBlock || state.getBlock() instanceof DoorBlock)) {
//									world.destroyBlock(targetPos, shouldDropBlocks);
//								}
//
//								BlockEntity blockEntity = world.getBlockEntity(targetPos);
//								CompoundTag tag = block.nbt();
//								if (state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER)) {
//									tag = null;
//								}
//
//								if (blockEntity != null) {
//									tag = NBTProcessors.process(state, blockEntity, tag, false);
//								}
//
//								if (blockEntity != null && tag != null) {
//									tag.putInt("x", targetPos.getX());
//									tag.putInt("y", targetPos.getY());
//									tag.putInt("z", targetPos.getZ());
//									if (verticalRotation && blockEntity instanceof PulleyBlockEntity) {
//										tag.remove("Offset");
//										tag.remove("InitialOffset");
//									}
//
//									if (blockEntity instanceof IMultiBlockEntityContainer && (tag.contains("LastKnownPos") || this.capturedMultiblocks.isEmpty())) {
//										tag.put("LastKnownPos", NbtUtils.writeBlockPos(BlockPos.ZERO.below(2147483646)));
//										tag.remove("Controller");
//									}
//
//									blockEntity.loadWithComponents(tag, world.registryAccess());
//								}
//
//								this.storage.unmount(world, block, targetPos, blockEntity);
//								if (blockEntity != null) {
//									transform.apply(blockEntity);
//								}
//							} else {
//								if (targetPos.getY() == world.getMinBuildHeight()) {
//									targetPos = targetPos.above();
//								}
//
//								world.levelEvent(2001, targetPos, Block.getId(state));
//								if (shouldDropBlocks) {
//									Block.dropResources(state, world, targetPos, (BlockEntity)null);
//								}
//							}
//						}
//					}
//				}
//			}
//
//			for(StructureTemplate.StructureBlockInfo block : this.blocks.values()) {
//				if (this.shouldUpdateAfterMovement(block)) {
//					BlockPos targetPos = transform.apply(block.pos());
//					world.markAndNotifyBlock(targetPos, world.getChunkAt(targetPos), block.state(), block.state(), 67, 512);
//				}
//			}
//
//			for(AABB box : this.superglue) {
//				box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)), transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
//				if (!world.isClientSide) {
//					world.addFreshEntity(new SuperGlueEntity(world, box));
//				}
//			}
//
//		}
//	}
//
//	protected void translateMultiblockControllers(StructureTransform transform) {
//		if (transform.rotationAxis != null && transform.rotationAxis != Axis.Y && transform.rotation != Rotation.NONE) {
//			this.capturedMultiblocks.values().forEach((info) -> info.nbt().put("LastKnownPos", NbtUtils.writeBlockPos(BlockPos.ZERO.below(2147483646))));
//		} else {
//			this.capturedMultiblocks.keySet().forEach((controllerPos) -> {
//				Collection<StructureTemplate.StructureBlockInfo> multiblockParts = this.capturedMultiblocks.get(controllerPos);
//				Optional<BoundingBox> optionalBoundingBox = BoundingBox.encapsulatingPositions(multiblockParts.stream().map((info) -> transform.apply(info.pos())).toList());
//				if (!optionalBoundingBox.isEmpty()) {
//					BoundingBox boundingBox = (BoundingBox)optionalBoundingBox.get();
//					BlockPos newControllerPos = new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());
//					BlockPos otherPos = transform.unapply(newControllerPos);
//					multiblockParts.forEach((info) -> info.nbt().put("Controller", NbtUtils.writeBlockPos(newControllerPos)));
//					if (!controllerPos.equals(otherPos)) {
//						StructureTemplate.StructureBlockInfo prevControllerInfo = (StructureTemplate.StructureBlockInfo)this.blocks.get(controllerPos);
//						StructureTemplate.StructureBlockInfo newControllerInfo = (StructureTemplate.StructureBlockInfo)this.blocks.get(otherPos);
//						if (prevControllerInfo != null && newControllerInfo != null) {
//							this.blocks.put(otherPos, new StructureTemplate.StructureBlockInfo(newControllerInfo.pos(), newControllerInfo.state(), prevControllerInfo.nbt()));
//							this.blocks.put(controllerPos, new StructureTemplate.StructureBlockInfo(prevControllerInfo.pos(), prevControllerInfo.state(), newControllerInfo.nbt()));
//						}
//					}
//				}
//			});
//		}
//	}
//
//	public void addPassengersToWorld(Level world, StructureTransform transform, List<Entity> seatedEntities) {
//		for(Entity seatedEntity : seatedEntities) {
//			if (!this.getSeatMapping().isEmpty()) {
//				Integer seatIndex = (Integer)this.getSeatMapping().get(seatedEntity.getUUID());
//				if (seatIndex != null) {
//					BlockPos seatPos = (BlockPos)this.getSeats().get(seatIndex);
//					seatPos = transform.apply(seatPos);
//					if (world.getBlockState(seatPos).getBlock() instanceof SeatBlock && !SeatBlock.isSeatOccupied(world, seatPos)) {
//						SeatBlock.sitDown(world, seatPos, seatedEntity);
//					}
//				}
//			}
//		}
//
//	}
//
//	public void startMoving(Level world) {
//		this.disabledActors.clear();
//
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.actors) {
//			MovementContext context = new MovementContext(world, (StructureTemplate.StructureBlockInfo)pair.left, this);
//			MovementBehaviour behaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(((StructureTemplate.StructureBlockInfo)pair.left).state());
//			if (behaviour != null) {
//				behaviour.startMoving(context);
//			}
//
//			pair.setRight(context);
//			if (behaviour instanceof ContraptionControlsMovement) {
//				this.disableActorOnStart(context);
//			}
//		}
//
//		for(ItemStack stack : this.disabledActors) {
//			this.setActorsActive(stack, false);
//		}
//
//	}
//
//	protected void disableActorOnStart(MovementContext context) {
//		if (ContraptionControlsMovement.isDisabledInitially(context)) {
//			ItemStack filter = ContraptionControlsMovement.getFilter(context);
//			if (filter != null) {
//				if (!this.isActorTypeDisabled(filter)) {
//					this.disabledActors.add(filter);
//				}
//			}
//		}
//	}
//
//	public boolean isActorTypeDisabled(ItemStack filter) {
//		return this.disabledActors.stream().anyMatch((i) -> ContraptionControlsMovement.isSameFilter(i, filter));
//	}
//
//	public void setActorsActive(ItemStack referenceStack, boolean enable) {
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.actors) {
//			MovementBehaviour behaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(((StructureTemplate.StructureBlockInfo)pair.left).state());
//			if (behaviour != null) {
//				ItemStack behaviourStack = behaviour.canBeDisabledVia((MovementContext)pair.right);
//				if (behaviourStack != null && (referenceStack.isEmpty() || ContraptionControlsMovement.isSameFilter(referenceStack, behaviourStack))) {
//					((MovementContext)pair.right).disabled = !enable;
//					if (!enable) {
//						behaviour.onDisabledByControls((MovementContext)pair.right);
//					}
//				}
//			}
//		}
//
//	}
//
//	public List<ItemStack> getDisabledActors() {
//		return this.disabledActors;
//	}
//
//	public void stop(Level world) {
//		this.forEachActor(world, (behaviour, ctx) -> {
//			behaviour.stopMoving(ctx);
//			ctx.position = null;
//			ctx.motion = Vec3.ZERO;
//			ctx.relativeMotion = Vec3.ZERO;
//			ctx.rotation = (v) -> v;
//		});
//	}
//
//	public void forEachActor(Level world, BiConsumer<MovementBehaviour, MovementContext> callBack) {
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.actors) {
//			MovementBehaviour behaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(((StructureTemplate.StructureBlockInfo)pair.getLeft()).state());
//			if (behaviour != null) {
//				callBack.accept(behaviour, (MovementContext)pair.getRight());
//			}
//		}
//
//	}
//
//	protected boolean shouldUpdateAfterMovement(StructureTemplate.StructureBlockInfo info) {
//		if (PoiTypes.forState(info.state()).isPresent()) {
//			return false;
//		} else {
//			return !(info.state().getBlock() instanceof SlidingDoorBlock);
//		}
//	}
//
//	public void expandBoundsAroundAxis(Direction.Axis axis) {
//		Set<BlockPos> blocks = this.getBlocks().keySet();
//		int radius = (int)Math.ceil(getRadius(blocks, axis));
//		int maxX = radius + 2;
//		int maxY = radius + 2;
//		int maxZ = radius + 2;
//		int minX = -radius - 1;
//		int minY = -radius - 1;
//		int minZ = -radius - 1;
//		if (axis == Axis.X) {
//			maxX = (int)this.bounds.maxX;
//			minX = (int)this.bounds.minX;
//		} else if (axis == Axis.Y) {
//			maxY = (int)this.bounds.maxY;
//			minY = (int)this.bounds.minY;
//		} else if (axis == Axis.Z) {
//			maxZ = (int)this.bounds.maxZ;
//			minZ = (int)this.bounds.minZ;
//		}
//
//		this.bounds = new AABB((double)minX, (double)minY, (double)minZ, (double)maxX, (double)maxY, (double)maxZ);
//	}
//
//	public Map<UUID, Integer> getSeatMapping() {
//		return this.seatMapping;
//	}
//
//	public BlockPos getSeatOf(UUID entityId) {
//		if (!this.getSeatMapping().containsKey(entityId)) {
//			return null;
//		} else {
//			int seatIndex = (Integer)this.getSeatMapping().get(entityId);
//			return seatIndex >= this.getSeats().size() ? null : (BlockPos)this.getSeats().get(seatIndex);
//		}
//	}
//
//	public BlockPos getBearingPosOf(UUID subContraptionEntityId) {
//		return this.stabilizedSubContraptions.containsKey(subContraptionEntityId) ? ((BlockFace)this.stabilizedSubContraptions.get(subContraptionEntityId)).getConnectedPos() : null;
//	}
//
//	public void setSeatMapping(Map<UUID, Integer> seatMapping) {
//		this.seatMapping = seatMapping;
//	}
//
//	public List<BlockPos> getSeats() {
//		return this.seats;
//	}
//
//	public Map<BlockPos, StructureTemplate.StructureBlockInfo> getBlocks() {
//		return this.blocks;
//	}
//
//	public Object2BooleanMap<BlockPos> getIsLegacy() {
//		return this.isLegacy;
//	}
//
//	public List<MutablePair<StructureTemplate.StructureBlockInfo, MovementContext>> getActors() {
//		return this.actors;
//	}
//
//	public @Nullable MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> getActorAt(BlockPos localPos) {
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.actors) {
//			if (localPos.equals(((StructureTemplate.StructureBlockInfo)pair.left).pos())) {
//				return pair;
//			}
//		}
//
//		return null;
//	}
//
//	public Map<BlockPos, MovingInteractionBehaviour> getInteractors() {
//		return this.interactors;
//	}
//
//	public void invalidateColliders() {
//		this.getContraptionWorld();
//		this.simplifiedEntityColliders.size = 0;
//		CollisionList.Populate populate = new CollisionList.Populate(this.simplifiedEntityColliders);
//
//		for(Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : this.blocks.entrySet()) {
//			StructureTemplate.StructureBlockInfo info = (StructureTemplate.StructureBlockInfo)entry.getValue();
//			BlockPos localPos = (BlockPos)entry.getKey();
//			VoxelShape collisionShape = info.state().getCollisionShape(this.collisionLevel, localPos, CollisionContext.empty());
//			if (!collisionShape.isEmpty()) {
//				populate.offsetX = localPos.getX();
//				populate.offsetY = localPos.getY();
//				populate.offsetZ = localPos.getZ();
//				collisionShape.forAllBoxes(populate);
//			}
//		}
//
//	}
//
//	public static double getRadius(Iterable<? extends Vec3i> blocks, Direction.Axis axis) {
//		Direction.Axis axisA;
//		Direction.Axis axisB;
//		switch (axis) {
//			case X:
//				axisA = Axis.Y;
//				axisB = Axis.Z;
//				break;
//			case Y:
//				axisA = Axis.X;
//				axisB = Axis.Z;
//				break;
//			case Z:
//				axisA = Axis.X;
//				axisB = Axis.Y;
//				break;
//			default:
//				throw new IllegalStateException("Unexpected value: " + String.valueOf(axis));
//		}
//
//		int maxDistSq = 0;
//
//		for(Vec3i vec : blocks) {
//			int a = vec.get(axisA);
//			int b = vec.get(axisB);
//			int distSq = a * a + b * b;
//			if (distSq > maxDistSq) {
//				maxDistSq = distSq;
//			}
//		}
//
//		return Math.sqrt((double)maxDistSq);
//	}
//
//	public MountedStorageManager getStorage() {
//		return this.storage;
//	}
//
//	public boolean isHiddenInPortal(BlockPos localPos) {
//		return false;
//	}
//
//	public @Nullable CollisionList getSimplifiedEntityColliders() {
//		return this.simplifiedEntityColliders;
//	}
//
//	public void tickStorage(AbstractContraptionEntity entity) {
//		this.getStorage().tick(entity);
//	}
//
//	public boolean containsBlockBreakers() {
//		for(MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> pair : this.actors) {
//			MovementBehaviour behaviour = (MovementBehaviour)MovementBehaviour.REGISTRY.get(((StructureTemplate.StructureBlockInfo)pair.getLeft()).state());
//			if (behaviour instanceof BlockBreakingMovementBehaviour || behaviour instanceof HarvesterMovementBehaviour) {
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	public final ClientContraption getOrCreateClientContraptionLazy() {
//		ClientContraption out = (ClientContraption)this.clientContraption.getAcquire();
//		if (out == null) {
//			this.clientContraption.compareAndExchangeRelease((Object)null, this.createClientContraption());
//			out = (ClientContraption)this.clientContraption.getAcquire();
//		}
//
//		return out;
//	}
//
//	@Contract(" -> new")
//	protected ClientContraption createClientContraption() {
//		return new ClientContraption(this);
//	}
//
//	public void resetClientContraption() {
//		ClientContraption maybeNullClientContraption = (ClientContraption)this.clientContraption.getAcquire();
//		if (maybeNullClientContraption != null) {
//			maybeNullClientContraption.resetRenderLevel();
//		}
//
//	}
//
//	public void invalidateClientContraptionStructure() {
//		ClientContraption maybeNullClientContraption = (ClientContraption)this.clientContraption.getAcquire();
//		if (maybeNullClientContraption != null) {
//			maybeNullClientContraption.invalidateStructure();
//		}
//
//	}
//
//	public void invalidateClientContraptionChildren() {
//		ClientContraption maybeNullClientContraption = (ClientContraption)this.clientContraption.getAcquire();
//		if (maybeNullClientContraption != null) {
//			maybeNullClientContraption.invalidateChildren();
//		}
//
//	}
//
//	public @Nullable BlockEntity getBlockEntityClientSide(BlockPos localPos) {
//		ClientContraption maybeNullClientContraption = (ClientContraption)this.clientContraption.getAcquire();
//		return maybeNullClientContraption == null ? null : maybeNullClientContraption.getBlockEntity(localPos);
//	}
//}
