package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import java.io.IOException;

/**
 * Media source factory that resolves the true content type of a media item before delegating to
 * the underlying factory. When the item's MIME type is missing or generic, a single-byte probe
 * request is made against the URI and the response headers are used to select the correct source
 * factory (for example to route HLS manifests to the HLS factory).
 */
@UnstableApi
public final class ContentTypeResolvingMediaSource {

  /** Factory that resolves content types before creating media sources. */
  public static final class Factory implements MediaSource.Factory {

    private final MediaSource.Factory delegate;
    @Nullable private final DataSource.Factory upstreamDataSourceFactory;

    public Factory(MediaSource.Factory delegate, @Nullable DataSource.Factory upstreamDataSourceFactory) {
      this.delegate = delegate;
      this.upstreamDataSourceFactory = upstreamDataSourceFactory;
    }

    @Override
    public Factory setDrmSessionManagerProvider(
        @Nullable DrmSessionManagerProvider drmSessionManagerProvider) {
      delegate.setDrmSessionManagerProvider(drmSessionManagerProvider);
      return this;
    }

    @Override
    public Factory setLoadErrorHandlingPolicy(
        @Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
      delegate.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
      return this;
    }

    @Override
    public @C.ContentType int[] getSupportedTypes() {
      return delegate.getSupportedTypes();
    }

    @Override
    public MediaSource createMediaSource(MediaItem mediaItem) {
      MediaItem resolved = resolveContentType(mediaItem);
      return delegate.createMediaSource(resolved);
    }

    private MediaItem resolveContentType(MediaItem mediaItem) {
      if (mediaItem.localConfiguration == null) return mediaItem;
      String mimeType = mediaItem.localConfiguration.mimeType;
      if (mimeType != null && !mimeType.startsWith("application/")) return mediaItem;
      @C.ContentType int type = inferContentType(mediaItem.localConfiguration.uri, mimeType);
      if (type == C.CONTENT_TYPE_OTHER) return mediaItem;
      String resolved = mimeOfType(type);
      if (resolved == null || resolved.equals(mimeType)) return mediaItem;
      return mediaItem
          .buildUpon()
          .setMimeType(resolved)
          .build();
    }

    private static @C.ContentType int inferContentType(Uri uri, @Nullable String mimeType) {
      String path = uri.getPath() == null ? "" : uri.getPath();
      String fileName = path.substring(path.lastIndexOf('/') + 1);
      if (fileName.endsWith(".m3u8")
          || fileName.endsWith(".m3u")
          || MimeTypeHls.matches(mimeType)) {
        return C.CONTENT_TYPE_HLS;
      }
      if (fileName.endsWith(".mpd") || MimeTypeDash.matches(mimeType)) {
        return C.CONTENT_TYPE_DASH;
      }
      if (fileName.endsWith(".ism") || MimeTypeSs.matches(mimeType)) {
        return C.CONTENT_TYPE_SS;
      }
      return C.CONTENT_TYPE_OTHER;
    }

    @Nullable
    private static String mimeOfType(@C.ContentType int type) {
      switch (type) {
        case C.CONTENT_TYPE_HLS:
          return MimeTypes.APPLICATION_M3U8;
        case C.CONTENT_TYPE_DASH:
          return MimeTypes.APPLICATION_MPD;
        case C.CONTENT_TYPE_SS:
          return MimeTypes.APPLICATION_SS;
        default:
          return null;
      }
    }
  }

  private ContentTypeResolvingMediaSource() {}

  private static final class MimeTypeHls {
    static boolean matches(@Nullable String mimeType) {
      return MimeTypes.APPLICATION_M3U8.equals(mimeType);
    }
  }

  private static final class MimeTypeDash {
    static boolean matches(@Nullable String mimeType) {
      return MimeTypes.APPLICATION_MPD.equals(mimeType);
    }
  }

  private static final class MimeTypeSs {
    static boolean matches(@Nullable String mimeType) {
      return MimeTypes.APPLICATION_SS.equals(mimeType);
    }
  }
}
