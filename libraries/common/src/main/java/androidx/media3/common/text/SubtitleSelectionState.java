package androidx.media3.common.text;

import androidx.annotation.Nullable;
import androidx.media3.common.TrackSelectionOverride;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of the dual-subtitle selection: which track serves the primary subtitle role, which
 * (if any) serves the secondary role, and the candidates the user may still pick for the
 * secondary slot.
 */
public final class SubtitleSelectionState {

  /** A state describing a player without any subtitle selection. */
  public static final SubtitleSelectionState EMPTY = new SubtitleSelectionState();

  /** The override currently driving the primary subtitle renderer, or {@code null}. */
  @Nullable public final TrackSelectionOverride primarySelection;

  /** The override currently driving the secondary subtitle renderer, or {@code null}. */
  @Nullable public final TrackSelectionOverride activeSecondarySelection;

  /** The secondary candidates that the player can expose, most suitable first. */
  public final List<TrackSelectionOverride> secondaryCandidates;

  /** The user-requested secondary selection, kept even while it cannot be applied. */
  @Nullable public final TrackSelectionOverride requestedSecondarySelection;

  /** Whether the secondary selection has been promoted into the primary slot. */
  public final boolean secondaryPromotedToPrimary;

  private SubtitleSelectionState() {
    this(
        null,
        null,
        Collections.emptyList(),
        null,
        /* secondaryPromotedToPrimary= */ false);
  }

  public SubtitleSelectionState(
      @Nullable TrackSelectionOverride primarySelection,
      @Nullable TrackSelectionOverride activeSecondarySelection,
      List<TrackSelectionOverride> secondaryCandidates,
      @Nullable TrackSelectionOverride requestedSecondarySelection,
      boolean secondaryPromotedToPrimary) {
    this.primarySelection = primarySelection;
    this.activeSecondarySelection = activeSecondarySelection;
    this.secondaryCandidates = Collections.unmodifiableList(new ArrayList<>(secondaryCandidates));
    this.requestedSecondarySelection = requestedSecondarySelection;
    this.secondaryPromotedToPrimary = secondaryPromotedToPrimary;
  }

  /** Returns whether the given override is the selection driving the primary subtitle. */
  public boolean isPrimary(TrackSelectionOverride selection) {
    return primarySelection != null && primarySelection.equals(selection);
  }

  /** Returns whether the given override is the selection driving the secondary subtitle. */
  public boolean isActiveSecondary(TrackSelectionOverride selection) {
    return activeSecondarySelection != null && activeSecondarySelection.equals(selection);
  }

  /** Returns whether the given override participates in any subtitle role. */
  public boolean isSubtitleRole(TrackSelectionOverride selection) {
    return isPrimary(selection) || isActiveSecondary(selection);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SubtitleSelectionState)) return false;
    SubtitleSelectionState other = (SubtitleSelectionState) obj;
    return java.util.Objects.equals(primarySelection, other.primarySelection)
        && java.util.Objects.equals(activeSecondarySelection, other.activeSecondarySelection)
        && secondaryCandidates.equals(other.secondaryCandidates)
        && java.util.Objects.equals(requestedSecondarySelection, other.requestedSecondarySelection)
        && secondaryPromotedToPrimary == other.secondaryPromotedToPrimary;
  }

  @Override
  public int hashCode() {
    int result = primarySelection == null ? 0 : primarySelection.hashCode();
    result = 31 * result + (activeSecondarySelection == null ? 0 : activeSecondarySelection.hashCode());
    result = 31 * result + secondaryCandidates.hashCode();
    result = 31 * result + (requestedSecondarySelection == null ? 0 : requestedSecondarySelection.hashCode());
    result = 31 * result + (secondaryPromotedToPrimary ? 1 : 0);
    return result;
  }
}
