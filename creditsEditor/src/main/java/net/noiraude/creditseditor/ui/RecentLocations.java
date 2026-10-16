package net.noiraude.creditseditor.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Small, user-local history of successfully opened or saved credits locations. */
final class RecentLocations {

    private final Preferences preferences;

    RecentLocations(Preferences preferences) {
        this.preferences = preferences;
    }

    static RecentLocations user() {
        return new RecentLocations(Preferences.userNodeForPackage(RecentLocations.class));
    }

    List<String> paths() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String value = preferences.get("recent." + i, "");
            if (!value.isEmpty()) paths.add(value);
        }
        return List.copyOf(paths);
    }

    void remember(Path location) {
        String path = location.toAbsolutePath()
            .normalize()
            .toString();
        List<String> paths = new ArrayList<>(paths());
        paths.remove(path);
        paths.addFirst(path);
        for (int i = 0; i < 10; i++) {
            if (i < paths.size()) preferences.put("recent." + i, paths.get(i));
            else preferences.remove("recent." + i);
        }
    }
}
