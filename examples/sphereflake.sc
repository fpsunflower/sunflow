image {
	resolution 1920 1080
	aa 0 2
	filter gaussian
}

trace-depths {
	diff 1
	refl 1
	refr 10
}


% |persp|perspShape
camera {
	type   pinhole
	eye    -5 0 -0.9
	target 0 0 0.2
	up     0 0 1
	fov    60
	aspect 1.77777777777
}

gi { type path samples 16 }

shader {
  name simple1
  type diffuse
  diff 0.5 0.5 0.5
}

shader {
  name glass
  type glass
  eta 1.333
  color 0.8 0.8 0.8
  absorbtion.distance 15
  absorbtion.color { "sRGB nonlinear" 0.2 0.7 0.2 }
}

light {
  type sunsky
  up 0 0 1
  east 0 1 0
  sundir -1 1 0.2
  turbidity 2
  samples 32
}

object {
	shader simple1
	type sphereflake
	name left
	level 9
}

object {
    shader simple1
    type plane
    p 0 0 -1
    n 0 0 1
}

shader { name ao type amb-occ diff 1 1 1 }
% override ao true
