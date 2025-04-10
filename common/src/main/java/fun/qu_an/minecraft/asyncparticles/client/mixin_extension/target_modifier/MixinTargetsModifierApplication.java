package fun.qu_an.minecraft.asyncparticles.client.mixin_extension.target_modifier;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.bawnorton.mixinsquared.reflection.FieldReference;
import com.llamalad7.mixinextras.utils.ClassGenUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IClassBytecodeProvider;
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
    private static final FieldReference<List<IMixinConfig>> pendingConfigs;
    private static final FieldReference<?> mixinProcessor;

    static {
        try {
            Class<?> mixinConfigClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
            pluginClassName = new FieldReference<>(mixinConfigClass, "pluginClassName");
            mixinService = new FieldReference<>(mixinConfigClass, "service");
            Class<?> mixinProcessorClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinProcessor");
            pendingConfigs = new FieldReference<>(mixinProcessorClass, "pendingConfigs");
            Class<?> mixinTransformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
            mixinProcessor = new FieldReference<>(mixinTransformerClass, "processor");
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

    public List<String> applyModifiers() {
        IMixinTransformer activeTransformer =
            (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        // FIXME: this is unstable, but it works for now
        List<IMixinConfig> pendingConfigs = MixinTargetsModifierApplication.pendingConfigs.get(
            mixinProcessor.get(activeTransformer));
        IMixinConfig mixinConfig = null;
        String pluginClass = mixinSquaredPlugin.getClass().getName();
        for (IMixinConfig config : pendingConfigs) {
            mixinConfig = config;
            String aPlugin = pluginClassName.get(mixinConfig);
            if (pluginClass.equals(aPlugin)) {
                break;
            }
        }
        assert pluginClass.equals(pluginClassName.get(mixinConfig));
        IMixinService service = mixinService.get(mixinConfig);
        MixinServiceWrapper mixinServiceWrapper;
        if (!(service instanceof MixinServiceWrapper)) {
            LOGGER.info("Wrapping mixin service for {} so that we can modify target classes.", mixinConfig);
            mixinServiceWrapper = new MixinServiceWrapper(service);
            mixinService.set(mixinConfig, mixinServiceWrapper);
        } else {
            mixinServiceWrapper = (MixinServiceWrapper) service;
        }
        IClassBytecodeProvider bytecodeProvider = mixinServiceWrapper.getBytecodeProvider();
        for (MixinTargetModifier modifier : MODIFIERS.values()) {
            applyModifier(bytecodeProvider, modifier);
        }
        List<String> list = new ArrayList<>(generatedToOriginalMixins.size());
        for (String s : generatedToOriginalMixins.keySet()) {
            String substring = s.substring(s.lastIndexOf('.') + 1);
            list.add(substring);
        }
        return list;
    }

    public boolean shouldApplyMixin(String targetClassName, String generatedMixinClassName) {
        String mixinClassName = generatedToOriginalMixins.get(generatedMixinClassName);
        return MODIFIERS.get(mixinClassName).shouldApplyMixin(targetClassName);
    }

    private void applyModifier(IClassBytecodeProvider bytecodeProvider, MixinTargetModifier modifier) {
        String mixinClassName = modifier.getMixinClassName();
        ClassNode cNode;
        try {
            cNode = bytecodeProvider.getClassNode(mixinClassName);
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
        List<String> targets = new ArrayList<>();
        AnnotationNode aNode = Annotations.getInvisible(cNode, Mixin.class);
        List<Object> values = aNode.values;
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
                // remove original class targets
                iterator.set(Collections.emptyList());
            } else if ("targets".equals(key)) {
                @SuppressWarnings("unchecked")
                List<String> originalTargets = (List<String>) value;
                if (originalTargets.isEmpty()) {
                    continue;
                }
                targets.addAll(originalTargets);
                targetIndex = iterator.previousIndex();
            }
        }
        List<String> unmodifiableList = Collections.unmodifiableList(targets);
        List<String> mixins = modifier.getTargets(unmodifiableList);
        if (mixins == null || mixins == unmodifiableList) {
            return;
        }
        if (targetIndex > -1) {
            values.set(targetIndex, new ArrayList<>(mixins));
        } else {
            values.add("targets");
            values.add(new ArrayList<>(mixins));
        }
        String generatedMixin = generatedMixinPrefix +
                                mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        ClassRenamer.renameClass(cNode, generatedMixin);
        ClassGenUtils.defineClass(cNode, lookup);
        generatedToOriginalMixins.put(generatedMixin, mixinClassName);
        originalMixins.add(mixinClassName);
    }
}
