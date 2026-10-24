#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in uvec2 UV1;
in uvec2 UV2;
in float LineWidth;

out vec2 v_local;
flat out vec2 u_rectSize;
flat out float u_radius;
flat out float u_blurRadius;
flat out vec4 u_tint;
flat out int u_mode;
flat out float u_pieInner;
flat out float u_pieOuter;
flat out float u_pieStart;
flat out float u_pieEnd;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    v_local = UV0;
    float a = uintBitsToFloat(UV1.x | (UV1.y << 16));
    float b = uintBitsToFloat(UV2.x | (UV2.y << 16));
    u_rectSize = vec2(a, b);
    u_radius = Position.z;
    u_blurRadius = LineWidth;
    u_tint = Color;
    u_mode = Position.z < 0.0 ? 1 : 0;
    u_pieInner = -Position.z - 1.0;
    u_pieOuter = abs(UV0.x) - 1.0;
    u_pieStart = a;
    u_pieEnd = b;
}
