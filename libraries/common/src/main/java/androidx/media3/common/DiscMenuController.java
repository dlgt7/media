package androidx.media3.common;

import java.util.function.Consumer;

/**
 * Handle to an optical disc navigation session (ISO / Blu-ray menu navigation) attached to a
 * player. Implementations are provided by the playback engine; the app uses this handle to query
 * state and dispatch user input while navigation is active.
 */
public interface DiscMenuController {

  /** Outcome of an asynchronous disc menu open request. */
  enum OpenResult {
    /** The menu opened and navigation is active. */
    OPENED,
    /** The BD-J runtime module is not installed. */
    RUNTIME_MISSING,
    /** No menu is available for the current media. */
    UNAVAILABLE,
    /** Opening the menu timed out. */
    TIMED_OUT
  }

  /** Returns the {@link DiscMenuState} describing the navigation session. */
  DiscMenuState getDiscMenuState();

  /** Returns whether an interactive menu is currently active. */
  default boolean isActive() {
    return getDiscMenuState().phase == DiscMenuState.PHASE_ACTIVE;
  }

  /** Returns whether a navigation playback (menu triggered stream) is ongoing. */
  default boolean isNavigationPlayback() {
    return getDiscMenuState().navigationPlaybackOngoing;
  }

  /**
   * Requests to open the disc menu, invoking the callback with the outcome. Implementations must
   * invoke the callback exactly once.
   *
   * @param action The opening action, or an implementation defined default when empty.
   * @param callback Receives the {@link OpenResult}.
   * @return {@code false} when opening cannot even be attempted.
   */
  default boolean open(String action, Consumer<OpenResult> callback) {
    callback.accept(OpenResult.UNAVAILABLE);
    return true;
  }

  /** Dispatches a navigation action (e.g. {@code "prev"}) to the active menu. */
  default boolean sendAction(String action) {
    return false;
  }

  /** Returns whether the menu supports pointer (touch) input. */
  default boolean supportsPointer() {
    return false;
  }
}
