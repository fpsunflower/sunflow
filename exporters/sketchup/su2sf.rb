#su2sf
# Written by Tony Wooster, though not quite done yet, 24 MAR 07
#
# Edited by dandruff, added glas, transparent and phong support as
# well as tre different sizes of output picture, 9 MAY 07
#
# Added by D. Bur:
# dialog boxes for render, background, camera, GI settings
# ground plane, sky/background color

require 'sketchup.rb'

#-------------------- Slice float to x decimals
class Float
    def prec( x )
        sprintf( "%.*f", x, self ).to_f
    end
end

class SU2SF
  #-- Constants --
  GROUP_PREFIX = "Group_"
  CDEF_PREFIX = "Defn_"
  CINST_PREFIX = "Inst_"
  MATERIAL_PREFIX = "Shader_"
  SCALE_FACTOR = 0.0254
  SCALE_TRANSFORM = Geom::Transformation.scaling(SCALE_FACTOR,SCALE_FACTOR,SCALE_FACTOR)
  GENERIC_NAME = "Generic"
  DEFAULT_MATERIAL = MATERIAL_PREFIX + "Default"
  MODEL_NAME = "BaseModel"
  PRECISION = "%.8f"
  
  #-- Accessible Member Variables --
  attr_accessor :scene_settings
  
  #-- Public Functions --
  def initialize
    reset_state
    @scene_settings = {
        "aa_min" => 1,
        "aa_max" => 2,
        "filter" => "mitchell",
        "gi" => nil,
        "sky_samples" => 32,
        "sky_turbidity" => 5,
        "trace-diff" => 4,
        "trace-refl" => 4,
        "trace-refr" => 4,
        "image_width" => 800,
        "image_height" => 600,
    }
    @export_textures = true
  end
  
  def reset_state
    @stream = nil
    @exported_names = []
    @component_tree = {}
    @component_shaders = {}
    @empty_components = []
    @exported_components = {}
    @exported_materials = {}
  end

  def output_begin( stream, name=nil )
    @stream = stream
    if name
      @stream.print "%% Model Name: {name}\n"
      set_status "Beginning export of model #{name}"
    else
      set_status "Beginning export of model..."
    end
    @stream.print "%% Exported from Sketchup\n\n"
  end
  
  def output_image_settings( view )
    set_status("Exporting image settings...")
    @stream.print "image {",
      "\n\tresolution #{@scene_settings["image_width"]} #{@scene_settings["image_height"]}",
      #"\n\taa #{@scene_settings["aa_min"]} #{@scene_settings["aa_max"]}",
      "\n\taa " + $aa_min + " " + $aa_max,
      "\n\tfilter #{@scene_settings["filter"]}",
      "\n}\n\n" 
    
    if @scene_settings["gi"]
      @stream.print "gi { #{@scene_settings["gi"]} }\n\n"
    end
    #TODO output trace settings here
  end
 
  def output_background_settings
    case $background_type
      when "Solid color"
        set_status("Exporting background color...")
        bc=Sketchup.active_model.rendering_options["BackgroundColor"]
        backc = normalize_color(bc)
        skyc=Sketchup.active_model.rendering_options["SkyColor"]
        @stream.print "background {"
        @stream.print "\n\tcolor { \"sRGB nonlinear\" " + backc + " }"
        @stream.print"\n}\n\n"
      when "Sky"
        shinfo=Sketchup.active_model.shadow_info
        sd = shinfo["SunDirection"]
        # FIXME: adjust for NorthAngle
        set_status("Exporting sun...")
        @stream.print "light {",
        "\n\ttype sunsky",
        "\n\tup 0 0 1",
        "\n\teast 1 0 0",
        "\n\tsundir #{PRECISION % (sd.x)} #{PRECISION % (sd.y)} #{PRECISION % (sd.z)}",
        "\n\tturbidity #{@scene_settings["sky_turbidity"]}",
        "\n\tsamples #{@scene_settings["sky_samples"]}",
        "\n}\n\n"
      end
  end
  
  def output_ground_plane
    if $ground_plane == "Yes"
      gc=Sketchup.active_model.rendering_options["GroundColor"]
      groundc = normalize_color(gc)
      set_status("Exporting Ground plane...")
      @stream.print "shader {",
      "\n\tname \"Shader_ground\"",
      "\n\ttype diffuse",
      "\n\tdiff " + groundc,
      "\n}\n\n"
      @stream.print "object {",
      "\n\tshader \"Shader_ground\"",
      "\n\ttype plane",
      "\n\tp 0 0 0",
      "\n\tn 0 0 1",
      "\n}\n\n"
    end
  end
  
  def output_sky_settings( shinfo )
    # if a solid color background, then set a directional light instead of sky
    if $background_type == "Solid color"
      sd = shinfo["SunDirection"]
      set_status("Exporting default directional light...")
      @stream.print "light {",
      "\n\ttype directional",
      "\n\tsource " + (sd.x*2000).to_s + " " + (sd.y*2000).to_s + " " + (sd.z*2000).to_s,
      "\n\ttarget 0 0 0",
      "\n\tradius 2000",
      "\n\temit { \"sRGB nonlinear\" 1.000 1.000 1.000 }",
      "\n}\n\n"
    else    
      #sd = shinfo["SunDirection"]
      ## FIXME: adjust for NorthAngle
      #set_status("Exporting sun...")
      #@stream.print "light {",
      #"\n\ttype sunsky",
      #"\n\tup 0 0 1",
      #"\n\teast 1 0 0",
      #"\n\tsundir #{PRECISION % (sd.x)} #{PRECISION % (sd.y)} #{PRECISION % (sd.z)}",
      #"\n\tturbidity #{@scene_settings["sky_turbidity"]}",
      #"\n\tsamples #{@scene_settings["sky_samples"]}",
      #"\n}\n\n"
    end
  end

  def output_sky_settingslow( shinfo )    
    sd = shinfo["SunDirection"]
    # FIXME: adjust for NorthAngle
    set_status("Exporting sun...")
    @stream.print "light {",
      "\n\ttype sunsky",
      "\n\tup 0 0 1",
      "\n\teast 1 0 0",
      "\n\tsundir #{PRECISION % (sd.x)} #{PRECISION % (sd.y)} #{PRECISION % (sd.z)}",
      "\n\tturbidity #{@scene_settings["sky_turbidity"]}",
      "\n\tsamples #{((@scene_settings["sky_samples"])/2)}",
      "\n}\n\n"
  end
  
  def output_camera( camera )
    #aspect = camera.aspect_ratio
    #aspect = (@scene_settings["image_width"].to_f / @scene_settings["image_height"].to_f) if !aspect or aspect <= 0
    set_status("Exporting camera...")
    
    vfovdeg = camera.fov
    view=Sketchup.active_model.active_view
    w = Float(view.vpwidth)
	  h = Float(view.vpheight)
	  vfovrad = vfovdeg*Math::PI/180.0
	  f = (h/2)/Math.tan(vfovrad/2.0)
	  hfovrad = 2.0*Math.atan2((w/2),f)
	  hfovdeg = hfovrad*180.0/Math::PI
  
    @stream.print "camera {\n\ttype " + $camera_type.downcase
    @stream.print "\n\teye\t" + camera.eye.x.to_m.to_f.to_s + " " + camera.eye.y.to_m.to_f.to_s + " " + camera.eye.z.to_m.to_f.to_s
    @stream.print "\n\ttarget\t" + camera.target.x.to_m.to_f.to_s + " " + camera.target.y.to_m.to_f.to_s + " " + camera.target.z.to_m.to_f.to_s
    @stream.print "\n\tup\t" + camera.up.x.to_s + " " + camera.up.y.to_s + " " + camera.up.z.to_s
    
    case $camera_type
      when "Pinhole"
        @stream.print "\n\tfov\t" + hfovdeg.to_s #camera.fov.to_s
        @stream.print "\n\taspect\t" + ($image_width.to_f / $image_height.to_f).to_s
        if $pinhole_shift_x != "0" or $pinhole_shift_y != "0"
          @stream.print "\n\tshift " + $pinhole_shift_x + " " + $pinhole_shift_y
        end
      when "Thinlens"
        @stream.print "\n\tfov\t" + hfovdeg.to_s #camera.fov.to_s
        @stream.print "\n\taspect\t" + ($image_width.to_f / $image_height.to_f).to_s
        @stream.print "\n\tfdist " + $thinlens_fdist
        @stream.print "\n\tlensr " + $thinlens_lensr
        if $thinlens_sides != "0"
          @stream.print "\n\tsides " + $thinlens_sides
          @stream.print "\n\trotation " + $thinlens_rotat
        end
    end
    @stream.print "\n}\n\n"
  end
  
  def output_gi
    return if $gi_type == "None"
    case $gi_type
      when "Instant GI"
        @stream.print "gi {",
        "\n\ttype igi",
        "\n\tsamples 64",
        "\n\tsets 1",
        "\n\tb 0.01",
        "\n\tbias-samples 10",
        "\n}\n\n"
      when "Irradiance Caching"
        @stream.print "gi {",
        "\n\ttype irr-cache",
        "\n\tsamples 512",
        "\n\ttolerance 0.01",
        "\n\tspacing 0.05 5.0",
        "\n\tglobal 1000000 grid 100 0.75",
        "\n}\n\n"
      when "Path Tracing"
        @stream.print "gi {",
        "\n\ttype path",
        "\n\tsamples 32",
        "\n}\n\n"
      when "Ambient Occlusion"
        @stream.print "gi {",
        "\n\ttype ambocc",
        "\n\tbright { \"sRGB nonlinear\" 1 1 1 }",
        "\n\tdark { \"sRGB nonlinear\" 0 0 0 }",
        "\n\tsamples 32",
        "\n\tmaxdist 3.0",
        "\n}\n\n"
      when "Fake Ambient"
        @stream.print "gi {",
        "\n\ttype fake",
        "\n\tup 0 1 0",
        "\n\tsky { \"sRGB nonlinear\" 0 0 0 }",
        "\n\tground { \"sRGB nonlinear\" 1 1 1 }",
        "\n}\n\n"
    end
  end
  
  def output_scene_objects( ents )
    # Presumably, we're getting the root list of entities, and this function will only
    # be called once per export. This is not striclty necessary, however. This function
    # does support being called more than once, and stores state information of
    # components and materials that have been exported in class members to prevent
    # redeclaration of scene information. Efficiency!
    export_default_material

    output_group_geometry( ents, MODEL_NAME, SCALE_TRANSFORM )
  end
  
  def output_end
    set_status "Export complete"
    @stream.print "%% EOF\n"
  end
  
  #-- Internal Methods --
  private
  def set_status(s)
    Sketchup::set_status_text "SU2SF: " + s
  end
  
  def output_group_geometry( ents, name, trans, mat=DEFAULT_MATERIAL )
    meshes = []
    materials = []
    shaders = [mat]
    points = 0
    tris = 0
    name = reserve_name( GROUP_PREFIX + (name || GENERIC_NAME) )
    ents.each do |x|
      if x.class == Sketchup::Face
        if x.material == nil # No material defined, so
          materials.push 0   # use default material
        else
          m = export_material(x.material)
          i = shaders.index(m)
          unless i
            shaders.push m
            i = shaders.length - 1
          end
          materials.push i
        end

        mesh = x.mesh 5
        points += mesh.count_points
        tris += mesh.count_polygons
        meshes.push mesh
      elsif x.class == Sketchup::Group
        if x.material
          output_group_geometry( x.entities, x.name, trans * x.transformation, export_material(x.material) )
        else
          output_group_geometry( x.entities, x.name, trans * x.transformation, mat )
        end
      elsif x.class == Sketchup::ComponentInstance
        puts " Trans: " + trans.to_a.collect{|b|b.to_s}.join(" ")
        output_component_instance( x, trans, mat )
      end
    end
    
    return unless points > 0
    
    Sketchup.set_status_text "Outputting group mesh: #{name} [#{tris} Triangles]"
    output_geometry( name, meshes, points, tris, materials, shaders, trans, false )
  end

  def output_component_instance( comp, basetrans, basemat )
    cdef = comp.definition
    basemat = export_material( comp.material ) if comp.material
    basetrans = basetrans * comp.transformation
    output_component_definition cdef
    output_component_instance_cached( comp, basetrans, basemat )
  end
  
  def output_component_definition( cdef )
    return if @exported_components[cdef]

    meshes = []
    materials = []
    shaders = [DEFAULT_MATERIAL]
    @component_tree[cdef] = []
    points, tris = output_component_definition_recursive( cdef, cdef.entities, meshes, materials, shaders, Geom::Transformation.new, DEFAULT_MATERIAL )
    name = reserve_name( CDEF_PREFIX + ( cdef.name || GENERIC_NAME) )
    @exported_components[cdef] = name
    @component_shaders[cdef] = shaders
    if points == 0
      @empty_components.push cdef
    else
      output_geometry( name, meshes, points, tris, materials, shaders, nil, true )
    end
  end

  def output_component_definition_recursive( cdef, ents, meshes, materials, shaders, trans, mat )
    points = 0
    tris = 0
    ents.each do |x|
      if x.class == Sketchup::Face
        if x.material == nil # No material defined, so
          materials.push 0   # use default material
        else
          m = export_material(x.material)
          i = shaders.index(m)
          unless i
            shaders.push m
            i = shaders.length - 1
          end
          materials.push i
        end

        mesh = x.mesh 5
        mesh.transform! trans
        points += mesh.count_points
        tris += mesh.count_polygons
        meshes.push mesh
      elsif x.class == Sketchup::Group
        if x.material
          p, t = output_component_definition_recursive( cdef, x.entities, meshes, materials, shaders, trans * x.transformation, export_material( x.material ) )
        else
          p, t = output_component_definition_recursive( cdef, x.entities, meshes, materials, shaders, trans * x.transformation, mat )
        end
        points += p
        tris += t
      elsif x.class == Sketchup::ComponentInstance
        @component_tree[cdef].push [ x.definition, x.transformation, export_material(x.material) ]
        output_component_definition( x.definition )
      end
    end
    return points, tris
  end
  
  def output_component_instance_cached( com, trans, mat )
    if com.class == Sketchup::ComponentDefinition
      cdef = com
    else
      cdef = com.definition
    end
    return false unless (geom = @exported_components[ cdef ])

    @component_tree[cdef].each do |c|
      output_component_instance_cached( c[0], trans * c[1], (c[2] || mat) )
    end

    return true if @empty_components.include? cdef
    
    name = reserve_name( CINST_PREFIX + ( com.name || cdef.name || GENERIC_NAME ) )
    shaders = @component_shaders[cdef]
    shaders = [mat] + shaders[1..-1] if mat
    
    # "\n\ttransform col ", trans.to_a[0..-2].collect{ |x| PRECISION % x }.join( " " ), " 1.0",    
    
    @stream.print "instance {",
      "\n\tname \"#{name}\"",
      "\n\tgeometry \"#{geom}\"",
      "\n\ttransform col ", trans.to_a.collect{ |x| PRECISION % x }.join( " " ),
      "\n\tshaders #{shaders.length} ", shaders.collect{ |x| '"' + x + '"' }.join(" "),
      "\n}\n\n"      
    return true
  end
  
  def output_geometry( name, meshes, points, tris, materials, shaders, trans, noinst=false )
    @stream.print "object {"
    @stream.print "\n\tshaders #{shaders.length} ", shaders.collect{ |x| '"' + x + '"' }.join(" "),
      "\n\ttransform col ", trans.to_a.collect{ |x| PRECISION % x }.join( " " ) if noinst == false
    @stream.print "\n\tnoinstance" if noinst == true
    @stream.print "\n\ttype generic-mesh",
      "\n\tname \"#{name}\"",
      "\n\tpoints #{points}"

    meshes.each do |m|
      m.points.each do |x|
        #p = SCALE_TRANSFORM *  x
        p = x
        @stream.print "\n\t\t#{PRECISION % (p.x.to_inch)} #{PRECISION % (p.y.to_inch)} #{PRECISION % (p.z.to_inch)}"
      end
    end
    @stream.print "\n\ttriangles #{tris}"
    i = -1
    meshes.each do |m|
      m.polygons.each do |p|
        @stream.print "\n\t\t", p.collect{ |x| x.abs+i }.join(" ")
      end
      i+=m.count_points
    end
    @stream.print "\n\tnormals facevarying"
    meshes.each do |m|
      m.polygons.each do |p|
        @stream.print "\n\t\t", p.collect{ |x| m.normal_at(x.abs).to_a.collect{ |x| PRECISION % x }.join(" ") }.join(" ")
      end
    end
    @stream.print "\n\tuvs none" # TODO fix uv mapping
    @stream.print "\n\tface_shaders"
    i = 0
    meshes.each do |m|
      @stream.print "\n\t\t"
      m.count_polygons.times { |z| @stream.print materials[i].to_s + " " }
      i+=1
    end
    @stream.print "\n}\n\n"
  end
  
  def export_material( mat )
    return unless mat
    
    name = @exported_materials[mat]
    return name if name
    
    name = reserve_name( MATERIAL_PREFIX + mat.display_name )
    @exported_materials[mat] = name

    if mat.display_name[0,5] == "sfgla"
      @stream.print "shader {",
        "\n\tname \"#{name}\"",
        "\n\ttype glass",
        "\n\teta 1.5",
        "\n\tcolor { \"sRGB nonlinear\" ", mat.color.to_a[0..2].collect{ |x| "%.3f" % (x.to_f/255) }.join( " " ), " }",
        "\n\tabsorbtion.distance 20.0",
        "\n\tabsorbtion.color { \"sRGB nonlinear\" 0.5 0.5 0.5 }",
        "\n}\n\n"
      return name
    else
      if mat.display_name[0,5] == "sftra"
        @stream.print "shader {",
          "\n\tname \"#{name}\"",
          "\n\ttype glass",
          "\n\teta 1.0",
          "\n\tcolor { \"sRGB nonlinear\" ", mat.color.to_a[0..2].collect{ |x| "%.3f" % (x.to_f/255) }.join( " " ), " }",
          "\n\tabsorbtion.distance 20.0",
          "\n\tabsorbtion.color { \"sRGB nonlinear\" 0.5 0.5 0.5 }",
          "\n}\n\n"
        return name
      else
        if mat.display_name[0,5] == "sfpho"
          @stream.print "shader {",
            "\n\tname \"#{name}\"",
            "\n\ttype phong",
            "\n\tdiff { \"sRGB nonlinear\" ", mat.color.to_a[0..2].collect{ |x| "%.3f" % (x.to_f/255) }.join( " " ), " }",
            "\n\tspec { \"sRGB nonlinear\" ", mat.color.to_a[0..2].collect{ |x| "%.3f" % (x.to_f/255) }.join( " " ), " } 150 ",
            "\n\tsamples 8",
            "\n}\n\n"
          return name
        else
          @stream.print "shader {",
            "\n\tname \"#{name}\"",
            "\n\ttype diffuse",
            "\n\tdiff ", mat.color.to_a[0..2].collect{ |x| "%.3f" % (x.to_f/255) }.join( " " ),
            "\n}\n\n"
          return name
        end
      end
    end
  end
  
  # Materialcode
  def export_default_material
    return if @exported_names.include? DEFAULT_MATERIAL
    reserve_name( DEFAULT_MATERIAL )
    @stream.print "shader {",
      "\n\tname \"", DEFAULT_MATERIAL, '"',
      "\n\ttype diffuse",
      "\n\tdiff 0.8 0.8 0.8",
      "\n}\n\n"
  end
  
  # Necessary?
  def cleanup_name(n)
    n
  end
  
  def reserve_name(n)
    i = 0
    n = cleanup_name(n)
    b = n
    b = n + (i+=1).to_s while @exported_names.include? b
    @exported_names.push b
    return b
  end

end # of Class

def normalize_color(c)
  return (c.red / 255.0).to_s + " " + (c.green / 255.0).to_s + " " + (c.blue / 255.0).to_s
end

def su2sf_ui_settings
ui_types = ["Standard", "Advanced"]
list1 = ui_types.join("|")
enums=[list1]
prompts = ["UI type: "]
values = [$ui]
results = inputbox prompts, values, enums, "User interface"
return nil if not results
$ui = results[0]
end

def su2sf_background_settings
background_types = ["Solid color", "Sky"]
ground_plane = ["Yes", "No"]
list1 = background_types.join("|")
list2 = ground_plane.join("|")
enums=[list1, list2]
prompts = ["Background type: " , "Ground plane: ", "Sky samples: ", " Sky turbidity: "]
values = [$background_type, $ground_plane, $sky_samples, $sky_turbidity]
results = inputbox prompts, values, enums, "Background settings"
return nil if not results
$background_type = results[0]
$ground_plane = results[1]
$sky_samples = results[2]
$sky_turbidity = results[3]

end

def su2sf_dialogs(box, ui)
case box
  when "render"
    if ui == "Standard"
      su2sf_standard_render_settings
    else
      su2sf_advanced_render_settings
    end
end
end

def su2sf_standard_render_settings
image_sizes = ["Sketchup window", "320 x 200", "400 x 300", "640 x 480", "768 x 576", "800 x 600", "1024 x 768", "1280 x 1024", "2048 x 1536"]
if not image_sizes.include? $image_size
  $image_size = "Sketchup window"
end
list1 = image_sizes.join("|")
list2 = [["None", "Standard", "Medium", "High", "Final"].join("|")]
list3 = [["None", "Standard", "Medium", "High", "Final"].join("|")]
enums=[list1, list2, list3]
prompts = ["Image size: " , "Global Illumination: ", "Antialiasing: "]
values = [$image_size, $gi, $aa_type]
results = inputbox prompts, values, enums, "Render settings"
return nil if not results
$image_size = results[0]
$gi = results[1]
$aa_type = results[2]
$trace_diff = 4
$trace_refl = 4
$trace_refr = 4
# Image width and height:
if $image_size == "Sketchup window"
  $image_size = Sketchup.active_model.active_view.vpwidth.to_s + " x " + Sketchup.active_model.active_view.vpheight.to_s
  $image_width = Sketchup.active_model.active_view.vpwidth.to_s
  $image_height = Sketchup.active_model.active_view.vpheight.to_s
  else
  $image_width = $image_size.split()[0]
  $image_height = $image_size.split()[2]
end
# GI settings
case $gi
  when "Standard"
    $gi_type = "Fake Ambient"
  when "Medium"
    $gi_type = "Instant GI"
  when "High"
    $gi_type = "Irradiance Caching"
  when "Final"
    $gi_type = "Path tracing"
end
# Antialiasing settings
case $aa_type
  when "Standard"
    $aa_min = "-1"
    $aa_max = "0"
  when "Medium"
    $aa_min = "0"
    $aa_max = "1"
  when "High"
    $aa_min = "1"
    $aa_max = "4"
  when "Final"
    $aa_min = "2"
    $aa_max = "8"
end
end

def su2sf_advanced_render_settings
image_sizes = ["Sketchup window", "320 x 200", "400 x 300", "640 x 480", "768 x 576", "800 x 600", "1024 x 768", "1280 x 1024", "2048 x 1536"]
if not image_sizes.include? $image_size
  $image_size = "Sketchup window"
end
list1 = image_sizes.join("|")
list2 = [["None", "Instant GI", "Irradiance Caching", "Path Tracing", "Ambient Occlusion", "Fake Ambient"].join("|")]
enums=[list1, list2]
prompts = ["Image size: " , "Global Illumination: ", "Antialiasing minimum: ", "Antialiasing maximum: ", "Trace diffuse: ", "Trace reflection:", "Trace refraction: "]
values = [$image_size, $gi, $aa_min, $aa_max, $trace_diff, $trace_refl, $trace_refr]
results = inputbox prompts, values, enums, "Render settings"
return nil if not results
$image_size = results[0]
$gi = results[1]
$aa_min = results[2]
$aa_max = results[3]
$trace_diff = results[4]
$trace_refl = results[5]
$trace_refr = results[6]
# Image width and height:
if $image_size == "Sketchup window"
  $image_size = Sketchup.active_model.active_view.vpwidth.to_s + " x " + Sketchup.active_model.active_view.vpheight.to_s
  $image_width = Sketchup.active_model.active_view.vpwidth.to_s
  $image_height = Sketchup.active_model.active_view.vpheight.to_s
  else
  $image_width = $image_size.split()[0]
  $image_height = $image_size.split()[2]
end
# GI type
$gi_type = $gi
end

def su2sf_camera_settings
camera_types = ["Pinhole", "Thinlens", "Spherical", "Fisheye"]
list = [camera_types.join("|")]
prompts = ["Camera type: "]
values = [$camera_type]
results = inputbox prompts, values,list, "Camera settings"
return nil if not results
$camera_type = results[0]

case $camera_type
  when "Pinhole"
    # Shift
    prompts = ["Pinhole shift X: ", "Pinhole shift Y: "]
    values = [$pinhole_shift_x, $pinhole_shift_y]
    results = inputbox prompts, values, "Pinhole settings"
    return nil if not results
    $pinhole_shift_x = results[0]
    $pinhole_shift_y = results[1]
  when "Thinlens"
    # Focal distance, lens radius, sides, rotation
    prompts = ["Focal distance: ", "Lens radius: ", "Bokeh sides: ", "Effect rotation: "]
    values = [$thinlens_fdist, $thinlens_lensr, $thinlens_sides, $thinlens_rotat]
    results = inputbox prompts, values, "Thinlens settings"
    return nil if not results
    $thinlens_fdist = results[0]
    $thinlens_lensr = results[1]
    $thinlens_sides = results[2]
    $thinlens_rotat = results[3]
end
end

#def SU2SF::export_dialoglow
def SU2SF::export(selection)
  model = Sketchup.active_model
  
  if selection == true
    ents = model.selection
  else
    ents = model.entities
  end

  model_filename = File.basename( model.path )
  if model_filename != ""
    model_name = model_filename.split(".")[0]
    model_name += ".sc"
  else
    model_name = "Untitled.sc"
  end

  output_filename = UI.savepanel( "Export to SunFlow", "", model_name );
  return if output_filename == nil

  output_file = File.new( output_filename, "w+" )
  return if output_file == nil

  exporter = SU2SF.new
  #exporter.scene_settings["image_width"] = $image_size.split()[0]
  #exporter.scene_settings["image_height"] = $image_size.split()[2]
  exporter.scene_settings["image_width"] = $image_width
  exporter.scene_settings["image_height"] = $image_height
  exporter.output_begin( output_file )
  exporter.output_image_settings( model.active_view )
  exporter.output_background_settings
  exporter.output_sky_settings( model.shadow_info )
  exporter.output_gi
  exporter.output_camera( model.active_view.camera )
  exporter.output_scene_objects( ents )
  exporter.output_ground_plane
  exporter.output_end
  #exporter.instance_variables.each { |v| puts v + " = " + exporter.instance_variable_get(v).to_s }
  output_file.close
end


# Interface default
$ui = "Standard" if not $ui

# Render Defaults
$aa_min = "0" if not $aa_min
$aa_max = "2" if not $aa_max
$filter = "mitchell" if not $filter
$gi = "None" if not $gi
$trace_diff = "4" if not $trace_diff
$trace_refl = "4" if not $trace_refl
$trace_refr = "4" if not $trace_refr
$image_size = "800 x 600" if not $image_size
$image_width = "800"
$image_height = "600"
$gi_type = "None" if not $gi_type
$aa_type = "None" if not $aa_type

# GI defaults
$gi_instant_samples = "64" if not $gi_instant_samples
$gi_instant_sets = "1" if not $gi_instant_sets
$gi_instant_b = "0.01" if not $gi_instant_b
$gi_instant_bias_samples = "10" if not $gi_instant_bias_samples
$gi_icaching_samples = "512" if not $gi_icaching_samples
$gi_icaching_tolerance = "0.01" if not $gi_icaching_tolerance
$gi_icaching_spacing = "0.05 5.0" if not $gi_icaching_spacing
$gi_icaching_global = "1000000" if not $gi_icaching_global
$gi_icaching_grid = "100 0.75" if not $gi_icaching_grid
$gi_ptracing_samples = "32"  if not $gi_ptracing_samples     
# Camera Defaults
$camera_type = "Pinhole" if not $camera_type
$pinhole_shift_x = "0" if not $pinhole_shift_x
$pinhole_shift_y = "0" if not $pinhole_shift_y
$thinlens_fdist = "10" if not $thinlens_fdist
$thinlens_lensr = "0.05" if not $thinlens_lensr
$thinlens_sides = "0" if not $thinlens_sides
$thinlens_rotat = "0" if not $thinlens_rotat

# Background Defaults
$background_type = "Solid color" if not $background_type
$ground_plane = "No" if not $ground_plane
$sky_samples = "32" if not $sky_samples
$sky_turbidity = "5" if not $sky_turbidity

unless file_loaded? "su2sf.rb" 

	main_menu = UI.menu("Plugins").add_submenu("SunFlow Exporter")
  main_menu.add_item("UI settings") { su2sf_ui_settings }
  main_menu.add_item("Render settings") { su2sf_dialogs "render", $ui }
  main_menu.add_item("Camera settings") { su2sf_camera_settings }
  main_menu.add_item("Background settings") { su2sf_background_settings }
  main_menu.add_separator
  main_menu.add_item("Export selection") { (SU2SF.export true) }
	main_menu.add_item("Export Model") { (SU2SF.export false) }
  

end

file_loaded "su2sf.rb"
