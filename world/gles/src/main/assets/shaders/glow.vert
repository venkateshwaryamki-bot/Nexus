#version 300 es
// Camera-facing glow sprites: a cheap stand-in for bloom on emissive hot spots.
layout(location = 0) in vec4 aPosSize; // xyz, world size
layout(location = 1) in vec4 aColorPhase; // rgb, phase
uniform mat4 uViewProj;
uniform mat4 uModel;
uniform float uViewportHeight;
uniform float uProjScale; // projection[1][1]
uniform float uTime;
uniform float uBoost;
out vec3 vColor;
void main() {
    vec4 clip = uViewProj * uModel * vec4(aPosSize.xyz, 1.0);
    gl_Position = clip;
    float pulse = 0.85 + 0.15 * sin(uTime * 2.5 + aColorPhase.w);
    gl_PointSize = aPosSize.w * uProjScale * uViewportHeight * 0.5 / max(clip.w, 0.1) * pulse * (1.0 + uBoost * 0.6);
    vColor = aColorPhase.rgb * (1.0 + uBoost);
}
