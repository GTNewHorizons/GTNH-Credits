package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapHuge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal indeterminate progress dialog driven by a {@link SwingWorker}.
 *
 * <p>
 * Use {@link #runWith} to run a worker while displaying a "please wait" indicator.
 * The dialog is shown modally on the EDT and disposed automatically when the worker
 * transitions to {@link StateValue#DONE}, regardless of whether the worker succeeded or
 * threw. The caller still has to {@link SwingWorker#get()} the result for completion
 * status and exception propagation.
 *
 * <p>
 * Has no Cancel button: the load and save operations the editor wraps are not safely
 * interruptible mid-stream.
 */
public final class ProgressDialog extends JDialog {

    private ProgressDialog(@Nullable Window owner, @NotNull String title, @NotNull String message) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, gapMedium));
        content.setBorder(BorderFactory.createEmptyBorder(gapHuge, gapHuge, gapHuge, gapHuge));
        content.add(new JLabel(message), BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        Dimension barSize = bar.getPreferredSize();
        bar.setPreferredSize(new Dimension(Math.max(barSize.width, 320), barSize.height));
        content.add(bar, BorderLayout.CENTER);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Runs {@code worker} while showing a modal indeterminate progress dialog over {@code owner}.
     *
     * <p>
     * The dialog is constructed and {@link SwingWorker#execute()} is called before
     * {@link #setVisible(boolean)} blocks the EDT in the modal pump; a
     * {@code PropertyChangeListener} disposes the dialog once the worker is {@link StateValue#DONE}.
     * Must be invoked on the EDT.
     */
    public static void runWith(@Nullable Window owner, @NotNull String title, @NotNull String message,
        @NotNull SwingWorker<?, ?> worker) {
        assert SwingUtilities.isEventDispatchThread();
        ProgressDialog dialog = new ProgressDialog(owner, title, message);
        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && evt.getNewValue() == StateValue.DONE) {
                dialog.dispose();
            }
        });
        worker.execute();
        dialog.setVisible(true);
    }
}
