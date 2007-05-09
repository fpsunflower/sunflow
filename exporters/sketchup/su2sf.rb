#su2sf
# Written by Tony Wooster, though not quite done yet, 24 MAR 07
#
# Edited by dandruff, added glas, transparent and phong support as
# well as tre different sizes of output picture, 9 MAY 07

require 'sketchup.rb'

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
    @export_textures = false
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
      "\n\taa #{@scene_settings["aa_min"]} #{@scene_settings["aa_max"]}",
      "\n\tfilter #{@scene_settings["filter"]}",
      "\n}\n\n" 
    
    if @scene_settings["gi"]
      @stream.print "gi { #{@scene_settings["gi"]} }\n\n"
    end
    #TODO output trace settings here
  end
 
  def output_sky_settings( shinfo )    
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
    aspect = camera.aspect_ratio
    aspect = (@scene_settings["image_width"].to_f / @scene_settings["image_height"].to_f) if !aspect or aspect <= 0

    set_status("Exporting camera...")
    @stream.print "camera {",
      "\n\ttype pinhole",
      "\n\teye\t#{PRECISION % (camera.eye.x.to_inch * SCALE_FACTOR)} #{PRECISION % (camera.eye.y.to_inch * SCALE_FACTOR)} #{PRECISION % (camera.eye.z.to_inch * SCALE_FACTOR)}",
      "\n\ttarget\t#{PRECISION % (camera.target.x.to_inch * SCALE_FACTOR)} #{PRECISION % (camera.target.y.to_inch * SCALE_FACTOR)} #{PRECISION % (camera.target.z.to_inch * SCALE_FACTOR)}",
      "\n\tup\t#{PRECISION % camera.up.x}\t#{PRECISION % camera.up.y}\t#{PRECISION %  camera.up.z}",
      "\n\tfov\t#{camera.fov}",
      "\n\taspect\t" + "%.6f" % aspect,
      "\n}\n\n"
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

end

def SU2SF::export_dialoglow
  model = Sketchup.active_model

  if model.selection.length > 0 then
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
  exporter.scene_settings["image_width"] = (1600 / 4)
  exporter.scene_settings["image_height"] = (1200 / 4)
  exporter.output_begin( output_file )
  exporter.output_image_settings( model.active_view )
  exporter.output_sky_settings( model.shadow_info )
  exporter.output_camera( model.active_view.camera )
  exporter.output_scene_objects( ents )
  exporter.output_end
  #exporter.instance_variables.each { |v| puts v + " = " + exporter.instance_variable_get(v).to_s }
  output_file.close
end

def SU2SF::export_dialogmed
  model = Sketchup.active_model

  if model.selection.length > 0 then
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
  exporter.scene_settings["image_width"] = (600)
  exporter.scene_settings["image_height"] = (450)
  exporter.output_begin( output_file )
  exporter.output_image_settings( model.active_view )
  exporter.output_sky_settings( model.shadow_info )
  exporter.output_camera( model.active_view.camera )
  exporter.output_scene_objects( ents )
  exporter.output_end
  #exporter.instance_variables.each { |v| puts v + " = " + exporter.instance_variable_get(v).to_s }
  output_file.close
end

def SU2SF::export_dialoghigh
  model = Sketchup.active_model

  if model.selection.length > 0 then
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
  exporter.scene_settings["image_width"] = (1600)
  exporter.scene_settings["image_height"] = (1200)
  exporter.output_begin( output_file )
  exporter.output_image_settings( model.active_view )
  exporter.output_sky_settings( model.shadow_info )
  exporter.output_camera( model.active_view.camera )
  exporter.output_scene_objects( ents )
  exporter.output_end
  #exporter.instance_variables.each { |v| puts v + " = " + exporter.instance_variable_get(v).to_s }
  output_file.close
end

unless file_loaded? "su2sf.rb" 

	main_menu = UI.menu("Plugins").add_submenu("SunFlow Exporter")
	main_menu.add_item("Export Model -Small") { (SU2SF.export_dialoglow) }
	main_menu.add_item("Export Model -Medium") { (SU2SF.export_dialogmed) }
	main_menu.add_item("Export Model -Big") { (SU2SF.export_dialoghigh) }

end

file_loaded "su2sf.rb"