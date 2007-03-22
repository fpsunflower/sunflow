#!BPY

"""
Name: 'Sunflow Exporter 1.0.5 (.sc)...'
Blender: 2.43
Group: 'Export'
Tip: ''
"""

"""
Version         :       1.0.5 (March 2007)
Author          :       R Lindsay (hayfever) / Christopher Kulla / MADCello / 
			olivS / Eugene Reilly / Heavily Tessellated / Humfred
Description     :       Export to Sunflow renderer http://sunflow.sourceforge.net/
"""

###############
##  imports  ##
###############

import Blender, os, sys, time

from Blender import Mathutils, NMesh, Lamp, Object, Scene, Mesh, Material, Draw, BGL
from math import *

######################
##  global gui vars ##
######################

## Default values of buttons ##
###############################

FILETYPE = Draw.Create(1)

#AA panel
MINAA = Draw.Create(0)
MAXAA = Draw.Create(2)
AASAMPLES = Draw.Create(1)
AAJITTER = Draw.Create(0)
DSAMPLES = Draw.Create(16)
IMGFILTER = Draw.Create(1)
IMGFILTERW = Draw.Create(1)
IMGFILTERH = Draw.Create(1)

# Camera panel
DOF = Draw.Create(0)
DOFRADIUS = Draw.Create(1.00)
LENSSIDES = Draw.Create(2)
LENSROTATION = Draw.Create(0.0)
SPHERICALCAMERA = Draw.Create(0)
FISHEYECAMERA = Draw.Create(0)

# Background panel
IMP_BCKGRD  = Draw.Create(1)
IMP_SUN  = Draw.Create(0)
BCKGRDR = Draw.Create(0.000)
BCKGRDG = Draw.Create(0.000)
BCKGRDB = Draw.Create(0.000)
BACKGROUND = Draw.Create(0)
SUN_TURB = Draw.Create(6.0)
SUN_SAMPLES = Draw.Create(16)
IBL = Draw.Create(0)
IBLLOCK = Draw.Create(0)
IBLSAMPLES = Draw.Create(16)

# Light panel
MESHLIGHTPOWER = Draw.Create(100)
LAMPPOWER = Draw.Create(100)
CONVLAMP    = Draw.Create(0)

# Config panel
MEM = Draw.Create(1024)
THREADS    = Draw.Create(0)

#GI/Caustic panel
CAUSTICS = Draw.Create(0)
PHOTONNUMBER = Draw.Create(1000000)
PHOTONMAP = Draw.Create(1)
PHOTONESTIMATE = Draw.Create(100)
PHOTONRADIUS = Draw.Create(0.5)
INSTANTGI = Draw.Create(0)
IGISAMPLES = Draw.Create(64)
IGISETS = Draw.Create(1)
IGIBIAS = Draw.Create(0.00003)
IGIBIASSAMPLES = Draw.Create(0)
IRRCACHE = Draw.Create(0)
IRRSAMPLES = Draw.Create(512)
IRRTOLERANCE = Draw.Create(0.01)
IRRSPACEMIN = Draw.Create(0.05)
IRRSPACEMAX = Draw.Create(5.0)
USEGLOBALS = Draw.Create(0)
gPHOTONNUMBER = Draw.Create(1000000)
gPHOTONMAP = Draw.Create(2)
gPHOTONESTIMATE = Draw.Create(100)
gPHOTONRADIUS = Draw.Create(0.5)
PATHTRACE = Draw.Create(0)
PATHSAMPLES = Draw.Create(32)
VIEWCAUSTICS = Draw.Create(0)
VIEWGLOBALS = Draw.Create(0)
VIEWGI = Draw.Create(0)
OCCLUSION = Draw.Create(0)
OCCBRIGHTR  = Draw.Create(1.0)
OCCBRIGHTG  = Draw.Create(1.0)
OCCBRIGHTB  = Draw.Create(1.0)
OCCDARKR    = Draw.Create(0.0)
OCCDARKG    = Draw.Create(0.0)
OCCDARKB    = Draw.Create(0.0)
OCCSAMPLES  = Draw.Create(32)
OCCDIST     = Draw.Create(0.0)

# Shader panel
SHADTYPE = Draw.Create(1)

# Render panel
QUICKOPT = Draw.Create(1)
IPR = Draw.Create(0)
EXP_ANIM = Draw.Create(0)
DEPTH_DIFF = Draw.Create(1)
DEPTH_REFL = Draw.Create(4)
DEPTH_REFR = Draw.Create(4)
NOGI    = Draw.Create(0)
NOCAUSTICS    = Draw.Create(0)
QUICKOCC    = Draw.Create(0)
QOCCDIST    = Draw.Create(0.5)
NOGUI    = Draw.Create(0)
SMALLMESH    = Draw.Create(0)

## Events numbers ##
####################

CHANGE_EXP = 1
NO_EVENT   = 2
DOF_CAMERA = 3
SPHER_CAMERA = 4
FISH_CAMERA = 5
FILE_TYPE = 6
FILTER_EVENT = 7
CHANGE_AA = 8
CHANGE_SHAD = 9
CHANGE_CAM = 10
CHANGE_ACC = 11
CHANGE_LIGHT = 12
CHANGE_BCKGRD = 13
CHANGE_GI = 14
PHOTON_EVENT = 15
gPHOTON_EVENT = 16
FORCE_INSTANTGI = 17
FORCE_IRRCACHE = 18
FORCE_PATHTRACE = 19
FORCE_GLOBALS = 20
OVERRIDE_CAUSTICS = 21
OVERRIDE_GLOBALS = 22
OVERRIDE_GI = 23
EVENT_CAUSTICS = 24
IBL_EVENT = 25
EXP_EVT = 26
REND_EVT = 27
CONV_LAMP = 28
QUICK_OPT = 29
QUICK_OCC = 30
SUN_EVENT = 31
BCKGRD_EVENT = 32
CBCKGRD_EVENT = 33
LOCK_EVENT = 34
SET_PATH = 35
CHANGE_CFG = 36
SET_JAVAPATH = 37
SHADER_TYPE = 38
SHAD_OK = 39

## Lists ##
###########

IMGFILTERLIST = ["box", "gaussian", "mitchell", "triangle", "catmull-rom", "blackman-harris", "sinc", "lanczos"]
FILETYPE
PHOTONMAPLIST = ["kd"]
gPHOTONMAPLIST = ["kd", "grid"]

###################
##  global vars  ##
###################

global FILE, SCENE, IM_HEIGHT, IM_WIDTH, TEXTURES, OBJECTS, IBLLIGHT, LAYERS, SCREEN
global DOFDIST
FILENAME = Blender.Get('filename').replace(".blend", ".sc")
SFPATH = ""
JAVAPATH = ""

#######################
##  start of export  ##
#######################

print "\n\n"
print "blend2sunflow v1.0.5"

## Export logic for simple options ##
#####################################

def export_output():
	print "o exporting output details..."
	FILE.write("image {\n")
	FILE.write("\tresolution %d %d\n" % (IM_WIDTH, IM_HEIGHT))
	FILE.write("\taa %s %s\n" % (MINAA.val, MAXAA.val))
	FILE.write("\tfilter %s\n" % IMGFILTERLIST[IMGFILTER.val-1])
	if AAJITTER == 1:
		FILE.write("\tjitter true\n")
	FILE.write("}")
	FILE.write("\n")
	
	print "o exporting trace-depths options..."
	FILE.write("trace-depths {\n")
	FILE.write("\tdiff %s \n" % DEPTH_DIFF)
	FILE.write("\trefl %s \n" % DEPTH_REFL)
	FILE.write("\trefr %s\n" % DEPTH_REFR)
	FILE.write("}")
	FILE.write("\n")
	if IMP_BCKGRD.val == 1:
		print "o exporting background..."
		world = Blender.World.GetCurrent() 
		horcol = world.getHor()
		horcol0, horcol1, horcol2 = horcol[0], horcol[1], horcol[2]
		FILE.write("background {\n")
		FILE.write("\tcolor  { \"sRGB nonlinear\" %s %s %s }\n" % (horcol0, horcol1, horcol2))
		FILE.write("}")
		FILE.write("\n")
	elif BACKGROUND.val == 1:
		print "o creating background..."
		FILE.write("background {\n")
		FILE.write("\tcolor  { \"sRGB nonlinear\" %s %s %s }\n" % (BCKGRDR.val, BCKGRDG.val, BCKGRDB.val))
		FILE.write("}")
		FILE.write("\n")

## Export caustic and global illumination settings ##
#####################################################

def export_gi():
	#Caustic Settings 
	if CAUSTICS.val == 1:
		print "o exporting caustic settings..."
		FILE.write("\nphotons {\n")
		FILE.write("\tcaustics %s" % PHOTONNUMBER.val)
		FILE.write(" %s " % PHOTONMAPLIST[PHOTONMAP.val-1])
		FILE.write("%s %s\n" % (PHOTONESTIMATE.val, PHOTONRADIUS.val))
		FILE.write("}\n")
	#Instant GI Settings
	if INSTANTGI.val == 1:
		print "o exporting Instant GI settings..."
		FILE.write("\ngi {\n")
		FILE.write("\ttype igi\n")
		FILE.write("\tsamples %s\n" % IGISAMPLES.val)
		FILE.write("\tsets %s\n" % IGISETS.val)
		FILE.write("\tb %s\n" % IGIBIAS.val)
		FILE.write("\tbias-samples %s\n" % IGIBIASSAMPLES.val)
		FILE.write("}\n")
	#Irradiance Cache GI Settings
	if IRRCACHE.val == 1:
		print "o exporting Irradiance Cache GI settings..."
		FILE.write("\ngi {\n")
		FILE.write("\ttype irr-cache\n")
		FILE.write("\tsamples %s\n" % IRRSAMPLES.val)
		FILE.write("\ttolerance %s\n" % IRRTOLERANCE.val)
		FILE.write("\tspacing %s %s\n" % (IRRSPACEMIN.val, IRRSPACEMAX.val))
		if USEGLOBALS.val == 0:
			FILE.write("}\n")
	#No Path Tracing on Secondary Bounces in Irradiance Cache Settings
	if USEGLOBALS.val == 1:
		FILE.write("\tglobal %s" % gPHOTONNUMBER.val)
		FILE.write(" %s " % gPHOTONMAPLIST[gPHOTONMAP.val-1])
		FILE.write("%s %s\n" % (gPHOTONESTIMATE.val, gPHOTONRADIUS.val))
		FILE.write("}\n")
	#Path Tracing GI Settings
	if PATHTRACE.val == 1:
		print "o exporting Path Tracing GI settings..."
		FILE.write("\ngi {\n")
		FILE.write("\ttype path\n")
		FILE.write("\tsamples %s\n" % PATHSAMPLES.val)
		FILE.write("}\n")
	#View Overrides
	if VIEWCAUSTICS.val == 1:
		print "o exporting caustic override..."
		FILE.write("\nshader {\n")
		FILE.write("\tname debug_caustics\n")
		FILE.write("\ttype view-caustics\n")
		FILE.write("}\n")
		FILE.write("override debug_caustics false\n")
	if VIEWGLOBALS.val == 1:
		print "o exporting globals override..."
		FILE.write("\nshader {\n")
		FILE.write("\tname debug_globals\n")
		FILE.write("\ttype view-global\n")
		FILE.write("}\n")
		FILE.write("override debug_globals false\n")
	if VIEWGI.val == 1:
		print "o exporting irradiance override..."
		FILE.write("\nshader {\n")
		FILE.write("\tname debug_gi\n")
		FILE.write("\ttype view-irradiance\n")
		FILE.write("}\n")
		FILE.write("override debug_gi false\n")
	
## Export logic for materials ##
################################

def export_shaders():
	print "o exporting shaders..."
	# default shader
	FILE.write("\n\nshader {\n\tname def\n\ttype diffuse\n\tdiff  1 1 1\n}")
	if OCCLUSION.val == 1:
		FILE.write("\n\nshader {\n   name amboccshader\n   type amb-occ2\n")
		FILE.write("\tbright { \"sRGB nonlinear\" %s %s %s }\n" % (OCCBRIGHTR.val, OCCBRIGHTG.val, OCCBRIGHTB.val))
		FILE.write("\tdark { \"sRGB nonlinear\" %s %s %s }\n" % (OCCDARKR.val, OCCDARKG.val, OCCDARKB.val))
		FILE.write("\tsamples %s\n" % OCCSAMPLES.val)
		FILE.write("\tdist %s\n}" % OCCDIST.val)
		FILE.write("\n\noverride amboccshader true")

	materials = Blender.Material.get()

	for mat in materials:
		RGB = mat.getRGBCol()
		speccol = mat.getSpecCol()
		textures = mat.getTextures()
		flags = mat.getMode()

		if mat.users == 0: 
			continue 

		# UBER shader
		if mat.name.startswith("sfube"):
			textu = textures[0]
			textu2 = textures[2]
			colvalue = textu.colfac
			cspvalue = textu.varfac
			
			print "  o exporting uber shader "+mat.name+"..."
			FILE.write("\n\nshader {\n")
			FILE.write("\tname \""+mat.name+".shader\"\n")	
			FILE.write("\ttype uber\n")
			
			# DIFF values
			FILE.write("\tdiff %s %s %s\n" % (RGB[0], RGB[1], RGB[2]))
			if textures[0] <> None and textures[0].tex.getType() == "Image" and textu.tex.getImage() != None:
				FILE.write("\tdiff.texture \"" + textu.tex.getImage().getFilename() + "\"\n")
			else:
				FILE.write("\tdiff.texture None")	
			FILE.write("\tdiff.blend %s\n" % (colvalue * 1.0))
			
			# SPEC values
			FILE.write("\tspec %s %s %s\n" % (speccol[0], speccol[1], speccol[2]))
			if textures[2] <> None and textures[2].tex.getType() == "Image" and textu2.tex.getImage() != None:
				FILE.write("\tspec.texture \"" + textu2.tex.getImage().getFilename() + "\"\n")
			else:
				FILE.write("\tspec.texture None")	
			FILE.write("\tspec.blend %s\n" % (cspvalue * 0.1))
			FILE.write("\tglossy .1\n")
			FILE.write("\tsamples 4\n}")

		elif textures[0] <> None and textures[0].tex.getType() == "Image":
			textu = textures[0]
			# image texture without image !!??
			if textu.tex.getImage() == None:
				print ("You material named " +mat.name+ " have an image texture with no image!")
				print ("replacing with default shader!")
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" % (1.0, 1.0, 1.0))

			# ambocc texture shader
			elif mat.name.startswith("sfamb"):	
				print "  o exporting ambient occlusion texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype amb-occ\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\tdark %s %s %s\n" % (speccol[0], speccol[1], speccol[2]))
				FILE.write("\tsamples 32\n")
				FILE.write("\tdist 0.0\n}")
				
			# diffuse texture shader
			elif mat.name.startswith("sfdif"):
				print "  o exporting diffuse texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")
				
			# phong texture shader
			elif mat.name.startswith("sfpho"):
				print "  o exporting phong texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype phong\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\tspec { \"sRGB nonlinear\" %s %s %s } %s\n" %(speccol[0], speccol[1], speccol[2], mat.hard))
				FILE.write("\tsamples 4\n}")
				
			# ward texture shader
			elif mat.name.startswith("sfwar"):
				print "  o exporting ward texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype ward\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				speccol = mat.specCol
				FILE.write("\tspec { \"sRGB nonlinear\" %s %s %s }\n" %(speccol[0], speccol[1], speccol[2]))
				FILE.write("\trough .2 .01\n")
				FILE.write("\tsamples 4\n}")

			# shiny texture shader
			elif mat.name.startswith("sfshi"):
				print "  o exporting shiny texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype shiny\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\trefl %s\n}" % mat.getRayMirr())
			
			# newcommers default diffuse texture shader
			else:
				print "  o exporting diffuse texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")

		else:
		
			FILE.write("\n\nshader {\n")
			FILE.write("\tname \""+mat.name+".shader\"\n")
			
			## diffuse shader
			if mat.name.startswith("sfdif"):
				print "  o exporting diffuse shader "+mat.name+"..."
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" % (RGB[0], RGB[1], RGB[2]))
			
			## shiny shader
			elif mat.name.startswith("sfshi"):
				print "  o exporting shiny shader "+mat.name+"..."
				FILE.write("\ttype shiny\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n" % (RGB[0], RGB[1], RGB[2]))
				FILE.write("\trefl %s\n}" % mat.getRayMirr())
				
			## amb-occ shader
			elif mat.name.startswith("sfamb"):
				print "  o exporting ambient occlusion shader "+mat.name+"..."
				FILE.write("\ttype amb-occ\n")
				FILE.write("\tbright %s %s %s\n" % (RGB[0], RGB[1], RGB[2]))
				FILE.write("\tdark %s %s %s\n" % (speccol[0], speccol[1], speccol[2]))
				FILE.write("\tsamples 32\n")
				FILE.write("\tdist 0.0\n}")
				
			## phong shader
			elif mat.name.startswith("sfpho"):
				print "  o exporting phong shader "+ mat.name+"..."
				FILE.write("\ttype phong\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n" % (RGB[0],RGB[1],RGB[2]))
				FILE.write("\tspec { \"sRGB nonlinear\" %s %s %s } %s\n" %(speccol[0], speccol[1], speccol[2], mat.hard))
				FILE.write("\tsamples 4\n}")
				
			## ward shader
			elif mat.name.startswith("sfwar"):
				print "  o exporting ward shader "+ mat.name+"..."
				FILE.write("\ttype ward\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n" %(RGB[0],RGB[1],RGB[2]))
				speccol = mat.specCol
				FILE.write("\tspec { \"sRGB nonlinear\" %s %s %s }\n" %(speccol[0], speccol[1], speccol[2]))
				FILE.write("\trough .2 .01\n")
				FILE.write("\tsamples 4\n}")

			## reflection (mirror) shader
			elif mat.name.startswith("sfmir") and flags & Material.Modes['RAYMIRROR']:
				print "  o exporting mirror shader "+mat.name+"..."
				FILE.write("\ttype mirror\n")
				FILE.write("\trefl { \"sRGB nonlinear\" %s %s %s }\n}" %(RGB[0],RGB[1],RGB[2]))
				
			## glass shader
			elif mat.name.startswith("sfgla") and flags & Material.Modes['RAYTRANSP']:
				print "  o exporting glass shader "+mat.name+"..."
				FILE.write("\ttype glass\n")
				FILE.write("\teta " + str(mat.getIOR()) + "\n")
				FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n" %(RGB[0],RGB[1],RGB[2]))
				FILE.write("\tabsorbtion.distance 5.0\n")
				FILE.write("\tabsorbtion.color { \"sRGB nonlinear\" %s %s %s }\n}" %(speccol[0], speccol[1], speccol[2]))
				
			## constant shader
			elif mat.name.startswith("sfcon"):
				print "  o exporting constant shader "+mat.name+"..."
				FILE.write("\ttype constant\n")
				FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n}" % (RGB[0], RGB[1], RGB[2]))

			## newcommers default diffuse shader
			else:
				print "  o exporting default diffuse shader "+mat.name+"..."
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" % (RGB[0], RGB[1], RGB[2]))

## Export modifiers ##
######################

def export_modifiers():
	print "o exporting modifiers..."
		
	materials = Blender.Material.get()
	modifs_list = []
	
	for mat in materials:
		
		textures = mat.getTextures()
		flags = mat.getMode()

		if textures[1] <> None and textures[1].tex.getType() == "Image":
			textu = textures[1]
			Scale_value = str(textu.norfac * textu.mtNor)
			
			if textu.tex.name.startswith("bump"):
				if textu.tex.getName() not in modifs_list:
					modifs_list.append (str(textu.tex.getName()))
					print "  o exporting modifier "+str(textu.tex.getName())+"..."
					FILE.write("\n\nmodifier {\n\tname "+str(textu.tex.getName())+"\n\ttype bump\n")
					FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
					FILE.write("\tscale %s \n}\n" % Scale_value)
				
			elif textu.tex.name.startswith("normal"):
				if textu.tex.getName() not in modifs_list:
					modifs_list.append (str(textu.tex.getName()))
					print "  o exporting modifier "+str(textu.tex.getName())+"..."
					FILE.write("\n\nmodifier {\n\tname "+str(textu.tex.getName())+"\n\ttype normalmap\n")
					FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")
			else:
				pass

## Export logic for Blender's light sources ##
##############################################

def export_lights(lmp):

	# only lamps type 0, 1 and 4 supported at the moment
	# lamp types are: 0 - Lamp, 1 - Sun, 2 - Spot, 3 - Hemi, 4 - Area
	# Spots are replaced by directional (cylinrical) lights: adjust dist as close as possible to the ground receiving the
	# cone of light if you want Radius as close as possible
	lamp = lmp.getData()
	print "o exporting lamps"

	if lamp.type == 0:
		print "  o exporting lamp "+lmp.name+"..."
		# get the rgb component for the lamp
		red   = lamp.col[0]
		green = lamp.col[1]
		blue  = lamp.col[2]
		power = lamp.energy * LAMPPOWER.val

		# get the location of the lamp
		objmatrix = lmp.matrix
		lampV = Mathutils.Vector([0, 0, 0, 1])
		lampV = lampV * objmatrix

		FILE.write("\n\nlight {\n")
		FILE.write("\ttype point\n")
		FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
		FILE.write("\tpower %s\n" % (power))
		FILE.write("\tp %s %s %s\n" % (lampV[0], lampV[1], lampV[2]))
		FILE.write("}")
	elif lamp.type == 1:
		if IMP_SUN.val == 1:
			print "  o exporting sun-light "+lmp.name+"..."
			invmatrix = Mathutils.Matrix(lmp.getInverseMatrix())
			FILE.write("\nlight {\n")
			FILE.write("\ttype sunsky\n")
			FILE.write("\tup 0 0 1\n")
			FILE.write("\teast 0 1 0\n")
			FILE.write("\tsundir %f %f %f\n" % (invmatrix[0][2], invmatrix[1][2], invmatrix[2][2]))
			FILE.write("\tturbidity %s\n" % SUN_TURB.val)
			FILE.write("\tsamples %s\n" % SUN_SAMPLES.val)
			FILE.write("}")
		else:
			print "  o exporting lamp "+lmp.name+"..."
			# get the rgb component for the lamp
			red   = lamp.col[0]
			green = lamp.col[1]
			blue  = lamp.col[2]
			power = lamp.energy * LAMPPOWER.val

			# get the location of the lamp
			objmatrix = lmp.matrix
			lampV = Mathutils.Vector([0, 0, 0, 1])
			lampV = lampV * objmatrix

			FILE.write("\n\nlight {\n")
			FILE.write("\ttype point\n")
			FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
			FILE.write("\tpower %s\n" % (power))
			FILE.write("\tp %s %s %s\n" % (lampV[0], lampV[1], lampV[2]))
			FILE.write("}")

	elif lamp.type == 4:
		print "  o exporting area-light "+lmp.name+"..."
		objmatrix = lmp.matrix
		xsize = lamp.areaSizeX * 0.5
		if lamp.areaSizeY:
			# If rectangular area:
			print "o exporting rectangular area-light "+lmp.name+"..."
			ysize = lamp.areaSizeY * 0.5
		else:
			# Else, square area:
			print "o exporting square area-light "+lmp.name+"..."
			ysize = xsize
		lampV0 = Mathutils.Vector([-xsize, ysize, 0, 1])
		lampV1 = Mathutils.Vector([ xsize, ysize, 0, 1])
		lampV2 = Mathutils.Vector([ xsize, -ysize, 0, 1])
		lampV3 = Mathutils.Vector([-xsize, -ysize, 0, 1])

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
	elif lamp.type == 2:
		print "  o exporting spotlight "+lmp.name+"..."
		objmatrix = lmp.matrix
		targetV = Mathutils.Vector([0, 0, -1, 1])
		targetV = targetV * objmatrix
		lampV = Mathutils.Vector([0, 0, 0, 1])
		lampV = lampV * objmatrix
		angle = lamp.getSpotSize()*pi/360
		dist = lamp.getDist()
		radius = dist/cos(angle)*sin(angle)

		red   = lamp.col[0]
		green = lamp.col[1]
		blue  = lamp.col[2]
		radiance = lamp.energy * LAMPPOWER.val

		FILE.write("\n\nlight {\n")
		FILE.write("\ttype directional\n")
		FILE.write("\tsource %s %s %s\n" % (lampV[0], lampV[1], lampV[2]))
		FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tradius %s\n" % (radius))
		FILE.write("\temit { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
		FILE.write("}")

	else:
		print "Unsupported type lamp detected"
		if CONVLAMP.val == 1:
			print "  o exporting lamp "+lmp.name+"..."
			# get the rgb component for the lamp
			red   = lamp.col[0]
			green = lamp.col[1]
			blue  = lamp.col[2]
			power = lamp.energy * LAMPPOWER.val

			# get the location of the lamp
			objmatrix = lmp.matrix
			lampV = Mathutils.Vector([0, 0, 0, 1])
			lampV = lampV * objmatrix

			FILE.write("\n\nlight {\n")
			FILE.write("\ttype point\n")
			FILE.write("\tcolor { \"sRGB nonlinear\" %s %s %s }\n" % (red, green, blue))
			FILE.write("\tpower %s\n" % (power))
			FILE.write("\tp %s %s %s\n" % (lampV[0], lampV[1], lampV[2]))
			FILE.write("}")

## Export method for Image Based Lights ##
##########################################

def export_ibl():
	global IBLLIGHT

	try:
		ibllighttext = Blender.Texture.Get("ibllight")
		if ibllighttext <> "":
			if ibllighttext.users > 0:
				if ibllighttext <> None and ibllighttext.getType() == "Image":
					IBLLIGHT = ibllighttext.getImage().getFilename()
				else:
					IBLLIGHT = ""
			else:
				IBLLIGHT = ""
	except:
		IBLLIGHT = ""

	if IBL == 1:
		if IBLLIGHT <> "":
			print "o exporting ibllight..."
			print "  o using texture %s" % (IBLLIGHT)
			FILE.write("\n\nlight {\n")
			FILE.write("\ttype ibl\n")
			FILE.write("\timage \"%s\"\n" % (IBLLIGHT))
			FILE.write("\tcenter 1 0 0\n")
			FILE.write("\tup 0 0 1\n")
			if IBLLOCK == 1:
				FILE.write("\tlock false\n")	# lock false means "use importance sampling"
			else:
				FILE.write("\tlock true\n")
			FILE.write("\tsamples %s\n" % IBLSAMPLES.val)
			FILE.write("}")

## Export method for Blender camera ##
######################################

def export_camera(cam):

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

	DOFDIST=camera.dofDist

	FILE.write("\n\ncamera {\n")

	if DOF.val == 1:
		camtype = "thinlens"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %s %s %s\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %s %s %s\n" % (upV[0], upV[1], upV[2]))
		FILE.write("\tfov    %s \n" % fov)
		FILE.write("\taspect %s \n" % (1.0 * IM_WIDTH / IM_HEIGHT))
		FILE.write("\tfdist %s \n" % DOFDIST)
		FILE.write("\tlensr %s \n" % DOFRADIUS)
		FILE.write("\tsides %s \n" % LENSSIDES)		#added by olivS
		FILE.write("\trotation %s \n" % LENSROTATION)	#added by olivS
	elif SPHERICALCAMERA.val == 1:
		camtype = "spherical"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %s %s %s\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %s %s %s\n" % (upV[0], upV[1], upV[2]))
	elif FISHEYECAMERA.val == 1:
		camtype = "fisheye"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %s %s %s\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %s %s %s\n" % (upV[0], upV[1], upV[2]))
	else:
		camtype = "pinhole"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %s %s %s\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %s %s %s\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %s %s %s\n" % (upV[0], upV[1], upV[2]))
		FILE.write("\tfov    %s \n" % fov)
		FILE.write("\taspect %s \n" % (1.0 * IM_WIDTH / IM_HEIGHT))
	FILE.write("}")

## Export method for meshes ##
##############################

def export_geometry(obj):
	#mesh = "";verts="";faces="";numverts=""
	islight = obj.name.startswith("meshlight")
	if islight:
		print "o exporting meshlight " + obj.name+"..."
	else:
		print "o exporting mesh " + obj.name+"..."
	# get the mesh data
	mesh = NMesh.GetRawFromObject(obj.name)
	mesh.transform(obj.getMatrix(), 1)
	verts = mesh.verts
	faces = mesh.faces

	numverts = verts.__len__()

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
				
				####### NEW PART FOR MODIFIERS ####################
				for mat in mesh.materials:
					textures = mat.getTextures()
					textu = textures[1]
					if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
						FILE.write("\tmodifier \"" + str(textu.tex.getName()) + "\"\n")
				###### End new part #######
				
			elif len(mesh.materials) > 1:
				FILE.write("\tshaders %d\n" % (len(mesh.materials)))
				
				for mat in mesh.materials:
					FILE.write("\t\t\"" + mat.name + ".shader\"\n")
					
				####### NEW PART FOR MODIFIERS ####################
				
				FILE.write("\tmodifiers %d\n" % (len(mesh.materials)))
				for mat in mesh.materials:

					textures = mat.getTextures()
					textu = textures[1]
					if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
						FILE.write("\t\t\"" + textu.tex.getName() + "\"\n")
					else:
						FILE.write("\t\t\"" + "None" + "\"\n")

				###### End new part #######
			else:
				FILE.write("\tshader def\n")
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
			smooth = face.smooth <> 0
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
					if face.smooth <> 0:
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

########################
## Main export method ##
########################

def exportfile(filename):
	global FILE, SCENE, IM_HEIGHT, IM_WIDTH, TEXTURES, OBJECTS, LAYERS, IBLLIGHT, EXP_ANIM, fname, destname
	global PATH

	SCENE     = Blender.Scene.GetCurrent()
	IM_HEIGHT = SCENE.getRenderingContext().imageSizeY() * (SCENE.getRenderingContext().getRenderWinSize() / 100.0) 
	IM_WIDTH = SCENE.getRenderingContext().imageSizeX() * (SCENE.getRenderingContext().getRenderWinSize() / 100.0) 
	TEXTURES  = Blender.Texture.Get()

	LAYERS    = SCENE.getLayers()
	STARTFRAME = SCENE.getRenderingContext().startFrame()
	ENDFRAME  = SCENE.getRenderingContext().endFrame()
	CTX       = SCENE.getRenderingContext()
	orig_frame = CTX.currentFrame()

	if not filename.endswith(".sc"):
		filename = filename + ".sc"
	fname = filename
	destname = fname.replace(".sc", "")

	ANIM = EXP_ANIM.val

	if ANIM == 1:
		base = os.path.splitext(os.path.basename(filename))[0]
		FILE = open(filename.replace(".sc", ".java"), "wb")
		FILE.write("""
public void build() {
	parse("%s" + ".settings.sc");
	parse("%s" + "." + getCurrentFrame() + ".sc");
}
""" % (base, base)) ### FIM DO FILE.WRITE ###

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
		OBJECTS = Blender.Scene.GetCurrent().getChildren()

		if ANIM == 1:
			filename = filename.replace(".sc", ".%d.sc" % (CTX.currentFrame()))

		print "Exporting to: %s" % (filename)

		FILE = open(filename, 'wb')

		if ANIM == 0:
			export_output()
			export_gi()
		export_shaders()
		export_modifiers()
		export_ibl()
		export_camera(SCENE.getCurrentCamera())
		for object in OBJECTS:
			if object.users > 1 and object.layers[0] in LAYERS:
				if object.getType() == 'Lamp':
					export_lights(object)
				if object.getType() == 'Mesh' or object.getType() == 'Surf':
					if object.name.startswith("meshlight"):
						export_geometry(object)
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
						export_geometry(object)
		FILE.close()

	if ANIM == 1:
		CTX.currentFrame(orig_frame)
		SCENE.makeCurrent()

	print "Export finished."

if __name__ == '__main__':
	Blender.Window.FileSelector(exportfile, "Export .sc", Blender.sys.makename(ext=".sc"))

## Global event handler ##
##########################

def event(evt, val):
	if evt == Draw.ESCKEY:
		print "quitting exporter..."
		Draw.Exit()

## Macro for writing the SF config file ##
##########################################

def setpath(SFPATH):
	datadir=Blender.Get("datadir")
	# create 'path2sf.cfg
	f = open(datadir + '/path2sf.cfg', 'w')
	f.write(str(SFPATH)+"\n")
	f.write(str(MEM)+"\n")
	f.write(str(THREADS)+"\n")
	f.close()

def setjavapath(JAVAPATH):
	datadir=Blender.Get("datadir")
	f = open(datadir + '/path2sf.cfg', 'a')
	f.write(str(JAVAPATH))
	f.close()

## Macro for render execution ##
################################

def render():
	# TODO:
	# 1- Make compatible with animations
	global COMMAND, memory, threads, sfpath, trial, javapath, fname, destname

	exportfile(fname)
	destname = fname.replace(".sc", "")
	# Get blenders 'bpydata' directory:
	datadir=Blender.Get("datadir")
	# Check existence of SF config file:
	trial=os.path.exists(datadir+'/path2sf.cfg')
	# Continue rendering if file exists:
	if trial == True:
		print "True"
		# open 'path2sf.cfg'
		f = open(datadir + '/path2sf.cfg', 'r+')
		lines = f.readlines()
		# Remove EOL:
		lines[0]=lines[0].rstrip("\n")
		lines[1]=lines[1].rstrip("\n")
		lines[2]=lines[2].rstrip("\n")
		lines[3]=lines[3].rstrip("\n")
		# Allocate values to variables:
		sfdir = lines[0]
		memory = lines[1]
		threads = lines[2]
		javadir = lines[3]
		f.close()

		# base command for executing SF:
		EXEC="%s\java -server -Xmx%sM -jar \"%ssunflow.jar\"" % (javadir, memory, sfdir)
		#print EXEC

		# Building the options to be passed to SF:
		if NOGUI == 1:
			option1="-nogui "
		else:
			option1=""
		option2="-threads %s " % THREADS
		if SMALLMESH == 1:
			option3="-smallmesh "
		else:
			option3=""
		if NOGI == 1:
			option4="-nogi "
		else:
			option4=""
		if NOCAUSTICS == 1:
			option5="-nocaustics "
		else:
			option5=""
		if QUICKOPT.val == 1:
			option6=""
		if QUICKOPT.val == 2:
			option6="-quick_uvs "
		elif QUICKOPT.val == 3:
			option6="-quick_normals "
		elif QUICKOPT.val == 4:
			option6="-quick_id "
		elif QUICKOPT.val == 5:
			option6="-quick_prims "
		elif QUICKOPT.val == 6:
			option6="-quick_gray "
		elif QUICKOPT.val == 7:
			option6="-quick_wire -aa 2 3 -filter mitchell "
		if QUICKOCC == 1:
			option7="-quick_ambocc %s" % (QOCCDIST)
		else:
			option7=""
		if IPR == 1:
			option8="-ipr "
		else:
			option8=""
		print option6

 		if FILETYPE == 1:
			ext="png"
		elif FILETYPE == 2:
			ext="tga"
		elif FILETYPE == 3:
			ext="hdr"
		elif FILETYPE == 4:
			ext="exr"

		# Definition of the command to render the scene:
		COMMAND="%s \"%s\" %s%s%s%s%s%s%s%s -o \"%s.%s\"" % (EXEC, fname, option1, option2, option3, option4, option5, option6, option7, option8, destname, ext)
		print COMMAND
		# Execute the command:
		pid=os.system(COMMAND)
	
## GUI button event handler ##
##############################

def buttonEvent(evt):
	global FILE, SCREEN, SETPATH, SETJAVAPATH, FILENAME

	## Common events:
	if evt == EXP_EVT:
		Blender.Window.FileSelector(exportfile, "Export .sc", FILENAME)
	if evt == SET_PATH:
		Blender.Window.FileSelector(setpath, "SET SF PATH", SFPATH)
		setpath(SFPATH)
	if evt == SET_JAVAPATH:
		Blender.Window.FileSelector(setjavapath, "SET JAVA PATH", JAVAPATH)
		setjavapath(JAVAPATH)
	if evt == REND_EVT:
		render()

	## Setting exclusive buttons rules:
	if evt == DOF_CAMERA:
		if DOF.val == 1:
			if SPHERICALCAMERA.val == 1:
				SPHERICALCAMERA.val = 0
				Draw.Redraw()
			elif FISHEYECAMERA.val == 1:
				FISHEYECAMERA.val = 0
				Draw.Redraw()
	if evt == SPHER_CAMERA:
		if SPHERICALCAMERA.val == 1:
			if DOF.val == 1:
				DOF.val = 0
				Draw.Redraw()
			elif FISHEYECAMERA.val == 1:
				FISHEYECAMERA.val = 0
				Draw.Redraw()
	if evt == FISH_CAMERA:
		if FISHEYECAMERA.val == 1:
			if DOF.val == 1:
				DOF.val = 0
				Draw.Redraw()
			elif SPHERICALCAMERA.val == 1:
				SPHERICALCAMERA.val = 0
				Draw.Redraw()
	if evt == FILTER_EVENT:
		Draw.Redraw()
	if evt == FORCE_INSTANTGI:
		if INSTANTGI.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_IRRCACHE:
		if IRRCACHE.val == 1:
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
		if IRRCACHE.val == 0:
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
	if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == FORCE_PATHTRACE:
		if PATHTRACE.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_GLOBALS:
		if USEGLOBALS.val == 1:
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
		if IRRCACHE.val == 0:
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
		if USEGLOBALS.val == 0:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == EVENT_CAUSTICS:
		if CAUSTICS.val == 0:
			if VIEWCAUSTICS.val == 1:
				VIEWCAUSTICS.val = 0
				Draw.Redraw()	
	if evt == OVERRIDE_CAUSTICS:
		if CAUSTICS.val == 0:
			if VIEWCAUSTICS.val == 1:
				VIEWCAUSTICS.val = 0
				Draw.Redraw()
	if evt == OVERRIDE_GLOBALS:
		if USEGLOBALS.val == 0:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == OVERRIDE_GI:
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == IBL_EVENT:
			if IBL.val == 0:
				if IBLLOCK.val == 1:
					IBLLOCK.val = 0
					Draw.Redraw()
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
	if evt == LOCK_EVENT:
			if IBL.val == 0:
				if IBLLOCK.val == 1:
					IBLLOCK.val = 0
					Draw.Redraw()
	if evt == SUN_EVENT:
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == BCKGRD_EVENT:
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == CBCKGRD_EVENT:
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == QUICK_OPT:
		if QUICKOCC.val == 1:
			QUICKOCC.val = 0
			Draw.Redraw()
	if evt == QUICK_OCC:
		QUICKOPT.val = 1
		Draw.Redraw(0)

	## Rules for displaying the different panels:
	if evt == CHANGE_AA:
		SCREEN=0
		Draw.Redraw()
		return
	if evt == CHANGE_CAM:
		SCREEN=2
		Draw.Redraw()
		return
	if evt == CHANGE_BCKGRD:
		SCREEN=3
		Draw.Redraw()
		return
	if evt == CHANGE_LIGHT:
		SCREEN=4
		Draw.Redraw()
		return
	if evt == CHANGE_GI:
		SCREEN=5
		Draw.Redraw()
		return
	if evt == CHANGE_SHAD:
		SCREEN=6
		Draw.Redraw()
		return
	if evt == CHANGE_EXP:
		SCREEN=7
		Draw.Redraw()
		return
	if evt == CHANGE_CFG:
		SCREEN=8
		Draw.Redraw()
		return
	# Go back to config panel if user tries to render withour configuring SF:
	if evt == REND_EVT and trial == False:
		SCREEN=8
		Draw.Redraw()
		return
	if evt == SHAD_OK:
		Draw.Redraw()
		return

## Draws the individual panels ##
#################################

def drawGUI():
	global SCREEN
	if SCREEN==0:
		drawAA()
	if SCREEN==2:
		drawCamera()
	if SCREEN==3:
		drawBackground()
	if SCREEN==4:
		drawLights()
	if SCREEN==5:
		drawGI()
	if SCREEN==6:
		drawShad()
	if SCREEN==7:
		drawRender()
	if SCREEN==8:
		drawConfig()

## Draw AA settings ##
######################

def drawAA():
	global MINAA, MAXAA, AASAMPLES, AAJITTER, IMGFILTERW, IMGFILTERH, IMGFILTER
	col=10; line=175; BGL.glRasterPos2i(col, line); Draw.Text("AA:")
	col=100; MINAA=Draw.Number("Min AA", 2, col, line, 120, 18, MINAA.val, -4, 5); 
	col=230; MAXAA=Draw.Number("Max AA", 2, col, line, 120, 18, MAXAA.val, -4, 5)
	col=100; line=150; AASAMPLES=Draw.Number("Samples", 2, col, line, 120, 18, AASAMPLES.val, 0, 32)
	col=230; AAJITTER=Draw.Toggle("AA Jitter", 2, col, line, 120, 18, AAJITTER.val, "Use jitter for anti-aliasing")
	col=10; line=130; BGL.glRasterPos2i(col, line); Draw.Text("Image Filter:")
	col=100; line=125; IMGFILTER=Draw.Menu("%tImage Filter|box|gaussian|mitchell|triangle|catmull-rom|blackman-harris|sinc|lanczos", FILTER_EVENT, col, line, 120, 18, IMGFILTER.val)
	drawButtons()

## Draw camera options ##
#########################

def drawCamera(): 
	global DOF, DOFRADIUS, SPHERICALCAMERA, FISHEYECAMERA, LENSSIDES, LENSROTATION
	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("Camera:")
	col=100; line=145; DOF=Draw.Toggle("DOF", DOF_CAMERA, col, line, 120, 18, DOF.val, "Turn on depth of field")
	col=225; DOFRADIUS=Draw.Number("Radius", 2, col, line, 120, 18, DOFRADIUS.val, 0.00, 99.99)
	col=100; line=125; BGL.glRasterPos2i(col, line); Draw.Text("Bokeh shape -->")
	col=225; line=120; LENSSIDES=Draw.Number("Sides", 2, col, line, 120, 18, LENSSIDES.val, 2, 8)
	col=350; LENSROTATION=Draw.Number("Rotation", 2, col, line, 120, 18, LENSROTATION.val, 0.0, 360.0)
	col=100; line=95; SPHERICALCAMERA=Draw.Toggle("Spherical", SPHER_CAMERA, col, line, 120, 18, SPHERICALCAMERA.val, "Use the sperical camera type")
	col=100; line=70; FISHEYECAMERA=Draw.Toggle("Fisheye", FISH_CAMERA, col, line, 120, 18, FISHEYECAMERA.val, "Use the fisheye camera type")
	drawButtons()

## Draw Background options ##
#############################

def drawBackground():
	global IMP_BCKGRD, IMP_SUN, BACKGROUND, BCKGRDR, BCKGRDG, BCKGRDB, IBL, IBLLOCK, IBLSAMPLES, SUN_TURB, SUN_SAMPLES
	col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Simple background:")
	col=10; line=200; IMP_BCKGRD=Draw.Toggle("Import World", BCKGRD_EVENT, col, line, 120, 18, IMP_BCKGRD.val, "Set World's Horizon color as background")
	col=10; line=175; BACKGROUND=Draw.Toggle("Create Background", CBCKGRD_EVENT, col, line, 120, 18, BACKGROUND.val, "Define a background color")
	col=135; BCKGRDR=Draw.Number("Bckgrd (R)", 2, col, line, 125, 18, BCKGRDR.val, 0.000, 1.000)
	col=270; BCKGRDG=Draw.Number("Bckgrd (G)", 2, col, line, 125, 18, BCKGRDG.val, 0.000, 1.000)
	col=400; BCKGRDB=Draw.Number("Bckgrd (B)", 2, col, line, 125, 18, BCKGRDB.val, 0.000, 1.000)
	col=10; line=155; BGL.glRasterPos2i(col, line); Draw.Text("High Dynamic Range Illumination (Please set a texture named 'ibllight', lower case):")
	col=10; line=125; IBL=Draw.Toggle("Image Based Light", IBL_EVENT, col, line, 140, 18, IBL.val, "Use IBL/hdr type light")
	col=160; IBLLOCK=Draw.Toggle("Importance Sampling", LOCK_EVENT, col, line, 130, 18, IBLLOCK.val, "Use importance sampling (lock false), IBL must be on")
	col=300; IBLSAMPLES=Draw.Number("Samples", 2, col, line, 100, 18, IBLSAMPLES.val, 1, 1024)
	col=10; line=105; BGL.glRasterPos2i(col, line); Draw.Text("Sunsky:")
	col=10; line=75; IMP_SUN=Draw.Toggle("Import Sun", SUN_EVENT, col, line, 120, 18, IMP_SUN.val, "Import existing Sun")
	col=140; line=75; SUN_TURB=Draw.Number("Turbidity", 2, col, line, 120, 18, SUN_TURB.val, 0.0, 32.0)
	col=270; SUN_SAMPLES=Draw.Number("Samples", 2, col, line, 120, 18, SUN_SAMPLES.val, 1, 10000)
	drawButtons()

## Draw light options ##
########################

def drawLights():
	global MESHLIGHTPOWER, LAMPPOWER, DSAMPLES, CONVLAMP
	col=10; line=205; BGL.glRasterPos2i(col, line); Draw.Text("Global Options:")
	col=10; line=180; BGL.glRasterPos2i(col, line); Draw.Text("Meshlight:")
	col=110; line=175; MESHLIGHTPOWER=Draw.Number("Power", 2, col, line, 200, 18, MESHLIGHTPOWER.val, 1, 10000)
	col=10; line=155; BGL.glRasterPos2i(col, line); Draw.Text("Light:")
	col=110; line=150; LAMPPOWER=Draw.Number("Power", 2, col, line, 200, 18, LAMPPOWER.val, 1, 10000)
	col=10; line=130; BGL.glRasterPos2i(col, line); Draw.Text("Lightserver:")
	col=110; line=125; DSAMPLES=Draw.Number("Direct Samples", 2, col, line, 200, 18, DSAMPLES.val, 0, 1024) 
	col=10; line=105; BGL.glRasterPos2i(col, line); Draw.Text("Unknown lamps:")
	col=10; line = 75; CONVLAMP=Draw.Toggle("Convert lamps", 2, col, line, 140, 18, CONVLAMP.val, "Convert unsupported lamps into point light")
	drawButtons()

## Draw Shader options ##
#########################

def drawShad():
	global SHADTYPE, SHADOK
	col=10; line=400; BGL.glRasterPos2i(col, line); Draw.Text("Specific instructions for exporting shaders:")
	col=10; line=375; BGL.glRasterPos2i(col, line); Draw.Text("For exporting bump and normal maps, have the second texture slot (slot 1)")
	col=10; line=350; BGL.glRasterPos2i(col, line); Draw.Text("name begin with bump or normal")	
	col=10; line=325; BGL.glRasterPos2i(col, line); Draw.Text("Regarding Textures: Diffuse, shiny, ambocc, phong, and ward materials will")
	col=10; line=300; BGL.glRasterPos2i(col, line); Draw.Text("use textures as the diffuse channel if the texture is in the first texture slot.")
	col=10; line=275; SHADTYPE=Draw.Menu("%tSelect shader|Uber|Diffuse|Shiny|AO|Phong|Ward|Mirror|Glass|Constant", SHADER_TYPE, col, line, 85, 18, SHADTYPE.val)
	col=100; SHADOK=Draw.Button("OK", SHAD_OK, col, line, 30, 18, "Print on screen instructions")
	if SHADTYPE == 1:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Uber: shader name should start with 'sfube' - imports Blender's Col and Spe RGB")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("values")
		col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("\t\tIF Texture Slot 0: diffuse texture(Mapto Col value), else Col RGB values")
		col=10; line=175; BGL.glRasterPos2i(col, line); Draw.Text("\t\tIF Texture Slot 2: specular texture(Mapto Var value), else Spe RGB values")
	if SHADTYPE == 2:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Diffuse: shader name should start with 'sfdif' - imports Blender's Col RGB values")
	if SHADTYPE == 3:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Shiny: shader name sould start with 'sfshi' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("RayMirr values")
	if SHADTYPE == 4:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Ambient Occlusion: shader name sould start with 'sfamb' - imports Blender's")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Col RGB (Bright) and Spe RGB (Dark) values")
	if SHADTYPE == 5:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Phong: shader name sould start with 'sfpho' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Spe RGB values")
	if SHADTYPE == 6:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Ward: shader name sould start with 'sfwar' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Spe RGB values")
	if SHADTYPE == 7:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Mirror: shader name sould start with 'sfmir' - imports Blender's Col RGB values,")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Ray Mir button must be on")
	if SHADTYPE == 8:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Glass: shader name sould start with 'sfgla' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("IOR values, Ray Transp button must be on")
	if SHADTYPE == 9:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Constant: shader name should start with 'sfcon' - imports Blender's Col RGB values")
	drawButtons()

## Draw export and render options settings ##
#############################################

def drawRender():
	global EXPORT, RENDER, SMALLMESH, NOGI, NOCAUSTICS, QUICKUV, QUICKNORM, QUICKID, QUICKPRIMS, QUICKGRAY, QUICKWIRE, QUICKOCC, QOCCDIST, FILETYPE, DEPTH_DIFF, DEPTH_REFL, DEPTH_REFR, QUICKOPT, EXP_ANIM, IPR
	col=10; line=325; BGL.glRasterPos2i(col, line); Draw.Text("Rendering actions:")
	col=10; line=300; EXPORT=Draw.Button("Export .sc", EXP_EVT, col, line, 140, 18, "Export the scene to .sc file")
	col=160; RENDER=Draw.Button("Render exported", REND_EVT, col, line, 130, 18, "Render it (Export a .sc file first)")
	col=300; FILETYPE=Draw.Menu("%tFile type|png|tga|hdr|exr", FILE_TYPE, col, line, 85, 18, FILETYPE.val)
	col=10; line=275; BGL.glRasterPos2i(col, line); Draw.Text("Max raytrace depths:")
	col=10; line=250; DEPTH_DIFF=Draw.Number("Diffuse Depth", 2, col, line, 125, 18, DEPTH_DIFF.val, 1, 100)
	col=150; DEPTH_REFL=Draw.Number("Reflection Depth", 2, col, line, 125, 18, DEPTH_REFL.val, 1, 100)
	col=290; DEPTH_REFR=Draw.Number("Refraction Depth", 2, col, line, 125, 18, DEPTH_REFR.val, 1, 100)
	col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Rendering options:")
	col=10; line=200; SMALLMESH=Draw.Toggle("Small mesh", 2, col, line, 85, 18, SMALLMESH.val, "Load triangle meshes using triangles optimized for memory use")
	col=100; NOGI=Draw.Toggle("No GI", 2, col, line, 85, 18, NOGI.val, "Disable any global illumination engines in the scene")
	col=190; NOCAUSTICS=Draw.Toggle("No Caustics", 2, col, line, 85, 18, NOCAUSTICS.val, "Disable any caustic engine in the scene")
	col=280; NOGUI=Draw.Toggle("No GUI", 2, col, line, 85, 18, NOCAUSTICS.val, "Don't open the frame showing rendering progress")
	col=370; IPR=Draw.Toggle("IPR", 2, col, line, 85, 18, IPR.val, "Render using progressive algorithm")
	col=10; line=180; BGL.glRasterPos2i(col, line); Draw.Text("Quick override options:")
	col=10; line=150; QUICKOPT=Draw.Menu("%tQuick option|None|Quick UVs|Quick Normals|Quick ID|Quick Primitives|Quick Gray|Quick Wire", QUICK_OPT, col, line, 100, 18, QUICKOPT.val)
	col=10; line=125; QUICKOCC=Draw.Toggle("Quick Amb Occ", QUICK_OCC, col, line, 85, 18, QUICKOCC.val, "Applies ambient occlusion to the scene with specified maximum distance")
	col=100; QOCCDIST=Draw.Number("Distance", 2, col, line, 125, 18, QOCCDIST.val, 0.00, 1000.00)
	col=10; line=75; EXP_ANIM=Draw.Toggle("Export As Animation", 2, col, line, 140, 18, EXP_ANIM.val, "Export the scene as animation")
	drawButtons()

## Draw the SF configuration settings ##
########################################

def drawConfig():
	global SET_PATH, THREADS, MEM, SET_JAVAPATH
	col=155; line = 230; BGL.glRasterPos2i(col, line); Draw.Text("(threads=0 means auto-detect)")
	col=10; line = 225; THREADS=Draw.Number("Threads", 2, col, line, 140, 18, THREADS.val, 0, 8)
	col=10; line = 200; MEM=Draw.Number("Memory (MB)", 2, col, line, 140, 18, MEM.val, 256, 2048)
	col= 10; line = 175; Draw.Button("Store SF Path & Settings", SET_PATH, col, line, 140,18)
	col= 10; line = 150; Draw.Button("Store Java Path", SET_JAVAPATH, col, line, 140,18)
	# Get blenders 'bpydata' directory:
	datadir=Blender.Get("datadir")
	# Check if the config file exists:
	trial=os.path.exists(datadir+'/path2sf.cfg')
	# If config file exists, retrieve existing values:
	if trial == 1:
		# open 'path2sf.cfg'
		f = open(datadir + '/path2sf.cfg', 'r+')
		sfdir = f.readline()
		memory = f.readline()
		threads = f.readline()
		javadir = f.readline()
		col=10; line = 130; BGL.glRasterPos2i(col, line); Draw.Text("Sunflow path: %s" % sfdir)
		col=10; line = 110; BGL.glRasterPos2i(col, line); Draw.Text("Memory: %s MB" % memory)
		col=10; line = 90; BGL.glRasterPos2i(col, line); Draw.Text("Threads: %s" % threads)
		col=10; line = 70; BGL.glRasterPos2i(col, line); Draw.Text("Java path: %s" % javadir)
		f.close()
	# If config file doesn't exist, issue a warning:
	if trial == 0:
		col=10; line = 105; BGL.glRasterPos2i(col, line); Draw.Text("Sunflow is not configured yet - set Memory, Number of Threads and Path to sunflow.jar")
	drawButtons()

## Draw caustic and global illumination settings ##
###################################################

def drawGI():
	global CAUSTICS, PHOTONNUMBER, PHOTONMAP, PHOTONESTIMATE, PHOTONRADIUS
	global INSTANTGI, IGISAMPLES, IGISETS, IGIBIAS, IGIBIASSAMPLES
	global IRRCACHE, IRRSAMPLES, IRRTOLERANCE, IRRSPACEMIN, IRRSPACEMAX
	global gPHOTONNUMBER, gPHOTONESTIMATE, gPHOTONRADIUS, gPHOTONMAP, USEGLOBALS
	global PATHTRACE, PATHSAMPLES
	global VIEWCAUSTICS, VIEWGLOBALS, VIEWGI
	global OCCLUSION, OCCBRIGHTR, OCCBRIGHTG, OCCBRIGHTB, OCCDARKR, OCCDARKG, OCCDARKB, OCCSAMPLES, OCCDIST

	col=10; line=375; BGL.glRasterPos2i(col, line); Draw.Text("Caustics and Global Illumination")
	col=10; line=350; CAUSTICS=Draw.Toggle("Caustics", EVENT_CAUSTICS, col, line, 85, 18, CAUSTICS.val, "Turn on caustics in the scene")
	col=100; PHOTONNUMBER=Draw.Number("Photons", 2, col, line, 125, 18, PHOTONNUMBER.val, 0, 5000000)
	col=230; PHOTONMAP=Draw.Menu("%tCaustics Photon Map|kd", PHOTON_EVENT, col, line, 60, 18, PHOTONMAP.val)
	col=295; PHOTONESTIMATE=Draw.Number("Photon Estim.", 2, col, line, 125, 18, PHOTONESTIMATE.val, 0, 1000)
	col=425; PHOTONRADIUS=Draw.Number("Photon Radius", 2, col, line, 125, 18, PHOTONRADIUS.val, 0.00, 10.00)
	col=10; line=325; INSTANTGI=Draw.Toggle("Instant GI", FORCE_INSTANTGI, col, line, 85, 18, INSTANTGI.val, "Enable Instant GI for GI in the scene")
	col=100; IGISAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, IGISAMPLES.val, 0, 1024)
	col=230; IGISETS=Draw.Number("Number of Sets", 2, col, line, 125, 18, IGISETS.val, 1.0, 100.0)
	col=100; line=300; IGIBIAS=Draw.Number("Bias", 2, col, line, 125, 18, IGIBIAS.val, 0.00000, 0.00009)
	col=230; IGIBIASSAMPLES=Draw.Number("Bias Samples", 2, col, line, 125, 18, IGIBIASSAMPLES.val, 0, 1)
	col=10; line=275; IRRCACHE=Draw.Toggle("Irr. Cache", FORCE_IRRCACHE, col, line, 85, 18, IRRCACHE.val, "Enable Irradiance Caching for GI in the scene")
	col=100; IRRSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, IRRSAMPLES.val, 0, 1024)
	col=230; IRRTOLERANCE=Draw.Number("Tolerance", 2, col, line, 125, 18, IRRTOLERANCE.val, 0.0, 0.10)
	col=100; line=250; IRRSPACEMIN=Draw.Number("Min. Space", 2, col, line, 125, 18, IRRSPACEMIN.val, 0.00, 10.00)
	col=230; IRRSPACEMAX=Draw.Number("Max. Space", 2, col, line, 125, 18, IRRSPACEMAX.val, 0.00, 10.00)
	col=10; line=225; USEGLOBALS=Draw.Toggle("Use Globals", FORCE_GLOBALS, col, line, 85, 18, USEGLOBALS.val, "Use global photons instead of path tracing for Irr. Cache secondary bounces") 
	col=100; gPHOTONNUMBER=Draw.Number("Glob. Phot.", 2, col, line, 125, 18, gPHOTONNUMBER.val, 0, 5000000)
	col=230; gPHOTONMAP=Draw.Menu("%tGlobal Photon Map|kd|grid", gPHOTON_EVENT, col, line, 60, 18, gPHOTONMAP.val)
	col=295; gPHOTONESTIMATE=Draw.Number("Global Estim.", 2, col, line, 125, 18, gPHOTONESTIMATE.val, 0, 1000)
	col=425; gPHOTONRADIUS=Draw.Number("Global Radius", 2, col, line, 125, 18, gPHOTONRADIUS.val, 0.00, 10.00)
	col=10; line=200; PATHTRACE=Draw.Toggle("Path Tracing", FORCE_PATHTRACE, col, line, 85, 18, PATHTRACE.val, "Enable Path Tracing for GI in the scene")
	col=100; PATHSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, PATHSAMPLES.val, 0, 1024)
	col=100; line=175; VIEWCAUSTICS=Draw.Toggle("Just Caustics", OVERRIDE_CAUSTICS, col, line, 85, 18, VIEWCAUSTICS.val, "Render only the caustic photons in the scene (Caustics must be on)")
	col=190; VIEWGLOBALS=Draw.Toggle("Just Globals", OVERRIDE_GLOBALS, col, line, 85, 18, VIEWGLOBALS.val, "Render only the global photons in the scene (No Irr. Path must be on)")
	col=280; VIEWGI=Draw.Toggle("Just GI", OVERRIDE_GI, col, line, 85, 18, VIEWGI.val, "Render only the gi components in the scene (A GI engine must be selected)")

	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("Ambient Occlusion")
	col=10; line=125; OCCLUSION=Draw.Toggle("Amb Occ", 2, col, line, 85, 18, OCCLUSION.val, "Turn on ambient occlusion for the whole scene")
	col=100; OCCBRIGHTR=Draw.Number("Bright (R)", 2, col, line, 125, 18, OCCBRIGHTR.val, 0.0, 1.0)
	col=230; OCCBRIGHTG=Draw.Number("Bright (G)", 2, col, line, 125, 18, OCCBRIGHTG.val, 0.0, 1.0)
	col=360; OCCBRIGHTB=Draw.Number("Bright (B)", 2, col, line, 125, 18, OCCBRIGHTB.val, 0.0, 1.0)
	col=100; line=100; OCCDARKR=Draw.Number("Dark (R)", 2, col, line, 125, 18, OCCDARKR.val, 0.00, 1.0)
	col=230; OCCDARKG=Draw.Number("Dark (G)", 2, col, line, 125, 18, OCCDARKG.val, 0.0, 1.0)
	col=360; OCCDARKB=Draw.Number("Dark (B)", 2, col, line, 125, 18, OCCDARKB.val, 0.0, 1.0)
	col=100; line=75; OCCSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, OCCSAMPLES.val, 0, 256)
	col=230; OCCDIST=Draw.Number("Distance", 2, col, line, 125, 18, OCCDIST.val, -1.0, 150.0)

	drawButtons()

## Draw the bottom bar of buttons in the interface ##
#####################################################

def drawButtons():
	Draw.Button("Configure SF", CHANGE_CFG, 20, 10, 90, 50)
	Draw.Button("AA", CHANGE_AA, 115 , 40, 90, 20)
	Draw.Button("Camera", CHANGE_CAM, 210, 40, 90, 20)
	Draw.Button("Light", CHANGE_LIGHT, 305, 40, 90, 20)
	Draw.Button("Caustics/GI", CHANGE_GI, 115, 10, 90, 20)
	Draw.Button("Shader Info", CHANGE_SHAD, 210, 10, 90, 20)
	Draw.Button("Background", CHANGE_BCKGRD,305, 10, 90, 20)
	Draw.Button("Render settings", CHANGE_EXP, 400, 10, 90, 50)

SCREEN=0
Draw.Register(drawGUI, event, buttonEvent)
