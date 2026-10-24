#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in uint BoneIndex;
in uint Cullable;

uniform usamplerBuffer Bones;
layout(std140) uniform ModelData {
    mat4 ModelPose;
    mat4 ModelNormal;
    ivec2 PackedOverlay;
    int BonesStart;
};

out vec2 texCoord0;
out vec4 vertexColor;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
flat out uint packedLight;
flat out uint cullable;

void main() {
    int offset = BonesStart + int(BoneIndex) * 9;
    uvec4 flags = texelFetch(Bones, offset + 8);
    cullable = Cullable;
    packedLight = flags.x;
    texCoord0 = UV0;
    sphericalVertexDistance = 0.0;
    cylindricalVertexDistance = 0.0;
    vertexColor = vec4(0.0);
    if (flags.y != 0u) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }
    mat4 bonePose = mat4(uintBitsToFloat(texelFetch(Bones, offset)), uintBitsToFloat(texelFetch(Bones, offset + 1)), uintBitsToFloat(texelFetch(Bones, offset + 2)), uintBitsToFloat(texelFetch(Bones, offset + 3)));
    mat4 boneNormal = mat4(uintBitsToFloat(texelFetch(Bones, offset + 4)), uintBitsToFloat(texelFetch(Bones, offset + 5)), uintBitsToFloat(texelFetch(Bones, offset + 6)), uintBitsToFloat(texelFetch(Bones, offset + 7)));
    vec4 position = ModelPose * bonePose * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * position;
    vec3 normal = normalize((ModelNormal * boneNormal * vec4(Normal, 0.0)).xyz);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normal, vec4(1.0));
    sphericalVertexDistance = fog_spherical_distance(position.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(position.xyz);
}
