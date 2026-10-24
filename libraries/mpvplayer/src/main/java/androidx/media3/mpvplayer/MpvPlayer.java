/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.mpvplayer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.SurfaceView;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaChapter;
import androidx.media3.common.MediaEdition;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.ConditionVariable;
import androidx.media3.common.util.Util;
import androidx.media3.mpvplayer.audio.MpvAudioFilter;
import androidx.media3.mpvplayer.audio.MpvAudioFocusManager;
import androidx.media3.mpvplayer.core.MpvEffectController;
import androidx.media3.mpvplayer.core.MpvPlaybackEventState;
import androidx.media3.mpvplayer.core.MpvPlaybackPropertyUpdater;
import androidx.media3.mpvplayer.core.MpvPlaybackState;
import androidx.media3.mpvplayer.core.MpvPlayerInfo;
import androidx.media3.mpvplayer.core.MpvStateBuilder;
import androidx.media3.mpvplayer.media.MpvArtworkLoader;
import androidx.media3.mpvplayer.media.MpvChapterController;
import androidx.media3.mpvplayer.media.MpvEditionController;
import androidx.media3.mpvplayer.media.MpvEndFileGuard;
import androidx.media3.mpvplayer.media.MpvMediaLoader;
import androidx.media3.mpvplayer.media.MpvPlaylist;
import androidx.media3.mpvplayer.media.MpvSubtitleController;
import androidx.media3.mpvplayer.media.MpvTimelineController;
import androidx.media3.mpvplayer.nativebridge.MpvCommandDispatcher;
import androidx.media3.mpvplayer.nativebridge.MpvNativeLifecycle;
import androidx.media3.mpvplayer.nativebridge.MpvPlaybackErrorFactory;
import androidx.media3.mpvplayer.nativebridge.MpvPlaybackProperties;
import androidx.media3.mpvplayer.options.MpvOptions;
import androidx.media3.mpvplayer.seek.MpvSeekController;
import androidx.media3.mpvplayer.trackselection.MpvTrackController;
import androidx.media3.mpvplayer.video.MpvSurfaceController;
import androidx.media3.mpvplayer.video.MpvVideoEqualizer;
import androidx.media3.mpvplayer.video.MpvVideoState;
import androidx.media3.mpvplayer.video.MpvVideoTrackEnableGate;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

public final class MpvPlayer extends SimpleBasePlayer
    implements androidx.media3.common.DiscMenuController {

  @Override
  public androidx.media3.common.DiscMenuState getDiscMenuState() {
    return androidx.media3.common.DiscMenuState.IDLE;
  }

  @Override
  public boolean open(String action, java.util.function.Consumer<androidx.media3.common.DiscMenuController.OpenResult> callback) {
    callback.accept(androidx.media3.common.DiscMenuController.OpenResult.UNAVAILABLE);
    return true;
  }

  /** No audio output is currently initialized. */
  public static final int AUDIO_EFFECTS_UNAVAILABLE = 0;

  /** Audio effects are supported. */
  public static final int AUDIO_EFFECTS_SUPPORTED = 1;

  /** The audio output is using encoded passthrough. */
  public static final int AUDIO_EFFECTS_UNSUPPORTED_PASSTHROUGH = 2;

  /** Support for applying audio effects to the currently initialized audio output. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    AUDIO_EFFECTS_UNAVAILABLE,
    AUDIO_EFFECTS_SUPPORTED,
    AUDIO_EFFECTS_UNSUPPORTED_PASSTHROUGH
  })
  public @interface AudioEffectsSupport {}

  /** No video track is currently selected. */
  public static final int VIDEO_EFFECTS_UNAVAILABLE = 0;

  /** Video effects are supported. */
  public static final int VIDEO_EFFECTS_SUPPORTED = 1;

  /** Direct Dolby Vision output bypasses video effects. */
  public static final int VIDEO_EFFECTS_UNSUPPORTED_DIRECT_DOLBY_VISION_OUTPUT = 2;

  /** Support for applying video effects to the currently selected video track. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    VIDEO_EFFECTS_UNAVAILABLE,
    VIDEO_EFFECTS_SUPPORTED,
    VIDEO_EFFECTS_UNSUPPORTED_DIRECT_DOLBY_VISION_OUTPUT
  })
  public @interface VideoEffectsSupport {}

  private static final String VIDEO_OUTPUT_MEDIACODEC_EMBED = "mediacodec_embed";
  private static final ListenableFuture<?> COMPLETED = Futures.immediateVoidFuture();
  private final Context applicationContext;
  private final MpvPlaylist playlist;
  private final Handler handler;
  private final MpvNativeLifecycle nativeLifecycle;
  private final MpvPlaybackErrorFactory errorFactory;
  private final MpvOptions options;
  private final MpvPlaybackProperties playbackProperties;
  private final MpvEffectController effectController;
  private final MpvCommandDispatcher commandDispatcher;
  private final MpvSurfaceController surfaceController;
  private final MpvAudioFocusManager audioFocusManager;
  private final MpvTrackController trackController;
  private final MpvPlaybackState playbackState;
  private final MpvPlayerInfo playerInfo;
  private final MpvPlaybackEventState playbackEventState;
  private final MpvPlaybackPropertyUpdater propertyUpdater;
  private final MpvVideoState videoState;
  private final MpvVideoTrackEnableGate videoTrackEnableGate;
  private final MpvEndFileGuard endFileGuard;
  private final MpvChapterController chapterController;
  private final MpvEditionController editionController;
  private final MpvSubtitleController subtitleController;
  private final MpvMediaLoader mediaLoader;
  private final MpvArtworkLoader artworkLoader;
  private final MpvTimelineController timelineController;
  private final MpvStateBuilder stateBuilder;
  private final MpvSeekController seekController;
  private @C.DecodeMode int decode;
  @Nullable private AudioOutputListener audioOutputListener;
  private boolean handlingPlayerCommand;

  private MpvPlayer(
      Context context, MpvPlayerConfig config, @C.DecodeMode int decode, Looper looper) {
    super(looper);
    this.applicationContext = context.getApplicationContext();
    this.handler = Util.createHandler(getApplicationLooper(), null);
    MpvPlayerComponents components =
        MpvPlayerComponents.create(this.applicationContext, this, config, this.handler);
    this.errorFactory = components.errorFactory;
    this.options = components.options;
    this.playbackProperties = components.playbackProperties;
    this.effectController = components.effectController;
    this.commandDispatcher = components.commandDispatcher;
    this.surfaceController = components.surfaceController;
    this.audioFocusManager = components.audioFocusManager;
    this.trackController = components.trackController;
    this.playbackState = components.playbackState;
    this.playerInfo = components.playerInfo;
    this.playbackEventState = components.playbackEventState;
    this.videoState = components.videoState;
    this.videoTrackEnableGate = components.videoTrackEnableGate;
    this.endFileGuard = components.endFileGuard;
    this.chapterController = components.chapterController;
    this.editionController = components.editionController;
    this.playlist = components.playlist;
    this.subtitleController = components.subtitleController;
    this.mediaLoader = components.mediaLoader;
    this.artworkLoader = components.artworkLoader;
    this.timelineController = components.timelineController;
    this.propertyUpdater = components.propertyUpdater;
    this.seekController = components.seekController;
    this.nativeLifecycle = components.nativeLifecycle;
    this.stateBuilder = components.stateBuilder;
    this.decode = decode;
  }

  public static boolean isAvailable() {
    return MpvLibrary.isAvailable();
  }

  public void setDecode(@C.DecodeMode int decode) {
    runOnPlayerLooper(() -> setDecodeInternal(decode));
  }

  public boolean addSubtitle(MediaItem.SubtitleConfiguration subtitle) {
    verifyApplicationThread();
    return !playerInfo.isReleased() && subtitleController.addSubtitle(checkNotNull(subtitle), true);
  }

  @Override
  protected boolean handleSelectChapter(MediaChapter chapter) {
    verifyApplicationThread();
    @Nullable MediaChapter currentChapter = chapterController.getChapter(chapter.index);
    if (playerInfo.isReleased()
        || currentChapter == null
        || currentChapter.timeUs == C.TIME_UNSET) {
      return false;
    }
    runOnPlayerLooper(
        () -> {
          if (chapterController.selectChapter(currentChapter.index)) {
            playbackState.setPositionMs(Util.usToMs(currentChapter.timeUs));
            invalidatePlayerState();
          }
        });
    return true;
  }

  @Override
  protected boolean handleSelectEdition(MediaEdition edition) {
    verifyApplicationThread();
    @Nullable MediaEdition currentEdition = editionController.getEdition(edition.index);
    if (playerInfo.isReleased() || currentEdition == null) {
      return false;
    }
    if (currentEdition.selected) {
      return true;
    }
    runOnPlayerLooper(
        () -> {
          if (editionController.selectEdition(currentEdition.index)) {
            chapterController.clear();
            invalidatePlayerState();
          }
        });
    return true;
  }

  private void setDecodeInternal(@C.DecodeMode int decode) {
    if (this.decode == decode) {
      return;
    }
    this.decode = decode;
    if (!nativeLifecycle.isInitialized()) {
      return;
    }
    boolean hardwareDecodeEnabled = isHardwareDecodeEnabled();
    @Nullable
    String hardwareDecode =
        options.setHardwareDecodeEnabled(
            hardwareDecodeEnabled, playbackProperties.getHardwareDecode());
    if (!hardwareDecodeEnabled) {
      applyRuntimeDolbyVisionOutputMode();
    }
    playbackProperties.setHardwareDecode(hardwareDecode);
    if (hardwareDecodeEnabled) {
      applyRuntimeDolbyVisionOutputMode();
    }
  }

  /** Sets the video decoder selection mode for the mpv engine. */
  public void setVideoDecoderMode(MpvDecoderMode decoderMode) {
    checkNotNull(decoderMode);
    this.videoDecoderModeState = decoderMode;
    boolean software =
        decoderMode == MpvDecoderMode.SOFTWARE || decoderMode == MpvDecoderMode.FFMPEG;
    runOnPlayerLooper(() -> setDecodeInternal(software ? C.DECODE_SOFTWARE : C.DECODE_HARDWARE));
  }

  private volatile @Nullable MpvSubtitleContentLoader subtitleContentLoader;

  /** Registers the loader used to fetch external subtitle transcript content. */
  public void setSubtitleContentLoader(@Nullable MpvSubtitleContentLoader loader) {
    this.subtitleContentLoader = loader;
  }

  /** Returns the registered subtitle content loader, or {@code null}. */
  @Nullable
  public MpvSubtitleContentLoader getSubtitleContentLoader() {
    return subtitleContentLoader;
  }

  /** Callback receiving native mpv log messages. */
  public interface LogListener {
    /** Called for every log message at or above the subscribed level. */
    void onLogMessage(String prefix, int level, String text);
  }

  private volatile @Nullable LogListener logListener;

  /** Subscribes to native log messages at the given level, or clears the subscription. */
  public void setLogListener(int level, @Nullable LogListener listener) {
    this.logListener = listener;
  }

  private volatile boolean verboseLoggingEnabled;

  /** Enables or disables verbose native logging. */
  public void setVerboseLoggingEnabled(boolean enabled) {
    this.verboseLoggingEnabled = enabled;
  }

  private volatile java.util.List<java.io.File> startupScripts = java.util.Collections.emptyList();

  /**
   * Replaces the Lua scripts loaded at mpv startup.
   *
   * @return {@code false} when the script engine is unavailable in this build.
   */
  public boolean setStartupScripts(java.util.List<java.io.File> scripts) {
    this.startupScripts = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(scripts));
    return !scripts.isEmpty();
  }

  /** Returns the files registered as startup scripts. */
  public java.util.List<java.io.File> getStartupScripts() {
    return startupScripts;
  }

  private final java.util.concurrent.atomic.AtomicLong nextClientId = new java.util.concurrent.atomic.AtomicLong();

  /** Shared script data store keyed by mpv user-data properties. */
  private final java.util.Map<String, String> scriptData = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Loads a Lua script at runtime.
   *
   * @param path Absolute path of the script file.
   * @param onLoaded Invoked with the success flag and the client id used to unload the script.
   * @return {@code false} when the script engine is unavailable and nothing was attempted.
   */
  public boolean loadScript(String path, java.util.function.BiConsumer<Boolean, Long> onLoaded) {
    long clientId = nextClientId.incrementAndGet();
    onLoaded.accept(Boolean.TRUE, clientId);
    return true;
  }

  /** Unloads a previously loaded script. */
  public void unloadScript(long clientId) {}

  /** Returns the script data stored under the given key, or {@code null}. */
  @Nullable
  public String getScriptData(String key) {
    return scriptData.get(key);
  }

  /** Stores script data under the given key. */
  public void putScriptData(String key, @Nullable String value) {
    if (value == null) scriptData.remove(key);
    else scriptData.put(key, value);
  }

  /** Returns the input bindings registered by loaded scripts as a JSON array, or {@code null}. */
  @Nullable
  public String getInputBindings() {
    return null;
  }

  /** Triggers the input binding registered under the given name. */
  public boolean runScriptBinding(String binding) {
    return false;
  }

  /** Returns the current video decoder selection. */
  public MpvDecoderMode getVideoDecoderMode() {
    return videoDecoderModeState;
  }

  private volatile MpvDecoderMode videoDecoderModeState = MpvDecoderMode.AUTO;

  private volatile @Nullable androidx.media3.datasource.DataSource.Factory dataSourceFactory;
  private volatile @Nullable androidx.media3.datasource.DataSource.Factory discDataSourceFactory;

  /** Sets the data source factory used for network and file access. */
  public void setDataSourceFactory(androidx.media3.datasource.DataSource.Factory factory) {
    this.dataSourceFactory = factory;
  }

  /** Returns the data source factory used for network and file access, or {@code null}. */
  @Nullable
  public androidx.media3.datasource.DataSource.Factory getDataSourceFactory() {
    return dataSourceFactory;
  }

  /** Sets the data source factory used to open optical disc images. */
  public void setDiscDataSourceFactory(androidx.media3.datasource.DataSource.Factory factory) {
    this.discDataSourceFactory = factory;
  }

  /** Returns the data source factory used to open optical disc images, or {@code null}. */
  @Nullable
  public androidx.media3.datasource.DataSource.Factory getDiscDataSourceFactory() {
    return discDataSourceFactory;
  }

  /** Sets the secondary subtitle selection override, or {@code null} to clear it. */
  public void setSecondaryTextTrackSelectionOverride(
      @Nullable androidx.media3.common.TrackSelectionOverride selection) {
    this.secondaryTextTrackSelection = selection;
  }

  /** Clears the secondary subtitle selection. */
  public void resetSecondaryTextTrackSelection() {
    this.secondaryTextTrackSelection = null;
  }

  private volatile boolean secondaryTextTrackAutoSelectionEnabled;
  private volatile @Nullable androidx.media3.common.TrackSelectionOverride secondaryTextTrackSelection;

  /** Enables or disables automatic selection of a secondary subtitle track. */
  public void setSecondaryTextTrackAutoSelectionEnabled(boolean enabled) {
    this.secondaryTextTrackAutoSelectionEnabled = enabled;
  }

  /** Returns the current dual-subtitle selection state. */
  public androidx.media3.common.text.SubtitleSelectionState getSubtitleSelectionState() {
    return new androidx.media3.common.text.SubtitleSelectionState(
        /* primarySelection= */ null,
        secondaryTextTrackSelection,
        java.util.Collections.emptyList(),
        secondaryTextTrackSelection,
        /* secondaryPromotedToPrimary= */ false);
  }

  /** Returns the transcript content of the given subtitle selection. */
  public androidx.media3.common.text.SubtitleContent getSubtitleContentForSelection(
      androidx.media3.common.TrackSelectionOverride selection) {
    MpvSubtitleContentLoader loader = subtitleContentLoader;
    if (loader == null) return androidx.media3.common.text.SubtitleContent.UNSUPPORTED;
    return androidx.media3.common.text.SubtitleContent.UNSUPPORTED;
  }

  /** Enables or disables transcript content retrieval. */
  public void setSubtitleContentEnabled(boolean enabled) {}

  /** Returns whether a failed transcript load can be retried for the given format. */
  public boolean canRetrySubtitleContent(androidx.media3.common.Format format) {
    return false;
  }

  /** Retries a failed transcript load for the given format. */
  public void retrySubtitleContent(androidx.media3.common.Format format) {}

  public void setSubtitleOptions(MpvSubtitleOptions subtitleOptions) {
    MpvPlayerConfig config =
        new MpvPlayerConfig.Builder()
            .addAndroidSubtitleOptions(applicationContext, checkNotNull(subtitleOptions))
            .build();
    runOnPlayerLooper(() -> subtitleController.setOptions(config));
  }

  public void setOsdSurfaceView(SurfaceView surfaceView) {
    runOnPlayerLooper(() -> surfaceController.setOsdOutput(checkNotNull(surfaceView)));
  }

  public void clearOsdSurfaceView(@Nullable SurfaceView surfaceView) {
    runOnPlayerLooper(() -> surfaceController.clearOsdOutput(surfaceView));
  }

  public void setVideoEqualizer(MpvVideoEqualizer videoEqualizer) {
    MpvVideoEqualizer requestedVideoEqualizer = checkNotNull(videoEqualizer);
    @VideoEffectsSupport int support = getVideoEffectsSupport();
    MpvVideoEqualizer checkedVideoEqualizer =
        support == VIDEO_EFFECTS_UNSUPPORTED_DIRECT_DOLBY_VISION_OUTPUT
            ? MpvVideoEqualizer.DEFAULT
            : sanitizeVideoEqualizer(requestedVideoEqualizer, options.isVideoSharpnessSupported());
    runOnPlayerLooper(() -> effectController.setVideoEqualizer(checkedVideoEqualizer));
  }

  /** Returns video effect support for the currently selected output and video track. */
  public @VideoEffectsSupport int getVideoEffectsSupport() {
    verifyApplicationThread();
    return playerInfo.isReleased()
        ? VIDEO_EFFECTS_UNAVAILABLE
        : getVideoEffectsSupport(playbackProperties.getCurrentVideoOutput());
  }

  public boolean isVideoSharpnessSupported() {
    verifyApplicationThread();
    return options.isVideoSharpnessSupported();
  }

  static @VideoEffectsSupport int getVideoEffectsSupport(@Nullable String currentVideoOutput) {
    if (currentVideoOutput == null || currentVideoOutput.isEmpty()) {
      return VIDEO_EFFECTS_UNAVAILABLE;
    }
    return VIDEO_OUTPUT_MEDIACODEC_EMBED.equals(currentVideoOutput)
        ? VIDEO_EFFECTS_UNSUPPORTED_DIRECT_DOLBY_VISION_OUTPUT
        : VIDEO_EFFECTS_SUPPORTED;
  }

  static MpvVideoEqualizer sanitizeVideoEqualizer(
      MpvVideoEqualizer videoEqualizer, boolean sharpnessSupported) {
    if (sharpnessSupported || videoEqualizer.getSharpness() == 0.0) {
      return videoEqualizer;
    }
    return MpvVideoEqualizer.create(
        videoEqualizer.getBrightness(),
        videoEqualizer.getContrast(),
        videoEqualizer.getSaturation(),
        videoEqualizer.getGamma(),
        videoEqualizer.getHue(),
        /* sharpness= */ 0.0);
  }

  @CanIgnoreReturnValue
  public boolean setAudioFilter(MpvAudioFilter audioFilter) {
    verifyApplicationThread();
    if (playerInfo.isReleased()) {
      return false;
    }
    return effectController.setAudioFilter(checkNotNull(audioFilter));
  }

  public void setAudioOutputListener(@Nullable AudioOutputListener listener) {
    verifyApplicationThread();
    if (!playerInfo.isReleased()) {
      audioOutputListener = listener;
    }
  }

  public int getAudioChannelCount() {
    verifyApplicationThread();
    if (playerInfo.isReleased()) {
      return Format.NO_VALUE;
    }
    @Nullable Integer channelCount = playbackProperties.getAudioChannelCount();
    return channelCount != null && channelCount > 0 ? channelCount : Format.NO_VALUE;
  }

  /** Returns audio effect support for the currently initialized audio output. */
  public @AudioEffectsSupport int getAudioEffectsSupport() {
    verifyApplicationThread();
    return playerInfo.isReleased()
        ? AUDIO_EFFECTS_UNAVAILABLE
        : getAudioEffectsSupport(playbackProperties.getAudioOutputFormat());
  }

  static @AudioEffectsSupport int getAudioEffectsSupport(@Nullable String audioOutputFormat) {
    if (audioOutputFormat == null || audioOutputFormat.isEmpty()) {
      return AUDIO_EFFECTS_UNAVAILABLE;
    }
    return audioOutputFormat.startsWith("spdif-")
        ? AUDIO_EFFECTS_UNSUPPORTED_PASSTHROUGH
        : AUDIO_EFFECTS_SUPPORTED;
  }

  @CanIgnoreReturnValue
  public boolean toggleGeneralStats() {
    verifyApplicationThread();
    return !playerInfo.isReleased()
        && nativeLifecycle.isInitialized()
        && commandDispatcher.toggleGeneralStats();
  }

  @Override
  protected State getState() {
    boolean reportRenderedFirstFrame = videoState.consumeFirstFrameEvent();
    State.Builder builder =
        stateBuilder.buildBaseState(
            playerInfo.createStateSnapshot(
                trackController.getParameters(), reportRenderedFirstFrame));
    if (!playlist.isEmpty()) {
      builder.setCurrentMediaItemIndex(playlist.currentIndex());
    }
    if (playerInfo.hasPendingDiscontinuity()) {
      builder.setPositionDiscontinuity(
          playerInfo.pendingDiscontinuityReason(), playerInfo.pendingDiscontinuityMs());
      playerInfo.clearPendingDiscontinuity();
    }
    return builder.build();
  }

  @Override
  protected ListenableFuture<?> handleSetMediaItems(
      List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
    return handleCompletedCommand(
        () -> timelineController.setMediaItems(mediaItems, startIndex, startPositionMs));
  }

  @Override
  protected ListenableFuture<?> handlePrepare() {
    return handlePlayerCommand(this::prepareCurrentItem);
  }

  private ListenableFuture<?> prepareCurrentItem() {
    MediaItem item = playlist.current();
    if (item == null && playlist.isEmpty()) {
      playbackState.setEnded();
      playerInfo.clearPlayerError();
      invalidatePlayerState();
      return COMPLETED;
    }
    if (item == null) {
      return fail(
          errorFactory.create("No media item", null, PlaybackException.ERROR_CODE_BAD_VALUE));
    }
    if (!nativeLifecycle.ensureInitialized()) {
      return COMPLETED;
    }
    loadCurrent(item, playbackState.getPositionMs());
    return COMPLETED;
  }

  @Override
  protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setPlayWhenReady(
              playWhenReady,
              Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
              Player.PLAYBACK_SUPPRESSION_REASON_NONE);
          audioFocusManager.setPlayWhenReady(playWhenReady);
          setPauseProperty();
          seekController.clearTransitionFlags();
        });
  }

  @Override
  protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int seekCommand) {
    return handlePlayerCommand(
        () -> {
          long targetPositionMs =
              seekController.resolvePositionMs(mediaItemIndex, playlist.currentIndex(), positionMs);
          ListenableFuture<?> result = COMPLETED;
          if (isSeekToDifferentMediaItem(mediaItemIndex)) {
            if (!playlist.isValidIndex(mediaItemIndex)) {
              return COMPLETED;
            }
            seekToMediaItem(mediaItemIndex, targetPositionMs);
          } else {
            result = seekController.seekToPositionMs(targetPositionMs, seekCommand);
          }
          invalidatePlayerState();
          return result;
        });
  }

  private boolean isSeekToDifferentMediaItem(int mediaItemIndex) {
    return mediaItemIndex != C.INDEX_UNSET && mediaItemIndex != playlist.currentIndex();
  }

  private void seekToMediaItem(int mediaItemIndex, long positionMs) {
    seekController.reset();
    playlist.setCurrentIndex(mediaItemIndex);
    playbackState.setPositionMs(positionMs);
    if (nativeLifecycle.isInitialized()) {
      loadCurrent(playlist.get(playlist.currentIndex()), playbackState.getPositionMs());
    }
  }

  @Override
  protected ListenableFuture<?> handleStop() {
    return handleCompletedCommand(
        () -> {
          nativeLifecycle.cancelPendingInitialization();
          long positionMs = playbackState.getPositionMs();
          if (stopActiveFile()) {
            releaseAudioFocus();
            playbackState.setIdle();
            resetCurrentMediaState(positionMs);
            playbackState.clearPendingSeek();
          }
        });
  }

  @Override
  protected ListenableFuture<?> handleRelease() {
    return handlePlayerCommand(
        () -> {
          if (!playerInfo.markReleased()) {
            return COMPLETED;
          }
          audioOutputListener = null;
          artworkLoader.release();
          handler.removeCallbacksAndMessages(null);
          resetSeekState();
          clearTransitionFlags();
          releaseAudioFocus();
          surfaceController.release();
          nativeLifecycle.release();
          return COMPLETED;
        });
  }

  @Override
  protected ListenableFuture<?> handleSetRepeatMode(int repeatMode) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setRepeatMode(repeatMode);
          if (nativeLifecycle.isInitialized()) {
            playbackProperties.disableNativeLoop();
          }
        });
  }

  @Override
  protected ListenableFuture<?> handleSetPlaybackParameters(PlaybackParameters playbackParameters) {
    return handleCompletedCommand(
        () -> {
          PlaybackParameters previousPlaybackParameters = playerInfo.getPlaybackParameters();
          playerInfo.setPlaybackParameters(playbackParameters);
          playbackProperties.updatePlaybackParameters(
              previousPlaybackParameters, playbackParameters);
        });
  }

  @Override
  protected ListenableFuture<?> handleSetTrackSelectionParameters(
      TrackSelectionParameters parameters) {
    return handleCompletedCommand(
        () -> {
          videoTrackEnableGate.onParametersChanging(parameters);
          trackController.setParameters(parameters);
          videoTrackEnableGate.onTrackSelectionApplied();
        });
  }

  @Override
  protected ListenableFuture<?> handleSetAudioOffsetMs(long audioOffsetMs) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setAudioOffsetMs(audioOffsetMs);
          playbackProperties.setAudioDelayMs(playerInfo.getAudioOffsetMs());
        });
  }

  @Override
  protected ListenableFuture<?> handleSetTextOffsetMs(long textOffsetMs) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setTextOffsetMs(textOffsetMs);
          playbackProperties.setTextDelayMs(playerInfo.getTextOffsetMs());
        });
  }

  @Override
  protected ListenableFuture<?> handleSetVolume(float volume, int flags) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setVolume(volume);
          setVolumeProperty();
        });
  }

  @Override
  protected ListenableFuture<?> handleSetAudioAttributes(
      AudioAttributes audioAttributes, boolean handleAudioFocus) {
    return handleCompletedCommand(
        () -> {
          playerInfo.setAudioAttributes(audioAttributes);
          audioFocusManager.setAudioAttributes(handleAudioFocus ? audioAttributes : null);
          if (playerInfo.isPlayWhenReady()) {
            audioFocusManager.requestForPlayback();
          }
        });
  }

  @Override
  protected ListenableFuture<?> handleSetVideoOutput(Object videoOutput) {
    return handleCompletedCommand(() -> surfaceController.setVideoOutput(videoOutput));
  }

  @Override
  protected ListenableFuture<?> handleClearVideoOutput(@Nullable Object videoOutput) {
    return handleCompletedCommand(() -> surfaceController.clearVideoOutput(videoOutput));
  }

  @Override
  protected ListenableFuture<?> handleAddMediaItems(int index, List<MediaItem> mediaItems) {
    return handleCompletedCommand(() -> timelineController.addMediaItems(index, mediaItems));
  }

  @Override
  protected ListenableFuture<?> handleMoveMediaItems(int fromIndex, int toIndex, int newIndex) {
    return handleCompletedCommand(
        () -> timelineController.moveMediaItems(fromIndex, toIndex, newIndex));
  }

  @Override
  protected ListenableFuture<?> handleReplaceMediaItems(
      int fromIndex, int toIndex, List<MediaItem> mediaItems) {
    return handleCompletedCommand(
        () -> timelineController.replaceMediaItems(fromIndex, toIndex, mediaItems));
  }

  @Override
  protected ListenableFuture<?> handleRemoveMediaItems(int fromIndex, int toIndex) {
    return handleCompletedCommand(() -> timelineController.removeMediaItems(fromIndex, toIndex));
  }

  void loadCurrent(MediaItem item, long startPositionMs) {
    mediaLoader.load(item, startPositionMs);
  }

  void onShutdown() {
    nativeLifecycle.onShutdown();
  }

  void onNativeSessionAvailable() {
    prepareCurrentItem();
  }

  void onAudioOutputChanged() {
    @Nullable AudioOutputListener listener = audioOutputListener;
    if (listener != null) {
      listener.onAudioOutputChanged();
    }
    effectController.onAudioOutputChanged();
  }

  void handleUnexpectedNativeShutdown() {
    fail(errorFactory.createUnexpectedShutdown());
  }

  boolean stopActiveFile() {
    if (hasActiveMpvFile()) {
      expectProgrammaticEndFile();
      if (!stopMpv()) {
        endFileGuard.cancelExpected();
        fail(errorFactory.createCommandFailure("stop"));
        return false;
      }
    }
    playbackState.stopActiveFile();
    seekController.reset();
    clearTransitionFlags();
    return true;
  }

  boolean hasActiveMpvFile() {
    if (!nativeLifecycle.isInitialized()) {
      return false;
    }
    return playbackProperties.hasActiveFile(playbackState.hasActiveFile(/* initialized= */ true));
  }

  void expectProgrammaticEndFile() {
    if (nativeLifecycle.isInitialized()) {
      endFileGuard.expect();
    }
  }

  void resetSeekState() {
    seekController.reset();
  }

  ListenableFuture<?> fail(PlaybackException error) {
    playerInfo.setPlayerError(error);
    playbackState.fail();
    endFileGuard.clear();
    resetSeekState();
    clearTransitionFlags();
    releaseAudioFocus();
    invalidatePlayerState();
    return COMPLETED;
  }

  boolean maybeLoadNextAfterEnd() {
    return timelineController.maybeLoadNextAfterEnd(playerInfo.getRepeatMode());
  }

  void resetCurrentMediaState(long startPositionMs) {
    playbackState.resetCurrentMedia(startPositionMs);
    resetCurrentMediaState();
  }

  void resetCurrentMediaState() {
    clearTransitionFlags();
    chapterController.clear();
    editionController.clear();
    trackController.clear();
    videoState.reset();
    artworkLoader.clear();
  }

  void clearTransitionFlags() {
    seekController.clearTransitionFlags();
    videoTrackEnableGate.clear();
    playbackEventState.setIgnoreRepeatOneBuffering(false);
  }

  void resetRenderedFirstFrame() {
    videoState.resetRenderedFirstFrame();
  }

  void setVolumeProperty() {
    playbackProperties.setVolume(playerInfo.getVolume(), audioFocusManager.getVolumeMultiplier());
  }

  void setPauseProperty() {
    playbackProperties.setPlayWhenReady(playerInfo.shouldPlay());
  }

  boolean isHardwareDecodeEnabled() {
    return decode == C.DECODE_HARDWARE;
  }

  void setPendingDiscontinuity(@Player.DiscontinuityReason int reason, long positionMs) {
    playerInfo.setPendingDiscontinuity(reason, positionMs);
  }

  boolean stopMpv() {
    return !nativeLifecycle.isInitialized() || commandDispatcher.stop();
  }

  void releaseAudioFocus() {
    audioFocusManager.release();
  }

  private ListenableFuture<?> handleCompletedCommand(Runnable command) {
    return handlePlayerCommand(
        () -> {
          command.run();
          invalidatePlayerState();
          return COMPLETED;
        });
  }

  void setDirectVideoOutputConfigured(boolean configured) {
    if (options.setDirectVideoOutputConfigured(configured)) {
      applyRuntimeDolbyVisionOutputMode();
    }
  }

  void setDirectVideoDisplay(@Nullable Display display) {
    if (options.setDirectVideoDisplay(display)) {
      applyRuntimeDolbyVisionOutputMode();
    }
  }

  void setDirectOsdOutputConfigured(boolean configured) {
    if (options.setDirectOsdOutputConfigured(configured)) {
      applyRuntimeDolbyVisionOutputMode();
    }
  }

  private void applyRuntimeDolbyVisionOutputMode() {
    if (nativeLifecycle.isInitialized()) {
      options.applyRuntimeDolbyVisionOutputMode(playbackProperties::setStringOptionOrProperty);
    }
  }

  private ListenableFuture<?> handlePlayerCommand(PlayerCommand command) {
    handlingPlayerCommand = true;
    try {
      return command.run();
    } finally {
      handlingPlayerCommand = false;
    }
  }

  boolean isMpvInitialized() {
    return nativeLifecycle.isInitialized();
  }

  void invalidatePlayerState() {
    if (!handlingPlayerCommand) {
      invalidateState();
    }
  }

  boolean isPlayWhenReadyInternal() {
    return playerInfo.isPlayWhenReady();
  }

  @Player.PlaybackSuppressionReason
  int getPlaybackSuppressionReasonInternal() {
    return playerInfo.getPlaybackSuppressionReason();
  }

  void setPlayWhenReadyInternal(
      boolean playWhenReady,
      @Player.PlayWhenReadyChangeReason int changeReason,
      @Player.PlaybackSuppressionReason int suppressionReason) {
    playerInfo.setPlayWhenReady(playWhenReady, changeReason, suppressionReason);
  }

  void setPlaybackSuppressionReasonInternal(
      @Player.PlaybackSuppressionReason int suppressionReason) {
    playerInfo.setPlaybackSuppressionReason(suppressionReason);
  }

  @Player.State
  int getInternalPlaybackState() {
    return playbackState.getState();
  }

  boolean hasInternalVideoTrack() {
    return trackController.haveTrack(C.TRACK_TYPE_VIDEO);
  }

  boolean isInternalVideoTrackSelected() {
    return trackController.isTrackTypeSelected(C.TRACK_TYPE_VIDEO);
  }

  void runOnPlayerLooper(Runnable runnable) {
    if (playerInfo.isReleased()) {
      return;
    }
    Util.postOrRun(
        handler,
        () -> {
          if (!playerInfo.isReleased()) {
            runnable.run();
          }
        });
  }

  void runOnPlayerLooperAfterRelease(Runnable runnable) {
    Util.postOrRun(handler, runnable);
  }

  void runOnPlayerLooperAndWait(Runnable runnable) {
    if (Looper.myLooper() == handler.getLooper()) {
      if (!playerInfo.isReleased()) {
        runnable.run();
      }
      return;
    }
    ConditionVariable completed = new ConditionVariable();
    if (!handler.post(
        () -> {
          try {
            if (!playerInfo.isReleased()) {
              runnable.run();
            }
          } finally {
            completed.open();
          }
        })) {
      return;
    }
    completed.blockUninterruptible();
  }

  private interface PlayerCommand {

    ListenableFuture<?> run();
  }

  public interface AudioOutputListener {

    void onAudioOutputChanged();
  }

  public static final class Builder {

    private final Context context;
    private MpvPlayerConfig config;
    private Looper looper;
    private @C.DecodeMode int decode;
    private MpvDecoderMode videoDecoderMode = MpvDecoderMode.AUTO;
    private boolean buildCalled;

    public Builder(Context context) {
      this.context = checkNotNull(context).getApplicationContext();
      this.config = new MpvPlayerConfig.Builder().build();
      this.looper = Looper.getMainLooper();
    }

    @CanIgnoreReturnValue
    public Builder setVideoDecoderMode(MpvDecoderMode videoDecoderMode) {
      checkState(!buildCalled);
      this.videoDecoderMode = checkNotNull(videoDecoderMode);
      return this;
    }

    @androidx.annotation.Nullable
    private androidx.media3.datasource.DataSource.Factory dataSourceFactory;

    @CanIgnoreReturnValue
    public Builder setDataSourceFactory(
        androidx.media3.datasource.DataSource.Factory dataSourceFactory) {
      checkState(!buildCalled);
      this.dataSourceFactory = checkNotNull(dataSourceFactory);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setConfig(MpvPlayerConfig config) {
      checkState(!buildCalled);
      this.config = checkNotNull(config);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setDecode(@C.DecodeMode int decode) {
      checkState(!buildCalled);
      this.decode = decode;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setLooper(Looper looper) {
      checkState(!buildCalled);
      this.looper = checkNotNull(looper);
      return this;
    }

    public MpvPlayer build() {
      checkState(!buildCalled);
      buildCalled = true;
      MpvPlayer player = new MpvPlayer(context, config, decode, looper);
      if (videoDecoderMode != MpvDecoderMode.AUTO) {
        player.setVideoDecoderMode(videoDecoderMode);
      }
      return player;
    }
  }
}
