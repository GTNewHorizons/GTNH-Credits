package net.noiraude.creditseditor.ui.dialog;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.service.TsvImporter;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the asynchronous TSV parse and the backing table model for a preview widget.
 *
 * <p>
 * Any in-flight parse is canceled when a new one is requested or when {@link #clear()} is
 * called. State changes (including post-error clears) invoke {@code onStateChange}; read
 * failures invoke {@code onError} with a user-facing message.
 */
final class TsvPreviewController {

    private final @NotNull DocumentBus bus;
    private final @NotNull Runnable onStateChange;
    private final @NotNull Consumer<@NotNull String> onError;
    private final @NotNull PreviewTableModel tableModel = new PreviewTableModel();

    private @NotNull List<ImportLine> lines = List.of();
    private @Nullable SwingWorker<List<ImportLine>, Void> worker;

    TsvPreviewController(@NotNull DocumentBus bus, @NotNull Runnable onStateChange,
        @NotNull Consumer<@NotNull String> onError) {
        this.bus = bus;
        this.onStateChange = onStateChange;
        this.onError = onError;
    }

    @Contract(pure = true)
    @NotNull
    AbstractTableModel tableModel() {
        return tableModel;
    }

    @Contract(pure = true)
    @NotNull
    List<ImportLine> lines() {
        return lines;
    }

    void clear() {
        if (worker != null) worker.cancel(true);
        worker = null;
        lines = List.of();
        tableModel.setLines(lines);
        onStateChange.run();
    }

    void reload(@NotNull File file, @NotNull String categoryId) {
        if (worker != null) worker.cancel(true);
        worker = new ParseWorker(file, categoryId);
        worker.execute();
    }

    /**
     * Extracts a non-null user-facing message from an {@link ExecutionException}.
     *
     * <p>
     * Prefers the cause's message when present; otherwise the outer exception's message; and
     * otherwise a placeholder. Protects against {@link NullPointerException} on exceptions
     * constructed without a cause.
     */
    @Contract(pure = true)
    static @NotNull String causeMessageOf(@NotNull ExecutionException ex) {
        Throwable cause = ex.getCause();
        String msg = cause != null ? cause.getMessage() : ex.getMessage();
        return msg != null ? msg : "unknown error";
    }

    private final class ParseWorker extends SwingWorker<List<ImportLine>, Void> {

        private final @NotNull File file;
        private final @NotNull String categoryId;

        ParseWorker(@NotNull File file, @NotNull String categoryId) {
            this.file = file;
            this.categoryId = categoryId;
        }

        @Override
        protected List<ImportLine> doInBackground() throws IOException {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                return TsvImporter.parse(reader, bus.creditsDoc(), categoryId);
            }
        }

        @Override
        protected void done() {
            if (isCancelled()) return;
            try {
                lines = get();
            } catch (InterruptedException ex) {
                Thread.currentThread()
                    .interrupt();
                lines = List.of();
            } catch (ExecutionException ex) {
                lines = List.of();
                onError.accept("Failed to read file:\n" + causeMessageOf(ex));
            }
            tableModel.setLines(lines);
            onStateChange.run();
        }
    }

    private static final class PreviewTableModel extends AbstractTableModel {

        private static final @NotNull String @NotNull [] COLUMNS = { "Name", "Roles", "Action" };
        private @NotNull List<ImportLine> lines = List.of();

        void setLines(@NotNull List<ImportLine> lines) {
            this.lines = lines;
            fireTableDataChanged();
        }

        @Contract(pure = true)
        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Contract(pure = true)
        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public @NotNull Object getValueAt(int row, int col) {
            ImportLine line = lines.get(row);
            return switch (col) {
                case 0 -> line.name;
                case 1 -> String.join(", ", line.roles);
                case 2 -> line.action.name()
                    .replace('_', ' ')
                    .toLowerCase();
                default -> "";
            };
        }
    }
}
