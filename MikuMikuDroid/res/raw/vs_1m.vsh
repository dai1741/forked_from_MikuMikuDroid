precision highp float;
attribute vec4 aPosition;
attribute vec4 aNormal;
attribute vec3 aBlend;
uniform vec3 uLightDir;
uniform mat4 uPMatrix;
uniform mat4 uMBone[%d];
uniform float uPow;
varying vec3 vTexCoord;
void main() {
  float v;
  vec3 n;
  vec4 pos;
  mat4 m;

  pos = vec4(aPosition.xyz, 1.0);
  m   = uMBone[int(aBlend.x)];
  gl_Position = uPMatrix * m * pos;

  n = mat3(m[0].x, m[1].x, m[2].x, m[0].y, m[1].y, m[2].y, m[0].z, m[1].z, m[2].z) * vec3(aPosition.w, aNormal.x, -aNormal.y);
  v = dot(n, uLightDir);
  v = v * 0.5 + 0.5;
  vTexCoord   = vec3(aNormal.zw, v);
}