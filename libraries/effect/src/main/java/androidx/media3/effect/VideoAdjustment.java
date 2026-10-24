package androidx.media3.effect;

import androidx.media3.common.util.UnstableApi;

/**
 * Aggregated user video adjustment (color and detail) shared by the color and detail effects.
 * See {@link Builder}.
 */
@UnstableApi
public final class VideoAdjustment {

  /** Builder for {@link VideoAdjustment}. */
  public static final class Builder {

    private float saturation = 1.0f;
    private float contrast = 1.0f;
    private float brightness;
    private float gamma = 1.0f;
    private float hue;
    private float redGain = 1.0f;
    private float blueGain = 1.0f;
    private float sharpness;
    private float threshold;
    private float shadowLift;

    /** Sets the saturation multiplier around 1.0f. */
    public Builder setSaturation(float saturation) {
      this.saturation = saturation;
      return this;
    }

    /** Sets the contrast multiplier around 1.0f. */
    public Builder setContrast(float contrast) {
      this.contrast = contrast;
      return this;
    }

    /** Sets the additive brightness offset in the -1..1 range. */
    public Builder setBrightness(float brightness) {
      this.brightness = brightness;
      return this;
    }

    /** Sets the gamma exponent around 1.0f. */
    public Builder setGamma(float gamma) {
      this.gamma = gamma;
      return this;
    }

    /** Sets the hue rotation in degrees. */
    public Builder setHue(float hue) {
      this.hue = hue;
      return this;
    }

    /** Sets the linear gain of the red channel around 1.0f. */
    public Builder setRedGain(float redGain) {
      this.redGain = redGain;
      return this;
    }

    /** Sets the linear gain of the blue channel around 1.0f. */
    public Builder setBlueGain(float blueGain) {
      this.blueGain = blueGain;
      return this;
    }

    /** Sets the sharpening strength in the 0..1 range. */
    public Builder setSharpness(float sharpness) {
      this.sharpness = sharpness;
      return this;
    }

    /** Sets the sharpening threshold in the 0..1 range. */
    public Builder setThreshold(float threshold) {
      this.threshold = threshold;
      return this;
    }

    /** Sets the shadow lift amount in the 0..1 range. */
    public Builder setShadowLift(float shadowLift) {
      this.shadowLift = shadowLift;
      return this;
    }

    public VideoAdjustment build() {
      return new VideoAdjustment(this);
    }
  }

  public final float saturation;
  public final float contrast;
  public final float brightness;
  public final float gamma;
  public final float hue;
  public final float redGain;
  public final float blueGain;
  public final float sharpness;
  public final float threshold;
  public final float shadowLift;

  private VideoAdjustment(Builder builder) {
    this.saturation = builder.saturation;
    this.contrast = builder.contrast;
    this.brightness = builder.brightness;
    this.gamma = builder.gamma;
    this.hue = builder.hue;
    this.redGain = builder.redGain;
    this.blueGain = builder.blueGain;
    this.sharpness = builder.sharpness;
    this.threshold = builder.threshold;
    this.shadowLift = builder.shadowLift;
  }
}
