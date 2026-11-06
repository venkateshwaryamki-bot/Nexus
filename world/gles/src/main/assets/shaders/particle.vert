#version 300 es
// Energy motes orbiting the planet. Motion is computed entirely on the GPU from static attributes.
layout(location = 0) in vec3 aOrbit;   // radius, phase, y
layout(location = 1) in vec3 aParams;  // speed, size, colorMix
uniform mat4 uViewProj;
uniform float uTime;
uniform float uPointScale;
uniform float uBoost;
out float vMix;
out float vAlpha;
void main() {
    float a = aOrbit.y + uTime * aParams.x;
    float hr = sqrt(max(aOrbit.x * aOrbit.x - aOrbit.z * aOrbit.z, 0.01));
    float bob = sin(uTime * 0.7 + aOrbit.y * 3.0) * 0.03;
    vec3 p = vec3(cos(a) * hr, aOrbit.z + bob, sin(a) * hr);
    vec4 clip = uViewProj * vec4(p, 1.0);
    gl_Position = clip;
    gl_PointSize = aParams.y * uPointScale * (1.0 + uBoost) / max(clip.w, 0.1);
    vMix = aParams.z;
    vAlpha = (0.35 + 0.65 * (0.5 + 0.5 * sin(uTime * 2.0 + aOrbit.y * 9.0))) * (1.0 + uBoost * 1.5);
}
