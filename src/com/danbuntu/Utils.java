package com.danbuntu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by Dan Griffin on 9/6/2016.
 * Have a great day!
 */
class Utils {

    static String sha256file(String fileName) {
        if(fileName == null) return null;

        File inFile = new File(fileName);
        if(!inFile.isFile()) return null;

        MessageDigest md;
        FileInputStream fis;
        int read;
        byte[] buffer = new byte[1024];

        try {

            md = MessageDigest.getInstance("SHA-256");
            fis = new FileInputStream(inFile);
            while((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            fis.close();

            return byte2hex(md.digest());

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byte2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(
                    sFill(Integer.toHexString(b & 0xFF), 2, "0".charAt(0))
            );
        }
        return sb.toString();
    }

    private static String sFill(String input, int fill, char with) {
        if(input.length() >=  fill) return input.substring(0, fill);

        StringBuilder sb = new StringBuilder();
        char[] f = new char[fill - input.length()];
        Arrays.fill(f, with);
        sb.append(new String(f));
        sb.append(input);

        return sb.toString();
    }

    static long getFileSize(File file) {
        if(file.isFile()) {
            return file.length();
        }
        return -1;
    }

    static String readableDuration(long duration) {
        int seconds = (int)duration / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        return String.format("%s hours %s minutes %s seconds %s ms",
                hours,
                minutes % 60,
                seconds % 60,
                duration % 1000);
    }

}
