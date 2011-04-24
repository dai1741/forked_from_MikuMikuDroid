precision mediump float;
attribute vec4 aPosition;
attribute vec3 aBlend;
uniform mat4 uPMatrix;
uniform mat4 uMBone[%d];
void main() {
  vec4 b1;
  vec4 b2;
  vec4 b;
  vec4 pos;
  mat4 m1;
  mat4 m2;

  pos = vec4(aPosition.xyz, 1.0);
  m1  = uMBone[int(aBlend.x)];
  m2  = uMBone[int(aBlend.y)];
  b1  = m1 * pos;
  b2  = m2 * pos;
  b   = mix(b2, b1, aBlend.z * 0.01);
  gl_Position = uPMatrix * b;
}