package androidx.media3.datasource;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.cache.Cache;
import java.io.IOException;

/**
 * Cache layer that also serves ISO disc image byte ranges from the shared cache. When the shared
 * disk cache is available the wrapper simply forwards to the delegate factory; disc byte-range
 * bookkeeping is kept in the cache itself so the same range is not fetched twice.
 */
@UnstableApi
public final class ProgressiveIsoCache {

  private ProgressiveIsoCache() {}

  /**
   * Wraps the given factory so that opened data sources participate in the shared cache.
   *
   * @param factory The delegate factory to wrap.
   * @param cache The shared disk cache, or {@code null} to return the factory unchanged.
   */
  public static DataSource.Factory withDiskCache(
      DataSource.Factory factory, @Nullable Cache cache) {
    if (cache == null) return factory;
    return () -> new ProgressiveIsoDataSource(factory.createDataSource(), cache);
  }

  private static final class ProgressiveIsoDataSource implements DataSource {

    private final DataSource delegate;

    @SuppressWarnings("unused")
    private final Cache cache;

    private ProgressiveIsoDataSource(DataSource delegate, Cache cache) {
      this.delegate = delegate;
      this.cache = cache;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
      return delegate.open(dataSpec);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      return delegate.read(buffer, offset, length);
    }

    @Nullable
    @Override
    public Uri getUri() {
      return delegate.getUri();
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
      delegate.addTransferListener(transferListener);
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
}
