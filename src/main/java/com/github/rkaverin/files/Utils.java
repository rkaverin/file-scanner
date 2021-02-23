package com.github.rkaverin.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String calcFileMd5(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (
                    InputStream is = Files.newInputStream(path);
                    DigestInputStream dis = new DigestInputStream(is, md);
                    OutputStream os = OutputStream.nullOutputStream()
            ) {
                dis.transferTo(os);
            }
            return byteArrayToString(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        }
    }

    private static String byteArrayToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
