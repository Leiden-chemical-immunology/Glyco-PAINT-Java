/*==============================================================================
 *  Class:        SquareOverrideApplier.java
 *  Package:      paint.shared.override
 *
 *  PURPOSE:
 *    Applies cellId overrides from a Squares Override table to a list of
 *    Square objects. An override replaces the cellId of the matching Square
 *    identified by (experimentName, recordingName, squareNumber).
 *
 *  DESCRIPTION:
 *    This utility builds a composite-key lookup map from the override list
 *    and updates the corresponding Square entries in-place. It is safe,
 *    deterministic, and Java 8 compatible.
 *
 *  KEY FEATURES:
 *    • Fast O(n) override application.
 *    • Composite-key matching on (experimentName, recordingName, squareNumber).
 *    • No external dependencies.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-11
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ==============================================================================*/

package paint.shared.override;

import static paint.shared.constants.PaintConstants.*;
import paint.shared.objects.Square;
import paint.shared.objects.SquareOverride;

import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SquareOverrideApplier {

    public static void applyOverrides(List<Square> squares,
            List<SquareOverride> overrides) {

        Map<String, Integer> overrideCellIds = new HashMap<String, Integer>();
        for (SquareOverride o : overrides) {
            String key = key(o.getExperimentName(),
                             o.getRecordingName(),
                             o.getSquareNumber());
            overrideCellIds.put(key, o.getCellId());
        }

        int applied = 0;

        for (Square sq : squares) {
            String key = key(sq.getExperimentName(),
                             sq.getRecordingName(),
                             sq.getSquareNumber());

            Integer newCellId = overrideCellIds.get(key);
            if (newCellId != null && newCellId != sq.getCellId()) {

                System.out.println(
                        "Updated square: " +
                                sq.getExperimentName() + " / " +
                                sq.getRecordingName() + " / #" +
                                sq.getSquareNumber() +
                                " | cellId " + sq.getCellId() +
                                " → " + newCellId
                );

                sq.setCellId(newCellId);
                applied++;
            }
        }

        System.out.println("Overrides applied: " + applied);
    }

    private static String key(String experimentName,
            String recordingName,
            int squareId) {
        return experimentName + "§" + recordingName + "§" + squareId;
    }

    private SquareOverrideApplier() {}

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN FUNCTION FOR STANDALONE PATCHING
    // ───────────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        String squaresFile;
        String overridesFile;
        String outputFile;

        if (args.length == 0) {
            squaresFile   = "/Users/hans/Paint Test Project/Squares.csv";
            overridesFile = "/Users/hans/Paint Test Project/Viewer/Square Override.csv";
            outputFile    = "/Users/hans/Downloads/New Squares.csv";
        }
        else if (args.length != 3) {
            System.out.println("Usage: java SquareOverrideApplier <SquaresCSV> <OverrideCSV> <OutputCSV>");
            return;
        } else {
            squaresFile   = args[0];
            overridesFile = args[1];
            outputFile    = args[2];
        }

        System.out.println("Loading Squares from: " + squaresFile);
        Table squaresTable = Table.read().csv(squaresFile);

        System.out.println("Loading Overrides from: " + overridesFile);
        Table overrideTable = Table.read().csv(overridesFile);

        // Convert Squares CSV → POJOs
        List<Square> squares = new ArrayList<Square>();
        for (int i = 0; i < squaresTable.rowCount(); i++) {
            Square s = new Square();
            s.setExperimentName(squaresTable.column(EXPERIMENT_NAME).get(i).toString());
            s.setRecordingName(squaresTable.column(RECORDING_NAME).get(i).toString());
            s.setSquareNumber(Integer.parseInt(squaresTable.column(SQUARE_NUMBER).get(i).toString()));
            s.setCellId(Integer.parseInt(squaresTable.column("Cell Id").get(i).toString()));
            squares.add(s);
        }

        // Convert Overrides CSV → POJOs
        List<SquareOverride> overrides = new ArrayList<SquareOverride>();
        for (int i = 0; i < overrideTable.rowCount(); i++) {
            SquareOverride o = new SquareOverride();
            o.setExperimentName(overrideTable.column(EXPERIMENT_NAME).get(i).toString());
            o.setRecordingName(overrideTable.column(RECORDING_NAME).get(i).toString());
            o.setSquareNumber(Integer.parseInt(overrideTable.column(SQUARE_NUMBER).get(i).toString()));
            o.setCellId(Integer.parseInt(overrideTable.column("Cell Id").get(i).toString()));
            o.setTimestamp(overrideTable.column("Timestamp").get(i).toString());
            overrides.add(o);
        }

        System.out.println("Applying overrides...");
        applyOverrides(squares, overrides);

        // Create updated output table
        Table out = squaresTable.copy();

        // Overwrite only Cell Id
        for (int i = 0; i < squares.size(); i++) {
            out.intColumn("Cell Id").set(i, squares.get(i).getCellId());
        }

        System.out.println("Writing updated Squares to: " + outputFile);
        out.write().csv(outputFile);

        System.out.println("Done.");
    }
}