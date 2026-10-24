package androidx.media3.exoplayer.libass;

import androidx.media3.common.util.UnstableApi;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads family names from TrueType/OpenType (sfnt) font files without a full font engine: the
 * {@code name} table is located through the table directory and its Windows/Unicode records are
 * decoded directly.
 */
@UnstableApi
public final class LibassFontFile {

  private LibassFontFile() {}

  /**
   * Returns the family name of the font file.
   *
   * @throws IOException When the file is not a readable sfnt font or carries no family name.
   */
  public static String getFamilyName(File file) throws IOException {
    try (InputStream stream = new FileInputStream(file)) {
      return getFamilyName(readAll(stream));
    }
  }

  private static String getFamilyName(byte[] font) throws IOException {
    if (font.length < 12) throw new EOFException();
    int scalarType = readInt(font, 0);
    if (scalarType != 0x00010000 && scalarType != 0x74727565 && scalarType != 0x4F54544F) {
      throw new IOException("Not an sfnt font");
    }
    int tableCount = readShort(font, 4);
    int nameTableOffset = -1;
    int directoryStart = 12;
    for (int i = 0; i < tableCount; i++) {
      int record = directoryStart + 16 * i;
      if (record + 16 > font.length) throw new EOFException();
      int tag = readInt(font, record);
      int offset = readInt(font, record + 8);
      if (tag == 0x6E616D65) {
        nameTableOffset = offset;
        break;
      }
    }
    if (nameTableOffset < 0 || nameTableOffset + 6 > font.length) {
      throw new IOException("Missing name table");
    }
    int count = readShort(font, nameTableOffset + 2);
    int stringOffset = nameTableOffset + readShort(font, nameTableOffset + 4);
    for (int i = 0; i < count; i++) {
      int record = nameTableOffset + 6 + 12 * i;
      if (record + 12 > font.length) throw new EOFException();
      int platformId = readShort(font, record) & 0xFFFF;
      int encodingId = readShort(font, record + 2) & 0xFFFF;
      int languageId = readShort(font, record + 4) & 0xFFFF;
      int nameId = readShort(font, record + 6) & 0xFFFF;
      int length = readShort(font, record + 8) & 0xFFFF;
      int offset = readShort(font, record + 10) & 0xFFFF;
      if (nameId != 1) continue;
      boolean unicode = platformId == 0 || (platformId == 3 && (encodingId == 0 || encodingId == 1));
      boolean macRoman = platformId == 1 && languageId == 0;
      if (!unicode && !macRoman) continue;
      int start = stringOffset + offset;
      if (start < 0 || start + length > font.length) throw new EOFException();
      String name =
          unicode
              ? new String(font, start, length, StandardCharsets.UTF_16BE)
              : new String(font, start, length, StandardCharsets.ISO_8859_1);
      name = name.trim();
      if (!name.isEmpty()) return name;
    }
    throw new IOException("Missing family name");
  }

  private static int readInt(byte[] data, int offset) {
    return ((data[offset] & 0xFF) << 24)
        | ((data[offset + 1] & 0xFF) << 16)
        | ((data[offset + 2] & 0xFF) << 8)
        | (data[offset + 3] & 0xFF);
  }

  private static int readShort(byte[] data, int offset) {
    return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
  }

  private static byte[] readAll(InputStream stream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(1 << 16);
    byte[] chunk = new byte[8192];
    int read;
    while ((read = stream.read(chunk)) > 0) buffer.write(chunk, 0, read);
    return buffer.toByteArray();
  }
}
