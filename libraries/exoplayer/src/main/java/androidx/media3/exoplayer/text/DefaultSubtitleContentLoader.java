package androidx.media3.exoplayer.text;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads an external subtitle file over a {@link DataSource} and converts it into a {@link
 * SubtitleContent} transcript. SRT, WebVTT and ASS/SSA text tracks are supported; other formats
 * report {@link SubtitleContent#UNSUPPORTED}.
 */
@UnstableApi
public final class DefaultSubtitleContentLoader {

  private static final int MAX_BYTES = 8 * 1024 * 1024;

  private final DataSource.Factory dataSourceFactory;

  public DefaultSubtitleContentLoader(DataSource.Factory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
  }

  /** Loads and parses the subtitle at {@code uri}. */
  public SubtitleContent load(MediaItem item, Uri uri, Format format) throws IOException {
    byte[] bytes = read(uri);
    String body = new String(bytes, StandardCharsets.UTF_8);
    if (body.startsWith("\uFEFF")) body = body.substring(1);
    List<SubtitleContent.Entry> entries = parse(body, mimeTypeOf(uri, format));
    if (entries == null) return SubtitleContent.UNSUPPORTED;
    return new SubtitleContent(SubtitleContent.State.COMPLETE, entries);
  }

  private byte[] read(Uri uri) throws IOException {
    DataSource dataSource = dataSourceFactory.createDataSource();
    dataSource.open(new DataSpec(uri));
    try (InputStream stream = new DataSourceInputStream(dataSource)) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 << 15);
      byte[] chunk = new byte[8192];
      int read;
      while ((read = stream.read(chunk)) > 0) {
        if (buffer.size() + read > MAX_BYTES) throw new IOException("Subtitle too large");
        buffer.write(chunk, 0, read);
      }
      return buffer.toByteArray();
    } finally {
      dataSource.close();
    }
  }

  private static @androidx.annotation.Nullable String mimeTypeOf(Uri uri, Format format) {
    String mime = format == null ? null : format.sampleMimeType;
    if (mime != null) return mime;
    String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.US);
    if (path.endsWith(".vtt")) return MimeTypes.TEXT_VTT;
    if (path.endsWith(".ass") || path.endsWith(".ssa")) return MimeTypes.TEXT_SSA;
    if (path.endsWith(".ttml") || path.endsWith(".xml") || path.endsWith(".dfxp")) {
      return MimeTypes.APPLICATION_TTML;
    }
    return MimeTypes.APPLICATION_SUBRIP;
  }

  private static @androidx.annotation.Nullable List<SubtitleContent.Entry> parse(
      String body, String mimeType) {
    if (MimeTypes.APPLICATION_TTML.equals(mimeType)) return null;
    if (MimeTypes.TEXT_SSA.equals(mimeType)) return parseAss(body);
    if (MimeTypes.TEXT_VTT.equals(mimeType)) return parseCueBased(body, ".");
    return parseCueBased(body, ",");
  }

  private static List<SubtitleContent.Entry> parseCueBased(String body, String msSeparator) {
    List<SubtitleContent.Entry> entries = new ArrayList<>();
    String[] blocks = body.replace("\r\n", "\n").replace("\r", "\n").split("\n\\s*\\n");
    for (String block : blocks) {
      String[] lines = block.trim().split("\n");
      int timingLine = -1;
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].contains("-->")) {
          timingLine = i;
          break;
        }
      }
      if (timingLine < 0 || timingLine + 1 >= lines.length) continue;
      long[] times = parseTiming(lines[timingLine], msSeparator);
      if (times == null) continue;
      StringBuilder text = new StringBuilder();
      for (int i = timingLine + 1; i < lines.length; i++) {
        if (text.length() > 0) text.append('\n');
        text.append(stripTags(lines[i]));
      }
      String value = text.toString().trim();
      if (value.isEmpty()) continue;
      entries.add(new SubtitleContent.Entry(times[0], times[1], value, false));
    }
    return entries;
  }

  private static List<SubtitleContent.Entry> parseAss(String body) {
    List<SubtitleContent.Entry> entries = new ArrayList<>();
    for (String line : body.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
      if (!line.startsWith("Dialogue:")) continue;
      String[] parts = line.substring("Dialogue:".length()).trim().split(",", 10);
      if (parts.length < 10) continue;
      long start = parseAssClock(parts[1]);
      long end = parseAssClock(parts[2]);
      if (start == C.TIME_UNSET || end == C.TIME_UNSET) continue;
      String text = parts[9].replace("\\N", "\n").replace("\\n", "\n").replaceAll("\\{[^}]*}", "").trim();
      if (text.isEmpty()) continue;
      entries.add(new SubtitleContent.Entry(start, end, text, false));
    }
    return entries;
  }

  private static long[] parseTiming(String line, String msSeparator) {
    int arrow = line.indexOf("-->");
    if (arrow < 0) return null;
    long start = parseClock(line.substring(0, arrow).trim(), msSeparator);
    String rest = line.substring(arrow + 3).trim();
    int space = rest.indexOf(' ');
    long end = parseClock(space < 0 ? rest : rest.substring(0, space), msSeparator);
    if (start == C.TIME_UNSET || end == C.TIME_UNSET) return null;
    return new long[] {start, end};
  }

  private static long parseClock(String clock, String msSeparator) {
    try {
      String[] split = clock.split(":");
      if (split.length < 2) return C.TIME_UNSET;
      double seconds = Double.parseDouble(split[split.length - 1].replace(msSeparator, "."));
      long minutes = Long.parseLong(split[split.length - 2].trim());
      long hours = split.length >= 3 ? Long.parseLong(split[split.length - 3].trim()) : 0;
      return (long) (((hours * 60 + minutes) * 60 + seconds) * 1_000_000);
    } catch (NumberFormatException error) {
      return C.TIME_UNSET;
    }
  }

  private static long parseAssClock(String clock) {
    return parseClock(clock.trim(), ".");
  }

  private static String stripTags(String line) {
    return line.replaceAll("<[^>]*>", "").trim();
  }

  private static final class DataSourceInputStream extends InputStream {

    private final DataSource dataSource;
    private boolean opened;

    private DataSourceInputStream(DataSource dataSource) {
      this.dataSource = dataSource;
      this.opened = true;
    }

    @Override
    public int read() throws IOException {
      byte[] single = new byte[1];
      int read = dataSource.read(single, 0, 1);
      return read < 0 ? -1 : (single[0] & 0xFF);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      return dataSource.read(buffer, offset, length);
    }

    @Override
    public void close() {
      if (opened) {
        opened = false;
        try {
          dataSource.close();
        } catch (IOException error) {
          // Closing twice or after a broken connection is not fatal here.
        }
      }
    }
  }
}
