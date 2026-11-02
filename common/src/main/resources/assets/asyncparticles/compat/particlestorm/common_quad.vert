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
uniform vec4 CameraRotation;
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

vec4 multiply(vec4 a, vec4 b) {
    return vec4(a.w * b.xyz + b.w * a.xyz + cross(a.xyz, b.xyz), a.w * b.w - dot(a.xyz, b.xyz));
}

vec3 rotate(vec4 q, vec3 v) {
    return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v) / dot(q, q);
}

vec4 lookAt(vec3 direction) {
    vec3 z = normalize(direction);
    vec3 x = normalize(cross(vec3(0.0, 1.0, 0.0), z));
    mat3 m = mat3(x, cross(z, x), z);
    float trace = m[0][0] + m[1][1] + m[2][2];
    if (trace >= 0.0) {
        float s = 2.0 * sqrt(trace + 1.0);
        return vec4(m[1][2] - m[2][1], m[2][0] - m[0][2], m[0][1] - m[1][0], 0.25 * s * s) / s;
    }
    if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
        float s = 2.0 * sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]);
        return vec4(0.25 * s * s, m[0][1] + m[1][0], m[0][2] + m[2][0], m[1][2] - m[2][1]) / s;
    }
    if (m[1][1] > m[2][2]) {
        float s = 2.0 * sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]);
        return vec4(m[0][1] + m[1][0], 0.25 * s * s, m[1][2] + m[2][1], m[2][0] - m[0][2]) / s;
    }
    float s = 2.0 * sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]);
    return vec4(m[0][2] + m[2][0], m[1][2] + m[2][1], 0.25 * s * s, m[0][1] - m[1][0]) / s;
}

void writeQuad(vec3 center, vec3 right, vec3 up) {
    Position_0 = center + right - up;
    UV0_0 = UVMax;
    Color_0 = Color;
    UV2_0 = Light;
    Position_1 = center + right + up;
    UV0_1 = vec2(UVMax.x, UVMin.y);
    Color_1 = Color;
    UV2_1 = Light;
    Position_2 = center - right + up;
    UV0_2 = UVMin;
    Color_2 = Color;
    UV2_2 = Light;
    Position_3 = center - right - up;
    UV0_3 = vec2(UVMin.x, UVMax.y);
    Color_3 = Color;
    UV2_3 = Light;
}

void main() {
    // 0: camera roll only, 1: camera XYZ, 2: camera Y, 3-5: look-at, 6: fixed facing.
    int mode = int(RotationAndMode.w);
    vec3 center = mix(OldPosition, Position, PartialTick) + CameraOffset;

    if (mode == 0) {
        // Camera axes are already cached; a Z-only roll just mixes these two vectors.
        float roll = mix(OldRotation.z, RotationAndMode.z, PartialTick);
        float s = sin(roll);
        float c = cos(roll);
        vec3 right = (-CameraLeft * c + CameraUp * s) * Size.x;
        vec3 up = (CameraLeft * s + CameraUp * c) * Size.y;
        writeQuad(center, right, up);
        return;
    }

    vec4 q = Facing;
    if (mode == 1) {
        q = CameraRotation;
    } else if (mode == 2) {
        q = vec4(0.0, CameraRotation.y, 0.0, CameraRotation.w);
    } else if (mode == 3 || mode == 4) {
        q = lookAt(-center);
        if (mode == 4) {
            q.xz = vec2(0.0);
        }
    } else if (mode == 5) {
        vec3 localCamera = rotate(vec4(-q.xyz, q.w), -center);
        float angle = 0.5 * atan(-localCamera.y, localCamera.z);
        q = multiply(q, vec4(sin(angle), 0.0, 0.0, cos(angle)));
    }
    vec3 angle = 0.5 * mix(OldRotation, RotationAndMode.xyz, PartialTick);
    // Keep interpolation continuous when any current angle reaches zero.
    q = multiply(q, vec4(sin(angle.x), 0.0, 0.0, cos(angle.x)));
    q = multiply(q, vec4(0.0, sin(angle.y), 0.0, cos(angle.y)));
    q = multiply(q, vec4(0.0, 0.0, sin(angle.z), cos(angle.z)));

    vec3 right = rotate(q, vec3(Size.x, 0.0, 0.0));
    vec3 up = rotate(q, vec3(0.0, Size.y, 0.0));
    writeQuad(center, right, up);
}
