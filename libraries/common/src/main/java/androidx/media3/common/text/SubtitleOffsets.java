package androidx.media3.common.text;

/** Per-role subtitle timing offsets applied on top of the embedded cues. */
public final class SubtitleOffsets {

  /** No offset on either role. */
  public static final SubtitleOffsets ZERO = new SubtitleOffsets(0, 0);

  public final long primaryMs;
  public final long secondaryMs;

  public SubtitleOffsets(long primaryMs, long secondaryMs) {
    this.primaryMs = primaryMs;
    this.secondaryMs = secondaryMs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SubtitleOffsets)) return false;
    SubtitleOffsets other = (SubtitleOffsets) obj;
    return primaryMs == other.primaryMs && secondaryMs == other.secondaryMs;
  }

  @Override
  public int hashCode() {
    int result = (int) (primaryMs ^ (primaryMs >>> 32));
    return 31 * result + (int) (secondaryMs ^ (secondaryMs >>> 32));
  }

  @Override
  public String toString() {
    return "SubtitleOffsets{primaryMs=" + primaryMs + ", secondaryMs=" + secondaryMs + '}';
  }
}
