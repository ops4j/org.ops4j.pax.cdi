/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.tck.porting.owb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.testharness.impl.util.Files;

public class JarCreator {

    private final File root;

    public JarCreator(File root) {
        this.root = root;
    }

    public InputStream jar() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        ZipOutputStream jarOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            jarOutputStream = new ZipOutputStream(byteArrayOutputStream);
            jar(root, jarOutputStream);
            jarOutputStream.close();
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }
        finally {
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
            else if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
        }
    }

    private void jar(File directory, ZipOutputStream jarOutputStream) throws IOException {
        // directory entries are required, or else bundle classpath may be broken
        if (!directory.equals(root)) {
            String path = normalizePath(directory);
            ZipEntry jarEntry = new ZipEntry(path + File.separator);
            jarOutputStream.putNextEntry(jarEntry);
        }
        File[] children = directory.listFiles();
        // loop through dirList, and zip the files
        for (File child : children) {
            if (child.isDirectory()) {
                jar(child, jarOutputStream);
            }
            else {
                FileInputStream fis = new FileInputStream(child);
                ZipEntry jarEntry = new ZipEntry(normalizePath(child));
                jarOutputStream.putNextEntry(jarEntry);
                Files.copy(fis, jarOutputStream);
                fis.close();
            }
        }
    }

    private String normalizePath(File file) {
        String relativePath = file.getPath().substring(root.getPath().length() + 1);
        String path = relativePath.replaceAll("\\" + File.separator, "/");
        return path;
    }

}
