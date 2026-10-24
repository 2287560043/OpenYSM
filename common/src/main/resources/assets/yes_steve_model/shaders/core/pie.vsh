#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in uvec2 UV1;
in uvec2 UV2;
in float LineWidth;

out vec2 v_pos;
flat out float u_outerRadius;
flat out float u_innerRadius;
flat out float u_startAngle;
flat out float u_endAngle;
flat out vec4 u_color;
flat out float u_feather;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    v_pos = UV0;
    u_outerRadius = abs(UV0.x) - (LineWidth + 1.0);
    u_innerRadius = Position.z;
    u_startAngle = uintBitsToFloat(UV1.x | (UV1.y << 16));
    u_endAngle = uintBitsToFloat(UV2.x | (UV2.y << 16));
    u_color = Color;
    u_feather = LineWidth;
}
