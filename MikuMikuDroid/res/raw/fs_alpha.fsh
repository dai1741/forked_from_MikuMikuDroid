precision mediump float;
varying vec4 vTexCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform vec4 uDif;
uniform vec4 uSpec;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 difamb;
  vec4 color;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  tex  = texture2D(sTex,  vTexCoord.xy);
  spec   = uSpec * vTexCoord.w;
  difamb = uDif  * toon;
  color = tex * difamb + spec;
  if(color.a == 0.0) {
  	discard;
  } else {
  	gl_FragColor = color;
  }
}