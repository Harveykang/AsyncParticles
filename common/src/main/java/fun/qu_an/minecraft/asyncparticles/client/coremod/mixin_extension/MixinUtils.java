package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension;

import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;

public class MixinUtils {
	private static final VarHandle MIXININFO$STATE;
	private static final VarHandle MIXININFO$STATE_CLASSNODE;

	static {
		try {
			Class<?> infoClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo");
			Class<?> stateClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo$State");
			MIXININFO$STATE = MethodHandles.privateLookupIn(infoClass, MethodHandles.lookup())
				.findVarHandle(infoClass, "state", stateClass);
			MIXININFO$STATE_CLASSNODE = MethodHandles.privateLookupIn(stateClass, MethodHandles.lookup())
				.findVarHandle(stateClass, "classNode", ClassNode.class);
		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static ClassNode getDirectClassNode(IMixinInfo mixinInfo) {
		return (ClassNode) MIXININFO$STATE_CLASSNODE.get(MIXININFO$STATE.get(mixinInfo));
	}

	public static Optional<TargetClassContextExtension> tryAs(ITargetClassContext reference) {
		if (reference.getClass().getName().equals("org.spongepowered.asm.mixin.transformer.TargetClassContext")) {
			return Optional.of(new TargetClassContextExtension(reference));
		}
		return Optional.empty();
	}
}
