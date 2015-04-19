#version 330 core

// Interpolated values from the vertex shaders
in vec3 fragmentColor;
in vec2 UV;

uniform sampler2D myTextureSampler;

// Ouput data
out vec4 color;

void main(){

	// Output color = color specified in the vertex shader, 
	// interpolated between all 3 surrounding vertices
	vec2 texCoord = UV * vec2(1,-1);
	vec3 texColor = texture2D(myTextureSampler, texCoord).rgb;
	if (texColor.r == 0) {
		// Black pixel in texture => white pixel on screen
		color = vec4(1,1,1,1);
	} else {
		// Transparent pixel
		color = vec4(0,0,0,0);
	}
	//color = fragmentColor * 
}