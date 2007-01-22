#include <maya/MSimple.h>
#include <maya/MGlobal.h>
#include <maya/MItDag.h>
#include <maya/MItDependencyNodes.h>
#include <maya/MDagPath.h>
#include <maya/MDagPathArray.h>
#include <maya/MFnMesh.h>
#include <maya/MFnCamera.h>
#include <maya/MFnDependencyNode.h>
#include <maya/MFnLambertShader.h>
#include <maya/MFnAreaLight.h>
#include <maya/MFnDirectionalLight.h>
#include <maya/MItMeshPolygon.h>
#include <maya/MIntArray.h>
#include <maya/MPointArray.h>
#include <maya/MVectorArray.h>
#include <maya/MObjectArray.h>
#include <maya/MPoint.h>
#include <maya/MVector.h>
#include <maya/MMatrix.h>
#include <maya/MAngle.h>
#include <maya/MSelectionList.h>
#include <maya/MPlugArray.h>
#include <iostream>
#include <fstream>
#include <set>
#include <vector>
#include <string>

// global variables:

float resolutionAspectRatio = 4.0f / 3.0f;

bool isObjectVisible(const MDagPath& path) {
    MStatus status;
    MFnDagNode node(path);
    MPlug vPlug = node.findPlug("visibility", &status);
    bool visible = true;
    if (status == MS::kSuccess)
        vPlug.getValue(visible);
    status.clear();
    MPlug iPlug = node.findPlug("intermediateObject", &status);
    bool intermediate = false;
    if (status == MS::kSuccess)
        iPlug.getValue(intermediate);
    return visible && !intermediate;
}

bool areObjectAndParentsVisible(const MDagPath& path) {
    bool result = true;
    MDagPath searchPath(path);
    for (;;) {
        if (!isObjectVisible(searchPath)) {
            result = false;
            break;
        }
        if (searchPath.length() <= 1)
            break;
        searchPath.pop();
    }
    return result;
}

bool isCameraRenderable(const MDagPath& path) {
    MStatus status;
    MFnDagNode node(path);
    MPlug rPlug = node.findPlug("renderable", &status);
    bool renderable = true;
    if (status == MS::kSuccess)
        rPlug.getValue(renderable);
    return renderable;
}

int getAttributeInt(const std::string& node, const std::string& attributeName, int defaultValue) {
    MStatus status;
    MSelectionList list;
    status = list.add((node + "." + attributeName).c_str());
    if (status != MS::kSuccess)
        return defaultValue;
    MPlug plug;
    status = list.getPlug(0, plug);
    if (status != MS::kSuccess)
        return defaultValue;
    int value;
    status = plug.getValue(value);
    if (status != MS::kSuccess)
        return defaultValue;
    return value;
}

bool getShaderFromEngine(const MObject& obj, MFnDependencyNode& node) {
    if (!obj.hasFn(MFn::kShadingEngine))
        return false; // not a shading engine
    MFnDependencyNode seNode(obj);
    // find connected shader
    MPlug shaderPlug = seNode.findPlug("surfaceShader");
    if (shaderPlug.isNull())
        return false; // this shading group does not contain any surface shaders
    // list all plugs which connect TO this shading group
    MPlugArray plugs; 
    shaderPlug.connectedTo(plugs, true, false);
    for (unsigned int i = 0; i < plugs.length(); i++) {
        MObject sObj = plugs[i].node();
        // FIXME: not all shaders derive from kLambert
        if (sObj.hasFn(MFn::kLambert)) {
            // we have found a shader
            node.setObject(sObj);
            return true;
        }
    }
    return false;
}

bool getShaderFromGeometry(const MDagPath& path, MFnDependencyNode& node) {
    MStatus status;
    MFnDagNode geom(path);
    MObject iog = geom.attribute("instObjGroups", &status);
    if (!status)
        return false; // not a renderable object
    MPlug iogPlug(geom.object(), iog);
    MPlugArray iogPlugs;
    iogPlug.elementByLogicalIndex(0).connectedTo(iogPlugs, false, true, &status);
    if (!status)
        return false; // no shading group defined
    for (unsigned int i = 0; i < iogPlugs.length(); i++) {
        MObject seObj = iogPlugs[i].node();
        if (seObj.hasFn(MFn::kShadingEngine))
            return getShaderFromEngine(seObj, node); // retrieve the actual shader node from the shading engine
    }
    return false; // no shading engines found
}


void exportMesh(const MDagPath& path, std::ofstream& file) {
    bool instancing = path.isInstanced();
    if (instancing) {
        if (path.instanceNumber() != 0)
            return; // this instance will be handled somewhere else
        else {
            MDagPathArray paths;
            path.getAllPathsTo(path.node(), paths);
            bool hasVisible = false;
            for (unsigned int i = 0; i < paths.length() && !hasVisible; i++)
                hasVisible |= areObjectAndParentsVisible(paths[i]);
            if (!hasVisible)
                return; // none of the instance are visible
        }
    } else if (!areObjectAndParentsVisible(path))
        return;

    MFnMesh mesh(path);

    int numPoints = mesh.numVertices();
    int numTriangles = 0;
    for (MItMeshPolygon it(path); !it.isDone(); it.next()) {
        int tri;
        it.numTriangles(tri);
        numTriangles += tri;
    }

    if (numPoints == 0 || numTriangles == 0) return;

    file << "object {" << std::endl;
    if (instancing) {
        // instances will be created manually later on
        file << "\tnoinstance" << std::endl;
    }
    // get shader table
    MObjectArray shaders;
    MIntArray polyShaderIndices;
    mesh.getConnectedShaders(path.instanceNumber(), shaders, polyShaderIndices);
    std::vector<std::string> shaderNames(shaders.length());
    for (unsigned int i = 0; i < shaders.length(); i++) {
        MObject engine = shaders[i];
        MFnDependencyNode shader;
        if (getShaderFromEngine(engine, shader))
            shaderNames[i] = shader.name().asChar();
        else
            shaderNames[i] = "default";
    }
    if (!instancing) {
        // write shaders
        if (shaderNames.size() == 0)
            file << "\tshader default" << std::endl;
        else if (shaderNames.size() == 1)
            file << "\tshader " << shaderNames[0] << std::endl;
        else {
            file << "\tshaders " << shaderNames.size() << std::endl;
            for (size_t i = 0; i < shaderNames.size(); i++)
                file << "\t\t" << shaderNames[i] << std::endl;
        }
        // instance this mesh directly
        file << "\ttransform col ";
        MMatrix o2w = path.inclusiveMatrix();
        file << o2w[0][0] << " " << o2w[0][1] << " " << o2w[0][2] << " " << o2w[0][3] << " ";
        file << o2w[1][0] << " " << o2w[1][1] << " " << o2w[1][2] << " " << o2w[1][3] << " ";
        file << o2w[2][0] << " " << o2w[2][1] << " " << o2w[2][2] << " " << o2w[2][3] << " ";
        file << o2w[3][0] << " " << o2w[3][1] << " " << o2w[3][2] << " " << o2w[3][3] << std::endl;
    }
    file << "\ttype generic-mesh" << std::endl;
    file << "\tname \"" << path.fullPathName().asChar() << "\"" << std::endl;
    file << "\tpoints " << numPoints << std::endl;

    // write points
    MSpace::Space space = MSpace::kObject;
    MPointArray points;
    mesh.getPoints(points, space);
    for (int i = 0; i < numPoints; i++) {
        file << "\t\t" << points[i].x << " " << points[i].y << " " << points[i].z << std::endl;
    }

    // get UVSets for this mesh
    MStringArray uvSets;
    mesh.getUVSetNames(uvSets);
    int numUVSets = uvSets.length();
    int numUVs = numUVSets > 0 ? mesh.numUVs(uvSets[0]) : 0;

    // get normals
    MFloatVectorArray normals;
    mesh.getNormals(normals, space);


    // write triangles
    file << "\ttriangles " << numTriangles << std::endl;
    unsigned int t = 0;
    MPointArray nonTweaked; // not used
    // object-relative vertex indices for each triangle
    MIntArray triangleVertices;
    // face-relative vertex indices for each triangle
    int localIndex[3];
    // object-relative indices for the vertices in each face.
    MIntArray polygonVertices;
    std::vector<float> faceNormals(3 * 3 * numTriangles);
    std::vector<float> faceUVs(numUVs > 0 ? 3 * 2 * numTriangles : 1);
    std::vector<int> faceMaterials(numTriangles);
    for (MItMeshPolygon mItMeshPolygon(path); !mItMeshPolygon.isDone(); mItMeshPolygon.next()) {
        mItMeshPolygon.getVertices(polygonVertices);
        int numVerts = (int) polygonVertices.length();
        // get triangulation of this poly.
        int numTriangles; mItMeshPolygon.numTriangles(numTriangles);
        while (numTriangles--) {
            mItMeshPolygon.getTriangle(numTriangles, nonTweaked, triangleVertices, MSpace::kObject);
            for (int gt = 0; gt < 3; gt++) {
                for (int gv = 0; gv < numVerts; gv++) {
                    if (triangleVertices[gt] == polygonVertices[gv]) {
                        localIndex[gt] = gv;
                        break;
                    }
                }
            }
            file << "\t\t" << triangleVertices[0] << " " << triangleVertices[1] << " " << triangleVertices[2] << std::endl;

            // per triangle normals
            int nidx0 = mItMeshPolygon.normalIndex(localIndex[0]);
            int nidx1 = mItMeshPolygon.normalIndex(localIndex[1]);
            int nidx2 = mItMeshPolygon.normalIndex(localIndex[2]);
            faceNormals[3 * 3 * t + 0] = normals[nidx0].x;
            faceNormals[3 * 3 * t + 1] = normals[nidx0].y;
            faceNormals[3 * 3 * t + 2] = normals[nidx0].z;
            faceNormals[3 * 3 * t + 3] = normals[nidx1].x;
            faceNormals[3 * 3 * t + 4] = normals[nidx1].y;
            faceNormals[3 * 3 * t + 5] = normals[nidx1].z;
            faceNormals[3 * 3 * t + 6] = normals[nidx2].x;
            faceNormals[3 * 3 * t + 7] = normals[nidx2].y;
            faceNormals[3 * 3 * t + 8] = normals[nidx2].z;

            // texture coordinates
            if (numUVs > 0) {
                float2 uv0;
                float2 uv1;
                float2 uv2;
                mItMeshPolygon.getUV(localIndex[0], uv0, &uvSets[0]);
                mItMeshPolygon.getUV(localIndex[1], uv1, &uvSets[0]);
                mItMeshPolygon.getUV(localIndex[2], uv2, &uvSets[0]);
                faceUVs[3 * 2 * t + 0] = uv0[0];
                faceUVs[3 * 2 * t + 1] = uv0[1];
                faceUVs[3 * 2 * t + 2] = uv1[0];
                faceUVs[3 * 2 * t + 3] = uv1[1];
                faceUVs[3 * 2 * t + 4] = uv2[0];
                faceUVs[3 * 2 * t + 5] = uv2[1];
            }

            // per face materials
            if (shaderNames.size() > 1)
                faceMaterials[t] = polyShaderIndices[mItMeshPolygon.index()];
            t++;
        }
    }
    // write normals
    file << "\tnormals facevarying" << std::endl;
    for (int t = 0, idx = 0; t < numTriangles; t++, idx += 9) {
        file << "\t\t" << faceNormals[idx + 0];
        for (int j = 1; j < 9; j++)
            file << " " << faceNormals[idx + j];
        file << std::endl;
    }
    // write uvs
    if (numUVs > 0) {
        file << "\tuvs facevarying" << std::endl;
        for (int t = 0, idx = 0; t < numTriangles; t++, idx += 6) {
            file << "\t\t" << faceUVs[idx + 0];
            for (int j = 1; j < 6; j++)
                file << " " << faceUVs[idx + j];
            file << std::endl;
        }
    } else
        file << "\tuvs none" << std::endl;
    // write per-face materials
    if (shaderNames.size() > 1) {
        file << "\tface_shaders" << std::endl;
        for (int t = 0; t < numTriangles; t++)
            file << "\t\t" << faceMaterials[t] << std::endl;
    }
    file << "}" << std::endl;
    file << std::endl;

    if (instancing) {
        MDagPathArray paths;
        path.getAllPathsTo(path.node(), paths);
        for (unsigned int i = 0; i < paths.length(); i++) {
            if (!areObjectAndParentsVisible(paths[i])) continue;
            file << "instance {" << std::endl;
            file << "\tname \"" << paths[i].fullPathName().asChar() << ".instance\"" << std::endl;
            file << "\tgeometry \"" << path.fullPathName().asChar() << "\"" <<  std::endl;
            file << "\ttransform col ";
            MMatrix o2w = paths[i].inclusiveMatrix();
            file << o2w[0][0] << " " << o2w[0][1] << " " << o2w[0][2] << " " << o2w[0][3] << " ";
            file << o2w[1][0] << " " << o2w[1][1] << " " << o2w[1][2] << " " << o2w[1][3] << " ";
            file << o2w[2][0] << " " << o2w[2][1] << " " << o2w[2][2] << " " << o2w[2][3] << " ";
            file << o2w[3][0] << " " << o2w[3][1] << " " << o2w[3][2] << " " << o2w[3][3] << std::endl;
            MObjectArray instanceShaders;
            MFnMesh instancedMesh(paths[i]);
            instancedMesh.getConnectedShaders(paths[i].instanceNumber(), instanceShaders, polyShaderIndices);
            std::vector<std::string> instanceShaderNames(instanceShaders.length());
            for (unsigned int i = 0; i < instanceShaders.length(); i++) {
                MObject engine = instanceShaders[i];
                MFnDependencyNode shader;
                if (getShaderFromEngine(engine, shader))
                    instanceShaderNames[i] = shader.name().asChar();
                else
                    instanceShaderNames[i] = "default";
            }
            if (instanceShaderNames.size() == 0)
                file << "\tshader default" << std::endl;
            else if (instanceShaderNames.size() == 1)
                file << "\tshader " << instanceShaderNames[0] << std::endl;
            else {
                file << "\tshaders " << instanceShaderNames.size() << std::endl;
                for (size_t i = 0; i < instanceShaderNames.size(); i++)
                    file << "\t\t" << instanceShaderNames[i] << std::endl;
            }
            file << "\t}" << std::endl;
        }
    }
}

void exportCamera(const MDagPath& path, std::ofstream& file) {
    MFnCamera camera(path);

    if (!isCameraRenderable(path)) return;
    if (camera.isOrtho()) return;

    MSpace::Space space = MSpace::kWorld;

    MPoint eye = camera.eyePoint(space);
    MVector dir = camera.viewDirection(space);
    MVector up = camera.upDirection(space);
    double fov = camera.horizontalFieldOfView() * (180.0 / 3.1415926535897932384626433832795);

    file << "% " << path.fullPathName().asChar() << std::endl;
    file << "camera {" << std::endl;
    file << "\ttype   pinhole" << std::endl;
    file << "\teye    " << eye.x << " " << eye.y << " " << eye.z << std::endl;
    file << "\ttarget " << (eye.x + dir.x) << " " << (eye.y + dir.y) << " " << (eye.z + dir.z) << std::endl;
    file << "\tup     " << up.x << " " << up.y << " " << up.z << std::endl;
    file << "\tfov    " << fov << std::endl;
    file << "\taspect " << resolutionAspectRatio << std::endl;
    file << "}" << std::endl;
    file << std::endl;
}

DeclareSimpleCommand(sunflowExport, "sunflow.sourceforge.net", "0.07.0");
MStatus sunflowExport::doIt(const MArgList& args) {
    if (args.length() < 1) return MS::kFailure;
    MString filename = args.asString(0);
    std::cout << "Exporting scene to: " << filename.asChar() << " ..." << std::endl;
    std::ofstream file(filename.asChar());


    // default settings
    int resX = getAttributeInt("defaultResolution", "width" , 640);
    int resY = getAttributeInt("defaultResolution", "height", 480);
    resolutionAspectRatio = (float) resX / (float) resY;
    file << "image {" << std::endl;
    file << "\tresolution " << resX << " " << resY << std::endl;
    file << "\taa 0 2" << std::endl;
    file << "\tfilter gaussian" << std::endl;
    file << "}" << std::endl; 
    file << std::endl;

    // default shader
    file << "shader {" << std::endl;
    file << "\tname default" << std::endl;
    file << "\ttype diffuse" << std::endl;
    file << "\tdiff { \"sRGB nonlinear\" 0.7 0.7 0.7 }" << std::endl;
    file << "}" << std::endl; 
    file << std::endl;


    MStatus status;
    std::set<std::string> shaderNodes;
    for (MItDependencyNodes it(MFn::kShadingEngine); !it.isDone(&status); it.next()) {
        MObject obj = it.item();
        MFnDependencyNode sNode;
        if (getShaderFromEngine(obj, sNode)) {
            std::string name = sNode.name().asChar();
            if (shaderNodes.find(name) != shaderNodes.end())
                continue; // already encountered a shader with the same name, skip
            std::cout << "Found surface shader: " << name << std::endl;
            if (sNode.object().hasFn(MFn::kLambert)) {
                MFnLambertShader shader(sNode.object());
                file << "shader {" << std::endl;
                file << "\tname " << name << std::endl;
                file << "\ttype diffuse" << std::endl;
                MColor col = shader.color();
                float d = shader.diffuseCoeff();
                file << "\tdiff { \"sRGB nonlinear\" " << (col.r * d) << " " << (col.g * d) << " " << (col.b * d) << " }" << std::endl;
                file << "}" << std::endl; 
                file << std::endl;
                // add into the shader map, so we don't export the same shader twice
                shaderNodes.insert(name);
            }
        }
    }
    bool exportedSun = false;
    for (MItDag mItDag = MItDag(MItDag::kBreadthFirst); !mItDag.isDone(&status); mItDag.next()) {
        MDagPath path;
        status = mItDag.getPath(path);
        switch (path.apiType(&status)) {
            case MFn::kMesh: {
                std::cout << "Exporting mesh: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportMesh(path, file);
            } break;
            case MFn::kCamera: {
                std::cout << "Exporting camera: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportCamera(path, file);
            } break;
            case MFn::kDirectionalLight: {
                if (!areObjectAndParentsVisible(path)) continue;
                if (exportedSun) continue;
                std::cout << "Exporting sunlight: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportedSun = true;
                MMatrix o2w = path.inclusiveMatrix();
                MVector dir(0, 0, 1);
                dir = dir * o2w;
                MFnDirectionalLight light(path);
                file << "light {" << std::endl;
                file << "\ttype sunsky" << std::endl;
                file << "\tup 0 1 0" << std::endl;
                file << "\teast 0 0 1" << std::endl;
                file << "\tsundir " << dir.x << " " << dir.y << " " << dir.z << std::endl;
                file << "\tturbidity 2" << std::endl;
                file << "\tsamples " << light.numShadowSamples() << std::endl;
                file << "}" << std::endl;
                file << std::endl;
            } break;
            case MFn::kAreaLight: {
                if (!areObjectAndParentsVisible(path)) continue;
                std::cout << "Exporting light: " << path.fullPathName().asChar() << " ..." << std::endl;
                MMatrix o2w = path.inclusiveMatrix();
                MPoint lampV0(-1,  1,  0);
                MPoint lampV1( 1,  1,  0);
                MPoint lampV2( 1, -1,  0);
                MPoint lampV3(-1, -1,  0);
                lampV0 = lampV0 * o2w;
                lampV1 = lampV1 * o2w;
                lampV2 = lampV2 * o2w;
                lampV3 = lampV3 * o2w;

                MFnAreaLight area(path);
                MColor c = area.color();
                float i = area.intensity();
                file << "\n\nlight {" << std::endl;
                file << "\ttype meshlight" << std::endl;
                file << "\tname " << path.fullPathName().asChar() << std::endl;
                file << "\temit { \"sRGB nonlinear\" " << c.r << " " << c.g << " " << c.b << " }" << std::endl;
                file << "\tradiance " << i << std::endl;
                file << "\tsamples " << area.numShadowSamples() << std::endl;
                file << "\tpoints 4" << std::endl;
                file << "\t\t" << lampV0.x << " " << lampV0.y << " " << lampV0.z << std::endl;
                file << "\t\t" << lampV1.x << " " << lampV1.y << " " << lampV1.z << std::endl;
                file << "\t\t" << lampV2.x << " " << lampV2.y << " " << lampV2.z << std::endl;
                file << "\t\t" << lampV3.x << " " << lampV3.y << " " << lampV3.z << std::endl;
                file << "\ttriangles 2" << std::endl;
                file << "\t\t0 1 2" << std::endl;
                file << "\t\t0 2 3" << std::endl;
                file << "}" << std::endl;
                file << std::endl;
            } break;
            default: break;
        }
    }
    std::cout << "Exporting scene done." << std::endl;
    file.close();
    return MS::kSuccess;
}
