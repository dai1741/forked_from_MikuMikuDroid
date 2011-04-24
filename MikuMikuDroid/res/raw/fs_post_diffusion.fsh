precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D sTex;
uniform sampler2D sSphere;
void main() {
  vec4 color;
  vec4 color_org;
  vec4 color_src;
  
  color_org = texture2D(sTex, vTexCoord);
  color     = texture2D(sSphere, vTexCoord);
  
//  color = color * color;
//  color_src = color_org * color_org;
  color_src = color_org;
  
  color = color + color_src - color * color_src;
  
  if(color_src.g > color.g) {
  	color = color_src;
  }
  color = color * color;
  color.a = color_org.a;
  gl_FragColor = color_org + (color - color_org) * 0.5;
}
