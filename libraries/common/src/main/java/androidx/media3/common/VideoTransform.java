package androidx.media3.common;

/**
 * User-applied spatial transform of the video (zoom and pan), independent of the aspect ratio
 * fit mode.
 */
public final class VideoTransform {

  /** Maximum scale factor that can be applied on either axis. */
  public static final float MAX_SCALE = 4.0f;

  public static final VideoTransform NONE = new VideoTransform(1.0f, 1.0f, 0.0f, 0.0f);

  /** Alias of {@link #NONE}. */
  public static final VideoTransform DEFAULT = NONE;

  public final float scaleX;
  public final float scaleY;
  public final float panX;
  public final float panY;

  public VideoTransform(float scaleX, float scaleY, float panX, float panY) {
    this.scaleX = clampScale(scaleX);
    this.scaleY = clampScale(scaleY);
    this.panX = panX;
    this.panY = panY;
  }

  private static float clampScale(float scale) {
    if (scale > 0.0f && Float.isFinite(scale)) return Math.min(scale, MAX_SCALE);
    return 1.0f;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof VideoTransform)) return false;
    VideoTransform other = (VideoTransform) obj;
    return scaleX == other.scaleX
        && scaleY == other.scaleY
        && panX == other.panX
        && panY == other.panY;
  }

  @Override
  public int hashCode() {
    int result = Float.floatToIntBits(scaleX);
    result = 31 * result + Float.floatToIntBits(scaleY);
    result = 31 * result + Float.floatToIntBits(panX);
    result = 31 * result + Float.floatToIntBits(panY);
    return result;
  }
}
