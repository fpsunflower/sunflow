/////////////////////////////////////////////////////////////////////////////////////////
//Sunflow Render Globals Node
/////////////////////////////////////////////////////////////////////////////////////////

#include <maya/MPxNode.h>

class sunflowGlobalsNode : public MPxNode
{
public:
	sunflowGlobalsNode(){};
	virtual	~sunflowGlobalsNode(){}; 

	virtual MStatus		compute( const MPlug& plug, MDataBlock& data );

	static  void*		creator();
	static  MStatus		initialize();

	static	MObject		preset;
	static	MObject		pixelFilter;
	static	MObject		minSamples;
	static	MObject		maxSamples;

	static	MObject		enablePhotons;
	static	MObject		Photons;
	static	MObject		PhotonsKd;
	static	MObject		PhotonsRadius;

	static	MObject		enableGI;
	static	MObject		GIMode;

	static	MObject		PTSamples;

	static	MObject		IGISamples;
	static	MObject		IGISets;
	static	MObject		IGIBias;
	static	MObject		IGIBSamples;

	static	MObject		ICSamples;
	static	MObject		ICTolerance;
	static	MObject		ICSpacingMin;
	static	MObject		ICSpacingMax;

	static	MObject		diffuseDepth;
	static	MObject		reflectionDepth;
	static	MObject		refractionDepth;

	static	MObject		exportPath;

	static	MObject		materialOverride;
	static	MObject		ambOverrideDist;

	static	MObject		javaPath;
	static	MObject		sunflowPath;



	static	MTypeId		id;
};