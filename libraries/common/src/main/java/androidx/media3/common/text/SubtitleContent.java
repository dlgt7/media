package androidx.media3.common.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transcript of a subtitle track: the timed entries of the selected subtitle, materialized so the
 * UI can render a full-text transcript independently of playback.
 */
public final class SubtitleContent {

  /** Content could not be produced for this selection. */
  public static final SubtitleContent UNSUPPORTED = new SubtitleContent(State.UNSUPPORTED);

  /** Content is still being loaded. */
  public static final SubtitleContent LOADING = new SubtitleContent(State.LOADING);

  /** Lifecycle state of the content. */
  public enum State {
    LOADING,
    COMPLETE,
    ERROR,
    UNSUPPORTED,
    PARTIAL,
    TRUNCATED
  }

  /** One timed subtitle entry. */
  public static final class Entry {

    public final long startTimeUs;
    public final long endTimeUs;
    public final String text;
    public final boolean image;

    public Entry(long startTimeUs, long endTimeUs, String text, boolean image) {
      this.startTimeUs = startTimeUs;
      this.endTimeUs = endTimeUs;
      this.text = text;
      this.image = image;
    }
  }

  public final State state;
  public final List<Entry> entries;

  public SubtitleContent(State state) {
    this(state, Collections.emptyList());
  }

  public SubtitleContent(State state, List<Entry> entries) {
    this.state = state;
    this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
  }
}
