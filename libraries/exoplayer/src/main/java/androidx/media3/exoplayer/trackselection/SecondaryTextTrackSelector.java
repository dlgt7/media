package androidx.media3.exoplayer.trackselection;

import android.content.Context;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.trackselection.TrackSelector;

/**
 * Track selector factory wrapper marking the selection chain as secondary-text aware. The
 * underlying selector is created by the wrapped {@link TrackSelector.Factory} (typically a
 * {@link DecodeTrackSelector} bound to the player's {@code DecoderManager}) and returned
 * unchanged, so decoder preferences and parameters configured by the delegate are preserved.
 */
@UnstableApi
public final class SecondaryTextTrackSelector {

  /** Factory creating secondary-text-aware selectors from a delegate factory. */
  public static final class Factory implements TrackSelector.Factory {

    private final TrackSelector.Factory delegate;

    public Factory(TrackSelector.Factory delegate) {
      this.delegate = delegate;
    }

    @Override
    public TrackSelector createTrackSelector(Context context) {
      return delegate.createTrackSelector(context);
    }
  }

  private SecondaryTextTrackSelector() {}
}
