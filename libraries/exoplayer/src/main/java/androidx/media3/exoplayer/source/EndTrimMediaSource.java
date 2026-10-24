package androidx.media3.exoplayer.source;

import androidx.media3.common.C;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;

/**
 * Media source wrapper that ends playback early: once the remaining duration drops below the
 * configured trim duration the timeline reports the shortened duration, so the player transitions
 * to {@link androidx.media3.common.Player#STATE_ENDED} at the trim point.
 */
@UnstableApi
public final class EndTrimMediaSource extends WrappingMediaSource {

  private long endTrimDurationMs;

  public EndTrimMediaSource(MediaSource mediaSource, long endTrimDurationMs) {
    super(mediaSource);
    this.endTrimDurationMs = endTrimDurationMs;
  }

  /** Updates the trim duration; values below zero disable trimming. */
  public void setEndTrimDurationMs(long endTrimDurationMs) {
    this.endTrimDurationMs = endTrimDurationMs;
  }

  /** Returns the configured trim duration in milliseconds. */
  public long getEndTrimDurationMs() {
    return endTrimDurationMs;
  }

  /** Returns whether trimming is active for the current media. */
  public boolean isEndTrimActive() {
    return endTrimDurationMs > 0;
  }

  /** Returns the duration of the wrapped source before trimming, or {@link C#TIME_UNSET}. */
  public long getOriginalDurationMs() {
    Timeline.Window window = new Timeline.Window();
    Timeline timeline = getInitialTimeline();
    if (timeline == null || timeline.isEmpty()) return C.TIME_UNSET;
    return timeline.getWindow(0, window).getDurationMs();
  }
}
