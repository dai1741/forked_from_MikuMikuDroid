precision mediump float;
varying vec4 vTexCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform bool bTexEn;
uniform vec4 vColor;
uniform vec4 vSpec;
uniform vec4 vAmb;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 tmp;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  if(bTexEn) {
    tex  = texture2D(sTex,  vTexCoord.xy);
  } else {
    tex  = vec4(1, 1, 1, 1);
  }
  spec = vSpec  * vTexCoord.w;
  tmp  = vColor * toon + vAmb;
  gl_FragColor = tex * tmp + spec;
}
