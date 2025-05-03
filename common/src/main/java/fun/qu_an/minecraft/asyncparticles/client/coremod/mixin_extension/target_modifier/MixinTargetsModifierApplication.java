package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.reflection.FieldReference;
import com.llamalad7.mixinextras.utils.ClassGenUtils;
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
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class MixinTargetsModifierApplication {
	static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared-target-modifier");
	static MixinTargetsModifierApplication INSTANCE;
	private static final FieldReference<String> pluginClassName;
	private static final FieldReference<IMixinService> mixinService;

	static {
		try {
			Class<?> mixinConfigClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
			pluginClassName = new FieldReference<>(mixinConfigClass, "pluginClassName");
			mixinService = new FieldReference<>(mixinConfigClass, "service");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * key: original mixin class name, value: target modifier
	 */
	static final Map<String, MixinTargetModifier> MODIFIERS = new HashMap<>();
	final Map<String, String> generatedToOriginalMixins = new HashMap<>();
	final Set<String> originalMixins = new HashSet<>();
	final MethodHandles.Lookup lookup;
	final IMixinConfigPlugin mixinSquaredPlugin;
	private final String generatedMixinPrefix;

	public static void init(MethodHandles.Lookup lookup, IMixinConfigPlugin mixinSquaredPlugin) {
		if (INSTANCE != null) {
			throw new IllegalStateException("TargetModifierApplication is already initialized");
		}
		INSTANCE = new MixinTargetsModifierApplication(lookup, mixinSquaredPlugin);
	}

	public static MixinTargetsModifierApplication getInstance() {
		return INSTANCE;
	}

	private MixinTargetsModifierApplication(MethodHandles.Lookup lookup, IMixinConfigPlugin mixinSquaredPlugin) {
		MixinCancellerRegistrar.register((targetClassName, mixinClassName) -> originalMixins.contains(mixinClassName));
		this.lookup = lookup;
		this.mixinSquaredPlugin = mixinSquaredPlugin;
		this.generatedMixinPrefix = lookup.lookupClass().getPackage().getName() +
									".MixinSquaredGenerated$";
	}

	public boolean shouldApplyMixin(String targetClassName, String generatedMixinClassName) {
		String mixinClassName = generatedToOriginalMixins.get(generatedMixinClassName);
		return MODIFIERS.get(mixinClassName).shouldApplyMixin(targetClassName);
	}

	public List<String> applyModifiers() {
		IMixinTransformer activeTransformer =
			(IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
		// FIXME: this is unsafe, fix it
		List<IMixinConfig> pendingConfigs = MixinTransformerExtension.tryAs(activeTransformer)
			.map(MixinTransformerExtension::getPendingConfigs)
			.orElseThrow(() -> new UnsupportedOperationException("Unsupported mixin transformer: " + activeTransformer.getClass()));
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
		IMixinService service = mixinService.get(mixinConfig);
		MixinServiceWrapper mixinServiceWrapper;
		if (!(service instanceof MixinServiceWrapper)) {
			LOGGER.info("Wrapping mixin service for {} so that we can modify target classes.", mixinConfig);
			mixinServiceWrapper = new MixinServiceWrapper(service);
			mixinService.set(mixinConfig, mixinServiceWrapper);
		} else {
			mixinServiceWrapper = (MixinServiceWrapper) service;
		}
		Map<String, IReferenceMapper> mappersCache = new HashMap<>();
		for (MixinTargetModifier modifier : MODIFIERS.values()) {
			applyModifier(mixinServiceWrapper, modifier, mappersCache);
		}
		List<String> list = new ArrayList<>(generatedToOriginalMixins.size());
		for (String s : generatedToOriginalMixins.keySet()) {
			String substring = s.substring(s.lastIndexOf('.') + 1);
			list.add(substring);
		}
		return list;
	}

	private void applyModifier(MixinServiceWrapper serviceWrapper,
							   MixinTargetModifier modifier,
							   Map<String, IReferenceMapper> mappersCache) {
		String mixinClassName = modifier.getMixinClassName();
		ClassNode cNode;
		try {
			cNode = serviceWrapper.getClassNode(mixinClassName, true);
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
		List<String> targets = new ArrayList<>();
		AnnotationNode aNode = Annotations.getInvisible(cNode, Mixin.class);
		List<Object> values = aNode.values;
		// The index of the "targets" key in the annotation
		int targetIndex = -1;
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
				// remove original "value"
				iterator.set(Collections.emptyList());
			} else if ("targets".equals(key)) {
				@SuppressWarnings("unchecked")
				List<String> originalTargets = (List<String>) value;
				if (originalTargets.isEmpty()) {
					continue;
				}
				targets.addAll(originalTargets);
				targetIndex = iterator.previousIndex();
				// We'll overwrite the original "targets" later
			}
		}
		List<String> unmodifiableList = Collections.unmodifiableList(targets);
		// Modify the target classes
		List<String> mixins = modifier.getTargets(unmodifiableList);
		if (mixins == null || mixins == unmodifiableList) {
			return;
		}
		// Remove the original targets
		// and write the modified to the annotation
		if (targetIndex > -1) {
			values.set(targetIndex, new ArrayList<>(mixins));
		} else {
			values.add("targets");
			values.add(new ArrayList<>(mixins));
		}
		String internalClassName = cNode.name;
		String refMapperConfig = modifier.getRefMapperConfig();
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
				if (!(o instanceof List<?> l) || l.isEmpty() || !(l.get(0) instanceof String)) {
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
						s1 = s1.substring(s1.indexOf(';') + 1);
					}
					iterator.set(s1);
				}
			}
		}
		String generatedMixin = generatedMixinPrefix +
								mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
		ClassRenamer.renameClass(cNode, generatedMixin);
		ClassGenUtils.defineClass(cNode, lookup);
		generatedToOriginalMixins.put(generatedMixin, mixinClassName);
		originalMixins.add(mixinClassName);
	}
}
