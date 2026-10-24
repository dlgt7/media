package androidx.media3.datasource;

import androidx.media3.common.util.UnstableApi;

/**
 * Helpers for ISO disc image URIs. Disc images carry the {@code iso://} scheme; query parameters
 * encode the edition to start.
 */
@UnstableApi
public final class IsoUri {

  private static final String SCHEME = "iso";

  private IsoUri() {}

  /**
   * Returns a copy of the disc image URI selecting the given edition index. Non-ISO URIs are
   * returned unchanged.
   */
  public static android.net.Uri withEditionIndex(android.net.Uri uri, int editionIndex) {
    if (!SCHEME.equals(uri.getScheme()) || editionIndex == androidx.media3.common.C.INDEX_UNSET) {
      return uri;
    }
    return uri.buildUpon().fragment("edition=" + editionIndex).build();
  }

  /** Returns the edition index encoded in the URI, or {@link androidx.media3.common.C#INDEX_UNSET}. */
  public static int getEditionIndex(android.net.Uri uri) {
    if (!SCHEME.equals(uri.getScheme())) return androidx.media3.common.C.INDEX_UNSET;
    String fragment = uri.getFragment();
    if (fragment == null) return androidx.media3.common.C.INDEX_UNSET;
    int equals = fragment.indexOf('=');
    if (equals < 0 || !"edition".equals(fragment.substring(0, equals))) {
      return androidx.media3.common.C.INDEX_UNSET;
    }
    try {
      return Integer.parseInt(fragment.substring(equals + 1));
    } catch (NumberFormatException error) {
      return androidx.media3.common.C.INDEX_UNSET;
    }
  }
}
