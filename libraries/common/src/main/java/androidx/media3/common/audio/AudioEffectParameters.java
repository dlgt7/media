package androidx.media3.common.audio;

import androidx.annotation.Nullable;
import java.util.Arrays;

/**
 * Immutable description of the requested audio processing chain (equalizer, channel transform,
 * loudness normalization) as configured by the user.
 */
public final class AudioEffectParameters {

  /** A parameter set that requests no processing. */
  public static final AudioEffectParameters DEFAULT = new Builder().build();

  /** Returns whether a front-center gain control can apply to the given layout. */
  public static boolean isCenterGainAvailable(int channelCount) {
    return channelCount >= 3;
  }

  private final int[] equalizerFrequenciesHz;
  private final short[] equalizerLevels;
  private final float stability;
  private final int boostMillibels;
  private final int preampMillibels;
  private final int centerGainMillibels;
  private final float balance;
  private final int channelMode;
  private final boolean loudnessEnabled;

  private AudioEffectParameters(Builder builder) {
    this.equalizerFrequenciesHz = builder.equalizerFrequenciesHz;
    this.equalizerLevels = builder.equalizerLevels;
    this.stability = builder.stability;
    this.boostMillibels = builder.boostMillibels;
    this.preampMillibels = builder.preampMillibels;
    this.centerGainMillibels = builder.centerGainMillibels;
    this.balance = builder.balance;
    this.channelMode = builder.channelMode;
    this.loudnessEnabled = builder.loudnessEnabled;
  }

  /** Returns whether any processing is requested at all. */
  public boolean hasEffect() {
    return hasBands() || channelModeTransformRequired() || loudnessEnabled || balance != 0.0f;
  }

  /** Returns whether an equalizer curve is present. */
  public boolean hasBands() {
    return equalizerLevels.length > 0;
  }

  /** Returns whether a center gain applies to the given layout. */
  public boolean hasCenterGain(int channelCount) {
    return isCenterGainAvailable(channelCount) && centerGainMillibels != 0;
  }

  /** Returns the center gain as a linear multiplier around 1.0f. */
  public float getCenterGainFactor() {
    return (float) Math.pow(10.0, centerGainMillibels / 2000.0);
  }

  /** Returns the equalizer band center frequencies in Hz, parallel to the levels. */
  public int[] getEqualizerFrequenciesHz() {
    return equalizerFrequenciesHz;
  }

  /** Returns the equalizer levels in millibels per band. */
  public short[] getEqualizerLevels() {
    return equalizerLevels;
  }

  /** Alias of {@link #getEqualizerLevels()}. */
  public short[] getEqualizerLevelsMillibels() {
    return equalizerLevels;
  }

  /**
   * Returns whether the output stage must be limited when applying the requested boost to the
   * given layout (multi-channel output can clip earlier than stereo).
   */
  public boolean shouldLimitOutput(int channelCount) {
    return boostMillibels > 0 && channelCount >= 3;
  }

  /** Returns the dynamic normalization stability in the 0..1 range. */
  public float getStability() {
    return stability;
  }

  /** Returns the output boost in millibels. */
  public int getBoostMillibels() {
    return boostMillibels;
  }

  /** Returns the pre-amplification in millibels. */
  public int getPreampMillibels() {
    return preampMillibels;
  }

  /** Returns the stored stereo balance in the -1..1 range. */
  public float getBalance() {
    return balance;
  }

  /** Returns the stored {@link AudioChannelMode}. */
  public int getChannelMode() {
    return channelMode;
  }

  /** Returns whether loudness normalization is requested. */
  public boolean isLoudnessEnabled() {
    return loudnessEnabled;
  }

  private boolean channelModeTransformRequired() {
    return channelMode == AudioChannelMode.MONO || channelMode == AudioChannelMode.REVERSE;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AudioEffectParameters)) return false;
    AudioEffectParameters other = (AudioEffectParameters) obj;
    return Arrays.equals(equalizerFrequenciesHz, other.equalizerFrequenciesHz)
        && Arrays.equals(equalizerLevels, other.equalizerLevels)
        && stability == other.stability
        && boostMillibels == other.boostMillibels
        && preampMillibels == other.preampMillibels
        && centerGainMillibels == other.centerGainMillibels
        && balance == other.balance
        && channelMode == other.channelMode
        && loudnessEnabled == other.loudnessEnabled;
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(equalizerFrequenciesHz);
    result = 31 * result + Arrays.hashCode(equalizerLevels);
    result = 31 * result + Float.floatToIntBits(stability);
    result = 31 * result + boostMillibels;
    result = 31 * result + preampMillibels;
    result = 31 * result + centerGainMillibels;
    result = 31 * result + Float.floatToIntBits(balance);
    result = 31 * result + channelMode;
    result = 31 * result + (loudnessEnabled ? 1 : 0);
    return result;
  }

  /** Builder for {@link AudioEffectParameters}. */
  public static final class Builder {

    private int[] equalizerFrequenciesHz = new int[0];
    private short[] equalizerLevels = new short[0];
    private float stability = 1.0f;
    private int boostMillibels;
    private int preampMillibels;
    private int centerGainMillibels;
    private float balance;
    private int channelMode = AudioChannelMode.STEREO;
    private boolean loudnessEnabled;

    /**
     * Sets the equalizer curve. Frequencies are in Hz; levels are in millibels. Both arrays must
     * have the same length.
     */
    public Builder setEqualizerBands(int[] frequenciesHz, short[] levelsMillibels) {
      if (frequenciesHz.length != levelsMillibels.length) {
        throw new IllegalArgumentException("Frequency/level length mismatch");
      }
      this.equalizerFrequenciesHz = frequenciesHz.clone();
      this.equalizerLevels = levelsMillibels.clone();
      return this;
    }

    /** Sets the dynamic normalization stability in the 0..1 range. */
    public Builder setStability(float stability) {
      this.stability = Math.max(0.0f, Math.min(1.0f, stability));
      return this;
    }

    /** Sets the output boost in millibels. */
    public Builder setBoostMillibels(int boostMillibels) {
      this.boostMillibels = boostMillibels;
      return this;
    }

    /** Sets the pre-amplification in millibels. */
    public Builder setPreampMillibels(int preampMillibels) {
      this.preampMillibels = preampMillibels;
      return this;
    }

    /** Sets the front-center gain in millibels. */
    public Builder setCenterGainMillibels(int centerGainMillibels) {
      this.centerGainMillibels = centerGainMillibels;
      return this;
    }

    /** Sets the stereo balance in the -1..1 range. */
    public Builder setBalance(float balance) {
      this.balance = Math.max(-1.0f, Math.min(1.0f, balance));
      return this;
    }

    /** Sets the {@link AudioChannelMode}. */
    public Builder setChannelMode(@AudioChannelMode.Mode int channelMode) {
      this.channelMode = channelMode;
      return this;
    }

    /** Sets whether loudness normalization is requested. */
    public Builder setLoudnessEnabled(boolean loudnessEnabled) {
      this.loudnessEnabled = loudnessEnabled;
      return this;
    }

    public AudioEffectParameters build() {
      return new AudioEffectParameters(this);
    }
  }
}
