#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;


uniform vec4 FogColor;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform sampler2D Sampler0;

out vec2 texCoord0;

bool approxEquals(vec3 v1, vec3 v2, float eps) {
  return all(greaterThan(
    vec3(eps),
    abs(v1 - v2)
  ));
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

vec4 corners = texture(Sampler0,vec2(0.0))*255.0;

    //checks corner pixel colours
    if(corners == vec4(1.0,2.0,3.0,255.0))
    {
        //checks custom biome fog colours
        if(approxEquals(FogColor.rgb * 255.0, vec3(50.0, 5.0, 97.0), 20.0))
        {
            texCoord0 = vec2(UV0.x,UV0.y/2+0.5);
        }
        else if(approxEquals(FogColor.rgb * 255.0, vec3(84.0, 107.0, 118.0), 20.0))
        {
            texCoord0 = vec2(UV0.x,UV0.y/2+0.5);
        }
        else if(approxEquals(FogColor.rgb * 255.0, vec3(91.0, 89.0, 89.0), 20.0))
        {
            texCoord0 = vec2(UV0.x,UV0.y/2+0.5);
        }
        else if(approxEquals(FogColor.rgb * 255.0, vec3(88.0, 12.0, 12.0), 20.0))
        {
            texCoord0 = vec2(UV0.x,UV0.y/2+0.5);
        }
        else if(approxEquals(FogColor.rgb * 255.0, vec3(51.0, 98.0, 119.0), 20.0))
        {
            texCoord0 = vec2(UV0.x,UV0.y/2+0.5);
        }
        else
        {
            texCoord0 = vec2(UV0.x,UV0.y/2);
        }
    }
    else
    {
        texCoord0 = UV0;
    }
}