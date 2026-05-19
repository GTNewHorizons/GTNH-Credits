package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;

import org.jetbrains.annotations.NotNull;

/** Top-level frame for the GTNH Credits Editor. */
public final class MainWindow extends JFrame {

    public MainWindow(@NotNull DocumentBus bus, @NotNull CommandExecutor commandExecutor) {
        super(AppInfo.name());
        setIconImages(AppIcons.load());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        EditorActions actions = new EditorActions(bus);
        EditorView view = new EditorView(bus, commandExecutor);
        EditorToolBar toolBar = new EditorToolBar(bus, actions);
        EditorMenuBar menuBar = new EditorMenuBar(actions);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        setJMenuBar(menuBar);
        addWindowListener(new AppLifecycleController(bus));
        finalizeSize(menuBar);
    }

    private void finalizeSize(@NotNull JMenuBar menuBar) {
        pack();
        Insets frameInsets = getInsets();
        Dimension contentMin = getContentPane().getMinimumSize();
        int menuBarHeight = menuBar.getPreferredSize().height;
        setMinimumSize(
            new Dimension(
                contentMin.width + frameInsets.left + frameInsets.right,
                contentMin.height + menuBarHeight + frameInsets.top + frameInsets.bottom));
        setSize(ScaledMetrics.mainWindowWidth, ScaledMetrics.mainWindowHeight);
        setLocationRelativeTo(null);
        ScaledMetrics.attachTo(this);
    }
}
