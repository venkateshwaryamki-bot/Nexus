#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in float aEmissive;
uniform mat4 uViewProj;
uniform mat4 uModel;
out vec3 vWorld;
out vec3 vNormal;
out vec3 vColor;
out float vEmissive;
out vec3 vLocal;
void main() {
    vec4 w = uModel * vec4(aPos, 1.0);
    vWorld = w.xyz;
    vLocal = aPos;
    vNormal = mat3(uModel) * aNormal;
    vColor = aColor;
    vEmissive = aEmissive;
    gl_Position = uViewProj * w;
}
