package androidx.media3.exoplayer.iso;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helpers for optical disc image (ISO / BDMV) media items. Disc images are recognized by their
 * {@code iso://} URI scheme, where the scheme-specific part carries the path of the image (and an
 * optional edition index fragment).
 */
@UnstableApi
public final class IsoDiscPreparation {

  private static final String SCHEME = "iso";

  /** Returns whether the media item references an optical disc image. */
  public static boolean isIso(@Nullable MediaItem mediaItem) {
    if (mediaItem == null || mediaItem.localConfiguration == null) return false;
    Uri uri = mediaItem.localConfiguration.uri;
    if (uri == null) return false;
    if (SCHEME.equals(uri.getScheme())) return true;
    String path = uri.getPath();
    return path != null && (path.endsWith(".iso") || path.contains("/BDMV/"));
  }

  private IsoDiscPreparation() {}
}
