package androidx.media3.common;

import androidx.annotation.IntDef;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Immutable snapshot describing the state of an optical disc navigation session. */
public final class DiscMenuState {

  /** The disc menu is not available for the current media item. */
  public static final int AVAILABILITY_UNAVAILABLE = 0;

  /** The disc menu is available for the current media item. */
  public static final int AVAILABILITY_AVAILABLE = 1;

  /** No navigation session; the player behaves like a regular progressive stream. */
  public static final int PHASE_IDLE = 0;

  /** Navigation is preparing (mounting the disc image or starting the runtime). */
  public static final int PHASE_OPENING = 1;

  /** Navigation is active; disc menus respond to user input. */
  public static final int PHASE_ACTIVE = 2;

  /** @deprecated Use {@link #AVAILABILITY_UNAVAILABLE}. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({AVAILABILITY_UNAVAILABLE, AVAILABILITY_AVAILABLE})
  public @interface Availability {}

  /** @deprecated Use {@link #PHASE_IDLE}. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({PHASE_IDLE, PHASE_OPENING, PHASE_ACTIVE})
  public @interface Phase {}

  /** A state that describes a regular (non disc) playback session. */
  public static final DiscMenuState IDLE =
      new DiscMenuState(AVAILABILITY_UNAVAILABLE, PHASE_IDLE, false);

  /** Alias of {@link #IDLE}. */
  public static final DiscMenuState EMPTY = IDLE;

  public final @Availability int availability;
  public final @Phase int phase;
  public final boolean navigationPlaybackOngoing;

  public DiscMenuState(
      @Availability int availability, @Phase int phase, boolean navigationPlaybackOngoing) {
    this.availability = availability;
    this.phase = phase;
    this.navigationPlaybackOngoing = navigationPlaybackOngoing;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof DiscMenuState)) return false;
    DiscMenuState other = (DiscMenuState) obj;
    return availability == other.availability
        && phase == other.phase
        && navigationPlaybackOngoing == other.navigationPlaybackOngoing;
  }

  @Override
  public int hashCode() {
    int result = availability;
    result = 31 * result + phase;
    result = 31 * result + (navigationPlaybackOngoing ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DiscMenuState{availability="
        + availability
        + ", phase="
        + phase
        + ", navigationPlaybackOngoing="
        + navigationPlaybackOngoing
        + '}';
  }
}
