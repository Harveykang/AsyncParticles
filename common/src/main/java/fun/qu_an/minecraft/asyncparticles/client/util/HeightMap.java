package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMaps;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.LongHash;
import org.jetbrains.annotations.NotNull;

public class HeightMap {
	public static final float DEFAULT_HEIGHT = Integer.MIN_VALUE;
	private static final LongHash.Strategy STRATEGY = new LongHash.Strategy() {
		@Override
		public int hashCode(long e) {
			return getZ(e) * 31 + getX(e);
		}

		@Override
		public boolean equals(long a, long b) {
			return a == b;
		}
	};
	private final float defaultHeight;
	protected int pendingCenterX, pendingCenterZ, pendingRange;
	protected Long2FloatMap heightMap, pendingHeightMap;
	protected boolean updating;
	protected volatile State state;

	public HeightMap() {
		this(DEFAULT_HEIGHT);
	}

	public HeightMap(float defaultHeight) {
		this.defaultHeight = defaultHeight;
		this.heightMap = newMap(defaultHeight);
		this.pendingHeightMap = newMap(defaultHeight);
		this.state = new State(0, 0, 0, Long2FloatMaps.EMPTY_MAP);
	}

	protected static @NotNull Long2FloatMap newMap(float defaultHeight) {
		Long2FloatMap map = new Long2FloatOpenCustomHashMap(STRATEGY);
		map.defaultReturnValue(defaultHeight);
		return map;
	}

	/**
	 * 开始批量更新（必须在写入线程调用）
	 *
	 * @throws IllegalStateException 如果已经在更新中
	 */
	public void beginUpdate(int centerX, int centerZ, int range) {
		if (isUpdating()) {
			throw new IllegalStateException("Cannot begin update while update is in progress");
		}
		this.pendingCenterX = centerX;
		this.pendingCenterZ = centerZ;
		this.pendingRange = range;
		updating = true;
	}

	/**
	 * 提交更新，原子性地切换到新状态
	 * 必须在写入线程调用，与 beginUpdate 配对使用
	 */
	public final void commitUpdate() {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before commitUpdate");
		}
		updating = false;
		this.state = createSnapshot();
		swapMaps();
	}

	protected void swapMaps() {
		Long2FloatMap oldMap = heightMap;
		heightMap = pendingHeightMap;
		oldMap.clear();
		this.pendingHeightMap = oldMap;
	}

	protected State createSnapshot() {
		return new State(pendingCenterX, pendingCenterZ, pendingRange, pendingHeightMap);
	}

	/**
	 * 设置中心点坐标（必须在 beginUpdate 后调用）
	 */
	public void setCenter(int x, int z) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setCenter");
		}
		pendingCenterX = x;
		pendingCenterZ = z;
	}

	/**
	 * 设置高度值（必须在 beginUpdate 后调用）
	 *
	 * @return true 如果设置成功，false 如果已存在
	 */
	public boolean setHeight(int x, int z, float height) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setHeight");
		}
		return setHeight0(asLong(x, z), height);
	}

	/**
	 * 设置高度值（必须在 beginUpdate 后调用）
	 *
	 * @return true 如果设置成功，false 如果已存在
	 */
	public boolean setHeight(long xz, float height) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setHeight");
		}
		return setHeight0(xz, height);
	}

	private boolean setHeight0(long xz, float height) {
		float v = pendingHeightMap.mergeFloat(xz, height, Float::max);
		return v == height;
	}

	public float getPendingHeight(int x, int z) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before getPendingHeight");
		}
		return getPendingHeight0(asLong(x, z));
	}

	public float getPendingHeight(long xz) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before getPendingHeight");
		}
		return getPendingHeight0(xz);
	}

	private float getPendingHeight0(long xz) {
		return pendingHeightMap.get(xz);
	}

	public State getState() {
		return state;
	}

	public float defaultHeight() {
		return defaultHeight;
	}

	public static long asLong(int x, int z) {
		return ((long) z << 32) | (x & 0xFFFFFFFFL);
	}

	public static int getX(long xz) {
		return (int) (xz & 0xFFFFFFFFL);
	}

	public static int getZ(long xz) {
		return (int) (xz >>> 32);
	}

	protected boolean isUpdating() {
		return updating;
	}

	public static boolean isOutOfRange(int x, int z, State state) {
		int range = state.range();
		return Math.abs(x - state.centerX()) > range
			|| Math.abs(z - state.centerZ()) > range;
	}

	public static class State {
		private final int centerX;
		private final int centerZ;
		private final int range;
		private final Long2FloatMap heightMap;

		private State(int centerX, int centerZ, int range, Long2FloatMap heightMap) {
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.range = range;
			this.heightMap = heightMap;
		}

		public State(State state) {
			this(state.centerX, state.centerZ, state.range, state.heightMap);
		}

		public float getHeight(int x, int z) {
			return heightMap.get(asLong(x, z));
		}

		public int centerX() {
			return centerX;
		}

		public int centerZ() {
			return centerZ;
		}

		public int range() {
			return range;
		}
	}
}
