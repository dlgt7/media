package androidx.media3.exoplayer.audio;

import android.content.Context;
import android.media.audiofx.Equalizer;
import androidx.annotation.Nullable;
import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Attaches a system {@link Equalizer} effect to the player's audio session and drives it from
 * {@link AudioEffectParameters}. Returns {@code false} from {@link #apply} when the device does
 * not expose an equalizer, letting the caller fall back to another strategy.
 */
@UnstableApi
public final class AudioEqualizerController {

  private final Context context;

  @Nullable private Equalizer equalizer;
  private int audioSessionId;

  public AudioEqualizerController(Context context) {
    this.context = context.getApplicationContext();
  }

  /** (Re)applies the equalizer curve to the player's audio session. */
  public boolean apply(ExoPlayer player, AudioEffectParameters config) {
    int sessionId = player.getAudioSessionId();
    if (sessionId == 0) return true;
    if (!config.hasBands()) {
      release();
      return true;
    }
    try {
      if (equalizer == null || audioSessionId != sessionId) {
        release();
        equalizer = new Equalizer(0, sessionId);
        audioSessionId = sessionId;
      }
      short[] bands = config.getEqualizerLevels();
      if (equalizer.getNumberOfBands() != bands.length) {
        release();
        return false;
      }
      equalizer.setEnabled(true);
      int bandCount = bands.length;
      for (short i = 0; i < bandCount; i++) {
        equalizer.setBandLevel(i, bands[i]);
      }
      return true;
    } catch (RuntimeException error) {
      release();
      return false;
    }
  }

  /** Releases the system effect. */
  public void release() {
    if (equalizer != null) {
      try {
        equalizer.setEnabled(false);
        equalizer.release();
      } catch (RuntimeException error) {
        // Effect already gone.
      }
      equalizer = null;
    }
  }
}
