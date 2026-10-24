package androidx.media3.exoplayer;

/**
 * ExoPlayer-specific decoder selection mode. Mirrors {@link androidx.media3.common.DecoderMode} so
 * engine code can convert between the shared preference and the renderer-facing selection.
 */
public enum DecoderMode {
  AUTO,
  HARDWARE,
  SOFTWARE,
  FFMPEG;

  /** Converts a shared {@link androidx.media3.common.DecoderMode} preference. */
  public static DecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
    if (mode == null) return AUTO;
    switch (mode) {
      case HARDWARE:
        return HARDWARE;
      case SOFTWARE:
        return SOFTWARE;
      case FFMPEG:
        return FFMPEG;
      case AUTO:
      default:
        return AUTO;
    }
  }

  /** Converts back to the shared {@link androidx.media3.common.DecoderMode} preference. */
  public androidx.media3.common.DecoderMode toCommonDecoderMode() {
    switch (this) {
      case HARDWARE:
        return androidx.media3.common.DecoderMode.HARDWARE;
      case SOFTWARE:
        return androidx.media3.common.DecoderMode.SOFTWARE;
      case FFMPEG:
        return androidx.media3.common.DecoderMode.FFMPEG;
      case AUTO:
      default:
        return androidx.media3.common.DecoderMode.AUTO;
    }
  }
}
