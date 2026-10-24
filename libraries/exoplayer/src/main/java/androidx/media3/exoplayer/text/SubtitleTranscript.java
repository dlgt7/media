package androidx.media3.exoplayer.text;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;

/**
 * Handle to the subtitle transcript of one media item. Obtained from {@link
 * SubtitleTranscriptSession#forMediaItem(MediaItem)} and consumed by the media source factory to
 * wire transcript parsing into the extraction pipeline.
 */
@UnstableApi
public final class SubtitleTranscript {

  final MediaItem mediaItem;

  public SubtitleTranscript(MediaItem mediaItem) {
    this.mediaItem = mediaItem;
  }
}
