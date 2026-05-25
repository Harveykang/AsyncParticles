package fun.qu_an.minecraft.asyncparticles.client.compat.create;

import fun.qu_an.minecraft.asyncparticles.client.util.HeightMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.LongHash;

public class ContraptionHeightMap extends HeightMap {
	public static final byte DEFAULT_MOVING = -1;
	private static final LongHash.Strategy HASH_STRATEGY = new LongHash.Strategy() {
		@Override
		public int hashCode(long e) {
			return getZ(e) * 31 + getX(e);
		}

		@Override
		public boolean equals(long a, long b) {
			return a == b;
		}
	};
	private Long2ByteMap movingMap, pendingMovingMap;
	private final byte defaultMoving;

	public ContraptionHeightMap() {
		this(DEFAULT_HEIGHT, DEFAULT_MOVING);
	}

	public ContraptionHeightMap(float defaultHeight, byte defaultMoving) {
		super(defaultHeight);
		this.defaultMoving = defaultMoving;
		this.movingMap = newMovingMap(defaultMoving);
		this.pendingMovingMap = newMovingMap(defaultMoving);
		HeightMap.State state = super.state;
		super.state = new State(state, Long2ByteMaps.EMPTY_MAP);
	}

	private static Long2ByteMap newMovingMap(byte defaultMoving) {
		Long2ByteMap map = new Long2ByteOpenCustomHashMap(HASH_STRATEGY);
		map.defaultReturnValue(defaultMoving);
		return map;
	}

	@Override
	protected void swapMaps() {
		super.swapMaps();
		Long2ByteMap oldMap = movingMap;
		movingMap = pendingMovingMap;
		oldMap.clear();
		pendingMovingMap = oldMap;
	}

	@Override
	protected State createSnapshot() {
		return new State(super.createSnapshot(), pendingMovingMap);
	}

	public void setMoving(int x, int z, boolean moving) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setMoving");
		}
		pendingMovingMap.put(asLong(x, z), moving ? (byte) 1 : (byte) 0);
	}

	public void setMoving(long xz, boolean moving) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setMoving");
		}
		pendingMovingMap.put(xz, moving ? (byte) 1 : (byte) 0);
	}

	public byte getPendingMoving(int x, int z) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before getPendingMoving");
		}
		return getPendingMoving0(asLong(x, z));
	}

	public byte getPendingMoving(long xz) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before getPendingMoving");
		}
		return getPendingMoving0(xz);
	}

	private byte getPendingMoving0(long xz) {
		return pendingMovingMap.get(xz);
	}

	@Override
	public State getState() {
		return (State) super.getState();
	}

	public byte getDefaultMoving() {
		return defaultMoving;
	}

	public static class State extends HeightMap.State {
		private final Long2ByteMap movingMap;

		public State(HeightMap.State parent, Long2ByteMap movingMap) {
			super(parent);
			this.movingMap = movingMap;
		}

		public byte isMoving(int x, int z) {
			return movingMap.get(asLong(x, z));
		}
	}
}
