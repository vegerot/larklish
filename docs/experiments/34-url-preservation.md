# Experiment 34 — Preserve URLs during ML Kit translation

Date: 2026-09-28. Run on the Pixel's installed ML Kit Translator with a temporary Android
instrumentation probe. Only the test APK was installed; the app APK, Backend, settings,
user-token file and notification records were not replaced. No messages were sent.

## Requirement and bound

The early Relay remains required (Experiment 33). A URL must retain its exact characters
so it still identifies the intended resource. Test one observed public URL failure plus
four fictional inputs: URL before prose, after prose, in the middle with a case-sensitive
path/query value, and two URLs separated by Chinese conjunctions.

Used the production MlKitTranslator and englishOf functions. Compared whole-string
translation with two small alternatives. Probe source is retained privately under
`replay-corpus/url-preservation-20260928/UrlTranslationExperiment.kt` for reproduction.
The experiment matched HTTP(S) runs with `https?://[^\s\p{IsHan}<>]+`.
This is a bounded probe, not a general URL parser or Unicode-URL implementation.

## Delete URL text from the translation work

First alternative: translate the prose segments separately, concatenate unchanged URLs.
All five URLs survived, but translated segments lost whitespace and sentence context.
In the two-URL case, the word “和” (and) became “Sum.” Reject this implementation.

Second alternative: replace URLs with URL0, URL1, etc.; translate the sentence once;
restore the original URLs by exact placeholder replacement. All placeholders survived
in these five inputs. Both probe runs completed successfully on the real phone.

| Input shape | Whole-string result | Placeholder result |
| --- | --- | --- |
| Observed Chinese life-guide title immediately followed by public GitHub URL | Changed account/path text and broke scheme spacing | URL restored byte-for-byte; title still mistranslated as “High Domestic Value Life Guide” |
| Chinese request around example.com/CaseSensitive?token=AbC123 | Changed path and lowercased query value | Exact URL, readable request |
| example.com/BuildStatus before build-failure prose | Lowercased path | Exact URL, failure/retry meaning retained |
| Release notice before example.com/ReleaseNotes | Broke scheme spacing and lowercased path | Exact URL, release notice retained |
| Compare example.com/Before and example.com/After | Changed path case | Exact URLs, “Please compare the results of URL0 and URL1” before restoration |

The fictional query value is test data, not a credential.

## Simplification decision

Numbered placeholders preserve sentence context with one translation call and succeed
on this sample. Segmentation is rejected. No production Translator change is made here.
The observed title mistranslation remains: protecting URLs does not solve translation
quality generally, and the experiment did not test arbitrary code identifiers.

Before shipping a wrapper, specify behavior when input already contains a marker, when
a translated marker changes/disappears, and for punctuation next to a URL. These are
contract questions for the small wrapper, not reasons to build a generic masking system.
Test a few explicit cases rather than assume ML Kit always preserves placeholders.

## Acceleration and automation

No dashboard, deployment automation, or permanent comparison runner was added. The
existing instrumentation path was sufficient. The experiment initially exposed a stale
NetworkStateTest call to Recorder.updated; updated its arguments to the current source,
message-type and failure-list contract so instrumentation builds again. The temporary
probe was removed after recording the result; the corrected existing test remains.

Final verification: Android unit tests and instrumentation APK build passed; the corrected
NetworkStateTest passed on the Pixel. Reinstalled the test APK without the temporary probe.
Local links in the revised current guidance resolve.
