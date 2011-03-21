precision mediump float;
varying vec4 vTexCoord;
varying vec2 vSphereCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform sampler2D sSphere;
uniform bool bTexEn;
uniform bool bSpaEn;
uniform bool bSphEn;
uniform vec4 vColor;
uniform vec4 vSpec;
uniform vec4 vAmb;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 spa;
  vec4 sph;
  vec4 tmp;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  if(bSpaEn) {
	  spa = texture2D(sSphere, vSphereCoord);
  } else {
      spa = vec4(0, 0, 0, 0);
  }
  if(bSphEn) {
	  sph = texture2D(sSphere, vSphereCoord);
  } else {
      sph = vec4(1.0, 1.0, 1.0, 1.0);
  }
  if(bTexEn) {
    tex  = texture2D(sTex,  vTexCoord.xy);
  } else {
    tex  = vec4(1.0, 1.0, 1.0, 1.0);
  }
  spec = vSpec  * vTexCoord.w;
  tmp  = (vColor + spa) * toon + vAmb;
  gl_FragColor = tex * tmp * sph + spec;
}
