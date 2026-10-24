package androidx.media3.exoplayer.text;

import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sink receiving the cues of the secondary subtitle renderer. Consumers poll {@link #getCues()}
 * to render the secondary line independently from the primary subtitle view.
 */
@UnstableApi
public final class SecondaryTextOutput implements TextOutput {

  private final List<Cue> cues = new CopyOnWriteArrayList<>();

  /** Returns the most recent cues. */
  public List<Cue> getCues() {
    return Collections.unmodifiableList(cues);
  }

  /** Clears the current cues. */
  public void clearCues() {
    cues.clear();
  }

  @Deprecated
  @Override
  public void onCues(List<Cue> cues) {
    this.cues.clear();
    this.cues.addAll(cues);
  }

  @Override
  public void onCues(CueGroup cueGroup) {
    onCues(cueGroup.cues);
  }
}
