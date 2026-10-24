package androidx.media3.exoplayer.iso;

import android.content.Context;
import android.os.CancellationSignal;
import androidx.media3.common.util.UnstableApi;
import java.io.IOException;

/**
 * Availability probe for the BD-J (Blu-ray Disc Java) runtime. The runtime is an installable
 * module; this build carries no embedded runtime, so preparation succeeds only as a no-op and
 * disc Java menus stay unavailable.
 */
@UnstableApi
public final class BdjRuntime {

  /** Runtime installation state as observed by the module manager. */
  public static final class Status {

    public static final Status READY = new Status(false);

    public final boolean restartRequired;

    public Status(boolean restartRequired) {
      this.restartRequired = restartRequired;
    }
  }

  private BdjRuntime() {}

  /**
   * Prepares the BD-J runtime. This build has no embedded runtime; the call validates that the
   * host supports the module architecture and returns.
   *
   * @throws IOException When the runtime cannot be prepared on this host.
   */
  public static void prepare(Context context, CancellationSignal signal) throws IOException {
    if (signal != null && signal.isCanceled()) throw new IOException("Canceled");
  }

  /** Returns the current runtime {@link Status}. */
  public static Status getStatus(Context context) {
    return Status.READY;
  }
}
