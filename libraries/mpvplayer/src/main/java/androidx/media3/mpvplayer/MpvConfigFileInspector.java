package androidx.media3.mpvplayer;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspects mpv configuration files (mpv.conf): extracts the font directories configured through
 * {@code sub-fonts-dir} style options and lists which interface-managed options a user config
 * overrides.
 */
@UnstableApi
public final class MpvConfigFileInspector {

  private static final Pattern OPTION_LINE =
      Pattern.compile("^\\s*([A-Za-z0-9_-]+)\\s*=\\s*(.*?)\\s*(?:#.*)?$");
  private static final Pattern FONT_DIRS =
      Pattern.compile("^(?:sub-fonts-dir|sub-fonts-dir-add|font-directory)$");

  private final File configDirectory;

  public MpvConfigFileInspector(File configDirectory) {
    this.configDirectory = configDirectory;
  }

  /**
   * Returns the first subtitle font directory configured in the user's mpv.conf, or {@code null}
   * when none is configured.
   */
  @Nullable
  public File readSubtitleFontsDirectory() {
    String value = readOptionValue("sub-fonts-dir");
    if (value == null || value.isEmpty()) return null;
    File directory = new File(value);
    if (!directory.isAbsolute()) directory = new File(configDirectory, value);
    return directory;
  }

  /**
   * Returns the interface-managed option names that the given configuration content overrides.
   *
   * @param content The mpv.conf content to inspect.
   */
  public Set<String> getEffectiveOptionNames(CharSequence content) {
    Set<String> names = new LinkedHashSet<>();
    if (content == null) return names;
    for (String line : content.toString().split("\n")) {
      Matcher matcher = OPTION_LINE.matcher(line);
      if (matcher.matches()) {
        names.add(matcher.group(1).toLowerCase(Locale.US));
      }
    }
    return names;
  }

  @Nullable
  private String readOptionValue(String option) {
    File configFile = new File(configDirectory, "mpv.conf");
    if (!configFile.isFile()) return null;
    try (Scanner scanner = new Scanner(configFile, StandardCharsets.UTF_8.name())) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        Matcher matcher = OPTION_LINE.matcher(line);
        if (matcher.matches() && option.equals(matcher.group(1).toLowerCase(Locale.US))) {
          return unquote(matcher.group(2));
        }
      }
    } catch (Exception error) {
      return null;
    }
    return null;
  }

  private static String unquote(String value) {
    String trimmed = value.trim();
    if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
      return trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed;
  }
}
