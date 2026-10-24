package androidx.media3.exoplayer.libass;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.text.SecondaryTextOutput;
import androidx.media3.exoplayer.trackselection.SecondaryTextTrackSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordinates the dual-subtitle selection: the primary subtitle renderer keeps its regular
 * selection while the secondary selection is tracked here and applied as a track selection
 * override.
 */
@UnstableApi
public final class LibassSubtitleController {

  private final ExoPlayer player;
  private final LibassPlaybackSession playbackSession;
  private final SecondaryTextTrackSelector.Factory trackSelectorFactory;
  private final SecondaryTextOutput secondaryTextOutput;

  @Nullable private TrackSelectionOverride requestedSecondarySelection;
  private boolean secondaryAutoSelectionEnabled;

  public LibassSubtitleController(
      ExoPlayer player,
      LibassPlaybackSession playbackSession,
      SecondaryTextTrackSelector.Factory trackSelectorFactory,
      SecondaryTextOutput secondaryTextOutput) {
    this.player = player;
    this.playbackSession = playbackSession;
    this.trackSelectorFactory = trackSelectorFactory;
    this.secondaryTextOutput = secondaryTextOutput;
  }

  /** Returns the current {@link SubtitleSelectionState} derived from the player. */
  public SubtitleSelectionState getSubtitleSelectionState() {
    Tracks tracks = player.getCurrentTracks();
    TrackSelectionOverride primary = null;
    TrackSelectionOverride activeSecondary = null;
    List<TrackSelectionOverride> candidates = new ArrayList<>();
    for (Tracks.Group group : tracks.getGroups()) {
      if (group.getType() != C.TRACK_TYPE_TEXT) continue;
      for (int i = 0; i < group.length; i++) {
        TrackSelectionOverride override =
            new TrackSelectionOverride(group.getMediaTrackGroup(), i);
        if (group.isTrackSelected(i)) {
          if (primary == null) primary = override;
          else if (activeSecondary == null) activeSecondary = override;
        } else {
          candidates.add(override);
        }
      }
    }
    boolean promoted = activeSecondary != null && primary == null;
    return new SubtitleSelectionState(
        primary,
        activeSecondary,
        candidates,
        requestedSecondarySelection != null ? requestedSecondarySelection : activeSecondary,
        promoted);
  }

  /**
   * Applies the secondary subtitle selection. Passing {@code null} clears the secondary
   * selection.
   */
  public void setSecondaryTextTrackSelectionOverride(@Nullable TrackSelectionOverride selection) {
    this.requestedSecondarySelection = selection;
    TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
    builder.clearOverridesOfType(C.TRACK_TYPE_TEXT);
    if (selection != null) builder.addOverride(selection);
    player.setTrackSelectionParameters(builder.build());
    secondaryTextOutput.clearCues();
  }

  /** Enables or disables automatic selection of a secondary subtitle track. */
  public void setSecondaryTextTrackAutoSelectionEnabled(boolean enabled) {
    this.secondaryAutoSelectionEnabled = enabled;
  }

  /** Returns whether automatic secondary selection is enabled. */
  public boolean isSecondaryTextTrackAutoSelectionEnabled() {
    return secondaryAutoSelectionEnabled;
  }

  /** Releases the controller. */
  public void close() {
    secondaryTextOutput.clearCues();
  }
}
