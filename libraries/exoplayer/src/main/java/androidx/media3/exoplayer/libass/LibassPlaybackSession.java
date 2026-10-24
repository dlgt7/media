package androidx.media3.exoplayer.libass;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.ExtractorsFactory;

/**
 * Playback session for the libass subtitle renderer.
 *
 * <p>This build does not bundle the libass native renderer, so the session always reports {@link
 * #isAvailable()} {@code false}; callers fall back to the platform text pipeline. All mutators
 * are safe no-ops so the surrounding engine code needs no branching.
 */
@UnstableApi
public final class LibassPlaybackSession {

  /**
   * Media components produced for a libass-enabled pipeline. Not created in this build because
   * {@link #isAvailable()} is always {@code false}.
   */
  public static final class MediaComponents {

    public final ExtractorsFactory extractorsFactory;
    public final androidx.media3.extractor.text.SubtitleParser.Factory subtitleParserFactory;

    public MediaComponents(
        ExtractorsFactory extractorsFactory,
        androidx.media3.extractor.text.SubtitleParser.Factory subtitleParserFactory) {
      this.extractorsFactory = extractorsFactory;
      this.subtitleParserFactory = subtitleParserFactory;
    }
  }

  private final LibassConfiguration configuration;
  private final boolean enabled;

  public LibassPlaybackSession(LibassConfiguration configuration, boolean enabled) {
    this.configuration = configuration;
    this.enabled = enabled;
  }

  /** Returns the active configuration. */
  public LibassConfiguration getConfiguration() {
    return configuration;
  }

  /** Always {@code false}: the libass native renderer is not part of this build. */
  public boolean isAvailable() {
    return false;
  }

  /** Returns whether the session was constructed with libass enabled by the user. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Records the media item about to be preloaded. No-op in this build. */
  public void setPreloadMediaItem(@Nullable MediaItem mediaItem) {}

  /** Sets the bottom position fraction of primary subtitles. No-op in this build. */
  public void setBottomPositionFraction(float fraction) {}

  /** Sets the bottom position fraction of secondary subtitles. No-op in this build. */
  public void setSecondaryBottomPositionFraction(float fraction) {}

  /** Sets the font scale, optionally re-applying the embedded style. No-op in this build. */
  public void setFontScale(float scale, boolean applyEmbeddedStyle) {}

  /** Releases the session. No-op in this build. */
  public void close() {}

  /**
   * Creates the clock renderer used to advance libass timings alongside the media clock. This
   * build has no libass renderer, so a no-op {@code NoSampleRenderer} is returned; callers may
   * add it to the renderer list unconditionally.
   */
  public androidx.media3.exoplayer.Renderer createClockRenderer() {
    return new androidx.media3.exoplayer.NoSampleRenderer() {
      @Override
      public String getName() {
        return "LibassClockRenderer";
      }

      @Override
      public void render(long positionUs, long elapsedRealtimeUs) {
        // No libass clock to advance in this build.
      }
    };
  }

  /**
   * Creates the media components used by the extraction pipeline for a libass-enabled item. The
   * extractors factory is passed through unchanged and the subtitle parser factory keeps the
   * transcript association.
   *
   * @param mediaItem The media item being prepared.
   * @param extractorsFactory The extractors factory selected by the caller.
   * @param transcript The transcript handle for the item.
   * @return Components wiring the given factories together.
   */
  public MediaComponents createMediaComponents(
      MediaItem mediaItem,
      androidx.media3.extractor.ExtractorsFactory extractorsFactory,
      androidx.media3.exoplayer.text.SubtitleTranscript transcript) {
    return new MediaComponents(
        extractorsFactory,
        new androidx.media3.exoplayer.text.SubtitleTranscriptParserFactory(transcript));
  }
}
