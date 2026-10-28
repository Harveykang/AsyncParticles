package fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.vulkan;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.ParticleLayout;

public record VkLayoutResult(ParticleLayout particleLayout, int workgroupCount) {
}
