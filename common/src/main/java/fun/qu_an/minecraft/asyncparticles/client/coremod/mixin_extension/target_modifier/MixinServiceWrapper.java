package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import com.llamalad7.mixinextras.utils.ClassGenUtils;
import fun.qu_an.minecraft.asyncparticles.client.coremod.PreLaunch;
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

@PreLaunch
public class MixinServiceWrapper implements IMixinService, IClassBytecodeProvider {
    private final IMixinService delegate;

    public MixinServiceWrapper(IMixinService delegate) {
        this.delegate = delegate;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        byte[] bytes = ClassGenUtils.getDefinitions().get(name.replace('/', '.'));
        if (bytes == null) {
            return delegate.getBytecodeProvider().getClassNode(name);
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
            return delegate.getBytecodeProvider().getClassNode(name, runTransformers);
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
            return delegate.getBytecodeProvider().getClassNode(name, runTransformers, readerFlags);
        }
        MixinTargetsModifierApplication.LOGGER.info("Redirecting IClassBytecodeProvider#getClassNode for {}", name);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, readerFlags);
        return node;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void prepare() {
        delegate.prepare();
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return delegate.getInitialPhase();
    }

    @Override
    public void offer(IMixinInternal internal) {
        delegate.offer(internal);
    }

    @Override
    public void init() {
        delegate.init();
    }

    @Override
    public void beginPhase() {
        delegate.beginPhase();
    }

    @Override
    public void checkEnv(Object bootSource) {
        delegate.checkEnv(bootSource);
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return delegate.getReEntranceLock();
    }

    @Override
    public IClassProvider getClassProvider() {
        return delegate.getClassProvider();
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return delegate.getTransformerProvider();
    }

    @Override
    public IClassTracker getClassTracker() {
        return delegate.getClassTracker();
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return delegate.getAuditTrail();
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return delegate.getPlatformAgents();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return delegate.getPrimaryContainer();
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return delegate.getMixinContainers();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return delegate.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return delegate.getSideName();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return delegate.getMinCompatibilityLevel();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return delegate.getMaxCompatibilityLevel();
    }

    @Override
    public ILogger getLogger(String name) {
        return delegate.getLogger(name);
    }
}
