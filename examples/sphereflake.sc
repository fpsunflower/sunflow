image {
	resolution 2048 2048
	aa 0 2
	filter gaussian
}

trace-depths {
	diff 1
	refl 0
	refr 0
}


% |persp|perspShape
camera {
	type   pinhole
	eye    -5 0 0
	target 0 0 0
	up     0 1 0
	fov    58
	aspect 1
}

% background { color { "sRGB nonlinear" 0.5 0.5 0.5 } }

gi { type path samples 16 }

shader {
  name simple1
  type diffuse
  diff { "sRGB nonlinear" 0.5 0.5 0.5 }
}

light {
  type spherical
  color { "sRGB nonlinear" 1 1 .6 }
  radiance 60
  center -5 7 5
  radius 2
  samples 8
}

light {
  type spherical
  color { "sRGB nonlinear"  .6 .6 1 }
  radiance 20
  center -15 -17 -15
  radius 5
  samples 8
}

object {
	shader simple1
	transform { scaleu 1.25 /* rotatey 45 rotatex -55 */  }
	type sphereflake
	name left
	level 9
}

shader { name ao type amb-occ diff 1 1 1 }
% override ao true
