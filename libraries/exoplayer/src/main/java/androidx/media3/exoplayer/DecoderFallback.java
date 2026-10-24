package androidx.media3.exoplayer;

import androidx.annotation.IntDef;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Degrades the decoder selection when playback fails with decoder errors: hardware decoding is
 * retried with software (FFmpeg) renderers before the error is surfaced to the app.
 */
@UnstableApi
public final class DecoderFallback {

  /** @deprecated Use the track type constants in {@link C}. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO})
  public @interface TrackType {}

  private final DecoderManager decoderManager;
  private boolean audioFallbackApplied;
  private boolean videoFallbackApplied;

  public DecoderFallback(DecoderManager decoderManager) {
    this.decoderManager = decoderManager;
  }

  /**
   * Attempts to recover from the given playback exception by downgrading the decoder selection.
   *
   * @return {@code true} if the selection was downgraded and playback should be retried.
   */
  public boolean retry(ExoPlaybackException exception) {
    switch (exception.errorCode) {
      case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
      case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED:
      case PlaybackException.ERROR_CODE_DECODING_FAILED:
      case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED:
        break;
      default:
        return false;
    }
    int trackType = exception.rendererIndex == C.INDEX_UNSET ? C.TRACK_TYPE_VIDEO : rendererTrackType(exception);
    if (trackType == C.TRACK_TYPE_AUDIO) return fallbackAudio();
    if (trackType == C.TRACK_TYPE_VIDEO) return fallbackVideo();
    return false;
  }

  /** Clears the fallback state for one track type so a new media item starts at full quality. */
  public void reset(@TrackType int trackType) {
    if (trackType == C.TRACK_TYPE_AUDIO) audioFallbackApplied = false;
    if (trackType == C.TRACK_TYPE_VIDEO) videoFallbackApplied = false;
  }

  private boolean fallbackAudio() {
    if (audioFallbackApplied) return false;
    audioFallbackApplied = true;
    decoderManager.selectAudioDecoder(DecoderMode.SOFTWARE);
    return true;
  }

  private boolean fallbackVideo() {
    if (videoFallbackApplied) return false;
    videoFallbackApplied = true;
    decoderManager.selectVideoDecoder(DecoderMode.SOFTWARE);
    return true;
  }

  private static int rendererTrackType(ExoPlaybackException exception) {
    if (exception.getRendererException() instanceof androidx.media3.exoplayer.audio.AudioSink.WriteException) {
      return C.TRACK_TYPE_AUDIO;
    }
    return C.TRACK_TYPE_VIDEO;
  }
}
