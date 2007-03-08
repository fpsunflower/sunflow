#include <maya/MFnUnitAttribute.h>
#include <maya/MFnEnumAttribute.h>
#include <maya/MFnNumericAttribute.h>
#include <maya/MFnTypedAttribute.h>
#include <maya/MGlobal.h>

#include "sunflowGlobalsNode.h"

MTypeId sunflowGlobalsNode::id( 0x00114153 );

MObject sunflowGlobalsNode::preset;
MObject sunflowGlobalsNode::pixelFilter;
MObject sunflowGlobalsNode::minSamples;
MObject sunflowGlobalsNode::maxSamples;
MObject sunflowGlobalsNode::enablePhotons;
MObject sunflowGlobalsNode::Photons;
MObject sunflowGlobalsNode::PhotonsKd;
MObject sunflowGlobalsNode::PhotonsRadius;
MObject sunflowGlobalsNode::enableGI;
MObject sunflowGlobalsNode::GIMode;
MObject sunflowGlobalsNode::PTSamples;
MObject sunflowGlobalsNode::IGISamples;
MObject sunflowGlobalsNode::IGISets;
MObject sunflowGlobalsNode::IGIBias;
MObject sunflowGlobalsNode::IGIBSamples;
MObject sunflowGlobalsNode::ICSamples;
MObject sunflowGlobalsNode::ICTolerance;
MObject sunflowGlobalsNode::ICSpacingMin;
MObject sunflowGlobalsNode::ICSpacingMax;
MObject sunflowGlobalsNode::diffuseDepth;
MObject sunflowGlobalsNode::reflectionDepth;
MObject sunflowGlobalsNode::refractionDepth;
MObject sunflowGlobalsNode::exportPath;
MObject sunflowGlobalsNode::materialOverride;
MObject sunflowGlobalsNode::ambOverrideDist;
MObject sunflowGlobalsNode::javaPath;
MObject sunflowGlobalsNode::sunflowPath;

MStatus sunflowGlobalsNode::compute( const MPlug& plug, MDataBlock& data )
{
	return MS::kUnknownParameter;
}

MStatus sunflowGlobalsNode::initialize(){
	MStatus				stat;
	MFnTypedAttribute   tAttr;
	MFnEnumAttribute	enumAttr;
	MFnNumericAttribute	numericAttr;

	preset = enumAttr.create( "preset", "p", 0, &stat );
	stat = enumAttr.addField( "draft", 0 );
	stat = enumAttr.addField( "preview", 1 );
	stat = enumAttr.addField( "previewPathTrace", 2 );
	stat = enumAttr.addField( "production", 3 );

	stat = addAttribute (preset);
	if (!stat) { stat.perror("addAttribute preset"); return stat;}

	pixelFilter = enumAttr.create( "pixelFilter", "pf", 0, &stat );
	stat = enumAttr.addField( "box", 0 );
	stat = enumAttr.addField( "triangle", 1 );
	stat = enumAttr.addField( "catmull-rom", 2 );
	stat = enumAttr.addField( "mitchell", 3 );
	stat = enumAttr.addField( "lanczos", 4 );
	stat = enumAttr.addField( "blackman-harris", 5 );
	stat = enumAttr.addField( "sinc", 6 );

	stat = addAttribute (pixelFilter);
	if (!stat) { stat.perror("addAttribute pixelFilter"); return stat;}
	
	minSamples = numericAttr.create( "minSamples", "mins",	MFnNumericData::kInt, 0, &stat );
	stat = addAttribute (minSamples);
		if (!stat) { stat.perror("addAttribute minSamples"); return stat;}

	maxSamples = numericAttr.create( "maxSamples", "maxs",	MFnNumericData::kInt, 1, &stat );
	stat = addAttribute (maxSamples);
		if (!stat) { stat.perror("addAttribute maxSamples"); return stat;}


	enablePhotons = numericAttr.create("enablePhotons", "eph", MFnNumericData::kBoolean, false, &stat);
	stat = addAttribute (enablePhotons);
		if (!stat) { stat.perror("addAttribute enablePhotons"); return stat;}

	Photons = numericAttr.create( "Photons", "gip",	MFnNumericData::kInt, 1000000, &stat );
	stat = addAttribute (Photons);
		if (!stat) { stat.perror("addAttribute Photons"); return stat;}

	PhotonsKd = numericAttr.create("PhotonsKd", "pkd", MFnNumericData::kInt, 100, &stat);
	stat = addAttribute (PhotonsKd);
		if (!stat) { stat.perror("addAttribute PhotonsKd"); return stat;}
	
	PhotonsRadius = numericAttr.create("PhotonsRadius", "pr", MFnNumericData::kFloat, 0.5, &stat);
	stat = addAttribute (PhotonsRadius);
		if (!stat) { stat.perror("addAttribute PhotonsRadius"); return stat;}	

	enableGI = numericAttr.create("enableGI", "egi", MFnNumericData::kBoolean, false, &stat);
	stat = addAttribute (enableGI);
		if (!stat) { stat.perror("addAttribute enableGI"); return stat;}

	GIMode = enumAttr.create( "GIMode", "gm", 0, &stat );
	stat = enumAttr.addField( "path tracing", 0 );
	stat = enumAttr.addField( "IGI", 1 );
	stat = enumAttr.addField( "Irradiance Cache", 2 );	
	stat = addAttribute (GIMode);
	if (!stat) { stat.perror("addAttribute GIMode"); return stat;}

	PTSamples = numericAttr.create( "PTSamples", "pts",	MFnNumericData::kInt, 16, &stat );
	stat = addAttribute (PTSamples);
		if (!stat) { stat.perror("addAttribute PTSamples"); return stat;}

	IGISamples = numericAttr.create("IGISamples", "igis", MFnNumericData::kInt, 64, &stat);
	stat = addAttribute (IGISamples);
		if (!stat) { stat.perror("addAttribute IGISamples"); return stat;}

	IGISets = numericAttr.create("IGISets", "igie", MFnNumericData::kInt, 1, &stat);
	stat = addAttribute (IGISets);
		if (!stat) { stat.perror("addAttribute IGISets"); return stat;}

	IGIBias = numericAttr.create("IGIBias", "igib", MFnNumericData::kFloat, 0.00003, &stat);
	stat = addAttribute (IGIBias);
		if (!stat) { stat.perror("addAttribute IGIBias"); return stat;}

	IGIBSamples = numericAttr.create("IGIBSamples", "igibs", MFnNumericData::kInt, 0, &stat);
	stat = addAttribute (IGIBSamples);
		if (!stat) { stat.perror("addAttribute IGIBSamples"); return stat;}

	ICSamples = numericAttr.create("ICSamples", "ics", MFnNumericData::kInt, 512, &stat);
	stat = addAttribute (ICSamples);
		if (!stat) { stat.perror("addAttribute ICSamples"); return stat;}

	ICTolerance = numericAttr.create("ICTolerance", "ict", MFnNumericData::kFloat, 0.01, &stat);
	stat = addAttribute (ICTolerance);
		if (!stat) { stat.perror("addAttribute ICTolerance"); return stat;}

	ICSpacingMin = numericAttr.create("ICSpacingMin", "ics0", MFnNumericData::kFloat, 0.05, &stat);
	stat = addAttribute (ICSpacingMin);
		if (!stat) { stat.perror("addAttribute ICSpacingMin"); return stat;}

	ICSpacingMax = numericAttr.create("ICSpacingMax", "ics1", MFnNumericData::kFloat, 5, &stat);
	stat = addAttribute (ICSpacingMax);
		if (!stat) { stat.perror("addAttribute ICSpacingMax"); return stat;}

	diffuseDepth = numericAttr.create("diffuseDepth", "dd", MFnNumericData::kInt, 1, &stat);
	stat = addAttribute (diffuseDepth);
		if (!stat) { stat.perror("addAttribute diffuseDepth"); return stat;}

	reflectionDepth = numericAttr.create("reflectionDepth", "fld", MFnNumericData::kInt, 1, &stat);
	stat = addAttribute (reflectionDepth);
		if (!stat) { stat.perror("addAttribute reflectionDepth"); return stat;}

	refractionDepth = numericAttr.create("refractionDepth", "frd", MFnNumericData::kInt, 1, &stat);
	stat = addAttribute (refractionDepth);
		if (!stat) { stat.perror("addAttribute refractionDepth"); return stat;}

	materialOverride = enumAttr.create( "materialOverride", "mo", 0, &stat );
	stat = enumAttr.addField( "none", 0 );
	stat = enumAttr.addField( "ambocc", 1 );
	stat = enumAttr.addField( "uvs", 2 );
	stat = enumAttr.addField( "normals", 3 );
	stat = enumAttr.addField( "id", 4 );
	stat = enumAttr.addField( "prims", 5 );
	stat = enumAttr.addField( "gray", 6 );
	stat = enumAttr.addField( "wire", 7 );
	stat = addAttribute (materialOverride);
	if (!stat) { stat.perror("addAttribute materialOverride"); return stat;}

	ambOverrideDist = numericAttr.create( "ambOverrideDist", "aod",	MFnNumericData::kFloat, 16, &stat );
	stat = addAttribute (ambOverrideDist);
		if (!stat) { stat.perror("addAttribute ambOverrideDist"); return stat;}

	exportPath = tAttr.create(MString("exportPath"), MString("ep"), MFnData::kString, exportPath, &stat );
	stat = addAttribute (exportPath);
		if (!stat) { stat.perror("addAttribute exportPath"); return stat;}

	javaPath = tAttr.create(MString("javaPath"), MString("jp"), MFnData::kString, javaPath, &stat );
	stat = addAttribute (javaPath);
		if (!stat) { stat.perror("addAttribute javaPath"); return stat;}

	sunflowPath = tAttr.create(MString("sunflowPath"), MString("sp"), MFnData::kString, sunflowPath, &stat );
	stat = addAttribute (sunflowPath);
		if (!stat) { stat.perror("addAttribute sunflowPath"); return stat;}

	return MS::kSuccess;
}

void* sunflowGlobalsNode::creator()
{
	return new sunflowGlobalsNode();
}
