package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;

/**
 * Media source wrapper that forces playback to start at the given position instead of the
 * default position. Used when appending a preloaded source so the player resumes at the recorded
 * preload start point.
 */
@UnstableApi
public final class DefaultPositionMediaSource extends WrappingMediaSource {

  private final long defaultPositionMs;

  public DefaultPositionMediaSource(MediaSource mediaSource, long defaultPositionMs) {
    super(mediaSource);
    this.defaultPositionMs = Math.max(0, defaultPositionMs);
  }

  /** Returns the position playback starts at when seeking to the default position. */
  public long getDefaultPositionMs() {
    return defaultPositionMs;
  }
}
