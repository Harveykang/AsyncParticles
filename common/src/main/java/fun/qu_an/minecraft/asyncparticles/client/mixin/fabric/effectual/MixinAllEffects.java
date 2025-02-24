package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effectual;

import com.imeetake.effects.Bubbles.BubbleBreathEffect;
import com.imeetake.effects.Bubbles.BubbleChestEffect;
import com.imeetake.effects.CaveDust.CaveDustEffect;
import com.imeetake.effects.Firefly.FireflyEffect;
import com.imeetake.effects.GoldGlow.LanternGlowEffect;
import com.imeetake.effects.MouthSteam.MouthSteamEffect;
import com.imeetake.effects.PlayerRunEffect.PlayerRunEffect;
import com.imeetake.effects.SoulGlow.SoulLanternGlowEffect;
import com.imeetake.effects.Sparks.CampfireImprovements;
import com.imeetake.effects.Sparks.FireImprovements;
import com.imeetake.effects.SparksCart.SparksCartEffect;
import com.imeetake.effects.SparksSoul.SoulCampfireImprovements;
import com.imeetake.effects.SparksSoul.SoulFireImprovements;
import com.imeetake.effects.SteamEffect.SteamEffect;
import com.imeetake.effects.WaterDrip.WaterDripEffect;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {
	SteamEffect.class,
	WaterDripEffect.class,
	SoulCampfireImprovements.class,
	SoulFireImprovements.class,
	SparksCartEffect.class,
	FireImprovements.class,
	CampfireImprovements.class,
	SoulLanternGlowEffect.class,
	PlayerRunEffect.class,
	MouthSteamEffect.class,
	LanternGlowEffect.class,
	FireflyEffect.class,
	CaveDustEffect.class,
	BubbleBreathEffect.class,
	BubbleChestEffect.class,
})
public abstract class MixinAllEffects {
	@Redirect(method = "register", remap = false, at = @At(value = "INVOKE", remap = false, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private static void register(Event<?> instance, Object t) {
		AsyncTicker.registerEndTickEvent((ClientTickEvents.EndTick) t);
	}
}
