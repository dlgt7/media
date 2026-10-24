package androidx.media3.effect;

import androidx.media3.common.util.UnstableApi;

/**
 * Color adjustment effect driven by a {@link VideoAdjustment}: saturation, contrast, brightness,
 * gamma, hue rotation and per-channel (temperature) gains. Implemented as an {@link RgbMatrix}
 * so it plugs into the standard video graph color pipeline.
 */
@UnstableApi
public final class ColorToneAdjustEffect implements RgbMatrix {

  private VideoAdjustment adjustment;
  private float[] matrix;
  private boolean identity;

  public ColorToneAdjustEffect() {
    this.adjustment =
        new VideoAdjustment.Builder()
            .setSaturation(1.0f)
            .setContrast(1.0f)
            .setGamma(1.0f)
            .build();
    this.matrix = identityMatrix();
    this.identity = true;
  }

  /** Updates the adjustment and recomputes the color matrix. */
  public void setAdjustment(VideoAdjustment adjustment) {
    this.adjustment = adjustment;
    this.matrix = buildMatrix(adjustment);
    this.identity = isIdentity(adjustment);
  }

  @Override
  public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
    return matrix;
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return identity;
  }

  private static float[] identityMatrix() {
    return new float[] {
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f
    };
  }

  private static boolean isIdentity(VideoAdjustment adjustment) {
    return adjustment.saturation == 1.0f
        && adjustment.contrast == 1.0f
        && adjustment.brightness == 0.0f
        && adjustment.gamma == 1.0f
        && adjustment.hue == 0.0f
        && adjustment.redGain == 1.0f
        && adjustment.blueGain == 1.0f;
  }

  private static float[] buildMatrix(VideoAdjustment adjustment) {
    float luminanceRed = 0.2126f;
    float luminanceGreen = 0.7152f;
    float luminanceBlue = 0.0722f;

    float saturation = Math.max(0.0f, adjustment.saturation);
    float keepR = (1.0f - saturation) * luminanceRed;
    float keepG = (1.0f - saturation) * luminanceGreen;
    float keepB = (1.0f - saturation) * luminanceBlue;

    float contrast = adjustment.contrast;
    float brightness = adjustment.brightness;
    float gamma = Math.max(0.05f, adjustment.gamma);
    float gammaFactor = (float) Math.pow(0.5, 1.0 - gamma);

    float redGain = Math.max(0.0f, adjustment.redGain);
    float blueGain = Math.max(0.0f, adjustment.blueGain);

    float radians = (float) Math.toRadians(adjustment.hue);
    float cosine = (float) Math.cos(radians);
    float sine = (float) Math.sin(radians);

    // Hue rotation matrix (normalized YIQ approximation) rows:
    float h00 = 0.299f + 0.701f * cosine + 0.168f * sine;
    float h01 = 0.587f - 0.587f * cosine + 0.330f * sine;
    float h02 = 0.114f - 0.114f * cosine - 0.497f * sine;
    float h10 = 0.299f - 0.299f * cosine - 0.328f * sine;
    float h11 = 0.587f + 0.413f * cosine + 0.035f * sine;
    float h12 = 0.114f - 0.114f * cosine + 0.292f * sine;
    float h20 = 0.299f - 0.300f * cosine + 1.250f * sine;
    float h21 = 0.587f - 0.588f * cosine - 1.050f * sine;
    float h22 = 0.114f + 0.886f * cosine - 0.203f * sine;

    float[] matrix = new float[16];
    // Row R
    matrix[0] = (keepR + saturation * redGain) * contrast * h00;
    matrix[1] = (keepG + saturation * redGain) * contrast * h01;
    matrix[2] = (keepB + saturation * redGain) * contrast * h02;
    // Row G
    matrix[4] = (keepR + saturation) * contrast * h10;
    matrix[5] = (keepG + saturation) * contrast * h11;
    matrix[6] = (keepB + saturation) * contrast * h12;
    // Row B
    matrix[8] = (keepR + saturation * blueGain) * contrast * h20;
    matrix[9] = (keepG + saturation * blueGain) * contrast * h21;
    matrix[10] = (keepB + saturation * blueGain) * contrast * h22;
    // Offsets: brightness shift plus contrast pivot; gamma approximated per channel below.
    float offset = (1.0f - contrast) * 0.5f + brightness;
    matrix[3] = offset;
    matrix[7] = offset;
    matrix[11] = offset;
    matrix[12] = 0.0f;
    matrix[13] = 0.0f;
    matrix[14] = 0.0f;
    matrix[15] = 1.0f;
    // gammaFactor is folded into the brightness offsets; full per-pixel gamma is not
    // expressible as a matrix, so the approximation keeps mid-tones aligned.
    float gammaOffset = (gammaFactor - 1.0f) * 0.5f;
    matrix[3] += gammaOffset;
    matrix[7] += gammaOffset;
    matrix[11] += gammaOffset;
    return matrix;
  }
}
