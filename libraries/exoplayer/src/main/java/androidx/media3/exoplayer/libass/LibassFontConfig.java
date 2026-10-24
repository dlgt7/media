package androidx.media3.exoplayer.libass;

import androidx.media3.common.util.UnstableApi;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generates a fontconfig {@code fonts.conf} that exposes the system font directories plus the
 * user-provided subtitle font directories to libass.
 */
@UnstableApi
public final class LibassFontConfig {

  private LibassFontConfig() {}

  /**
   * Writes a fontconfig document covering the given directories.
   *
   * @param output Target file for the generated {@code fonts.conf}.
   * @param cacheDirectory Directory fontconfig may use for its cache.
   * @param fontDirectories Directories to scan for fonts, in priority order.
   */
  public static void write(File output, File cacheDirectory, List<File> fontDirectories)
      throws IOException {
    File parent = output.getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Cannot create " + parent);
    }
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\"?>\n");
    xml.append("<!DOCTYPE fontconfig SYSTEM \"fonts.dtd\">\n");
    xml.append("<fontconfig>\n");
    xml.append("  <cachedir>")
        .append(escape(cacheDirectory.getAbsolutePath()))
        .append("</cachedir>\n");
    for (File directory : fontDirectories) {
      if (directory == null) continue;
      xml.append("  <dir>")
          .append(escape(directory.getAbsolutePath()))
          .append("</dir>\n");
    }
    xml.append("</fontconfig>\n");
    try (Writer writer =
        new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8)) {
      writer.write(xml.toString());
    }
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
