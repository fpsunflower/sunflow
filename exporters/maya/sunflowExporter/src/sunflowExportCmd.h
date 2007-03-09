#ifndef _SUNFLOW_EXPORT_CMD_H_
#define _SUNFLOW_EXPORT_CMD_H_
/////////////////////////////////////////////////////////////////////////////////////////
//Sunflow Export Command
/////////////////////////////////////////////////////////////////////////////////////////

#include <maya/MPxCommand.h>
#include <maya/MDagPath.h>
#include <maya/MFnDependencyNode.h>
#include <maya/MColor.h>
#include <maya/MObjectArray.h>
#include <fstream>

class sunflowExportCmd : public MPxCommand
{
public:
	sunflowExportCmd(){};
	virtual		~sunflowExportCmd(){};

    virtual MStatus doIt ( const MArgList& args );

    static void* creator();

	void getCustomAttribute(MFloatVector &colorAttribute, MString attribute, MFnDependencyNode &node);
	void getCustomAttribute(MString &texture, MFloatVector &colorAttribute, MString attribute, MFnDependencyNode &node);
	void getCustomAttribute(float &floatAttribute, MString attribute, MFnDependencyNode &node);
	void getCustomAttribute(int &intAttribute, MString attribute, MFnDependencyNode &node);
	void getCustomAttribute(MString &stringAttribute, MString attribute, MFnDependencyNode &node);
	void getCustomAttribute(bool &boolAttribute, MString attribute, MFnDependencyNode &node);

	bool	isObjectVisible(const MDagPath& path);
	bool	areObjectAndParentsVisible(const MDagPath& path);
	bool	isCameraRenderable(const MDagPath& path);
	int		getAttributeInt(const std::string& node, const std::string& attributeName, int defaultValue);
	float		getAttributeFloat(const std::string& node, const std::string& attributeName, float defaultValue);
	bool	getShaderFromEngine(const MObject& obj, MFnDependencyNode& node);
	bool	getShaderFromGeometry(const MDagPath& path, MFnDependencyNode& node);
	void	exportMesh(const MDagPath& path, std::ofstream& file);
	void	exportSurface(const MDagPath& path, std::ofstream& file);
	void	exportCamera(const MDagPath& path, std::ofstream& file);

	bool	getBumpFromShader(MFnDependencyNode& node, MString &texturePath, float &depth, MObject &bumpObject);
	MObjectArray shaderList;
	bool findShaderInList(MString shader);
};

#endif /* _SUNFLOW_EXPORT_CMD_H_ */
