package fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.util.HeightMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

public class ContraptionHeightMap extends HeightMap {
	public static final byte DEFAULT_MOVING = -1;
	protected volatile Long2ByteMap movingMap;
	protected Long2ByteMap pendingMovingMap;

	public ContraptionHeightMap() {
		this(DEFAULT_HEIGHT, DEFAULT_MOVING);
	}

	public ContraptionHeightMap(float defaultHeight, byte defaultMoving) {
		super(defaultHeight);
		this.movingMap = new Long2ByteOpenHashMap();
		movingMap.defaultReturnValue(defaultMoving);
		this.pendingMovingMap = new Long2ByteOpenHashMap();
		pendingMovingMap.defaultReturnValue(defaultMoving);
	}

	@Override
	public void commitUpdate() {
		super.commitUpdate();
		Long2ByteMap oldMap = movingMap;
		movingMap = pendingMovingMap;
		oldMap.clear();
		pendingMovingMap = oldMap;
	}

	public void setMoving(int x, int z, boolean moving) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setMoving");
		}
		setMoving0(asLong(x, z), moving);
	}

	public void setMoving(long xz, boolean moving) {
		if (!isUpdating()) {
			throw new IllegalStateException("Must call beginUpdate before setMoving");
		}
		setMoving0(xz, moving);
	}

	private void setMoving0(long xz, boolean moving) {
		pendingMovingMap.put(xz, moving ? (byte) 1 : (byte) 0);
	}

	public byte isMoving(int x, int z) {
		return isMoving(asLong(x, z));
	}

	public byte isMoving(long xz) {
		return movingMap.get(xz);
	}
}
