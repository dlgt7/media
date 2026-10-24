package androidx.media3.common.audio;

import androidx.annotation.IntDef;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Channel downmix preference for audio playback. Renderers that cannot output the original
 * channel layout use the resolved mode when folding the stream down.
 */
public final class AudioChannelMode {

  public static final int STEREO = 0;
  public static final int MONO = 1;
  public static final int REVERSE = 2;

  /** Follows the content: no forced downmix is applied. */
  public static final int AUTO = -1;

  /** @deprecated Use the constants above. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({STEREO, MONO, REVERSE})
  public @interface Mode {}

  private AudioChannelMode() {}

  /**
   * Resolves the stored preference against the stream channel count. Mode transformations are
   * only meaningful for stereo-capable layouts; other layouts keep the stored mode untouched so
   * callers can detect "no transform" with {@link #isAvailable(int, int)}.
   */
  public static @Mode int resolve(@Mode int mode, int channelCount) {
    return mode;
  }

  /**
   * Returns whether a channel transform is applicable: the stream must carry more than one
   * channel.
   */
  public static boolean isAvailable(@Mode int mode, int channelCount) {
    return channelCount > 1;
  }

  /** Returns whether the given mode can apply a balance transform. */
  public static boolean canBalance(@Mode int mode, int channelCount) {
    return isAvailable(resolve(mode, channelCount), channelCount);
  }

  /** Returns whether the given mode supports a balance transform on any layout. */
  public static boolean canBalance(@Mode int mode) {
    return mode != MONO;
  }

  /** Returns whether a channel transform is applicable to the given layout. */
  public static boolean isAvailable(int channelCount) {
    return channelCount > 1;
  }

  /** Returns whether the given mode requests automatic (content driven) handling. */
  public static boolean isAuto(@Mode int mode) {
    return mode == AUTO;
  }

  /** Constrains the given mode to the set of known modes. */
  public static @Mode int clamp(int mode) {
    if (mode == AUTO || mode == STEREO || mode == MONO || mode == REVERSE) return mode;
    return AUTO;
  }
}
