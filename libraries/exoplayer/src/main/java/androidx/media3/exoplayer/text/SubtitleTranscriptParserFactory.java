package androidx.media3.exoplayer.text;

import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;

/**
 * Subtitle parser factory that keeps the platform parsing behavior while staying associated with
 * the transcript of the current media item.
 */
@UnstableApi
public final class SubtitleTranscriptParserFactory implements SubtitleParser.Factory {

  private final SubtitleTranscript transcript;
  private final SubtitleParser.Factory delegate;

  public SubtitleTranscriptParserFactory(SubtitleTranscript transcript) {
    this.transcript = transcript;
    this.delegate = new DefaultSubtitleParserFactory();
  }

  /** Returns the transcript this factory is bound to. */
  public SubtitleTranscript getTranscript() {
    return transcript;
  }

  @Override
  public boolean supportsFormat(Format format) {
    return delegate.supportsFormat(format);
  }

  @Override
  public @Format.CueReplacementBehavior int getCueReplacementBehavior(Format format) {
    return delegate.getCueReplacementBehavior(format);
  }

  @Override
  public SubtitleParser create(Format format) {
    return delegate.create(format);
  }
}
