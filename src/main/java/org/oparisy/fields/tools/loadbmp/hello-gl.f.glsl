#version 110

uniform sampler2D texture;

varying vec2 texcoord;

void main()
{
	//gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
	
    gl_FragColor = texture2D(texture, texcoord);
    
}