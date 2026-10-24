package androidx.media3.exoplayer.hls;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;

/**
 * Detects CMG-protected live streams that must be routed to a dedicated playback pipeline. A
 * stream is considered CMG when its URI uses the {@code cmg://} scheme.
 */
@UnstableApi
public final class CmgConfiguration {

  private static final String SCHEME = "cmg";

  public final Uri uri;

  private CmgConfiguration(Uri uri) {
    this.uri = uri;
  }

  /** Returns the configuration for the given media URI, or {@code null} when it is not CMG. */
  @Nullable
  public static CmgConfiguration fromMediaUri(@Nullable Uri uri) {
    if (uri == null) return null;
    if (!SCHEME.equals(uri.getScheme())) return null;
    return new CmgConfiguration(uri);
  }
}
