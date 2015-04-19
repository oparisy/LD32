#version 330 core

#define CHAR_WIDTH 5.
#define CHAR_HORIZONTAL_MARGIN 1.
#define FONT_MAP_WIDTH 512.
#define INIT_ASCII_VALUE 48.

// Interpolated values from the vertex shaders
in vec3 fragmentColor;
in vec2 UV;

uniform sampler2D myTextureSampler;
uniform int ascii;

// Ouput data
out vec4 color;

void main(){

	// Second term: ensure only one character is mapped, swap Y axis
	// Third term: offset to the proper character
	float xOffset = (ascii - INIT_ASCII_VALUE) * (CHAR_WIDTH + CHAR_HORIZONTAL_MARGIN);
	vec2 texCoord = UV * vec2(CHAR_WIDTH / FONT_MAP_WIDTH, -1) + vec2(xOffset / FONT_MAP_WIDTH, 0);
	vec3 texColor = texture2D(myTextureSampler, texCoord).rgb;
	if (texColor.r == 0) {
		// Black pixel in texture => white pixel on screen
		color = vec4(1,1,1,0);
	} else {
		// Transparent pixel
		color = vec4(0,0,0,1);
	} 
}