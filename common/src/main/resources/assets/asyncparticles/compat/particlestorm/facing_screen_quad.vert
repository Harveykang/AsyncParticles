#version 150

in vec3 OldPosition;
in vec3 Position;
in vec2 Size;
in vec2 UVMin;
in vec2 UVMax;
in vec4 Color;
in ivec2 Light;
in vec3 OldRotation;
in vec4 RotationAndMode;
in vec4 Facing;

uniform float PartialTick;
uniform vec3 CameraOffset;
uniform vec3 CameraLeft;
uniform vec3 CameraUp;

out vec3 Position_0;
out vec2 UV0_0;
out vec4 Color_0;
out ivec2 UV2_0;
out vec3 Position_1;
out vec2 UV0_1;
out vec4 Color_1;
out ivec2 UV2_1;
out vec3 Position_2;
out vec2 UV0_2;
out vec4 Color_2;
out ivec2 UV2_2;
out vec3 Position_3;
out vec2 UV0_3;
out vec4 Color_3;
out ivec2 UV2_3;

void main() {
    vec3 pos = mix(OldPosition, Position, PartialTick) + CameraOffset;
    // Match ParticleStorm's quaternion roll with the ordinary renderer's left/up formula.
    float nroll = -mix(OldRotation.z, RotationAndMode.z, PartialTick);

    // Adapted from gl_particle_transformfeedback.vert for rectangular Size and one tick color.
    // Original algorithm: MoePus / Flerovium, under LGPL before its merge into Sodium.
    // https://github.com/MoePus/Flerovium/blob/e7e43403191613c77b0beded2f12136897c5041c/src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java#L89
    // https://github.com/MoePus/Flerovium/blob/e7e43403191613c77b0beded2f12136897c5041c/LICENSE
    vec2 ss = sin(nroll) * Size;
    vec2 cs = cos(nroll) * Size;
    vec2 a = cs - ss.yx;
    vec2 b = cs + ss.yx;
    vec3 c = a.x * CameraLeft + b.y * CameraUp;
    vec3 d = b.x * CameraLeft - a.y * CameraUp;

    Position_0 = pos - c;// left bottom
    UV0_0 = UVMax; // maxU, maxV
    Color_0 = Color;
    UV2_0 = Light;

    Position_1 = pos - d;// left top
    UV0_1 = vec2(UVMax.x, UVMin.y); // maxU, minV
    Color_1 = Color;
    UV2_1 = Light;

    Position_2 = pos + c;// right top
    UV0_2 = UVMin; // minU, minV
    Color_2 = Color;
    UV2_2 = Light;

    Position_3 = pos + d;// right bottom
    UV0_3 = vec2(UVMin.x, UVMax.y); // minU, maxV
    Color_3 = Color;
    UV2_3 = Light;
}
