package androidx.media3.exoplayer;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;

/**
 * Owns the decoder selection for the audio and video renderers and keeps the {@link
 * DecodeTrackSelector} in sync when the selection changes.
 */
@UnstableApi
public final class DecoderManager {

  private final Object lock;
  private DecoderMode audioMode;
  private DecoderMode videoMode;

  @Nullable private DecodeTrackSelector trackSelector;
  @Nullable private ExoPlayer player;

  public DecoderManager(DecoderMode audioMode, DecoderMode videoMode) {
    this.lock = new Object();
    this.audioMode = audioMode;
    this.videoMode = videoMode;
  }

  /** Creates the {@link DecodeTrackSelector} bound to this manager. */
  public DecodeTrackSelector createTrackSelector(Context context) {
    DecodeTrackSelector selector = new DecodeTrackSelector(context);
    synchronized (lock) {
      this.trackSelector = selector;
      applyToSelector(selector);
    }
    return selector;
  }

  /** Attaches the manager to a player instance. */
  public void attach(ExoPlayer player) {
    synchronized (lock) {
      this.player = player;
    }
  }

  /** Detaches the manager from its player. */
  public void detach() {
    synchronized (lock) {
      this.player = null;
    }
  }

  /** Returns the current audio decoder selection. */
  public DecoderMode getAudioMode() {
    synchronized (lock) {
      return audioMode;
    }
  }

  /** Returns the current video decoder selection. */
  public DecoderMode getVideoMode() {
    synchronized (lock) {
      return videoMode;
    }
  }

  /** Updates the audio decoder selection. */
  public void selectAudioDecoder(DecoderMode mode) {
    synchronized (lock) {
      this.audioMode = mode;
      applyLocked();
    }
  }

  /** Updates the video decoder selection. */
  public void selectVideoDecoder(DecoderMode mode) {
    synchronized (lock) {
      this.videoMode = mode;
      applyLocked();
    }
  }

  /** Updates both selections at once. */
  public void setDecoderModes(DecoderMode audioMode, DecoderMode videoMode) {
    synchronized (lock) {
      this.audioMode = audioMode;
      this.videoMode = videoMode;
      applyLocked();
    }
  }

  private void applyLocked() {
    if (trackSelector != null) applyToSelector(trackSelector);
  }

  private void applyToSelector(DecodeTrackSelector selector) {
    selector.setRendererDecodePreferences(
        toRendererPreference(audioMode), toRendererPreference(videoMode));
  }

  private static @C.DecodeMode int toRendererPreference(DecoderMode mode) {
    switch (mode) {
      case SOFTWARE:
      case FFMPEG:
        return C.DECODE_SOFTWARE;
      case HARDWARE:
      case AUTO:
      default:
        return C.DECODE_HARDWARE;
    }
  }
}
