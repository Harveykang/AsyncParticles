package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.reflection.FieldReference;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.*;

public class MixinClassAdjusterApplication {
	static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared-class-adjuster");
	private static MixinClassAdjusterApplication INSTANCE;
	private static final FieldReference<String> pluginClassName;
	private static final FieldReference<IMixinService> mixinService;
	/**
	 * key: original mixin class name, value: class adjuster
	 */
	private static Map<String, MixinClassAdjuster> ADJUSTERS;
	private static final Map<String, byte[]> RUNTIME_MIXINS = new HashMap<>();

	static {
		try {
			Class<?> mixinConfigClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
			pluginClassName = new FieldReference<>(mixinConfigClass, "pluginClassName");
			mixinService = new FieldReference<>(mixinConfigClass, "service");
		} catch (ClassNotFoundException e) {
			throw new MixinError(e);
		}
	}

	final Map<String, String> generatedToOriginalMixins = new HashMap<>();
	final Set<String> originalMixins = new HashSet<>();
	final IMixinConfigPlugin mixinSquaredPlugin;
	private final String generatedMixinPrefix;

	public static void init(String packageName, IMixinConfigPlugin mixinSquaredPlugin) {
		if (INSTANCE != null) {
			throw new IllegalStateException("MixinClassAdjusterApplication is already initialized");
		}
		INSTANCE = new MixinClassAdjusterApplication(packageName, mixinSquaredPlugin);
	}

	public static MixinClassAdjusterApplication getInstance() {
		return INSTANCE;
	}

	private MixinClassAdjusterApplication(String packagename, IMixinConfigPlugin mixinSquaredPlugin) {
		MixinCancellerRegistrar.register((targetClassName, mixinClassName) -> originalMixins.contains(mixinClassName));
		this.mixinSquaredPlugin = mixinSquaredPlugin;
		this.generatedMixinPrefix = packagename + ".MixinSquaredGenerated$";
	}

	public String getGeneratedMixinPrefix(String mixinClassName) {
		return generatedMixinPrefix + mixinClassName.replace("/", "$_").replace(".", "$_");
	}

	public boolean shouldApplyMixin(String targetClassName, String generatedMixinClassName) {
		String mixinClassName = generatedToOriginalMixins.get(generatedMixinClassName);
		if (mixinClassName == null) {
			return true;
		}
		return ADJUSTERS.get(mixinClassName).shouldApplyMixin(targetClassName);
	}

	public List<String> apply() {
		IMixinTransformer activeTransformer =
			(IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
		// FIXME: this is unsafe
		List<IMixinConfig> pendingConfigs = MixinTransformerExtension.tryAs(activeTransformer)
			.map(MixinTransformerExtension::getPendingConfigs)
			.orElseThrow(() -> new UnsupportedOperationException("Unsupported mixin transformer: " + activeTransformer.getClass()));
		// Find our mixin config
		IMixinConfig mixinConfig = null;
		String pluginClass = mixinSquaredPlugin.getClass().getName();
		for (IMixinConfig config : pendingConfigs) {
			String aPlugin = pluginClassName.get(config);
			if (pluginClass.equals(aPlugin)) {
				mixinConfig = config;
				break;
			}
		}
		assert mixinConfig != null;
		// Exchange mixin service with our own wrapper so that we can modify target classes
		IMixinService service = mixinService.get(mixinConfig);
		MixinServiceWrapper mixinServiceWrapper;
		if (!(service instanceof MixinServiceWrapper)) {
			LOGGER.info("Wrapping mixin service for {} so that we can modify target classes.", mixinConfig);
			mixinServiceWrapper = new MixinServiceWrapper(service);
			mixinService.set(mixinConfig, mixinServiceWrapper);
		} else {
			mixinServiceWrapper = (MixinServiceWrapper) service;
		}

		// Apply adjusters!
		Map<String, IReferenceMapper> mappersCache = new HashMap<>();
		ADJUSTERS = MixinClassAdjusterRegistrar.endAdjusters();
		for (MixinClassAdjuster adjuster : ADJUSTERS.values()) {
			applyAdjuster(mixinServiceWrapper, adjuster, mappersCache);
		}
		List<MixinClassProvider> providers = MixinClassAdjusterRegistrar.endProviders();
		// Collect generated mixin class's simple names
		ArrayList<String> mixins = new ArrayList<>(generatedToOriginalMixins.size() + providers.size());
		for (String s : generatedToOriginalMixins.keySet()) {
			String substring = s.substring(s.lastIndexOf('.') + 1);
			mixins.add(substring);
		}

		// Apply providers!
		for (MixinClassProvider provider : providers) {
			ClassNode mixinClass = provider.getMixinClass();
			ClassRenamer.renameClass(mixinClass, getGeneratedMixinPrefix(mixinClass.name));
			genClass(mixinClass);
			mixins.add(mixinClass.name.substring(mixinClass.name.lastIndexOf('/') + 1));
		}

		return mixins;
	}

	private void applyAdjuster(IClassBytecodeProvider bytecodeProvider,
							   MixinClassAdjuster adjuster,
							   Map<String, IReferenceMapper> mappersCache) {
		String mixinClassName = adjuster.getMixinClassName();
		// Get the original mixin class node
		ClassNode cNode;
		try {
			cNode = bytecodeProvider.getClassNode(mixinClassName);
		} catch (ClassNotFoundException | IOException e) {
			throw new MixinError(e);
		}
		// A list of original targets
		List<String> targets = new ArrayList<>();
		AnnotationNode aNode = Annotations.getInvisible(cNode, Mixin.class);
		List<Object> values = aNode.values;
		// The index of the "targets" key in the annotation
		int targetIndex = -1;
		int priorityIndex = -1;
		for (ListIterator<Object> iterator = values.listIterator(); iterator.hasNext(); ) {
			String key = (String) iterator.next();
			Object value = iterator.next();
			if ("value".equals(key)) {
				@SuppressWarnings("unchecked")
				List<Type> classes = (List<Type>) value;
				if (classes.isEmpty()) {
					continue;
				}
				for (Type c : classes) {
					targets.add(c.getClassName());
				}
				// remove original value
				iterator.set(Collections.emptyList());
			} else if ("targets".equals(key)) {
				@SuppressWarnings("unchecked")
				List<String> originalTargets = (List<String>) value;
				if (originalTargets.isEmpty()) {
					continue;
				}
				targets.addAll(originalTargets);
				targetIndex = iterator.previousIndex();
				iterator.set(Collections.emptyList());
			} else if ("priority".equals(key)) {
				priorityIndex = iterator.previousIndex();
			}
		}
		boolean modified = false;

		// Modify the priority
		Integer priority = adjuster.getPriority();
		if (priority != null) {
			if (priorityIndex > -1) {
				values.set(priorityIndex, priority);
			} else {
				values.add("priority");
				values.add(priority);
			}
			modified = true;
		}

		// Modify the target classes
		List<String> unmodifiableTargets = Collections.unmodifiableList(targets);
		List<String> modifiedTargets = adjuster.getTargets(unmodifiableTargets);
		Map<String, ClassNode> specialTargets = null;
		if (modifiedTargets != null && modifiedTargets != unmodifiableTargets) {// Remove the original targets
			// and write the modified to the annotation
			String internalClassName = cNode.name;
			String refMapperConfig = adjuster.getRefMapperConfig();
			if (refMapperConfig == null) {
				refMapperConfig = ReferenceMapper.DEFAULT_RESOURCE;
			}
			for (MethodNode mNode : cNode.methods) {
				if (mNode.visibleAnnotations == null) {
					continue;
				}
				for (AnnotationNode aNode1 : mNode.visibleAnnotations) {
					int i = aNode1.values.indexOf("method");
					if (i == -1) {
						continue;
					}
					Object o = aNode1.values.get(i + 1);
					List<?> l;
					if (!(o instanceof List<?>) || (l = (List<?>) o).isEmpty() || !(l.get(0) instanceof String)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					List<String> methods = (List<String>) l;
					IReferenceMapper mapper = mappersCache.computeIfAbsent(refMapperConfig,
						k -> RemappingReferenceMapper.of(MixinEnvironment.getDefaultEnvironment(),
							ReferenceMapper.read(k)));
					for (ListIterator<String> iterator = methods.listIterator(); iterator.hasNext(); ) {
						String s1 = iterator.next();
						s1 = mapper.remap(internalClassName, s1);
						if (s1.indexOf('(') > s1.indexOf(';')) {
							// MUST remove the owner descriptor
							// otherwise the injection will fail
							s1 = s1.substring(s1.indexOf(';') + 1);
						}
						iterator.set(s1);
					}
				}
			}

			// Set targets
			List<String> finalTargets = new ArrayList<>(modifiedTargets);
			if (targetIndex > -1) {
				values.set(targetIndex, finalTargets);
			} else {
				values.add("targets");
				targetIndex = values.size();
				values.add(finalTargets);
			}
			// Adjust specials
			List<String> toRemove = null;
			for (String modifiedTarget : finalTargets) {
				ClassNode modifiedMixin = adjuster.adjustSpecial(modifiedTarget, () -> {
					// Lazy, faster
					ClassNode modifiedClassNode = new ClassNode();
					// cNode now has modified targets
					cNode.accept(modifiedClassNode);
					return modifiedClassNode;
				});
				if (modifiedMixin != null) {
					if (specialTargets == null) {
						specialTargets = new HashMap<>();
						toRemove = new ArrayList<>();
					}
					toRemove.add(modifiedTarget);
					specialTargets.put(modifiedTarget, modifiedMixin);
				}
			}
			if (toRemove != null) {
				finalTargets.removeAll(toRemove);
			}

			modified = true;
		}

		if (modified) {
			// Rename the modified mixin class to a generated name
			String generatedMixin = getGeneratedMixinPrefix(mixinClassName);
			ClassRenamer.renameClass(cNode, generatedMixin);
			// Define the modified mixin class
			// TODO: Change the usage of ClassGenUtils to our implementation.
			genClass(cNode);
			// Done!
			generatedToOriginalMixins.put(generatedMixin, mixinClassName);
			originalMixins.add(mixinClassName);

			if (specialTargets != null) {
				for (Map.Entry<String, ClassNode> entry : specialTargets.entrySet()) {
					String target = entry.getKey();
					ClassNode modifiedMixin = entry.getValue();
					List<Object> values1 = Annotations.getInvisible(modifiedMixin, Mixin.class).values;
					ArrayList<String> targets1 = new ArrayList<>();
					targets1.add(target);
					values1.set(targetIndex, targets1);
					String generatedTarget = getGeneratedMixinPrefix(mixinClassName + "$TARGET_" + target.replace(".", "$_"));
					ClassRenamer.renameClass(modifiedMixin, generatedTarget);
					genClass(modifiedMixin);
					generatedToOriginalMixins.put(generatedTarget, mixinClassName);
				}
			}
		}
	}

	private void genClass(ClassNode node) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		node.accept(writer);
		byte[] bytes = writer.toByteArray();
		RUNTIME_MIXINS.put(node.name, bytes);
		IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
		((Extensions) transformer.getExtensions())
			.export(MixinEnvironment.getCurrentEnvironment(), node.name, false, node);
	}

	byte[] getByteCode(String name) {
		return RUNTIME_MIXINS.get(name.replace('.', '/'));
	}
}
