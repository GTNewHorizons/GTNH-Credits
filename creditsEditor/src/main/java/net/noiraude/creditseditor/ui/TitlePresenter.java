package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.util.Objects;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Keeps the frame title in sync with the active session. */
final class TitlePresenter {

    private final @NotNull Frame owner;
    private final @NotNull SessionHolder holder;

    TitlePresenter(@NotNull DocumentBus bus, @NotNull Frame owner, @NotNull SessionHolder holder) {
        this.owner = Objects.requireNonNull(owner);
        this.holder = Objects.requireNonNull(holder);
        bus.addListener(DocumentBus.TOPIC_DIRTY, e -> refresh());
        bus.addListener(DocumentBus.TOPIC_SESSION, e -> refresh());
        refresh();
    }

    private void refresh() {
        String appName = AppInfo.name();
        owner.setTitle(
            holder.get()
                .map(session -> titleFor(appName, session))
                .orElse(appName));
    }

    private static @NotNull String titleFor(@NotNull String appName, @NotNull EditorSession session) {
        String key = session.isDirty() ? "title.session.dirty" : "title.session";
        return I18n.get(key, MsgArg.text(appName), MsgArg.text(session.displayPath()));
    }
}
