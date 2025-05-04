package fun.qu_an.minecraft.asyncparticles.client.coremod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark coremod classes that can be loaded before the game is launched,
 * and visible for decompiled code.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@PreLaunch
public @interface PreLaunch {
}
