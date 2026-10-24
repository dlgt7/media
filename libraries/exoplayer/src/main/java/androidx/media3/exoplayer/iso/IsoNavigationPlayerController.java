package androidx.media3.exoplayer.iso;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.DiscMenuState;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;

/**
 * Controller bridging an {@link ExoPlayer} to an optical disc navigation session. When the media
 * is not a disc image the controller stays idle and forwards the regular media source untouched.
 */
@UnstableApi
public final class IsoNavigationPlayerController implements DiscMenuController {

  private final ExoPlayer player;
  private final DiscMenuState state = DiscMenuState.IDLE;

  public IsoNavigationPlayerController(ExoPlayer player) {
    this.player = player;
  }

  /** Returns the player facade driven by this controller. */
  public Player getPlayer() {
    return player;
  }

  /**
   * Prepares the media source for navigation. Returns {@code null} when the media references an
   * unsupported optical disc image, otherwise the passed-through source.
   */
  @Nullable
  public MediaSource prepareMediaSource(
      Context context, MediaSource source, DataSource.Factory dataSourceFactory) {
    MediaItem mediaItem = source.getMediaItem();
    if (IsoDiscPreparation.isIso(mediaItem)
        && mediaItem.localConfiguration != null
        && "iso".equals(mediaItem.localConfiguration.uri.getScheme())) {
      // Disc navigation is not available in this build; surface as unsupported.
      return null;
    }
    return source;
  }

  /** Returns whether a navigation session is currently active. */
  public boolean isStarted() {
    return state.phase != DiscMenuState.PHASE_IDLE;
  }

  /** Closes any active navigation session. */
  public void close() {}

  @Override
  public DiscMenuState getDiscMenuState() {
    return state;
  }
}
