package androidx.media3.exoplayer.text;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.mpvplayer.MpvSubtitleContentLoader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads and caches subtitle transcripts for the current media item. External subtitle files are
 * fetched through the {@link MpvSubtitleContentLoader} supplied by the app; embedded tracks have
 * no file to load and report {@link SubtitleContent#UNSUPPORTED}.
 */
@UnstableApi
public final class SubtitleTranscriptSession {

  private final ExecutorService executor;
  private final Map<String, SubtitleContent> cache = new HashMap<>();
  private final Map<String, Boolean> retryable = new HashMap<>();

  @Nullable private MpvSubtitleContentLoader loader;
  @Nullable private androidx.media3.common.Player player;
  @Nullable private MediaItem preloadMediaItem;
  private boolean contentEnabled = true;

  public SubtitleTranscriptSession() {
    this.executor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "SubtitleTranscript");
      thread.setDaemon(true);
      return thread;
    });
  }

  /** Sets the loader used to fetch external subtitle files. */
  public void setLoader(MpvSubtitleContentLoader loader) {
    this.loader = loader;
  }

  /** Sets the player whose media items are inspected for external subtitles. */
  public void setPlayer(@Nullable androidx.media3.common.Player player) {
    this.player = player;
  }

  /** Records the media item about to be preloaded. */
  public void setPreloadMediaItem(@Nullable MediaItem mediaItem) {
    this.preloadMediaItem = mediaItem;
  }

  /** Returns the transcript handle for the given media item. */
  public SubtitleTranscript forMediaItem(MediaItem mediaItem) {
    return new SubtitleTranscript(mediaItem);
  }

  /** Enables or disables transcript retrieval. */
  public void setContentEnabled(boolean enabled) {
    this.contentEnabled = enabled;
  }

  /**
   * Returns the transcript content for the given subtitle format. The first call for a format
   * kicks off an asynchronous load and reports {@link SubtitleContent#LOADING}.
   */
  public SubtitleContent getContent(Format format, long offsetMs) {
    if (!contentEnabled || loader == null || player == null) return SubtitleContent.UNSUPPORTED;
    String key = keyOf(format);
    SubtitleContent cached = cache.get(key);
    if (cached != null) return cached;
    MediaItem mediaItem = player.getCurrentMediaItem();
    if (mediaItem == null) mediaItem = preloadMediaItem;
    Uri uri = findSubtitleUri(mediaItem, format);
    if (mediaItem == null || uri == null) {
      cache.put(key, SubtitleContent.UNSUPPORTED);
      retryable.put(key, Boolean.FALSE);
      return SubtitleContent.UNSUPPORTED;
    }
    MediaItem item = mediaItem;
    cache.put(key, SubtitleContent.LOADING);
    retryable.put(key, Boolean.FALSE);
    executor.execute(() -> load(item, uri, format, key));
    return SubtitleContent.LOADING;
  }

  /** Returns whether a previously failed load can be retried for the given format. */
  public boolean canRetryContent(Format format) {
    return retryable.getOrDefault(keyOf(format), Boolean.FALSE);
  }

  /** Retries a previously failed load. */
  public void retryContent(Format format) {
    String key = keyOf(format);
    if (!Boolean.TRUE.equals(retryable.get(key))) return;
    cache.remove(key);
    retryable.remove(key);
  }

  /** Releases the session and drops all cached transcripts. */
  public void close() {
    cache.clear();
    retryable.clear();
    executor.shutdownNow();
  }

  private void load(MediaItem item, Uri uri, Format format, String key) {
    try {
      SubtitleContent content = loader.load(item, uri, format);
      synchronized (cache) {
        cache.put(key, content);
        retryable.put(key, content.state == SubtitleContent.State.ERROR ? Boolean.TRUE : Boolean.FALSE);
      }
    } catch (IOException error) {
      synchronized (cache) {
        cache.put(key, new SubtitleContent(SubtitleContent.State.ERROR));
        retryable.put(key, Boolean.TRUE);
      }
    } catch (RuntimeException error) {
      synchronized (cache) {
        cache.put(key, new SubtitleContent(SubtitleContent.State.ERROR));
        retryable.put(key, Boolean.TRUE);
      }
    }
  }

  @Nullable
  private static Uri findSubtitleUri(@Nullable MediaItem mediaItem, Format format) {
    if (mediaItem == null || mediaItem.localConfiguration == null) return null;
    List<MediaItem.SubtitleConfiguration> configurations =
        mediaItem.localConfiguration.subtitleConfigurations;
    if (configurations == null || configurations.isEmpty()) return null;
    for (MediaItem.SubtitleConfiguration configuration : configurations) {
      if (matches(configuration, format)) return configuration.uri;
    }
    return null;
  }

  private static boolean matches(MediaItem.SubtitleConfiguration configuration, Format format) {
    if (configuration.id != null && format.id != null) return configuration.id.equals(format.id);
    if (configuration.mimeType != null
        && format.sampleMimeType != null
        && !configuration.mimeType.equals(format.sampleMimeType)) return false;
    if (configuration.language != null
        && format.language != null
        && !configuration.language.equals(format.language)) return false;
    return true;
  }

  private static String keyOf(Format format) {
    return format.id + "|" + format.language + "|" + format.sampleMimeType;
  }
}
