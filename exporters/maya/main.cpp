#include <maya/MSimple.h>
#include <maya/MGlobal.h>
#include <maya/MItDag.h>
#include <maya/MItDependencyNodes.h>
#include <maya/MDagPath.h>
#include <maya/MFnMesh.h>
#include <maya/MFnCamera.h>
#include <maya/MItMeshPolygon.h>
#include <maya/MIntArray.h>
#include <maya/MPointArray.h>
#include <maya/MVectorArray.h>
#include <maya/MObjectArray.h>
#include <maya/MPoint.h>
#include <maya/MVector.h>
#include <maya/MAngle.h>
#include <iostream>
#include <fstream>
#include <vector>

void exportMesh(const MDagPath& path, std::ofstream& file) {
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
    file << "\tshader default" << std::endl;
    file << "\ttype generic-mesh" << std::endl;
    file << "\tname \"" << path.fullPathName().asChar() << "\"" << std::endl;
    file << "\tpoints " << numPoints << std::endl;

    // write points
    MSpace::Space space = MSpace::kWorld;
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

//    MIntArray polyShaderIndices; // needed later if we have multiple shaders
//    if (numUVs > 0)
//        mesh.getUVSetNames(uvSets);    
//    // write shaders
//    int numShaders = 0;
//    if (numShaders > 0) {
//        // get shader table
//        MObjectArray shaders;
//        mesh.getConnectedShaders(path.instanceNumber(), shaders, polyShaderIndices);
//        std::vector<std::string> shaderNames(shaders.length());
//        for (unsigned int i = 0; i < shaders.length(); i++) {
//            MObject engine = shaders[i];
//            MFnDependencyNode shader;
//            if (MayaHelpers::getShaderFromEngine(engine, shader))
//                shaderNames[i] = shader.name().asChar();
//            else
//                shaderNames[i] = "default-shader";
//        }
//    }


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
    for (MItMeshPolygon mItMeshPolygon(path); !mItMeshPolygon.isDone(); mItMeshPolygon.next()) {
        mItMeshPolygon.getVertices(polygonVertices);
        int numVerts = (int) polygonVertices.length();
        // get triangulation of this poly.
        int numTriangles; mItMeshPolygon.numTriangles(numTriangles);
        while (numTriangles--) {
            mItMeshPolygon.getTriangle(numTriangles, nonTweaked, triangleVertices, MSpace::kObject);
            //REQUIRE(triangleVertices.length() == 3);
            for (int gt = 0; gt < 3; gt++) {
                for (int gv = 0; gv < numVerts; gv++) {
                    if (triangleVertices[gt] == polygonVertices[gv]) {
                        localIndex[gt] = gv;
                        break;
                    }
                }
            }
//            mIndexes[t++] = triangleVertices[0];
//            mIndexes[t++] = triangleVertices[1];
//            mIndexes[t++] = triangleVertices[2];




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
//            if (numShaders > 1)
//                mIndexes[t++] = polyShaderIndices[mItMeshPolygon.index()];
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
    file << "}" << std::endl;
    file << std::endl;
}

void exportCamera(const MDagPath& path, std::ofstream& file) {
    MFnCamera camera(path);

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
    file << "\taspect " << 1.33333333f << std::endl;
    file << "}" << std::endl;
    file << std::endl;
}

DeclareSimpleCommand(sunflowExport, "sunflow.sourceforge.net", "0.05");
MStatus sunflowExport::doIt(const MArgList& args) {
    if (args.length() < 1) return MS::kFailure;
    MString filename = args.asString(0);
    std::cout << "Exporting scene to: " << filename.asChar() << " ..." << std::endl;
    std::ofstream file(filename.asChar());
    MStatus status;
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
            case MFn::kDirectionalLight:
            case MFn::kPointLight:
            case MFn::kSpotLight:
            case MFn::kVolumeLight:
            case MFn::kAmbientLight:
            case MFn::kAreaLight: {
                std::cout << "Exporting light: " << path.fullPathName().asChar() << " ..." << std::endl;
            } break;
            default: break;
        }
    }
    std::cout << "Exporting scene done." << std::endl;
    file.close();
    return MS::kSuccess;
}
