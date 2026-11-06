# Change Note: Fit Tau on the survival curve instead of the histogram

Status: **proposal, for discussion; nothing has been built.**
Date: 2026-11-04
Related: `Change Note - Exclude Edge Tracks.md`

> **Update 2026-11-06.** An analysis of real track durations is in
> `Analysis - Track Duration Distribution and Tau 2026-11.md`. Main points: the durations are
> not a single exponential (about 73% short, about 27% long tracks); the current histogram fit
> effectively gives the Tau of the fast population; a single exponential on the survival curve
> would give an average of the two populations and is not recommended. The real question is one
> versus two exponentials. Section 4.3 below overstates the R² problem: on real data the survival
> fit gave R² 0.85–0.89, so R² is inflated but still responds to a wrong model.

## 1. Background

Unbinding is a memoryless process: a bound molecule has a constant probability per unit time
of letting go (rate k_off). The number still bound then follows N(t) = N₀·e^(−t/τ), with
τ = 1/k_off the mean residence time. So track durations are exponentially distributed, and
Tau is obtained by fitting an exponential.

The same exponential can be fitted in two forms:

- **Histogram (density):** p(t) = (1/τ)·e^(−t/τ), the fraction of tracks that end around t.
- **Survival curve:** S(t) = e^(−t/τ), the fraction of tracks that last longer than t.
  It is built by taking, for every t, the number of tracks longer than t divided by the total.

They are related by p(t) = −dS/dt and have the same τ. The suggestion is that fitting the
survival curve, or using a maximum-likelihood estimate, is more robust than fitting the
histogram.

## 2. What the code does now

`paint-shared-utils/.../utils/CalculateTau.java`, called from
`CalculateSquareAttributes` for every square (line ~154) and for every recording (line ~239):

1. Builds a frequency table over every distinct `Track Duration` value
   (`createFrequencyDistribution`). Durations are whole frames (multiples of 0.05 s), so in
   practice this is a histogram with one bin per frame.
2. Fits `y = m·e^(−t·x) + b` with Levenberg–Marquardt least squares, all points weighted
   equally (`CalculateTauExpDecayFitter`).
3. Tau = 1000 / t (ms). R² is computed on the histogram points.
4. Status `TAU_SUCCESS` only if R² ≥ `Min Required R Squared`, otherwise
   `TAU_RSQUARED_TOO_LOW` or `TAU_NO_FIT`.

R² is used as a selection criterion: a square is only selected if its R² is not NaN and
is ≥ `Min Required R Squared` (`SharedSquareUtils`, around line 166). The same threshold is
applied again in the Viewer (`ViewerFrame`, and recording overrides via
`ImportRecordingOverride` / `ExportOverridesFromViewer`). The fit plots (`PlotUtils`, enabled
by `Tau Fitting Plots`) show the histogram with "Tau = … ms, R² = …".

## 3. Weaknesses of the current approach

1. **Noisy tail, weighted equally.** The tail bins hold 0, 1 or 2 tracks. The unweighted fit
   gives these noisy points the same weight as the well-filled bins at short durations. In
   addition, bins with zero tracks do not appear in the frequency table at all, so the tail
   is under-represented in a way that depends on the number of tracks.
2. **The `+ b` offset.** It absorbs a noise floor in the histogram, but has no physical
   meaning in the exponential model, and it interacts with τ during fitting.
3. **R² on a histogram** mainly measures how noisy the bins are, which depends strongly on the
   number of tracks, not only on how well a single exponential describes the data.

## 4. Points that apply to any new method

### 4.1 Minimum track length

The Fiji plugin keeps only tracks with at least `Min Number of Spots in Track` spots
(default 3, `TrackMateConfig`). `Track Duration` (TrackMate's `TRACK_DURATION`) is the time
between the first and last spot, so durations start at t_min = (minimum spots − 1) × 0.05 s,
not at 0.

Because unbinding is memoryless, the fix is simple: model the time beyond t_min,
S(t) = e^(−(t − t_min)/τ) for t ≥ t_min. τ is unchanged by this shift. Ignoring t_min biases
Tau.

### 4.2 Discrete frames

Durations come in steps of Δt = 0.05 s. For Tau values that are only a few frames long this
matters, and the estimate needs a small correction for whole frames (a geometric instead of
continuous exponential distribution).

### 4.3 R² loses its meaning on a survival curve

Each point of a survival curve contains all the tracks after it, so neighbouring points are
strongly correlated. A least-squares fit to them almost always gives R² close to 1. The
current selection rule `R² ≥ Min Required R Squared` would then accept almost every square.
**Whatever method is chosen, the quality criterion has to be replaced, not just
re-thresholded.**

### 4.4 Interaction with edge tracks

Long tracks are more likely to touch the first or last frame, so excluding edge tracks
removes slightly more long tracks than short ones. With Tau in the hundreds of milliseconds
and a 100 s recording this bias is negligible, but it should be stated in the methods.

## 5. Candidate methods

### M1: least-squares fit on the survival curve

- Build S(t) from the durations ≥ t_min. Fit S(t) = e^(−(t − t_min)/τ). There is no free
  amplitude, because S(t_min) = 1 by construction, and no offset.
- \+ Same kind of fit as now, easy to plot and to explain.
- − R² is no longer usable (4.3). The fit still treats correlated points as independent, so
  the uncertainty on τ is underestimated.

### M2: maximum likelihood (MLE)

- For a single exponential above t_min: τ = mean(duration − t_min), with the correction for
  whole frames (4.2). No iterative fit, nothing that can fail to converge.
- \+ The statistically best estimator for this model, with no binning, and simple, short
  code. A confidence interval follows directly from the number of tracks
  (relative standard error ≈ 1/√n).
- − No R². Quality has to be judged differently (section 6).
- − The plot is the survival curve with the fitted line through it, rather than a fit result
  on the plot.

### M3: keep the histogram, improve the fit

- Include empty bins, drop the `+ b` offset, and weight the points by their Poisson error.
- \+ Smallest change, R² remains meaningful.
- − The tail problem remains in reduced form. This doesn't address what the note is about.

## 6. Replacing the quality criterion (needed for M1 and M2)

Possible measures:

- **Relative uncertainty of Tau** (for MLE ≈ 1/√n): select squares with, for example,
  CI width / Tau below a threshold. This is effectively a minimum number of tracks, which
  `Min Tracks to Calculate Tau` already partly does.
- **Goodness-of-fit test against a single exponential**, for example a Kolmogorov–Smirnov
  statistic on the survival curve. This tests what R² is meant to test: does one exponential
  describe these tracks?
- **Comparison with a double exponential** (likelihood ratio), which flags squares with two
  populations (short non-specific and long specific binding).

This is the most important design choice. It determines which squares get selected, and
therefore all downstream results.

## 7. Proposed approach: in steps

1. **Compare first, change nothing yet.** Compute the new Tau (M2, and optionally M1)
   alongside the current one, as extra columns: `Tau MLE`, `Tau CI Low`, `Tau CI High`, and a
   goodness-of-fit value. Leave selection and the current `Tau` and `R Squared` columns
   unchanged.
2. **Evaluate** on the reference case and a few real experiments:
   - How much does Tau shift, systematically, and per square?
   - Which quality measure separates good and bad squares in the same way as the current R²?
   - Do survival curves on a log scale show straight lines (one population) or bends (two)?
3. **Decide** on method and quality criterion, then switch over. At that point the current
   histogram fit can be removed, or kept as an option.

Step 1 changes the output files (extra columns in `Squares.csv` and `Recordings.csv`), so the
schema change (strict reading, see the edge-track note, 3.3) applies here too.

## 8. Code involved (step 1)

| File | Change |
|------|--------|
| `paint-shared-utils/.../utils/CalculateTau.java` | Add the MLE estimate (and optionally the survival fit) next to the current fit; return both |
| `paint-generate-squares/.../calc/CalculateSquareAttributes.java` | Fill the extra values on square and recording |
| `paint-shared-utils/.../objects/Square.java`, `Recording.java` + their `TableIO` classes | New columns |
| `paint-generate-squares/.../calc/PlotUtils.java` | Optional: survival curve on log scale next to the histogram |
| Tests: `CalculateTauFitTest`, `CalculateTauTest` | MLE on synthetic exponential data with known τ, including t_min and whole frames |

t_min needs `Min Number of Spots in Track`, which is a TrackMate setting. Generate Squares
must be able to read it: it is recorded per experiment, or else it has to be taken from the
same configuration.

## 9. Consequences

- Tau values will differ systematically from the current ones, and from the values published
  in Steuten et al., Nat Commun 17, 886 (2026). For continuity with the paper, the old method
  must stay available until the comparison has been done.
- Square selection changes once the quality criterion changes. Densities, density ratios and
  recording Tau change with it.
- The regression references must be regenerated at step 3, not at step 1, as long as the
  existing columns stay identical.
- Manual and R scripts: updated only after the decision in step 3.

## 10. Open points (for Hans to decide)

1. Which method(s) to compute in step 1: M2 only, M1 + M2, or also M3?
2. Which quality measure(s) to compute in step 1 (section 6)?
3. Extra columns in the main output files, or a separate comparison file, so that the schema
   of `Squares.csv` and `Recordings.csv` doesn't change yet?
4. Survival curve plots: yes or no, and on a log scale?
5. Should a double-exponential fit be considered now, or later?
