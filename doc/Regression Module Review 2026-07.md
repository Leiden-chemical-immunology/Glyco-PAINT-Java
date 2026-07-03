# paint-regression — Review: distinguishing real differences from ordering

*2026-07-03 · Focus: how the module matches rows, why track comparisons produce ordering false-positives, and how to make it as strong as it can be.*

## The short version

The module contains **two comparators solving two different problems**, and the ordering pain comes from applying the wrong matching model to track data:

- **`CsvComparatorRegression`** — key-based 1:1 comparison (Java gold-standard vs new). Correct for Squares/Recordings, **wrong for Tracks** because its key isn't unique per track, so within a square it falls back to *positional* comparison.
- **`TracksCsvComparatorPythonJava`** — a genuinely sophisticated fuzzy matcher (Python vs Java), but it's a diagnostic report generator, not a pass/fail gate, and its matching is greedy with brittle exact-integer gates.

The single highest-value fix is small: **key track comparisons on the `Unique Key` column that already exists in `Tracks.csv`.** That removes ordering as a source of difference entirely for the regression use case.

---

## 1. Why ordering bites on tracks (root cause)

`CsvComparatorRegression.buildKey` (line 280) builds the row key from **Recording Name + Square Number** only:

```java
return sq.isEmpty() ? rec : rec + " - " + sq;
```

`toMultiMap` then groups rows into buckets by that key, and `compareFiles` walks each bucket **positionally**:

```java
Map<String,String> o = (i < ol.size()) ? ol.get(i) : null;   // line 104
Map<String,String> n = (i < nl.size()) ? nl.get(i) : null;   // line 105
```

For **Squares.csv / Recordings.csv** this is fine — `(Recording, Square)` identifies exactly one row, so each bucket has size 1 and ordering is irrelevant by construction. That's why the squares regression works well.

For **Tracks.csv** it breaks. There are ~44,000 tracks and many tracks per `(Recording, Square)`, so each bucket holds dozens of tracks compared **in whatever order they appear in the file**. If the two files contain the *same* tracks in a *different order*, `ol.get(i)` and `nl.get(i)` line up different tracks, and essentially every numeric field reports a difference. That is exactly the "is this a real difference or just ordering?" noise — it is an artifact of positional comparison, not real divergence.

## 2. The decisive fix: key tracks on `Unique Key`

`Tracks.csv` already carries a unique per-track identity in column 1:

```
Unique Key            = 221012-Exp-1-A1-1-0   (Recording Name + "-" + Track Id)
```

If track comparison keys/sorts on `Unique Key` instead of `Recording + Square`, each bucket becomes size 1 and the comparison is order-independent and strictly 1:1 — identical to how squares already work. After that, **any remaining difference is a real difference** (subject to tolerance), which is precisely the separation you want.

**Important scope caveat.** `Unique Key`/`Track Id` is stable *across runs of the same code on a fixed `Tracks.csv` input* — i.e. the deterministic regression scenario (freeze `Tracks.csv`, re-run generate-squares, compare). It is **not** stable across separate TrackMate runs, because TrackMate renumbers tracks nondeterministically. So:

- **Regression gate (fixed input, same code):** key on `Unique Key`. Clean, exact, order-independent. This is your goal.
- **Cross-run / Python-vs-Java (no stable IDs):** you genuinely need fuzzy matching — that's what `TracksCsvComparatorPythonJava` is for (see §4).

## 3. "STRICT" mode isn't actually strict — leniency rules hide real diffs

For same-code regression you want to catch *any* change. But several rules in `RegressionRules` mask real differences. They're reasonable for cross-provenance (Python↔Java) comparison, but they leak into strict mode and will hide genuine regressions:

- **`numericMissingSkipDifference` (line 328).** If a value is present on one side and missing on the other, it is reported as *no difference*. That hides a value appearing or disappearing — a real regression (e.g. a diffusion coefficient going null).
- **`emptyAndZeroEquiv` (line 213).** Treats empty as equal to `0`, `-1`, `-2`, `-3`. Given the `-1` missing-value sentinel bug elsewhere in the codebase, this masks exactly the change you'd expect to see when you *fix* that bug — so the test would stay green through a real behavioral change.
- **`correctedValueIfTrackDependent` (line 294).** Rescales `Density`/`Density Ratio Ori` by the old/new track-count ratio, i.e. it deliberately *corrects away* a difference caused by different track counts. Appropriate when comparing two different pipelines; wrong for a strict regression where a changed track count *is* the regression.

Recommendation: a truly strict mode should disable all three masks. Keep them only in the relaxed/cross-provenance profile. Right now `STRICT` still applies `emptyAndZeroEquiv` and `numericMissingSkipDifference` unconditionally.

## 4. Fuzzy matcher (`TracksCsvComparatorPythonJava`) — make it robust for cross-run use

The two-phase matcher is well thought out (strict pass, then RMS-scored diagnostic pass, then a tolerance sweep). Its weaknesses, in priority order:

1. **Greedy assignment (lines 210-234).** Old tracks are processed in sorted order and each greedily claims the first passing candidate (`usedNewIds`). Greedy bipartite matching is not optimal — an early track can "steal" the only candidate a later track needed, producing false *unmatched* rows that look like real differences. Replace with optimal assignment (Hungarian) or global best-score-first pairing.
2. **Ambiguity dumped to "unmatched" (lines 222-233).** When more than one candidate passes, the old track is pushed to `multipleMatches` *and* `unmatched` rather than taking the lowest-score candidate. With loose tolerances this discards legitimate matches. Pick the best-scoring candidate, then enforce one-to-one.
3. **Exact integer gate is too brittle (lines 500-506).** A candidate must match `Square Nr`, `Nr Spots`, `Nr Gaps`, `Longest Gap` *exactly*. But a single spot flipping in/out between TrackMate runs — the dominant nondeterminism you described — changes `Nr Spots` by 1 and kills the match, so the *same physical track* is reported as disappeared/appeared. Keep `Square Nr` exact (it's a spatial bucket), but allow `Nr Spots`/`Nr Gaps` to differ by ±1 or fold them into the score with a weight.
4. **Anchor on spatial position.** The most run-invariant property of a track is where it is (`Track X/Y Location`) and its frame span/duration. These should dominate the match score; derived quantities (speed, confinement) are noisier. Currently all eight fields are weighted equally in the RMS (line 582).
5. **Recording mapping by `startsWith` (line 374-376).** `findClosestRecording` matches on prefix, which can mis-map recordings whose names share a prefix. Prefer exact match after threshold-suffix stripping; fall back to prefix only when unambiguous.

## 5. Structural issues (both comparators)

- **~1900 LOC of duplicated, hand-rolled CSV parsing.** Both classes parse with `BufferedReader` + `split(",")` (`readCsv`, line 302 / 322), which the earlier code review already flagged: it bypasses the project's Tablesaw-based `BaseTableIO` and will corrupt on any quoted field containing a comma. Unify onto one parser.
- **Diagnostic-only, no assertions.** Both are `main()` programs that print and count. There is no pass/fail contract — "how many diffs is acceptable" is re-judged by eye each run.
- **Hardcoded `/Users/hans/…` paths** (`CsvComparatorRegression` main lines 373-408; tracks comparator lines 115-116) make the whole module unrunnable by anyone else or in CI.

## 6. Proposed target design

Collapse both comparators onto **one `TableComparer`** with three pluggable strategies:

- **Key strategy** — `Unique Key` for tracks; `Recording + Square Number` for squares; configurable per file type.
- **Match strategy** — *keyed-exact* (deterministic regression: align by key, compare 1:1, ordering impossible) vs *optimal-fuzzy* (cross-run/Python: position-anchored, optimal assignment, ±1 integer slack).
- **Tolerance profile** — *strict* (no masking; tight rounding/relative tolerance) vs *relaxed* (the current masks, for cross-provenance).

Return a structured `ComparisonResult` bucketing every row into **exact-match / within-tolerance / real-difference / missing / extra**, and expose it to JUnit so tests assert against an explicit budget (e.g. "0 real differences for deterministic squares"; "≤ N unmatched for a cross-run track comparison"). This is what turns the module from a diagnostic aid into a regression *gate*, and the category buckets are exactly the "ordering vs real difference" distinction made explicit and countable.

## 7. Suggested order of work

1. **Key tracks on `Unique Key`** in the keyed comparator (small change, removes ordering false-positives immediately). Verify against `reference-case/Tracks.csv` compared to itself → expect zero differences.
2. **Split strict vs relaxed masking** so strict regression stops hiding missing/sentinel/track-count changes.
3. **Wrap as JUnit tests** with resource-based inputs and an explicit diff budget; delete the hardcoded paths.
4. **Harden the fuzzy matcher** (optimal assignment, ±1 integer slack, position anchoring) for the cross-run/Python case.
5. **Unify onto one `TableComparer`** over `BaseTableIO`, retiring the duplicated hand-rolled parsing.
