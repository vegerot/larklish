# Move Lark translation to the Backend

## Summary

The phone will use on-device ML Kit to post the first Relay. It will call the Backend unless the message has no Han text and its Preview is complete. For other complete Previews, the Backend translates the Preview without a Lookup. For truncated Previews, it performs the existing Lookup and translates the Full text.

## Implementation

- Skip the Backend when the Preview is complete and its message contains no Han text, even if its title or Sender does; keep the initial Relay and record why no Update was needed. A truncated Preview still goes to the Backend even when its visible message contains no Han text.
- Keep the authenticated `POST /lookup` route, including its public PPE path. The Backend branches on the Preview’s existing truncation rule. A complete Preview that needs translation requires only its title, text, and flow ID; a truncated Preview also requires the notification time and user token. The phone obtains a user token only for the truncated branch.
- Translate the message first. If Lark cannot translate it after the existing Backend retry, return a distinct `translation-failed` result and leave the initial Relay in place. Record whether the Full-text Lookup succeeded, the failure category or Lark code, and the flow ID—without message text or credentials in timing records.
- When message translation succeeds, translate a Han title and, when applicable, a mixed-language Han Sender. Keep the phone’s existing romanization for bare Chinese personal names. If title or Sender translation fails, Update with the successful message and retain that field’s initial on-device wording. Return the fields, Preview-versus-Full-text source, failure categories, and existing timing spans in the Backend response.
- Remove the phone’s Lark translation implementation and its Lark-then-ML-Kit fallback chain. Use ML Kit for the initial Relay and the translator screen. Keep phone Lark HTTP code needed to refresh its user token; it will no longer call the **translation** endpoint. Update the event helper to count Preview Updates, Full-text Updates, Lookup misses, and translation failures separately. Update the project glossary, plan, and progress records to describe the new flow.

## Tests and PPE rollout

- Test both Backend branches: a complete Preview with a Han message makes no chat or message API calls; a truncated Preview still finds and translates Full text. A complete Preview with a message containing no Han text makes no Backend call, including when its title or Sender contains Han; a truncated Preview still calls the Backend when its visible message contains no Han. Cover Latin pass-through, bare-name romanization, mixed Han Sender, partial title/Sender failure, message translation failure after a successful Lookup, authentication, and timing/failure records.
- Run Go tests, Android unit tests and build, Kotlin formatting check, and helper tests. Review the change without incorporating the unrelated Obsidian files.
- Commit the tested change, build the exact SCM artifact from ByteDance `origin/main`, and use the recently verified **direct ByteFaaS release** to deploy only `mmyp0srw` in `ppe_deploy_i18n_1`. Verify the revision and both branches through the public PPE route before installing the APK in place on the Pixel. Preserve its token file and Recorder history.
- Verify a complete and a truncated Original on the phone, including Relay-to-Update timing and failure counts. Use naturally arriving Originals; if the test group still produces none, report Backend and phone-path checks separately and obtain authorization for a new marked test message. Soak the failure categories before deciding whether to add an ML Kit Full-text Update.

## Assumptions

- Backend message translation failure leaves the initial Relay intact. There is no phone-side Full-text fallback in this version.
- The rollout stops at PPE and the Pixel. Singapore production and Railway are outside this release.
