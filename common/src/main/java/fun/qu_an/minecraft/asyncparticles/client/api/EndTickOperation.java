//package fun.qu_an.minecraft.asyncparticles.client.api;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.resources.ResourceLocation;
//
///**
// * @apiNote Execution is not guaranteed.
// */
//public interface EndTickOperation extends Runnable {
//	/**
//	 * @apiNote Execution is not guaranteed.
//	 */
//	static void schedule(EndTickOperation task) {
//		AsyncTicker.scheduleOperation(task);
//	}
//
//	/**
//	 * @apiNote Execution is not guaranteed.
//	 */
//	static void schedule(ResourceLocation id, boolean parallel, Runnable task) {
//		schedule(new DefaultEndTickOperation(id, parallel, task));
//	}
//
//	/**
//	 * @apiNote Execution is not guaranteed.
//	 */
//	static void schedule(ResourceLocation id, Runnable task) {
//		schedule(id, false, task);
//	}
//
//	/**
//	 * @apiNote Execution is not guaranteed.
//	 */
//	static void schedule(ResourceLocation id, boolean parallel, MinecraftConsumer task) {
//		schedule(new DefaultEndTickOperation(id, parallel, () -> task.accept(Minecraft.getInstance())));
//	}
//
//	/**
//	 * @apiNote Execution is not guaranteed.
//	 */
//	static void schedule(ResourceLocation id, boolean parallel, ClientLevelConsumer task) {
//		schedule(new DefaultEndTickOperation(id, parallel, () -> task.accept(Minecraft.getInstance().level)));
//	}
//
//	/**
//	 * @implSpec Must be thread-safe.
//	 */
//	default boolean isParallel() {
//		return true;
//	}
//
//	/**
//	 * @implSpec Must be thread-safe.
//	 */
//	ResourceLocation getId();
//
//	@Override
//	void run();
//}
