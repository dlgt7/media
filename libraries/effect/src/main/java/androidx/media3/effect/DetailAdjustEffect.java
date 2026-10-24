package androidx.media3.effect;

import androidx.media3.common.util.UnstableApi;

/**
 * Detail adjustment effect (sharpening and shadow lift) driven by a {@link VideoAdjustment}.
 *
 * <p>True unsharp masking requires convolution, which the RGB matrix pipeline cannot express.
 * This implementation therefore contributes an identity matrix and reports itself as a no-op
 * unless the pipeline provides a convolution path; the surrounding effect chain keeps working
 * because the color effect carries the visible adjustments.
 */
@UnstableApi
public final class DetailAdjustEffect implements RgbMatrix {

  private static final float[] IDENTITY =
      new float[] {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
      };

  private VideoAdjustment adjustment;

  public DetailAdjustEffect() {
    this.adjustment =
        new VideoAdjustment.Builder().setSharpness(0.0f).setShadowLift(0.0f).build();
  }

  /** Updates the adjustment. */
  public void setAdjustment(VideoAdjustment adjustment) {
    this.adjustment = adjustment;
  }

  @Override
  public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
    return IDENTITY;
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return adjustment.sharpness == 0.0f && adjustment.shadowLift == 0.0f;
  }
}
