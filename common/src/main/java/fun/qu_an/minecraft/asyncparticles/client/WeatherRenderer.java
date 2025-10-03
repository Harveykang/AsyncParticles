//package fun.qu_an.minecraft.asyncparticles.client;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.BufferBuilder;
//import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//import com.mojang.blaze3d.vertex.MeshData;
//import com.mojang.blaze3d.vertex.VertexFormat;
//import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
//import fun.qu_an.minecraft.asyncparticles.client.util.BindingTesselator;
//import fun.qu_an.minecraft.asyncparticles.client.util.ExceptionUtil;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.WeatherEffectRenderer;
//import net.minecraft.world.phys.Vec3;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.concurrent.CompletableFuture;
//
//public class WeatherRenderer {
//	private static CompletableFuture<Void> weatherTask;
//	static WeatherEffectRenderer.ColumnInstance[] rainColumns = new WeatherEffectRenderer.ColumnInstance[0];
//	static WeatherEffectRenderer.ColumnInstance[] snowColumns = new WeatherEffectRenderer.ColumnInstance[0];
//	@Nullable
//	private static BindingTesselator rainBTesselator;
//	@Nullable
//	private static BindingTesselator snowBTesselator;
//	private static boolean weatherEnabled;
//
//	public static void beginWeather(float partialTick, Vec3 cameraPos, int rainDistance, WeatherEffectRenderer weatherRenderer, int ticks) {
////		if (!ConfigHelper.isRenderWeatherAsync()) { // checked outside
////			return;
////		}
//		if (!weatherEnabled) {
//			waitForWeatherTask();
//			return;
//		}
//		weatherEnabled = false;
//		Minecraft mc = Minecraft.getInstance();
//		float rainLevel = mc.level.getRainLevel(partialTick);
//		if (rainLevel > 0f) {
//			weatherTask = CompletableFuture.runAsync(() -> {
//				ObjectArrayList<WeatherEffectRenderer.ColumnInstance> rainColumns = ObjectArrayList.wrap(WeatherRenderer.rainColumns, 0);
//				ObjectArrayList<WeatherEffectRenderer.ColumnInstance> snowColumns = ObjectArrayList.wrap(WeatherRenderer.snowColumns, 0);
//				weatherRenderer.collectColumnInstances(mc.level,
//					ticks,
//					partialTick,
//					cameraPos,
//					rainDistance,
//					rainColumns,
//					snowColumns);
//				WeatherRenderer.rainColumns = rainColumns.elements();
//				WeatherRenderer.snowColumns = snowColumns.elements();
//				if (!rainColumns.isEmpty()) {
//					if (rainBTesselator == null) {
//						rainBTesselator = new BindingTesselator(1536, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
//					}
//					weatherRenderer.renderInstances(rainBTesselator.begin(), rainColumns, cameraPos, 1.0f, rainDistance, rainLevel);
//				}
//				if (!snowColumns.isEmpty()) {
//					if (snowBTesselator == null) {
//						snowBTesselator = new BindingTesselator(1536, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
//					}
//					weatherRenderer.renderInstances(snowBTesselator.begin(), snowColumns, cameraPos, 0.8f, rainDistance, rainLevel);
//				}
//			}, AsyncRenderer.EXECUTOR).exceptionally(throwable -> {
//				// TODO
////				return null;
//				throw ExceptionUtil.toThrowDirectly(throwable);
//			});
//		}
//	}
//
//	public static void endRain(RenderType renderType) {
//		end0(rainBTesselator, renderType);
//	}
//
//	public static void endSnow(RenderType renderType) {
//		end0(snowBTesselator, renderType);
//	}
//
//	private static void end0(@Nullable BindingTesselator tesselator, RenderType renderType) {
//		if (tesselator == null) {
//			return;
//		}
//		BufferBuilder builder = tesselator.getBuilder();
//		if (builder == null || !builder.building) {
//			return;
//		}
//		MeshData meshData = builder.build();
//		if (meshData == null) {
//			return;
//		}
//		if (renderType.sortOnUpload()) {
//			meshData.sortQuads(tesselator.buffer, RenderSystem.getProjectionType().vertexSorting());
//		}
//		renderType.draw(meshData);
//	}
//
//	public static void waitForWeatherTask() {
//		if (weatherTask != null) {
//			weatherTask.join();
//			weatherTask = null;
//		}
//	}
//
//	public static void reset() {
//		weatherEnabled = false;
//		if (rainBTesselator != null) {
//			rainBTesselator.close();
//			rainBTesselator = null;
//		}
//		if (snowBTesselator != null) {
//			snowBTesselator.close();
//			snowBTesselator = null;
//		}
//		rainColumns = new WeatherEffectRenderer.ColumnInstance[0];
//		snowColumns = new WeatherEffectRenderer.ColumnInstance[0];
//	}
//
//	public static boolean shouldRenderRain() {
//		waitForWeatherTask();
//		BufferBuilder builder;
//		return rainBTesselator != null && (builder = rainBTesselator.getBuilder()) != null && builder.building;
//	}
//
//	public static boolean shouldRenderSnow() {
//		waitForWeatherTask();
//		BufferBuilder builder;
//		return snowBTesselator != null && (builder = snowBTesselator.getBuilder()) != null && builder.building;
//	}
//
//	public static void markWeatherEnabled() {
//		weatherEnabled = true;
//	}
//}
