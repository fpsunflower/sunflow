package org.sunflow;

import java.io.FileOutputStream;
import java.io.IOException;

import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.parser.SCAbstractParser.Keyword;
import org.sunflow.math.Matrix4;

class AsciiFileSunflowAPI extends FileSunflowAPI {
    private FileOutputStream stream;

    AsciiFileSunflowAPI(String filename) throws IOException {
        stream = new FileOutputStream(filename);
    }

    protected void writeBoolean(boolean value) {
        if (value)
            writeString("true");
        else
            writeString("false");
    }

    protected void writeFloat(float value) {
        writeString(String.format("%s", value));
    }

    protected void writeInt(int value) {
        writeString(String.format("%d", value));
    }

    protected void writeInterpolationType(InterpolationType interp) {
        writeString(String.format("%s", interp.toString().toLowerCase()));
    }

    protected void writeKeyword(Keyword keyword) {
        writeString(String.format("%s", keyword.toString().toLowerCase().replace("_array", "[]")));
    }

    protected void writeMatrix(Matrix4 value) {
        writeString("row");
        for (float f : value.asRowMajor())
            writeFloat(f);
    }

    protected void writeNewline(int indentNext) {
        try {
            stream.write('\n');
            for (int i = 0; i < indentNext; i++)
                stream.write('\t');
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void writeString(String string) {
        try {
            // check if we need to write string with quotes
            if (string.contains(" ") && !string.contains("<code>"))
                stream.write(String.format("\"%s\"", string).getBytes());
            else
                stream.write(string.getBytes());
            stream.write(' ');
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void writeVerbatimString(String string) {
        writeString(String.format("<code>%s\n</code> ", string));
    }
}