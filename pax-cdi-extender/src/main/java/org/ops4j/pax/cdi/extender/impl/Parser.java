/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.cdi.extender.impl;

import java.util.ArrayList;
import java.util.List;

public final class Parser {

    private static final int CHAR = 1;
    private static final int DELIMITER = 2;
    private static final int STARTQUOTE = 4;
    private static final int ENDQUOTE = 8;

    private Parser() {
    }

    public static Clause[] parseHeader(String header) {
        Clause[] clauses = null;
        if (header != null) {
            if (header.length() == 0) {
                throw new IllegalArgumentException("The header cannot be an empty string.");
            }
            String[] ss = parseDelimitedString(header, ",");
            clauses = parseClauses(ss);
        }
        return (clauses == null) ? new Clause[0] : clauses;
    }

    public static Clause[] parseClauses(String[] ss) {
        if (ss == null) {
            return null;
        }

        List completeList = new ArrayList();
        for (int ssIdx = 0; ssIdx < ss.length; ssIdx++) {
            // Break string into semi-colon delimited pieces.
            String[] pieces = parseDelimitedString(ss[ssIdx], ";");

            // Count the number of different clauses; clauses
            // will not have an '=' in their string. This assumes
            // that clauses come first, before directives and
            // attributes.
            int pathCount = 0;
            for (int pieceIdx = 0; pieceIdx < pieces.length; pieceIdx++) {
                if (pieces[pieceIdx].indexOf('=') >= 0) {
                    break;
                }
                pathCount++;
            }

            // Error if no packages were specified.
            if (pathCount == 0) {
                throw new IllegalArgumentException("No path specified on clause: " + ss[ssIdx]);
            }

            // Parse the directives/attributes.
            Directive[] dirs = new Directive[pieces.length - pathCount];
            Attribute[] attrs = new Attribute[pieces.length - pathCount];
            int dirCount = 0;
            int attrCount = 0;
            int idx = -1;
            String sep = null;
            for (int pieceIdx = pathCount; pieceIdx < pieces.length; pieceIdx++) {
                // Check if it is a directive.
                // CHECKSTYLE:SKIP
                if ((idx = pieces[pieceIdx].indexOf(":=")) >= 0) {
                    sep = ":=";
                }
                // Check if it is an attribute.
                // CHECKSTYLE:SKIP
                else if ((idx = pieces[pieceIdx].indexOf("=")) >= 0) {
                    sep = "=";
                }
                // It is an error.
                else {
                    throw new IllegalArgumentException("Not a directive/attribute: " + ss[ssIdx]);
                }

                String key = pieces[pieceIdx].substring(0, idx).trim();
                String value = pieces[pieceIdx].substring(idx + sep.length()).trim();

                // Remove quotes, if value is quoted.
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Save the directive/attribute in the appropriate array.
                if (sep.equals(":=")) {
                    dirs[dirCount++] = new Directive(key, value);
                }
                else {
                    attrs[attrCount++] = new Attribute(key, value);
                }
            }

            // Shrink directive array.
            Directive[] dirsFinal = new Directive[dirCount];
            System.arraycopy(dirs, 0, dirsFinal, 0, dirCount);
            // Shrink attribute array.
            Attribute[] attrsFinal = new Attribute[attrCount];
            System.arraycopy(attrs, 0, attrsFinal, 0, attrCount);

            // Create package attributes for each package and
            // set directives/attributes. Add each package to
            // completel list of packages.
            Clause[] pkgs = new Clause[pathCount];
            for (int pkgIdx = 0; pkgIdx < pathCount; pkgIdx++) {
                pkgs[pkgIdx] = new Clause(pieces[pkgIdx], dirsFinal, attrsFinal);
                completeList.add(pkgs[pkgIdx]);
            }
        }

        Clause[] pkgs = (Clause[]) completeList.toArray(new Clause[completeList.size()]);
        return pkgs;
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     *
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return an array of string tokens or null if there were no tokens.
     */
    public static String[] parseDelimitedString(String value, String delim) {
        String val = (value != null) ? value : "";

        List list = new ArrayList();

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);
            boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0)) {
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if (isQuote && ((expecting & STARTQUOTE) > 0)) {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if (isQuote && ((expecting & ENDQUOTE) > 0)) {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0) {
                sb.append(c);
            }
            else {
                throw new IllegalArgumentException("Invalid delimited string: " + val);
            }
        }

        String s = sb.toString().trim();
        if (s.length() > 0) {
            list.add(s);
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    public static class Clause {

        private final String name;
        private final Directive[] directives;
        private final Attribute[] attributes;

        public Clause(String name, Directive[] directives, Attribute[] attributes) {
            this.name = name;
            this.directives = directives;
            this.attributes = attributes;
        }

        public String getName() {
            return name;
        }

        public Directive[] getDirectives() {
            return directives;
        }

        public Attribute[] getAttributes() {
            return attributes;
        }

        public String getDirective(String directive) {
            for (int i = 0; i < directives.length; i++) {
                if (directive.equals(directives[i].getName())) {
                    return directives[i].getValue();
                }
            }
            return null;
        }

        public String getAttribute(String attribute) {
            for (int i = 0; i < attributes.length; i++) {
                if (attribute.equals(attributes[i].getName())) {
                    return attributes[i].getValue();
                }
            }
            return null;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(name);
            for (int i = 0; directives != null && i < directives.length; i++) {
                sb.append(";").append(directives[i].getName()).append(":=");
                if (directives[i].getValue().indexOf(",") >= 0) {
                    sb.append("\"").append(directives[i].getValue()).append("\"");
                }
                else {
                    sb.append(directives[i].getValue());
                }
            }
            for (int i = 0; attributes != null && i < attributes.length; i++) {
                sb.append(";").append(attributes[i].getName()).append("=");
                if (attributes[i].getValue().indexOf(",") >= 0) {
                    sb.append("\"").append(attributes[i].getValue()).append("\"");
                }
                else {
                    sb.append(attributes[i].getValue());
                }
            }
            return sb.toString();
        }
    }

    public static class Directive {

        private final String name;
        private final String value;

        public Directive(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

    }

    public static class Attribute {

        private final String name;
        private final String value;

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

    }

}