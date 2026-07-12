# Handover — 2026-07-12

Written at the end of a working session, for whoever (or whatever) picks this up next.
Read `REVIEW.md` first: it is the work list. This file is the context around it.

---

## Where things stand right now

- Branch `develop`, version `0.0.138-SNAPSHOT`. **Everything is committed and pushed** except the
  items below.
- Build is green: `mvn clean install`. ~120 tests pass, CI runs them on every push.

**Uncommitted when this was written:**

- `REVIEW.md` — new, the output of the final review. Commit it.
- `CODE_REVIEW.md` and `DOCUMENTATION_REVIEW.md` — staged for deletion. Both are obsolete and
  actively misleading (see below). The deletion is correct.
- ⚠️ `git status` showed `CODE_REVIEW.md` as **both** staged-deleted **and** untracked at the same
  time. That is odd — check it is really gone before committing, and that you have not accidentally
  resurrected it.

---

## Start here tomorrow

**Fix the NaN bug.** It is the only thing found in two days of review that can silently change your
published numbers, and it is invisible to the golden-master gate.

`Math.round(double)` returns a `long`, and its contract states that NaN maps to **0**. So:

```java
// Miscellaneous.java:113
return Math.round(value * scale) / scale;     // round(NaN, 5) == 0.0, not NaN
```

NaN reaches it because the guard that should stop it has a hole — `NaN <= 0` is `false`:

```java
// SharedSquareUtils.java:337
if (area <= 0 || time <= 0 || concentration <= 0) {   // a NaN concentration sails through
```

A blank Concentration cell in `Experiment Info.csv` becomes NaN (Tablesaw's missing-value
sentinel), flows into `calculateDensity`, and is rounded to **0.0**. Every square and every
recording of that experiment gets `Density = 0.0`. No error, no NaN — a plausible, wrong number.

**And the regression gate cannot see it.** `paint-compare` has its own copy of `round()`
(`RegressionRules.java:275`) whose NaN short-circuit runs **only in relaxed mode**. In strict mode
old-NaN and new-0.0 both round to 0.0 and compare equal. `Density`, `Tau`, `R Squared`,
`Density Ratio` and `Variability` are all in the strict set.

The fix is small — `if (!Double.isFinite(value)) return value;` in *both* `round()` methods, and a
NaN-aware density guard — but **write the test first**, because the safety net you would normally
trust is blind here.

After that, `REVIEW.md` has the rest in priority order. The next two (the uncaught-exception
handler that discards every crash, and Generate Squares reporting success after an experiment
failed) are not bugs in themselves — they are what will make the next bug take a day instead of an
hour.

---

## How to work with me (constraints that are not negotiable)

My sandbox is not your machine. Concretely:

- **I cannot run `mvn`.** You build. I will hand you the command.
- **I cannot run `git` write commands** — no commit, no push, no `checkout`. I can read
  (`git log`, `git diff`, `git show`). You run everything that writes.
- **I cannot delete or move files.** `rm` and `git checkout --` fail with "Operation not permitted"
  on the mounted filesystem. If a file must go, I will tell you and you delete it.
- **Commit messages must be heredocs.** Your zsh chokes on em-dashes and quotes in `-m`. Always:

  ```bash
  git commit -F - <<'EOF'
  Subject line
  
  Body.
  EOF
  ```

- **Do not push unless asked.** Commit locally; pushing is your decision.
- **Never edit files in place with `perl -i` or `sed -i` on this mount.** It creates orphaned
  `.fuse_hidden*` files, which once got swept into a commit by `git add -A`. Rewrite files by
  truncating in place (Python `open(path,'w')`) instead. `.fuse_hidden*` is in `.gitignore` now,
  but the right answer is not to create them.

---

## Lessons from this session, worth keeping

**Verify before asserting.** My first documentation audit produced ~20 findings, of which **four
were false positives** — including one where I nearly "fixed" deliberate, correct behaviour
(the exclude file's reset-and-rebuild) as if it were data loss. Treat any review list, mine
included, as leads to be checked, not facts.

**A mechanical edit needs a mechanical invariant.** When stripping 129 file banners I mangled the
macOS installer by anchoring on the wrong `/**`. What saved it was adding a check that *every line
of code must survive byte-identical* — comments may change, code may not. That check should have
been there from the first run. If you do another sweeping edit, define the invariant first.

**The tests are the contract.** `paint-viewer` had zero tests this morning and now has 34. They
exist mostly to write down rules that lived only in someone's head — that `Recording Exclude.csv`
is authoritative and the Exclude column is rebuilt from it, that a cell ID of 0 means "remove" not
"cell zero", that `-A1-1` must never match `-A1-10`. See `TESTING.md`.

---

## Environment gotchas that cost real time today

- **macOS blocks external drives.** The Viewer could not read `.nd2` files from an external SSD.
  Not a bug — macOS gates removable volumes per application, and the permission belongs to whatever
  **launches** the JVM (IntelliJ, or the packaged app), not to `java`. It only takes effect after a
  full restart. Documented in `TROUBLESHOOTING.md`.
- **JDK 8 silently disables the JIT.** "CodeCache is full" while most of the cache is free. The app
  then runs *interpreted* — several times slower, with no error anywhere. The launcher scripts now
  pass `-XX:ReservedCodeCacheSize=512m -XX:-UseCodeCacheFlushing`; add the same to your IntelliJ run
  configuration. Note the parent pom had carried these settings in `app.jvm.args` for ages, wired to
  nothing.

---

## Documentation state

- `REVIEW.md` — the current work list. **Trust this one.**
- `TESTING.md` — what the ~120 tests check, in plain language, and what they deliberately do not.
- `TROUBLESHOOTING.md` — the external-drive permission, the code-cache stall, and where the log is.
- `CODE_REVIEW.md`, `DOCUMENTATION_REVIEW.md` — **being deleted.** `CODE_REVIEW.md` in particular
  still claims the project has "essentially zero automated tests" and that a search for `@Test`
  "returns nothing". There are 119. Anyone reading it — including a fresh AI session — would draw
  exactly the wrong conclusion about where the project stands.
- `REFACTORING_PROGRESS.md` — needs an update: four "remaining" items are done, and its test count
  contradicts the README.
- `CHANGELOG.md` — stale at v0.0.134; the pom says 0.0.138-SNAPSHOT.
- `README.md` — still says the pipeline is "a collection of Python and R scripts". There are no
  Python files. Copied from the old repo.
