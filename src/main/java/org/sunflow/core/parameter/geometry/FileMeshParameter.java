package org.sunflow.core.parameter.geometry;

import org.sunflow.SunflowAPIInterface;

public class FileMeshParameter extends GeometryParameter {

    String filename;
    boolean smoothNormals = false;

    @Override
    public void setup(SunflowAPIInterface api) {
        super.setup(api);
        api.parameter("filename", filename);
        api.parameter("smooth_normals", smoothNormals);
        api.geometry(name, TYPE_FILE_MESH);
        setupInstance(api);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isSmoothNormals() {
        return smoothNormals;
    }

    public void setSmoothNormals(boolean smoothNormals) {
        this.smoothNormals = smoothNormals;
    }
}
