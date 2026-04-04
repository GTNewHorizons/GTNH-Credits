package net.noiraude.gtnhcredits.client.gui.credits;

import net.noiraude.gtnhcredits.repository.CreditsController;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.layout.Flow;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CreditsBodyPanel extends ParentWidget<CreditsBodyPanel> {

    private static final int CONTENT_MARGIN = 10;

    private CreditsBodyPanel(int top, int bottom, CreditsController controller) {
        left(0);
        top(top);
        right(0);
        bottom(bottom);
        background((ctx, x, y, w, h, theme) -> GuiDraw.drawVerticalGradientRect(x, y, w, h, 0xC0101010, 0xD0101010));

        child(
            Flow.row()
                .full()
                .childPadding(CONTENT_MARGIN)
                .child(CreditsCategorySelector.create(140, controller))
                .child(
                    new CreditsView(controller).expanded()
                        .heightRel(1f)));
    }

    public static @NotNull CreditsBodyPanel create(int top, int bottom, CreditsController controller) {
        return new CreditsBodyPanel(top, bottom, controller);
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        Area area = getArea();
        int rx = area.rx, ry = area.ry, w = area.w(), h = area.h();
        GuiDraw.drawVerticalGradientRect(rx, ry, w, 4, 0xC0101010, 0x00101010);
        GuiDraw.drawVerticalGradientRect(rx, ry + h - 4, w, 4, 0x00101010, 0xC0101010);
    }
}
