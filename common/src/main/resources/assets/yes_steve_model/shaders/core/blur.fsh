#version 330

uniform sampler2D Sampler0;
in vec2 v_local;
flat in vec2 u_rectSize;
flat in float u_radius;
flat in float u_blurRadius;
flat in vec4 u_tint;
flat in int u_mode;
flat in float u_pieInner;
flat in float u_pieOuter;
flat in float u_pieStart;
flat in float u_pieEnd;
out vec4 fragColor;

const float TAU = 6.28318530718;

float weight(float t, float log2radius, float gamma) {
    return exp(-gamma * pow(log2radius - t, 2.0));
}

vec4 sampleBlurred(vec2 uv, float radius, float gamma) {
    vec4 pix = vec4(0.0);
    float norm = 0.0;
    float lr = log2(max(radius, 1.0));
    for (float i = 0.0; i < 10.0; i += 0.5) {
        float k = weight(i, lr, gamma);
        pix += k * textureLod(Sampler0, clamp(uv, 0.0, 1.0), i);
        norm += k;
    }
    return pix * pow(norm, -0.95);
}

float roundSDF(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

float rectMask() {
    vec2 halfSize = u_rectSize * 0.5;
    vec2 local = v_local;
    float r = u_radius;
    vec2 bound = halfSize - u_radius - 1.0;

    float dist = roundSDF(local, bound, r);
    return 1.0 - smoothstep(0.0, 1.0, dist);
}

float pieMask() {
    vec2 d = v_local;
    float r = length(d);
    float feather = 1.0;
    float outerM = 1.0 - smoothstep(u_pieOuter - feather, u_pieOuter, r);
    float innerM = u_pieInner <= 0.0
        ? 1.0
        : smoothstep(u_pieInner - feather, u_pieInner, r);
    float radial = outerM * innerM;

    float span = u_pieEnd - u_pieStart;
    float angular;
    if (span >= TAU - 1e-4) {
        angular = 1.0;
    } else {
        float a = atan(d.y, d.x) - u_pieStart;
        a = a - floor(a / TAU) * TAU;
        float angFeather = feather / max(r, 1.0);
        float lo = smoothstep(0.0, angFeather, a);
        float hi = 1.0 - smoothstep(span - angFeather, span, a);
        angular = lo * hi;
    }
    return radial * angular;
}

void main() {
    float maskAlpha = (u_mode == 1) ? pieMask() : rectMask();
    if (maskAlpha < 0.001) discard;

    vec2 screenUV = gl_FragCoord.xy / vec2(textureSize(Sampler0, 0));
    vec4 blurred = sampleBlurred(screenUV, u_blurRadius, 6.0);

    fragColor = vec4(blurred.rgb * u_tint.rgb, maskAlpha * u_tint.a);
}
