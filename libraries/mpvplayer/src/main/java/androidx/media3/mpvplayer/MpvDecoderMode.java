package androidx.media3.mpvplayer;

import androidx.media3.common.DecoderMode;

/**
 * mpv-specific video decoder selection mode. Mirrors {@link DecoderMode}; {@code FFMPEG} maps to
 * mpv's software decoder chain, {@code HARDWARE} to media-codec passthrough.
 */
public enum MpvDecoderMode {
  AUTO,
  HARDWARE,
  SOFTWARE,
  FFMPEG;

  /** Converts back to the shared {@link DecoderMode} preference. */
  public DecoderMode toCommonDecoderMode() {
    switch (this) {
      case HARDWARE:
        return DecoderMode.HARDWARE;
      case SOFTWARE:
        return DecoderMode.SOFTWARE;
      case FFMPEG:
        return DecoderMode.FFMPEG;
      case AUTO:
      default:
        return DecoderMode.AUTO;
    }
  }

  /** Converts a shared {@link DecoderMode} preference. */
  public static MpvDecoderMode fromCommonDecoderMode(DecoderMode mode) {
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
}
