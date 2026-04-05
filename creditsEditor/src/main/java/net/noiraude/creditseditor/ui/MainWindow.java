package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.noiraude.creditseditor.ResourceManager;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * The window contains a menu bar and an empty content area. Menu items that require an open
 * resource (Save, and the entire Edit menu) are disabled until a {@link ResourceManager} is
 * provided via {@link #setResourceManager(ResourceManager)}.
 */
public final class MainWindow extends JFrame {

    private ResourceManager resourceManager;

    private final JMenuItem menuItemSave;
    private final JMenu editMenu;

    public MainWindow(ResourceManager resourceManager) {
        super("GTNH Credits Editor");
        this.resourceManager = resourceManager;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                handleQuit();
            }
        });

        setLayout(new BorderLayout());

        // Content area — empty for now
        add(new JPanel(), BorderLayout.CENTER);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem menuItemOpen = new JMenuItem("Open Resources...");
        JMenuItem menuItemNew = new JMenuItem("New Resources...");
        menuItemSave = new JMenuItem("Save Resources");
        JMenuItem menuItemQuit = new JMenuItem("Quit");

        menuItemQuit.addActionListener(e -> handleQuit());

        fileMenu.add(menuItemOpen);
        fileMenu.add(menuItemNew);
        fileMenu.add(new JSeparator());
        fileMenu.add(menuItemSave);
        fileMenu.add(new JSeparator());
        fileMenu.add(menuItemQuit);

        // Edit menu
        editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem("Categories..."));
        editMenu.add(new JMenuItem("Persons..."));
        editMenu.add(new JMenuItem("Roles..."));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);

        updateMenuState();

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    /**
     * Replaces the active {@link ResourceManager} and refreshes gated menu items.
     * May be called with {@code null} to revert to the no-resource state.
     */
    public void setResourceManager(ResourceManager rm) {
        this.resourceManager = rm;
        updateMenuState();
    }

    private void updateMenuState() {
        boolean hasResource = resourceManager != null;
        menuItemSave.setEnabled(hasResource);
        editMenu.setEnabled(hasResource);
    }

    private void handleQuit() {
        if (resourceManager != null) {
            try {
                resourceManager.close();
            } catch (IOException e) {
                System.err.println("Warning: error closing resource: " + e.getMessage());
            }
        }
        System.exit(0);
    }
}
