precision highp float;
attribute vec4 aPosition;
attribute vec4 aNormal;
uniform vec3 uLightDir;
uniform mat4 uPMatrix;
varying vec3 vTexCoord;
void main() {
  float v;
  vec3 n;
  vec4 pos;

  pos = vec4(aPosition.xyz, 1.0);
  gl_Position = uPMatrix * pos;

  n = vec3(aPosition.w, aNormal.x, -aNormal.y);
  v = dot(n, uLightDir);
  v = v * 0.5 + 0.5;
  vTexCoord = vec3(aNormal.zw, v);
}
