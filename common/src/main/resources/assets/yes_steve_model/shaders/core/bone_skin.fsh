#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
layout(std140) uniform ModelData {
    mat4 ModelPose;
    mat4 ModelNormal;
    ivec2 PackedOverlay;
    int BonesStart;
};

in vec2 texCoord0;
in vec4 vertexColor;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
flat in uint packedLight;
flat in uint cullable;
out vec4 fragColor;

void main() {
    if (cullable != 0u && !gl_FrontFacing) discard;
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a < 0.1) discard;
#if YSM_ALPHA_MODE == 1
    if (textureColor.a < 0.99) discard;
#elif YSM_ALPHA_MODE == 2
    if (textureColor.a >= 0.99) discard;
#elif YSM_ALPHA_MODE == 3
    fragColor = ColorModulator;
    return;
#endif
    vec4 color = textureColor * vertexColor * ColorModulator;
    vec4 overlay = texelFetch(Sampler1, PackedOverlay, 0);
    color.rgb = mix(overlay.rgb, color.rgb, overlay.a);
    color.rgb *= sample_lightmap(Sampler2, ivec2(packedLight & 65535u, packedLight >> 16)).rgb;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
