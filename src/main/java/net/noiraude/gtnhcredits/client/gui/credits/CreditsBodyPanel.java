package net.noiraude.gtnhcredits.client.gui.credits;

import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.client.credits.CreditsController;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CreditsBodyPanel extends ParentWidget<CreditsBodyPanel> {

    private static final int CONTENT_MARGIN = 10;

    private final CreditsController controller = new CreditsController();

    private CreditsBodyPanel(int top, int bottom) {
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

    public static @NotNull CreditsBodyPanel create(int top, int bottom) {
        return new CreditsBodyPanel(top, bottom);
    }

    @NotNull
    TextFieldWidget createFilterField() {
        return new FilterTextField()
            .value(new StringValue.Dynamic(controller::getPersonFilter, controller::setPersonFilter))
            .autoUpdateOnChange(true)
            .hintText(StatCollector.translateToLocal("gui.credits.filter.hint"));
    }

    private static final class FilterTextField extends TextFieldWidget {

        @Override
        public void onFocus(ModularGuiContext context) {
            super.onFocus(context);
            Keyboard.enableRepeatEvents(true);
        }

        @Override
        public void onRemoveFocus(ModularGuiContext context) {
            super.onRemoveFocus(context);
            Keyboard.enableRepeatEvents(false);
        }

        @Override
        public @NotNull Interactable.Result onKeyPressed(char character, int keyCode) {
            boolean shift = Interactable.hasShiftDown();
            if (keyCode == Keyboard.KEY_HOME) {
                this.handler.setCursor(0, 0, !shift, true);
                return Interactable.Result.SUCCESS;
            }
            if (keyCode == Keyboard.KEY_END) {
                String line = this.handler.getText()
                    .isEmpty() ? ""
                        : this.handler.getText()
                            .get(0);
                this.handler.setCursor(0, line.length(), !shift, true);
                return Interactable.Result.SUCCESS;
            }
            return super.onKeyPressed(character, keyCode);
        }
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        Area area = getArea();
        int rx = area.rx, ry = area.ry, w = area.w(), h = area.h();
        GuiDraw.drawVerticalGradientRect(rx, ry, w, 4, 0xC0101010, 0x00101010);
        GuiDraw.drawVerticalGradientRect(rx, ry + h - 4, w, 4, 0x00101010, 0xC0101010);
    }
}
