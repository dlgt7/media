package androidx.media3.mpvplayer.media;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;

/**
 * DRM capability probe for the mpv engine. This build's mpv does not ship a DRM backend, so no
 * media item requires mpv: every DRM-protected item must go through the ExoPlayer engine.
 */
@UnstableApi
public final class MpvDrmSupport {

  private MpvDrmSupport() {}

  /**
   * Returns whether the mpv engine can play the given DRM-protected media item. Always {@code
   * false} in this build.
   */
  public static boolean supports(MediaItem mediaItem) {
    return false;
  }
}
