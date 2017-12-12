package com.enkeli;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Kristina on 7/8/17.
 */

public class FileUtil {

    private static String TEMP_IMAGES_PATH = "logs/";
    public static String logsFileName = "locations_middle_values_logs.csv";

    public static File createTempDir(Context context) {
        File tempDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            tempDir = context.getExternalFilesDir(TEMP_IMAGES_PATH);
        } else {
            tempDir = context.getCacheDir();
        }

        if (tempDir != null && !tempDir.exists()) {
            tempDir.mkdirs();
        }

        return tempDir;
    }

    public static File createTempFile(Context context, String filename) throws IOException {
        File tempDir = createTempDir(context);
        String filePath = filename;
        return new File(tempDir, filePath);
    }

    public static void cleanTempFolder(Context context) {
        File file = new File(context.getFilesDir(), TEMP_IMAGES_PATH);
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                f.delete();
            }
        } else {
            file.delete();
        }
    }

    public static void writeToFileSeparator(String separator, Context context) {
        String fileName = logsFileName;
        try {
            File file = FileUtil.createTempFile(context, fileName);
            FileWriter writer = new FileWriter(file.getPath(), true);
            writer.append(separator);
            writer.append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logError(context.getPackageName(), "Failed to write location logs", e);
        }
    }
}
