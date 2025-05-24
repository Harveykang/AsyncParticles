package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effectual;

import fun.qu_an.minecraft.asyncparticles.client.api.EndTickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {
	"com.imeetake.effects.SteamEffect.SteamEffect",
	"com.imeetake.effects.WaterDrip.WaterDripEffect",
	"com.imeetake.effects.SparksSoul.SoulCampfireImprovements",
	"com.imeetake.effects.SparksSoul.SoulFireImprovements",
	"com.imeetake.effects.SparksCart.SparksCartEffect",
	"com.imeetake.effects.Sparks.FireImprovements",
	"com.imeetake.effects.Sparks.FireEntitySparks",
	"com.imeetake.effects.Sparks.CampfireImprovements",
	"com.imeetake.effects.SoulGlow.SoulLanternGlowEffect",
	"com.imeetake.effects.PlayerRunEffect.PlayerRunEffect",
	"com.imeetake.effects.MouthSteam.MouthSteamEffect",
	"com.imeetake.effects.GoldGlow.LanternGlowEffect",
	"com.imeetake.effects.Firefly.FireflyEffect",
	"com.imeetake.effects.CaveDust.CaveDustEffect",
	"com.imeetake.effects.Bubbles.BubbleBreathEffect",
	"com.imeetake.effects.Bubbles.BubbleChestEffect",
	"com.imeetake.effects.Bubbles.BubblePotsEffect",
	"com.imeetake.effectual.effects.Bubbles.BubbleBreathEffect",
	"com.imeetake.effectual.effects.Bubbles.BubbleChestEffect",
	"com.imeetake.effectual.effects.Bubbles.BubblePotsEffect",
	"com.imeetake.effectual.effects.CaveDust.CaveDustEffect",
	"com.imeetake.effectual.effects.Firefly.FireflyEffect",
	"com.imeetake.effectual.effects.GoldGlow.LanternGlowEffect",
	"com.imeetake.effectual.effects.MouthSteam.MouthSteamEffect",
	"com.imeetake.effectual.effects.PlayerRunEffect.PlayerRunEffect",
	"com.imeetake.effectual.effects.SoulGlow.SoulLanternGlowEffect",
	"com.imeetake.effectual.effects.Sparks.CampfireImprovements",
	"com.imeetake.effectual.effects.Sparks.FireEntitySparks",
	"com.imeetake.effectual.effects.Sparks.FireImprovements",
	"com.imeetake.effectual.effects.SparksCart.SparksCartEffect",
	"com.imeetake.effectual.effects.SparksSoul.SoulCampfireImprovements",
	"com.imeetake.effectual.effects.SparksSoul.SoulFireImprovements",
	"com.imeetake.effectual.effects.SteamEffect.SteamEffect",
	"com.imeetake.effectual.effects.WaterDrip.WaterDripEffect",
})
public abstract class MixinAllEffects {
	@Redirect(method = "register", remap = false, at = @At(value = "INVOKE", remap = false, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private static void register(Event<?> instance, Object t) {
		EndTickEvent.register(false, ((ClientTickEvents.EndTick) t)::onEndTick);
	}
}
