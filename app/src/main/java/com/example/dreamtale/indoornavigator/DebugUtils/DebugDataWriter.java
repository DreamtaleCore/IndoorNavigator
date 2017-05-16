package com.example.dreamtale.indoornavigator.DebugUtils;

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by DreamTale on 2017/3/19.
 */

public class DebugDataWriter {
    private File mFile = null;
    private Context mContext;

    public DebugDataWriter(Context context) {
        this.mContext = context;
    }

    public void setFileName(String name) {
        String filePath = String.valueOf(mContext.getExternalFilesDir(null));
        mFile = new File(filePath, name);
        try {
            mFile.createNewFile();
            System.out.println("filePath = " + filePath);
            Toast.makeText(mContext, "**filePath = " + filePath, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDataSetHeader(String header) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(mFile, true);
            fw.write(header);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeData(String data) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(mFile, true);
            fw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
