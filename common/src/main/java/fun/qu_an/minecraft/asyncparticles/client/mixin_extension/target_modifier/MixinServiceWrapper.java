package fun.qu_an.minecraft.asyncparticles.client.mixin_extension.target_modifier;

import com.llamalad7.mixinextras.utils.ClassGenUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class MixinServiceWrapper implements IMixinService, IClassBytecodeProvider {
    private final IMixinService service;

    public MixinServiceWrapper(IMixinService service) {
        this.service = service;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        byte[] bytes = ClassGenUtils.getDefinitions().get(name.replace('/', '.'));
        if (bytes == null) {
            return service.getBytecodeProvider().getClassNode(name);
        }
        MixinTargetsModifierApplication.LOGGER.info("Redirecting IClassBytecodeProvider#getClassNode for {}", name);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        byte[] bytes = ClassGenUtils.getDefinitions().get(name.replace('/', '.'));
        if (bytes == null) {
            return service.getBytecodeProvider().getClassNode(name, runTransformers);
        }
        MixinTargetsModifierApplication.LOGGER.info("Redirecting IClassBytecodeProvider#getClassNode for {}", name);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        byte[] bytes = ClassGenUtils.getDefinitions().get(name.replace('/', '.'));
        if (bytes == null) {
            return service.getBytecodeProvider().getClassNode(name, runTransformers, readerFlags);
        }
        MixinTargetsModifierApplication.LOGGER.info("Redirecting IClassBytecodeProvider#getClassNode for {}", name);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, readerFlags);
        return node;
    }

    @Override
    public String getName() {
        return service.getName();
    }

    @Override
    public boolean isValid() {
        return service.isValid();
    }

    @Override
    public void prepare() {
        service.prepare();
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return service.getInitialPhase();
    }

    @Override
    public void offer(IMixinInternal internal) {
        service.offer(internal);
    }

    @Override
    public void init() {
        service.init();
    }

    @Override
    public void beginPhase() {
        service.beginPhase();
    }

    @Override
    public void checkEnv(Object bootSource) {
        service.checkEnv(bootSource);
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return service.getReEntranceLock();
    }

    @Override
    public IClassProvider getClassProvider() {
        return service.getClassProvider();
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return service.getTransformerProvider();
    }

    @Override
    public IClassTracker getClassTracker() {
        return service.getClassTracker();
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return service.getAuditTrail();
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return service.getPlatformAgents();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return service.getPrimaryContainer();
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return service.getMixinContainers();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return service.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return service.getSideName();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return service.getMinCompatibilityLevel();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return service.getMaxCompatibilityLevel();
    }

    @Override
    public ILogger getLogger(String name) {
        return service.getLogger(name);
    }
}
