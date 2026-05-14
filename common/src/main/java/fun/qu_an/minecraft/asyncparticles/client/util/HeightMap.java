package fun.qu_an.minecraft.asyncparticles.client.util;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.LongHash;
import org.jetbrains.annotations.NotNull;

public class HeightMap {
	public static final float DEFAULT_HEIGHT = Integer.MIN_VALUE;
	private static final LongHash.Strategy STRATEGY = new LongHash.Strategy() {
		@Override
		public int hashCode(long e) {
			return (int) ((e & 0xFFFFFFFFL) * 31 + (e >>> 32));
		}

		@Override
		public boolean equals(long a, long b) {
			return a == b;
		}
	};
	protected int centerX, centerZ, range;
	protected int pendingCenterX, pendingCenterZ, pendingRange;
	protected volatile Long2FloatMap heightMap;
	protected Long2FloatMap pendingHeightMap;
	private boolean updating;

	public HeightMap() {
		this(DEFAULT_HEIGHT);
	}

	public HeightMap(float defaultHeight) {
		this.heightMap = newMap(defaultHeight);
		this.pendingHeightMap = newMap(defaultHeight);
	}

	protected @NotNull Long2FloatMap newMap(float defaultHeight) {
		Long2FloatMap map = new Long2FloatOpenCustomHashMap(STRATEGY);
		map.defaultReturnValue(defaultHeight);
		return map;
	}

	/**
	 * 开始批量更新（必须在写入线程调用）
	 *
	 * @return 基于上一帧统计数据推荐的计算半径
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
	 * 提交更新，原子性地切换到新地图
	 * 必须在写入线程调用，与 beginUpdate 配对使用
	 */
	public void commitUpdate() {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before commitUpdate");
		}
		updating = false;

		centerX = pendingCenterX;
		centerZ = pendingCenterZ;
		range = pendingRange;
		Long2FloatMap oldMap = heightMap;
		heightMap = pendingHeightMap;
		oldMap.clear();
		pendingHeightMap = oldMap;
	}

	/**
	 * 设置中心点坐标（必须在 beginUpdate 后调用）
	 */
	public void setCenter(int x, int z) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setCenter");
		}
		centerX = x;
		centerZ = z;
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

	/**
	 * 获取高度值
	 */
	public float getHeight(int x, int z) {
		return getHeight(asLong(x, z));
	}

	public float getHeight(long xz) {
		return heightMap.get(xz);
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

	public float defaultHeight() {
		return heightMap.defaultReturnValue();
	}

	public boolean isOutOfRange(int x, int z) {
		return Math.abs(x - centerX) > range || Math.abs(z - centerZ) > range;
	}

	public int getRange() {
		return range;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterZ() {
		return centerZ;
	}
}
