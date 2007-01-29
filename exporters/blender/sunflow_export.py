#!BPY

"""
Name: 'Sunflow Exporter (.sc)...'
Blender: 2.4
Group: 'Export'
Tip: ''
"""

"""
Version         :       0.07.0 (January 2007)
Author          :       R Lindsay (hayfever) / Christopher Kulla
Description     :       Export to Sunflow renderer http://sunflow.sourceforge.net/
"""

##  imports  ##

import Blender, os, sys, time, struct
from Blender import Mathutils, NMesh, Lamp, Object, Scene, Mesh, Material, Draw, BGL
from math import *

##  end imports  ##

##  global gui vars ##

MINAA = Draw.Create(-2)
MAXAA = Draw.Create(0)
AASAMPLES = Draw.Create(1)
DSAMPLES = Draw.Create(16)
IMGFILTER = Draw.Create(1)
IMGFILTERW = Draw.Create(1)
IMGFILTERH = Draw.Create(1)
DOF = Draw.Create(0)
DOFRADIUS = Draw.Create(1.0)
DOFDIST = Draw.Create(10.0)
SPHERICALCAMERA = Draw.Create(0)
MESHLIGHTPOWER = Draw.Create(1)
OCCLUSSION = Draw.Create(0)
OCCBRIGHTR  = Draw.Create(1.0)
OCCBRIGHTG  = Draw.Create(1.0)
OCCBRIGHTB  = Draw.Create(1.0)
OCCDARKR    = Draw.Create(0.0)
OCCDARKG    = Draw.Create(0.0)
OCCDARKB    = Draw.Create(0.0)
OCCSAMPLES  = Draw.Create(32)
OCCDIST     = Draw.Create(0.0)

EXPORT_EVT = 1
NO_EVENT   = 2
DOF_CAMERA = 3
SPHER_CAMERA = 4
IRR_EVENT = 5
FILTER_EVENT = 7
CHANGE_AA = 8
CHANGE_CAM = 10
CHANGE_ACC = 11
CHANGE_LIGHT = 12
CHANGE_AO = 13

##  export options

EXP_ANIM = Draw.Create(0)

IMGFILTERLIST = ["box", "gaussian", "mitchell", "triangle", "catmull-rom", "blackman-harris", "sinc", "lanczos"]

##  end global gui vars  ##

##  global vars  ##

FILENAME  = Blender.Get('filename').replace(".blend", ".sc")
global FILE
global SCENE
global IM_HEIGHT
global IM_WIDTH
global TEXTURES
global OBJECTS
global IBLLIGHT
global LAYERS
global SCREEN

##  end global vars  ##

##  start of export  ##

print "\n\n"
print "blend2sunflow v0.07.0"

## Export logic for simple options
def export_output():
	print "o exporting output details..."
	FILE.write("image {\n")
	FILE.write("\tresolution %d %d\n" % (IM_WIDTH, IM_HEIGHT))
	FILE.write("\taa %s %s\n" % (MINAA.val, MAXAA.val))
	FILE.write("\tfilter %s\n" % IMGFILTERLIST[IMGFILTER.val-1])
	FILE.write("}")
	FILE.write("\n")


## Export logic for materials
def export_shaders():
      print "o exporting shaders..."

      # default shader
      FILE.write("\n\nshader {\n\tname def\n\ttype diffuse\n\tdiff  1 1 1\n}")
      if OCCLUSSION.val == 1:
               FILE.write("\n\nshader {\n   name amboccshader\n   type amb-occ2\n")
               FILE.write("\tbright { \"sRGB nonlinear\" %s %s %s }\n" % (OCCBRIGHTR.val, OCCBRIGHTG.val, OCCBRIGHTB.val))
               FILE.write("\tdark { \"sRGB nonlinear\" %s %s %s }\n" % (OCCDARKR.val, OCCDARKG.val, OCCDARKB.val))
               FILE.write("\tsamples %s\n" % OCCSAMPLES.val)
               FILE.write("\tdist %s\n}" % OCCDIST.val)
               FILE.write("\n\noverride amboccshader true")
      materials = Blender.Material.get()
      for mat in materials:
              FILE.write("\n\nshader {\n")
              FILE.write("\tname \""+mat.name+".shader\"\n")

              textures = mat.getTextures()
              flags = mat.getMode()
              if textures[0] <> None and textures[0].tex.getType() == "Image":
                              textu = textures[0]
                              if mat.name.startswith("sfambocc"):
                                      print "  o exporting ambient occlusion texture shader "+mat.name+"..."
                                      FILE.write("\ttype amb-occ\n")
                              else:
                                      print "  o exporting diffuse texture shader "+mat.name+"..."
                                      FILE.write("\ttype diffuse\n")

                              FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")
              else:
                      RGB = mat.getRGBCol()
                      ## shiny shader
                      if mat.name.startswith("sfshiny"):
                              print "  o exporting shiny shader "+mat.name+"..."
                              FILE.write("\ttype shiny\n")
                              FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n" % (RGB[0], RGB[1], RGB[2]))
                              FILE.write("\trefl %s\n}" % mat.getRayMirr())
                      ## amb-occ shader
                      elif mat.name.startswith("sfambocc"):
                              print "  o exporting ambient occlusion shader "+mat.name+"..."
                              FILE.write("\ttype amb-occ\n")
                              FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" % (RGB[0], RGB[1], RGB[2]))
                      ## phong shader
                      elif mat.name.startswith("sfphong"):
                              print "  o exporting phong shader "+ mat.name+"..."
                              FILE.write("\ttype phong\n")
                              FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n" % (RGB[0],RGB[1],RGB[2]))
                              speccol = mat.specCol
                              FILE.write("\tspec { \"sRGB nonlinear\" %s %s %s } %s\n" %(speccol[0], speccol[1], speccol[2], mat.hard))
                              FILE.write("\tsamples 4\n}")
                      ## reflection shader
                      elif flags & Material.Modes['RAYMIRROR']:
                              print "  o exporting mirror shader "+mat.name+"..."
                              FILE.write("\ttype mirror\n")
                              FILE.write("\trefl { \"sRGB nonlinear\" %s %s %s }\n}" %(RGB[0],RGB[1],RGB[2]))
                      ## glass shader
                      elif flags & Material.Modes['RAYTRANSP']:
                              print "  o exporting glass shader "+mat.name+"..."
                              FILE.write("\ttype glass\n")
                              FILE.write("\teta " + str(mat.getIOR()) + "\n")
                              FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n}" %(RGB[0],RGB[1],RGB[2]))
                      ## diffuse shader
                      else:
                              print "  o exporting diffuse shader "+mat.name+"..."
                              FILE.write("\ttype diffuse\n")
                              FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" %(RGB[0],RGB[1],RGB[2]))


## Export logic for Blender's light sources
def export_lights(lmp):
      # only lamp type 0 supported at the moment
      # lamp types are: 0 - Lamp, 1 - Sun, 2 - Spot, 3 - Hemi, 4 - Area
      lamp = lmp.getData()
      if lamp.type == 0:
              print "o exporting lamp "+lmp.name+"..."
              # get the rgb component for the lamp
              red   = lamp.col[0]
              green = lamp.col[1]
              blue  = lamp.col[2]
              power = lamp.energy

              # get the location of the lamp
              objmatrix = lmp.matrix
              lampV = Mathutils.Vector([0, 0,  0, 1])
              lampV = lampV * objmatrix

              FILE.write("\n\nlight {\n")
              FILE.write("\ttype point\n")
              FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
              FILE.write("\tpower %s\n" % (power))
              FILE.write("\tp %s %s %s\n" % (lampV[0], lampV[1], lampV[2]))
              FILE.write("}")
      elif lamp.type == 1:
              print "o exporting sun-light "+lmp.name+"..."
              invmatrix = Mathutils.Matrix(lmp.getInverseMatrix())
              FILE.write("\nlight {\n")
              FILE.write("\ttype sunsky\n")
              FILE.write("\tup 0 0 1\n")
              FILE.write("\teast 0 1 0\n")
              FILE.write("\tsundir %f %f %f\n" % (invmatrix[0][2], invmatrix[1][2], invmatrix[2][2]))
              FILE.write("\tturbidity 6\n")
              FILE.write("\tsamples %s\n" % DSAMPLES.val)
              FILE.write("}")
      elif lamp.type == 4:
              print "o exporting area-light "+lmp.name+"..."
              objmatrix = lmp.matrix
              size = lamp.areaSizeX * 0.5
              lampV0 = Mathutils.Vector([-size,  size,  0, 1])
              lampV1 = Mathutils.Vector([ size,  size,  0, 1])
              lampV2 = Mathutils.Vector([ size, -size,  0, 1])
              lampV3 = Mathutils.Vector([-size, -size,  0, 1])

              lampV0 = lampV0 * objmatrix
              lampV1 = lampV1 * objmatrix
              lampV2 = lampV2 * objmatrix
              lampV3 = lampV3 * objmatrix

              red   = lamp.col[0]
              green = lamp.col[1]
              blue  = lamp.col[2]
              radiance = lamp.energy * MESHLIGHTPOWER.val

              FILE.write("\n\nlight {\n")
              FILE.write("\ttype meshlight\n")
              FILE.write("\tname \"%s\"\n" % (lmp.name))
              FILE.write("\temit { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
              FILE.write("\tradiance %s\n" % (radiance))
              FILE.write("\tsamples %s\n" % DSAMPLES.val)
              FILE.write("\tpoints 4\n")
              FILE.write("\t\t%s %s %s\n" % (lampV0[0], lampV0[1], lampV0[2]))
              FILE.write("\t\t%s %s %s\n" % (lampV1[0], lampV1[1], lampV1[2]))
              FILE.write("\t\t%s %s %s\n" % (lampV2[0], lampV2[1], lampV2[2]))
              FILE.write("\t\t%s %s %s\n" % (lampV3[0], lampV3[1], lampV3[2]))
              FILE.write("\ttriangles 2\n")
              FILE.write("\t\t0 1 2\n")
              FILE.write("\t\t0 2 3\n")
              FILE.write("}")
      #elif lamp.type == 2:
      #        print "o exporting spotlight "+lmp.name+"..."
      #        FILE.write("\n\nlight {\n")
      #        FILE.write("\ttype directional\n")
      #        FILE.write("\ttarget\n")
      #        FILE.write("\tradius %s\n" % lamp.getSpotSize())
      #        FILE.write("\temit %s %s %s" %(lamp.col[0],lamp.col[1],lamp.col[2])+"\n")
      #        FILE.write("}")


## Export method for Blender camera
def export_camera(cam):
      global IBLLIGHT

      # get the camera
      camera = cam.getData()
      print "o exporting Camera "+camera.getName()+"..."

      # get the object matrix so we can calculate to and up
      objmatrix = cam.matrix
      eyeV    = Mathutils.Vector([0, 0,  0, 1])
      targetV = Mathutils.Vector([0, 0, -1, 1])
      upV     = Mathutils.Vector([0, 1,  0, 0])

      eyeV    = eyeV * objmatrix
      targetV = targetV * objmatrix
      upV     = upV * objmatrix

      # get the fov value
      fov = 360.0 * atan(16.0 / camera.getLens()) / pi

      FILE.write("\n\ncamera {\n")
      camtype = "pinhole"
      if(DOF.val == 1): camtype = "thinlens"
      if(SPHERICALCAMERA.val == 1): camtype = "spherical"

      FILE.write("\ttype   %s\n" % camtype)
      FILE.write("\teye    %s %s %s\n" % (eyeV[0], eyeV[1], eyeV[2]))
      FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
      FILE.write("\tup     %s %s %s\n" % (upV[0], upV[1], upV[2]))
      if SPHERICALCAMERA.val == 0: FILE.write("\tfov    %s \n" % fov)
      if SPHERICALCAMERA.val == 0: FILE.write("\taspect %s \n" % (1.0 * IM_WIDTH / IM_HEIGHT))
      if DOF.val == 1:
              FILE.write("\tfdist %s \n" % DOFDIST)
              FILE.write("\tlensr %s \n" % DOFRADIUS)
      FILE.write("}")

      if IBLLIGHT <> "":
              print "o exporting ibllight..."
              FILE.write("\n\nlight {\n")
              FILE.write("\ttype ibl\n")
              FILE.write("\timage \"%s\"\n" % (IBLLIGHT))
              FILE.write("\tcenter 1 0 0\n")
              FILE.write("\tup 0 0 1\n")
              FILE.write("\tlock true\n")
              FILE.write("\tsamples %s\n" % DSAMPLES.val)
              FILE.write("}")


## Export method for meshes
def export_geometry(obj, filename):
      #mesh = "";verts="";faces="";numverts=""
      islight = obj.name.startswith("meshlight")
      if islight:
          print "o exporting meshlight " + obj.name+"..."
      else:
          print "o exporting mesh " + obj.name+"..."
      # get the mesh data
      mesh = NMesh.GetRawFromObject(obj.name)
      if islight:
          mesh.transform(obj.getMatrix(), 1)
      verts = mesh.verts
      faces = mesh.faces
      numfaces = faces.__len__()
      numverts = verts.__len__()
      if numfaces > 100000:
          print "   -> large mesh detected - creating binary ra3 file"
          ra3filename = filename.replace(".sc", "_%s.ra3" % obj.name)
          RA3FILE = open(ra3filename, 'wb')
          print "   -> creating \"%s\" ..." % ra3filename
          print "   -> counting triangles ..."
          numtris = 0
          for face in faces:
                  num = len(face.v)
                  if num == 4:
                          numtris = numtris + 2
                  elif num == 3:
                          numtris = numtris + 1
          print "   -> writing %s points" % numverts
          RA3FILE.write(struct.pack("<II", numverts, numtris))
          for vert in verts:
                  RA3FILE.write(struct.pack("<fff", vert.co[0], vert.co[1], vert.co[2]))
          print "   -> writing %s triangles" % numtris
          for face in faces:
                      num = len(face.v)
                      if num == 4:
                              RA3FILE.write(struct.pack("<III", face.v[0].index, face.v[1].index, face.v[2].index))
                              RA3FILE.write(struct.pack("<III", face.v[0].index, face.v[2].index, face.v[3].index))
                      elif num == 3:
                              RA3FILE.write(struct.pack("<III", face.v[0].index, face.v[1].index, face.v[2].index))
          RA3FILE.close()
          print "   -> done writing file"
          FILE.write("\n\nobject {\n")
          if len(mesh.materials) == 1:
                  FILE.write("\tshader \"" + mesh.materials[0].name + ".shader\"\n")
          elif len(mesh.materials) > 1:
                  FILE.write("\tshaders %d\n" % (len(mesh.materials)))
                  for mat in mesh.materials:
                          FILE.write("\t\t\"" + mat.name + ".shader\"\n")
          else:
                  FILE.write("\tshader def\n")
          mat = obj.getMatrix()
          FILE.write("\ttransform col\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n" % (
                                        mat[0][0], mat[0][1], mat[0][2], mat[0][3], 
                                        mat[1][0], mat[1][1], mat[1][2], mat[1][3], 
                                        mat[2][0], mat[2][1], mat[2][2], mat[2][3], 
                                        mat[3][0], mat[3][1], mat[3][2], mat[3][3]))
          FILE.write("\ttype file-mesh\n")
          FILE.write("\tname \"" + obj.name + "\"\n")
          FILE.write("\tfilename \"%s\"\n" % os.path.basename(ra3filename))
          FILE.write("}\n")
          return
      if numverts > 0:
              if islight:
                      FILE.write("\n\nlight {\n")
                      FILE.write("\ttype meshlight\n")
                      FILE.write("\tname \"" + obj.name + "\"\n")
                      if len(mesh.materials) >= 1:
                              matrl = mesh.materials[0]
                              FILE.write("\temit { \"sRGB nonlinear\" %s %s %s }\n" % (matrl.R, matrl.G, matrl.B))
                      else:
                              FILE.write("\temit 1 1 1\n")
                      FILE.write("\tradiance %s\n" % (MESHLIGHTPOWER.val))
                      FILE.write("\tsamples %s\n" % DSAMPLES.val)
              else:
                      FILE.write("\n\nobject {\n")
                      if len(mesh.materials) == 1:
                              FILE.write("\tshader \"" + mesh.materials[0].name + ".shader\"\n")
                      elif len(mesh.materials) > 1:
                              FILE.write("\tshaders %d\n" % (len(mesh.materials)))
                              for mat in mesh.materials:
                                      FILE.write("\t\t\"" + mat.name + ".shader\"\n")
                      else:
                              FILE.write("\tshader def\n")
                      mat = obj.getMatrix()
                      FILE.write("\ttransform col\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n\t\t%s %s %s %s\n" % (
                                                    mat[0][0], mat[0][1], mat[0][2], mat[0][3], 
                                                    mat[1][0], mat[1][1], mat[1][2], mat[1][3], 
                                                    mat[2][0], mat[2][1], mat[2][2], mat[2][3], 
                                                    mat[3][0], mat[3][1], mat[3][2], mat[3][3]))
                      FILE.write("\ttype generic-mesh\n")
                      FILE.write("\tname \"" + obj.name + "\"\n")

              FILE.write("\tpoints %d\n" % (numverts))
              for vert in verts:
                      FILE.write("\t\t%s %s %s\n" % (vert.co[0], vert.co[1], vert.co[2]))
              numtris = 0
              for face in faces:
                      num = len(face.v)
                      if num == 4:
                              numtris = numtris + 2
                      elif num == 3:
                              numtris = numtris + 1
              FILE.write("\ttriangles %d\n" % (numtris))
              allsmooth = True
              allflat = True
              for face in faces:
                      num = len(face.v)
                      smooth = face.smooth != 0
                      allsmooth &= smooth
                      allflat &= not smooth
                      if num == 4:
                              FILE.write("\t\t%d %d %d\n" % (face.v[0].index, face.v[1].index, face.v[2].index))
                              FILE.write("\t\t%d %d %d\n" % (face.v[0].index, face.v[2].index, face.v[3].index))
                      elif num == 3:
                              FILE.write("\t\t%d %d %d\n" % (face.v[0].index, face.v[1].index, face.v[2].index))
              ## what kind of normals do we have?
              if not islight:
                      if allflat:
                              FILE.write("\tnormals none\n")
                      elif allsmooth:
                              FILE.write("\tnormals vertex\n")
                              for vert in verts:
                                      FILE.write("\t\t%s %s %s\n" % (vert.no[0], vert.no[1], vert.no[2]))
                      else:
                              FILE.write("\tnormals facevarying\n")
                              for face in faces:
                                      num = len(face.v)
                                      if face.smooth != 0:
                                          if num == 4:
                                              index0 = face.v[0].index
                                              index1 = face.v[1].index
                                              index2 = face.v[2].index
                                              index3 = face.v[3].index
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                            verts[index1].no[0], verts[index1].no[1], verts[index1].no[2],
                                                                                                                            verts[index2].no[0], verts[index2].no[1], verts[index2].no[2]))
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                            verts[index2].no[0], verts[index2].no[1], verts[index2].no[2],
                                                                                                                            verts[index3].no[0], verts[index3].no[1], verts[index3].no[2]))
                                          elif num == 3:
                                              index0 = face.v[0].index
                                              index1 = face.v[1].index
                                              index2 = face.v[2].index
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                            verts[index1].no[0], verts[index1].no[1], verts[index1].no[2],
                                                                                                                            verts[index2].no[0], verts[index2].no[1], verts[index2].no[2]))
                                      else:
                                          fnx = face.no[0]
                                          fny = face.no[1]
                                          fnz = face.no[2]
                                          if num == 4:
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                                          elif num == 3:
                                              FILE.write("\t\t%s %s %s %s %s %s %s %s %s\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                      if mesh.hasFaceUV():
                           tx = 1
                           ty = 1
                           if len(mesh.materials) >= 1:
                                if len(mesh.materials[0].getTextures()) >= 1:
                                    if mesh.materials[0].getTextures()[0] <> None:
                                         tx = mesh.materials[0].getTextures()[0].tex.repeat[0]
                                         ty = mesh.materials[0].getTextures()[0].tex.repeat[1]
                           FILE.write("\tuvs facevarying\n")
                           for face in faces:
                              num = len(face.v)
                              if num == 4:
                                      FILE.write("\t\t%s %s %s %s %s %s\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[1][0], ty * face.uv[1][1], tx * face.uv[2][0], ty * face.uv[2][1]))
                                      FILE.write("\t\t%s %s %s %s %s %s\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[2][0], ty * face.uv[2][1], tx * face.uv[3][0], ty * face.uv[3][1]))
                              elif num == 3:
                                      FILE.write("\t\t%s %s %s %s %s %s\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[1][0], ty * face.uv[1][1], tx * face.uv[2][0], ty * face.uv[2][1]))
                      else:
                           FILE.write("\tuvs none\n")
                      if len(mesh.materials) > 1:
                           FILE.write("\tface_shaders\n")
                           for face in faces:
                              num = len(face.v)
                              if num == 4:
                                      FILE.write("\t\t%d\n" % (face.materialIndex))
                                      FILE.write("\t\t%d\n" % (face.materialIndex))
                              elif num == 3:
                                      FILE.write("\t\t%d\n" % (face.materialIndex))
              FILE.write("}\n")


## Main export method
def exportfile(filename):
      global FILE, SCENE, IM_HEIGHT, IM_WIDTH, TEXTURES, OBJECTS, LAYERS, IBLLIGHT, EXP_ANIM

      SCENE     = Blender.Scene.GetCurrent()
      IM_HEIGHT = SCENE.getRenderingContext().imageSizeY()
      IM_WIDTH  = SCENE.getRenderingContext().imageSizeX()
      TEXTURES  = Blender.Texture.Get()
       
      LAYERS    = SCENE.getLayers()
      STARTFRAME = SCENE.getRenderingContext().startFrame()
      ENDFRAME  = SCENE.getRenderingContext().endFrame()
      CTX       = SCENE.getRenderingContext()
      orig_frame = CTX.currentFrame()

      if not filename.endswith(".sc"):
         filename = filename + ".sc"
      fname = filename
      ANIM = EXP_ANIM.val

      if ANIM == 1:
      	 base = os.path.splitext(os.path.basename(filename))[0]
         FILE = open(filename.replace(".sc", ".java"), "wb")
         FILE.write("""
public void build() {
	parse("%s" + ".settings.sc");
	parse("%s" + "." + getCurrentFrame() + ".sc");
}
""" % (base, base))
         FILE.close()
         FILE = open(filename.replace(".sc", ".settings.sc"), "wb")
         export_output()
         FILE.close()
      else:
         STARTFRAME = ENDFRAME = orig_frame

      for cntr in range(STARTFRAME, ENDFRAME + 1):
          filename = fname

          CTX.currentFrame(cntr)
          SCENE.makeCurrent()
          try:
             ibllighttext = Blender.Texture.Get("ibllight")
             if ibllighttext <> "":
                if ibllighttext <> None and ibllighttext.getType() == "Image":
                      IBLLIGHT = ibllighttext.getImage().getFilename()
          except:
             IBLLIGHT = ""

          OBJECTS   = Blender.Scene.GetCurrent().getChildren()
          
          if ANIM == 1:
             filename = filename.replace(".sc",  ".%d.sc" % (CTX.currentFrame()))
            
          print "Exporting to: %s" % (filename)

          FILE = open(filename, 'wb')
          if ANIM == 0:
             export_output()
          export_shaders()
          export_camera(SCENE.getCurrentCamera())
          for object in OBJECTS:
             if object.users > 1 and object.layers[0] in LAYERS:
                  if object.getType() == 'Lamp':
	                  export_lights(object)
                  if object.getType() == 'Mesh' or object.getType() == 'Surf':
                     if object.name.startswith("meshlight"):
                            export_geometry(object, filename)
          if ANIM == 0:
              FILE.write("\n\ninclude \"%s\"\n" % (os.path.basename(filename).replace(".sc", ".geo.sc")))
              FILE.close()
              ## open geo file
              filename = filename.replace(".sc", ".geo.sc")
              print "Exporting geometry to: %s" % (filename)
              FILE = open(filename, 'wb')

          for object in OBJECTS:
             if object.users > 1 and object.layers[0] in LAYERS:
                 if object.getType() == 'Mesh' or object.getType() == 'Surf':
                    if not object.name.startswith("meshlight"):
                        export_geometry(object, filename)
          FILE.close()

      if ANIM == 1:
          CTX.currentFrame(orig_frame)
          SCENE.makeCurrent()

      print "Export finished."

## Global event handler
def event(evt, val):
	if evt == Draw.ESCKEY:
		print "quitting exporter..."
		Draw.Exit()


## GUI button event handler
def buttonEvent(evt):
      global FILE,SCREEN
      if evt == EXPORT_EVT:
              Blender.Window.FileSelector(exportfile, "Export .sc", FILENAME)
      if evt == DOF_CAMERA:
              if DOF.val == 1:
                      if SPHERICALCAMERA.val == 1:
                              SPHERICALCAMERA.val = 0
                              Draw.Redraw()
      if evt == SPHER_CAMERA:
              if SPHERICALCAMERA.val == 1:
                      if DOF.val == 1:
                              DOF.val = 0
                              Draw.Redraw()
      if evt == IRR_EVENT:
              if GIIRR.val == 1:
                      if GIPATH.val == 1:
                              GIPATH.val = 0
                              Draw.Redraw()
      if evt == FILTER_EVENT:
              Draw.Redraw()
      if evt == CHANGE_AA:
              SCREEN=0
              Draw.Redraw()
              return
      if evt == CHANGE_CAM:
              SCREEN=2
              Draw.Redraw()
              return
      if evt == CHANGE_LIGHT:
              SCREEN=4
              Draw.Redraw()
              return
      if evt == CHANGE_AO:
              SCREEN=5
              Draw.Redraw()
              return


## Draws the individual panels
def drawGUI():
      global SCREEN
      if SCREEN==0:
             drawAA()
      if SCREEN==2:
             drawCamera()
      if SCREEN==4:
             drawLights()
      if SCREEN==5:
             drawAO()


## Draw AA settings
def drawAA():
	global MINAA, MAXAA, AASAMPLES
	global IMGFILTERW, IMGFILTERH, IMGFILTER
	global EXP_ANIM
	##  aa settings
	col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("AA:")
	col=100; MINAA=Draw.Number("Min AA ", 2, col, line, 120, 18, MINAA.val, -4, 5); 
	col=230; MAXAA=Draw.Number("Max AA  ", 2, col, line, 120, 18, MAXAA.val, -4, 5)
	col=360; AASAMPLES=Draw.Number("Samples", 2, col, line, 120, 18, AASAMPLES.val, 0, 32)
	col=10; line=175; BGL.glRasterPos2i(col, line); Draw.Text("Image Filter:")
	col=100; line=173; IMGFILTER=Draw.Menu("%tImage Filter|box|gaussian|mitchell|triangle|catmull-rom|blackman-harris|sinc|lanczos", FILTER_EVENT, col, line, 120, 18, IMGFILTER.val)
	col=10; line=120; EXP_ANIM=Draw.Toggle("Export As Animation", 2, col, line, 140, 18, EXP_ANIM.val)
	drawButtons()

## Draw camera options
def drawCamera():
	global DOF, DOFRADIUS, DOFDIST, SPHERICALCAMERA
	##  camera settings
	col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("Camera:")
	col=100; line=195; DOF=Draw.Toggle("DOF", DOF_CAMERA, col, line, 120, 18, DOF.val)
	col=225; DOFDIST=Draw.Number("Distance", 2, col, line, 120, 18, DOFDIST.val, 0.0, 200.00)
	col=350; DOFRADIUS=Draw.Number("Radius", 2, col, line, 120, 18, DOFRADIUS.val, 0.0, 200.00)
	col=100; line=170; SPHERICALCAMERA=Draw.Toggle("Spherical", SPHER_CAMERA, col, line, 120, 18, SPHERICALCAMERA.val)
	drawButtons()

## Draw light options
def drawLights():
	global MESHLIGHTPOWER, DSAMPLES
	## meshlight power slider
	col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("Meshlight:")
	col=100; line=195; MESHLIGHTPOWER=Draw.Number("Power", 2, col, line, 120, 18, MESHLIGHTPOWER.val, 1, 15)
	## lightserver settings
	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("Lightserver:")
	col=100; line=147; DSAMPLES=Draw.Number("Direct Samples  ", 2, col, line, 250, 18, DSAMPLES.val, 0, 1024); 
	drawButtons()


## Draw ambient occlusion override settings
def drawAO():
	global OCCLUSSION, OCCBRIGHTR, OCCBRIGHTG, OCCBRIGHTB, OCCDARKR, OCCDARKG, OCCDARKB, OCCSAMPLES, OCCDIST
	col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("Ambient Occlusion")
	col=10; line=175; OCCLUSSION=Draw.Toggle("Amb Occ", 2, col, line, 85, 18, OCCLUSSION.val)
	col=100; OCCBRIGHTR=Draw.Number("Bright (R)", 2, col, line, 125, 18, OCCBRIGHTR.val, 0.0, 1.0)
	col=230; OCCBRIGHTG=Draw.Number("Bright (G)", 2, col, line, 125, 18, OCCBRIGHTG.val, 0.0, 1.0)
	col=360; OCCBRIGHTB=Draw.Number("Bright (B)", 2, col, line, 125, 18, OCCBRIGHTB.val, 0.0, 1.0)
	col=100; line=150; OCCDARKR=Draw.Number("Dark (R)", 2, col, line, 125, 18, OCCDARKR.val, 0.00, 1.0)
	col=230; OCCDARKG=Draw.Number("Dark (G)", 2, col, line, 125, 18, OCCDARKG.val, 0.0, 1.0)
	col=360; OCCDARKB=Draw.Number("Dark (B)", 2, col, line, 125, 18, OCCDARKB.val, 0.0, 1.0)
	col=100; line=125; OCCSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, OCCSAMPLES.val, 0, 256)
	col=230; OCCDIST=Draw.Number("Distance", 2, col, line, 125, 18, OCCDIST.val, -1.0, 150.0)
	drawButtons()


## Draw the bottom bar of buttons in the interface
def drawButtons():
	Draw.Button("Export"     , EXPORT_EVT  , 20 , 10, 90, 20)
	Draw.Button("AA"         , CHANGE_AA   , 20 , 40, 90, 20)
	Draw.Button("Camera"     , CHANGE_CAM  , 115, 40, 90, 20)
	Draw.Button("Light"      , CHANGE_LIGHT, 210, 40, 90, 20)
	Draw.Button("AO Override", CHANGE_AO   , 305, 40, 90, 20)

SCREEN=0
Draw.Register(drawGUI, event, buttonEvent)
