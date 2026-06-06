#version 150
#extension GL_ARB_explicit_attrib_location : enable
//.add("oPosition", VertexFormatElement.POSITION)
//		.add("Position", VertexFormatElement.POSITION)
//		.add("Sizes", VertexFormatElement.UV0)
//		.add("oUV0", VertexFormatElement.UV0)
//		.add("UV0", VertexFormatElement.UV0)
//		.add("oColor", VertexFormatElement.COLOR)
//		.add("Color", VertexFormatElement.COLOR)
//		.add("Light", VertexFormatElement.UV2)
//		.add("Rolls", VertexFormatElement.UV0)
layout(location = 0) in vec3 oPosition;
layout(location = 1) in vec3 Position;
layout(location = 2) in vec2 Sizes;
layout(location = 3) in vec2 UVMin;
layout(location = 4) in vec2 UVMax;
layout(location = 5) in vec4 oColor;
layout(location = 6) in vec4 Color;
layout(location = 7) in ivec2 Light;
layout(location = 8) in vec2 Rolls;

//#if defined(GL_ARB_uniform_buffer_object) || __VERSION__ >= 140
//layout(std140) uniform FrameInfo {
//float PartialTick;
//vec3 CameraLeft;
//vec3 CameraUp;
//vec3 PartialCameraPos;
//};
//#else
uniform float PartialTick;
uniform vec3 CameraLeft;
uniform vec3 CameraUp;
uniform vec3 PartialCameraPos;
//#endif

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

// geom shader may be slower but more testing needed
void main() {
    vec3 pos = mix(oPosition, Position, PartialTick) + PartialCameraPos;
    float nroll = mix(Rolls.x, Rolls.y, PartialTick);
    float size = mix(Sizes.x, Sizes.y, PartialTick);
    vec4 color = mix(oColor, Color, PartialTick);

    // code from flerovium but on gpu
    // these code were under LGPL before merged into Sodium, safe to use.
    // Author: MoePus
    // Link: https://github.com/MoePus/Flerovium/blob/e7e43403191613c77b0beded2f12136897c5041c/src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java#L89
    // Lisence: https://github.com/MoePus/Flerovium/blob/e7e43403191613c77b0beded2f12136897c5041c/LICENSE
    float sinRoll = sin(nroll);
    float cosRoll = cos(nroll);
    vec3 left = cosRoll * CameraLeft + sinRoll * CameraUp;
    vec3 up = -sinRoll * CameraLeft + cosRoll * CameraUp;

    Position_0 = -(left + up) * size + pos;// left bottom
    UV0_0 = UVMax; // maxU, maxV
    Color_0 = color;
    UV2_0 = Light;

    Position_1 = -(left - up) * size + pos;// left top
    UV0_1 = vec2(UVMax.x, UVMin.y); // maxU, minV
    Color_1 = color;
    UV2_1 = Light;

    Position_2 =  (left + up) * size + pos;// right top
    UV0_2 = UVMin; // minU, minV
    Color_2 = color;
    UV2_2 = Light;

    Position_3 =  (left - up) * size + pos;// right bottom
    UV0_3 = vec2(UVMin.x, UVMax.y); // minU, maxV
    Color_3 = color;
    UV2_3 = Light;
}
