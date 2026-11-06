#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in float aIntensity;
uniform mat4 uViewProj;
uniform mat4 uModel;
out float vIntensity;
out float vPhase;
void main() {
    vIntensity = aIntensity;
    vPhase = aPos.x * 7.0 + aPos.z * 11.0 + aPos.y * 5.0;
    gl_Position = uViewProj * uModel * vec4(aPos, 1.0);
}
