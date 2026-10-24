package androidx.media3.common.audio;

import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.UnstableApi;

/**
 * Audio processor carrying the user's {@link AudioEffectParameters} through the audio sink chain.
 *
 * <p>The processor itself is transparent: it forwards PCM untouched. Downstream consumers (for
 * example an equalizer attached to the audio session) query {@link #getParameters()} to apply the
 * actual processing, which keeps the parameters in sync with the audio session lifecycle.
 */
@UnstableApi
public final class AudioEffectProcessor extends BaseAudioProcessor {

  @Nullable private AudioEffectParameters parameters;
  private boolean softwareEqualizer;

  /** Returns the parameters currently in effect, or {@code null} when none were set. */
  @Nullable
  public AudioEffectParameters getParameters() {
    return parameters;
  }

  /** Returns whether the equalizer must run in software for the current configuration. */
  public boolean isSoftwareEqualizer() {
    return softwareEqualizer;
  }

  /** Stores the parameters to expose to the audio chain. */
  public void setParameters(AudioEffectParameters parameters, boolean softwareEqualizer) {
    this.parameters = parameters;
    this.softwareEqualizer = softwareEqualizer;
  }

  /** Clears any previously stored parameters. */
  public void resetParameters() {
    this.parameters = null;
    this.softwareEqualizer = false;
  }

  @Override
  protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat)
      throws UnhandledAudioFormatException {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw new UnhandledAudioFormatException(inputAudioFormat);
    }
    return inputAudioFormat;
  }

  @Override
  public void queueInput(ByteBuffer inputBuffer) {
    int remaining = inputBuffer.remaining();
    if (remaining == 0) return;
    ByteBuffer buffer = replaceOutputBuffer(remaining);
    buffer.put(inputBuffer);
    buffer.flip();
  }
}
