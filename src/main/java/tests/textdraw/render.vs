#version 330 core

in vec2 vertexPosition_modelspace;
in vec3 vertexColor;
in vec2 vertexUV;

out vec3 fragmentColor;
out vec2 UV;

uniform mat4 PVM;

void main(){
	// This is a position so we need an homogeneous coordinate with w=1
    gl_Position = PVM * vec4(vertexPosition_modelspace, 0, 1);
    
    // Color is interpolated
    fragmentColor = vertexColor;
    
    UV = vertexUV;
}
