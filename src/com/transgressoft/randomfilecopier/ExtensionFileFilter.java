/******************************************************************************
 * Copyright 2016-2018 Octavio Calleya                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package com.transgressoft.randomfilecopier;

import java.io.*;
import java.util.*;

/**
 * This class implements <code>{@link java.io.FileFilter}</code> to
 * accept a file with some of the given extensionsToFilter. If no extensionsToFilter are given
 * the file is accepted. The extensionsToFilter must be given without the dot.
 *
 * @author Octavio Calleya
 * @version 0.2.6
 */
public class ExtensionFileFilter implements FileFilter {

    private String[] extensionsToFilter;
    private int numExtensions;

    public ExtensionFileFilter(String... extensionsToFilter) {
        this.extensionsToFilter = extensionsToFilter;
        numExtensions = extensionsToFilter.length;
    }

    public ExtensionFileFilter() {
        extensionsToFilter = new String[]{};
        numExtensions = 0;
    }

    public void addExtension(String extension) {
        boolean contains = false;
        for (String someExtension : extensionsToFilter)
            if (someExtension != null && extension.equals(someExtension)) {
                contains = true;
            }
        if (! contains) {
            ensureArrayLength();
            extensionsToFilter[numExtensions++] = extension;
        }
    }

    private void ensureArrayLength() {
        if (numExtensions == extensionsToFilter.length) {
            extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions == 0 ? 1 : 2 * numExtensions);
        }

    }

    public void removeExtension(String extension) {
        for (int i = 0; i < extensionsToFilter.length; i++)
            if (extensionsToFilter[i].equals(extension)) {
                extensionsToFilter[i] = null;
                numExtensions--;
            }
        extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions);
    }

    public String[] getExtensionsToFilter() {
        return extensionsToFilter;
    }

    public void setExtensionsToFilter(String... extensionsToFilter) {
        if (extensionsToFilter == null) {
            this.extensionsToFilter = new String[]{};
        }
        else {
            this.extensionsToFilter = extensionsToFilter;
        }
        numExtensions = this.extensionsToFilter.length;
    }

    @Override
    public boolean accept(File pathname) {
        boolean res = false;
        if (! pathname.isDirectory() && ! pathname.isHidden()) {
            int pos = pathname.getName().lastIndexOf('.');
            if (pos != - 1) {
                String extension = pathname.getName().substring(pos + 1);
                if (numExtensions == 0) {
                    res = true;
                }
                else {
                    res = hasExtension(extension);
                }
            }
        }
        return res;
    }

    public boolean hasExtension(String extension) {
        for (String someExtension : extensionsToFilter)
            if (extension.equals(someExtension)) {
                return true;
            }
        return false;
    }
}