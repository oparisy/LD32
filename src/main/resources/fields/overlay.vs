#version 330 core

in vec2 vertexPosition_modelspace;
in vec2 vertexUV;

out vec2 UV;

uniform mat4 PVM;
uniform mat4 charPos;

void main() {
	// This is a position so we need an homogeneous coordinate with w=1
    gl_Position = PVM * charPos * vec4(vertexPosition_modelspace, 0, 1);
    
    UV = vertexUV;
}
