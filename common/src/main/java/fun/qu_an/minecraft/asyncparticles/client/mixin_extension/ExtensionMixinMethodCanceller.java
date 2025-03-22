package fun.qu_an.minecraft.asyncparticles.client.mixin_extension;

import com.bawnorton.mixinsquared.ext.ExtensionRegistrar;
import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ExtensionMixinMethodCanceller implements IExtension {
	private static final ILogger LOGGER = MixinService.getService().getLogger("asyncparticles:mixin_method_canceller");
	private static final List<Canceller> CANCELLERS = new CopyOnWriteArrayList<>();
	private static boolean init;

	public static void init() {
		if (!init) {
			ExtensionRegistrar.register(new ExtensionMixinMethodCanceller());
			init = true;
		}
	}

	public static void register(Canceller canceller) {
		CANCELLERS.add(canceller);
	}

	private ExtensionMixinMethodCanceller() {
	}

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}

	@Override
	public void preApply(ITargetClassContext context) {
		if (CANCELLERS.isEmpty()) {
			return;
		}
		TargetClassContextExtension.tryAs(context).ifPresent(contextExtension -> {
			SortedSet<IMixinInfo> mixins = contextExtension.getMixins();
			mixins.forEach(mixin -> {
				ClassNode classNode = MixinUtils.getDirectClassNode(mixin);
				List<MethodNode> mixinMethods = classNode.methods;
				if (mixinMethods == null || mixinMethods.isEmpty()) {
					return;
				}
				String mixinClassName = mixin.getClassName();

				List<Canceller> cancellers = new ArrayList<>(CANCELLERS.size());
				CANCELLERS.forEach(canceller -> {
					if (canceller.preTest(mixinClassName)) {
						cancellers.add(canceller);
					}
				});
				if (cancellers.isEmpty()) {
					return;
				}

				for (Iterator<MethodNode> iterator = mixinMethods.iterator(); iterator.hasNext(); ) {
					MethodNode method = iterator.next();
					List<String> parameterNames;
					if (method.parameters == null || method.parameters.isEmpty()) {
						parameterNames = Collections.emptyList();
					} else {
						parameterNames = new ArrayList<>(method.parameters.size());
						method.parameters.forEach(parameter -> parameterNames.add(parameter.name));
					}
					for (Canceller canceller : cancellers) {
						if (canceller.test(mixinClassName, method.name, method.desc, parameterNames)) {
							iterator.remove();
							LOGGER.warn("Cancelled mixin method {}#{}", mixinClassName, method.desc);
							break;
						}
					}
				}
			});
		});
	}

	@Override
	public void postApply(ITargetClassContext context) {
	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
	}

	public interface Canceller {
		/**
		 * @return true if the canceller should be tested for the given mixin class name, false otherwise
		 */
		boolean preTest(String mixinClassName);

		/**
		 * @return true if the given method should be cancelled, false otherwise
		 */
		boolean test(String mixinClassName, String mixinMethodName, String mixinMethodDesc, List<String> mixinParameterNames);
	}
}
