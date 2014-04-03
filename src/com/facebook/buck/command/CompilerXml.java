/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.command;

import com.facebook.buck.util.BuckConstant;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

class CompilerXml {

  private static final ImmutableList<String> WILDCARD_RESOURCES = ImmutableList.of(
      "dtd",
      "ftl",
      "gif",
      "html",
      "jpeg",
      "jpg",
      "png",
      "properties",
      "tld",
      "xml",
      "json",
      "txt"
      );

  /**
   * Exclude most directories that contain files generated by Buck.
   */
  private static final ImmutableSet<String> TOP_LEVEL_EXCLUDE_DIRS = ImmutableSet.of(
      Project.ANDROID_GEN_DIR,
      BuckConstant.BIN_DIR,
      BuckConstant.GEN_DIR
  );

  private final List<Module> modules;

  CompilerXml(List<Module> modules) {
    this.modules = Lists.newArrayList(modules);

    // Sort the list of modules so that the XML generated by this class is consistent even if the
    // modules are read in in a different order between runs.
    Collections.sort(modules, Module.BUILDTARGET_NAME_COMARATOR);
  }

  private String generateXml() {
    StringBuilder buffer = new StringBuilder();

    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append("<project version=\"4\">\n");
    buffer.append("  <component name=\"CompilerConfiguration\">\n");
    buffer.append("    <option name=\"DEFAULT_COMPILER\" value=\"Javac\" />\n");

    // List all files that should be excluded from compilation.
    buffer.append("    <excludeFromCompile>\n");

    for (String excludeDir : TOP_LEVEL_EXCLUDE_DIRS) {
      buffer.append("      <directory url=\"file://$PROJECT_DIR$/" +
          excludeDir +
          "\" includeSubdirectories=\"true\" />\n");
    }

    // Exclude several files associated with each module.
    for (Module module : modules) {
      String url = "file://$PROJECT_DIR$/" + module.getModuleDirectoryPathWithSlash();
      buffer.append("      <file url=\"" + url + "BUCK\" />\n");

      if (module.isAndroidModule()) {
        buffer.append("      <directory url=\"" + url +
            "gen\" includeSubdirectories=\"true\" />\n");
        buffer.append("      <file url=\"" + url + "project.properties\" />\n");
      }
    }
    buffer.append("    </excludeFromCompile>\n");

    buffer.append("    <resourceExtensions />\n");
    buffer.append("    <wildcardResourcePatterns>\n");
    for (String extension : WILDCARD_RESOURCES) {
      buffer.append("      <entry name=\"?*." + extension + "\" />\n");
    }
    buffer.append("    </wildcardResourcePatterns>\n");

    buffer.append("  </component>\n");
    buffer.append("</project>\n");

    return buffer.toString();
  }

  /**
   * Writes the {@code compilerXml} file if and only if its content needs to be updated.
   * @return true if {@code compilerXml} was written
   */
  boolean write(File compilerXml) throws IOException {
    final Charset charset = Charsets.US_ASCII;

    String existingXml = null;
    if (compilerXml.exists()) {
      existingXml = Files.toString(compilerXml, charset);
    }

    String newXml = generateXml();
    boolean compilerXmlNeedsToBeWritten = !newXml.equals(existingXml);

    if (compilerXmlNeedsToBeWritten) {
      Files.write(newXml, compilerXml, charset);
    }

    return compilerXmlNeedsToBeWritten;
  }
}
