package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effectual;

import com.imeetake.effectual.effects.Bubbles.BubbleBreathEffect;
import com.imeetake.effectual.effects.Bubbles.BubbleChestEffect;
import com.imeetake.effectual.effects.Bubbles.BubblePotsEffect;
import com.imeetake.effectual.effects.CaveDust.CaveDustEffect;
import com.imeetake.effectual.effects.GoldGlow.LanternGlowEffect;
import com.imeetake.effectual.effects.MouthSteam.MouthSteamEffect;
import com.imeetake.effectual.effects.PlayerRunEffect.PlayerRunEffect;
import com.imeetake.effectual.effects.SoulGlow.SoulLanternGlowEffect;
import com.imeetake.effectual.effects.Sparks.CampfireImprovements;
import com.imeetake.effectual.effects.Sparks.FireEntitySparks;
import com.imeetake.effectual.effects.Sparks.FireImprovements;
import com.imeetake.effectual.effects.SparksCart.SparksCartEffect;
import com.imeetake.effectual.effects.SparksSoul.SoulCampfireImprovements;
import com.imeetake.effectual.effects.SparksSoul.SoulFireImprovements;
import com.imeetake.effectual.effects.SteamEffect.SteamEffect;
import com.imeetake.effectual.effects.WaterDrip.WaterDripEffect;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = {
	SteamEffect.class,
	WaterDripEffect.class,
	SoulCampfireImprovements.class,
	SoulFireImprovements.class,
	SparksCartEffect.class,
	FireImprovements.class,
	FireEntitySparks.class,
	CampfireImprovements.class,
	SoulLanternGlowEffect.class,
	PlayerRunEffect.class,
	MouthSteamEffect.class,
	LanternGlowEffect.class,
	CaveDustEffect.class,
	BubbleBreathEffect.class,
	BubbleChestEffect.class,
	BubblePotsEffect.class,
})
public abstract class MixinAllEffects {
	@Redirect(method = "register", remap = false, at = @At(value = "INVOKE", remap = false, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private static void register(Event<?> instance, Object t) {
		AsyncTicker.registerEndTickEvent(((ClientTickEvents.EndTick) t)::onEndTick);
	}
}
