package androidx.media3.ui.danmaku;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import okhttp3.OkHttpClient;

/**
 * Player-view facing danmaku controller: hosts a {@link DanmakuView} inside a {@link PlayerView}'s
 * overlay frame and delegates playback, data source and configuration to a {@link
 * DanmakuController}.
 */
@UnstableApi
public final class DanmakuPlayerViewController {

  private final DanmakuController controller;
  private final DanmakuView danmakuView;
  private final java.util.List<Danmaku> items = new java.util.concurrent.CopyOnWriteArrayList<>();

  @Nullable private PlayerView playerView;
  private boolean enabled = true;

  public DanmakuPlayerViewController() {
    this.controller = new DanmakuController();
    this.danmakuView = new DanmakuView(null);
    controller.setView(danmakuView);
  }

  /** Replaces the danmaku items shown in the view. */
  public void setItems(java.util.List<Danmaku> newItems) {
    items.clear();
    items.addAll(newItems);
    controller.setItems(new java.util.ArrayList<>(newItems));
  }

  /** Returns the danmaku items last set on this controller. */
  public java.util.List<Danmaku> getItems() {
    return java.util.Collections.unmodifiableList(items);
  }

  /** Attaches the danmaku view to the given player view's overlay frame. */
  public void bind(PlayerView playerView) {
    this.playerView = playerView;
    ViewGroup overlay = playerView.getOverlayFrameLayout();
    if (overlay == null) return;
    if (danmakuView.getParent() instanceof ViewGroup && danmakuView.getParent() != overlay) {
      ((ViewGroup) danmakuView.getParent()).removeView(danmakuView);
    }
    if (danmakuView.getParent() == null) {
      danmakuView.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      overlay.addView(danmakuView);
    }
    Player player = playerView.getPlayer();
    if (player != null) controller.setPlayer(player);
    applyEnabled();
  }

  /** Sets the HTTP client used to fetch danmaku sources. */
  public void setOkHttpClient(@Nullable OkHttpClient okHttpClient) {
    controller.setOkHttpClient(okHttpClient);
  }

  /** Enables or disables danmaku rendering. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    applyEnabled();
  }

  /** Applies the given danmaku configuration. */
  public void setConfig(DanmakuConfig config) {
    controller.setConfig(config);
  }

  /** Sets the danmaku source URI, or {@code null} to clear it. */
  public void setDataSource(@Nullable Uri uri) {
    controller.setDataSource(uri);
  }

  /** Posts a user-sent danmaku immediately. */
  public void sendNow(String text) {
    controller.sendNow(text);
  }

  /** Emits a danmaku (for example a live message) into the running view. */
  public void post(Danmaku danmaku) {
    controller.sendNow(danmaku);
  }

  /** Releases the controller and detaches the view. */
  public void close() {
    if (danmakuView.getParent() instanceof ViewGroup) {
      ((ViewGroup) danmakuView.getParent()).removeView(danmakuView);
    }
    playerView = null;
  }

  private void applyEnabled() {
    controller.setEnabled(enabled && playerView != null);
  }
}
