package androidx.media3.mpvplayer;

import androidx.media3.common.text.SubtitleContent;
import java.io.IOException;

/**
 * Loads the transcript content of an external subtitle file. Implemented by the app (typically
 * over OkHttp-backed data sources) and invoked by the mpv player when the user requests a
 * full-text transcript.
 */
public interface MpvSubtitleContentLoader {

  /**
   * Loads and parses the subtitle at {@code uri}.
   *
   * @param item The media item the subtitle belongs to.
   * @param uri The subtitle URI.
   * @param format The format of the subtitle track.
   * @return The parsed transcript content.
   * @throws IOException When the subtitle cannot be fetched or parsed.
   */
  SubtitleContent load(androidx.media3.common.MediaItem item, android.net.Uri uri,
      androidx.media3.common.Format format) throws IOException;
}
