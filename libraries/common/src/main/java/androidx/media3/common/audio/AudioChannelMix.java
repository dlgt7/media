package androidx.media3.common.audio;

/**
 * Channel matrix helpers producing linear downmix coefficients for a source with {@code
 * channelCount} channels. Matrices are row-major: {@code matrix[destination][source]}.
 */
public final class AudioChannelMix {

  private AudioChannelMix() {}

  /**
   * Returns a matrix folding all front channels into stereo. When {@code reversed} the left and
   * right outputs are swapped.
   */
  public static float[][] createStereoMix(int channelCount, boolean reversed) {
    float[][] mix = baseStereoMix(channelCount);
    if (reversed) {
      float[] left = mix[0];
      mix[0] = mix[1];
      mix[1] = left;
    }
    return mix;
  }

  /** Returns a matrix folding all channels into a single mono output. */
  public static float[][] createMonoMix(int channelCount) {
    float[] row = new float[channelCount];
    for (int i = 0; i < channelCount; i++) row[i] = frontWeight(i, channelCount);
    return new float[][] {row};
  }

  /**
   * Returns a stereo matrix that attenuates one side to move the perceived image towards the
   * other. {@code balance} ranges from -1.0f (full left) to 1.0f (full right).
   */
  public static float[][] createFrontBalanceMix(int channelCount, float balance) {
    float[][] mix = baseStereoMix(channelCount);
    float clamped = Math.max(-1.0f, Math.min(1.0f, balance));
    float leftGain = clamped > 0.0f ? 1.0f - clamped : 1.0f;
    float rightGain = clamped < 0.0f ? 1.0f + clamped : 1.0f;
    for (int source = 0; source < channelCount; source++) {
      boolean leftSource = source == 0 || source > 1;
      mix[0][source] *= leftSource ? leftGain : rightGain * leftGain;
      mix[1][source] *= leftSource ? leftGain * rightGain : rightGain;
    }
    return mix;
  }

  /**
   * Returns a stereo matrix applying gain to the front-center channel only. {@code gain} is a
   * linear multiplier around 1.0f.
   */
  public static float[][] createFrontCenterGainMix(int channelCount, float gain) {
    float[][] mix = baseStereoMix(channelCount);
    if (channelCount >= 3) {
      mix[0][2] *= gain;
      mix[1][2] *= gain;
    }
    return mix;
  }

  /** Composes two matrices: {@code compose(first, second)} first applies {@code second}. */
  public static float[][] compose(float[][] first, float[][] second) {
    int outputs = first.length;
    int sources = second[0].length;
    float[][] result = new float[outputs][sources];
    for (int dest = 0; dest < outputs; dest++) {
      for (int mid = 0; mid < second.length; mid++) {
        float scale = first[dest][mid];
        if (scale == 0.0f) continue;
        for (int source = 0; source < sources; source++) result[dest][source] += scale * second[mid][source];
      }
    }
    return result;
  }

  private static float[][] baseStereoMix(int channelCount) {
    float[][] mix = new float[2][channelCount];
    for (int source = 0; source < channelCount; source++) {
      float weight = frontWeight(source, channelCount);
      boolean leftSource = source == 0 || source > 1;
      mix[0][source] = leftSource ? weight : weight * 0.5f;
      mix[1][source] = leftSource ? weight * 0.5f : weight;
    }
    return mix;
  }

  private static float frontWeight(int channelIndex, int channelCount) {
    if (channelCount <= 2) return 1.0f;
    // LFE (index 1 in 5.1/7.1 layouts) carries no directional information.
    return channelIndex == 1 ? 0.0f : 1.0f / (channelCount - 1);
  }
}
