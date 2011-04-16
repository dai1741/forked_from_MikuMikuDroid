precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D sTex;
void main() {
  gl_FragColor = texture2D(sTex,  vTexCoord);
}