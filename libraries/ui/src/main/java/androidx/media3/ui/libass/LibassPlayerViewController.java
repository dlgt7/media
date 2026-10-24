package androidx.media3.ui.libass;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.libass.LibassSubtitleController;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bridges the libass subtitle pipeline into a {@link PlayerView}: renders primary subtitles with
 * libass styling and secondary subtitles into a separate {@link SubtitleView} instance.
 *
 * <p>This build carries no libass renderer, so the controller only maintains the style override
 * and secondary view configuration; cue rendering falls back to the platform pipeline.
 */
@UnstableApi
public final class LibassPlayerViewController {

  private final Player player;
  private final LibassPlaybackSession playbackSession;
  private final LibassSubtitleController subtitleController;

  @Nullable private PlayerView playerView;
  @Nullable private CaptionStyleCompat styleOverride;
  @Nullable private String fontFamily;
  @Nullable private Consumer<SubtitleView> secondaryViewConfigurator;
  @Nullable private SubtitleView secondarySubtitleView;

  public LibassPlayerViewController(
      Player player,
      LibassPlaybackSession playbackSession,
      LibassSubtitleController subtitleController) {
    this.player = player;
    this.playbackSession = playbackSession;
    this.subtitleController = subtitleController;
  }

  /** Binds the controller to a player view, attaching the secondary subtitle view. */
  public void bind(PlayerView playerView) {
    this.playerView = playerView;
    applyStyleOverride(playerView);
    attachSecondarySubtitleView(playerView);
  }

  /** Overrides the caption style (and optional font family) used for primary subtitles. */
  public void setStyleOverride(@Nullable CaptionStyleCompat styleOverride,
      @Nullable String fontFamily) {
    this.styleOverride = styleOverride;
    this.fontFamily = fontFamily;
    PlayerView bound = playerView;
    if (bound != null) applyStyleOverride(bound);
  }

  /** Registers the configurator applied to the secondary subtitle view. */
  public void setSecondarySubtitleViewConfigurator(@Nullable Consumer<SubtitleView> configurator) {
    this.secondaryViewConfigurator = configurator;
    SubtitleView secondary = secondarySubtitleView;
    if (secondary != null && configurator != null) configurator.accept(secondary);
  }

  /** Releases the controller and removes the secondary subtitle view. */
  public void close() {
    if (secondarySubtitleView != null
        && secondarySubtitleView.getParent() instanceof android.view.ViewGroup) {
      ((android.view.ViewGroup) secondarySubtitleView.getParent())
          .removeView(secondarySubtitleView);
    }
    secondarySubtitleView = null;
    playerView = null;
  }

  private void applyStyleOverride(PlayerView playerView) {
    SubtitleView subtitleView = playerView.getSubtitleView();
    if (subtitleView == null) return;
    CaptionStyleCompat style = styleOverride != null ? styleOverride : CaptionStyleCompat.DEFAULT;
    subtitleView.setStyle(style);
    subtitleView.setApplyEmbeddedStyles(styleOverride == null);
  }

  private void attachSecondarySubtitleView(PlayerView playerView) {
    android.view.ViewGroup overlay = playerView.getOverlayFrameLayout();
    Context context = playerView.getContext();
    if (overlay == null) return;
    if (secondarySubtitleView != null
        && secondarySubtitleView.getParent() instanceof android.view.ViewGroup) {
      ((android.view.ViewGroup) secondarySubtitleView.getParent())
          .removeView(secondarySubtitleView);
    }
    SubtitleView secondary = new SubtitleView(context);
    secondary.setLayoutParams(
        new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
    overlay.addView(secondary);
    secondary.setCues(Collections.<Cue>emptyList());
    this.secondarySubtitleView = secondary;
    Consumer<SubtitleView> configurator = secondaryViewConfigurator;
    if (configurator != null) configurator.accept(secondary);
  }

  /** Returns the cues currently shown by the secondary subtitle view. */
  public List<Cue> getSecondaryCues() {
    SubtitleView secondary = secondarySubtitleView;
    return secondary == null ? Collections.emptyList() : Collections.emptyList();
  }
}
