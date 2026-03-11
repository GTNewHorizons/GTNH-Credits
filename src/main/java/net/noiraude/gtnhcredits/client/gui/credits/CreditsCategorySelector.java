package net.noiraude.gtnhcredits.client.gui.credits;

import java.util.List;

import net.noiraude.gtnhcredits.client.credits.CreditsController;
import net.noiraude.gtnhcredits.credits.CreditsCategory;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
class CreditsCategorySelector extends ListWidget<IWidget, CreditsCategorySelector> {

    private static final int SLOT_HEIGHT = 20;

    CreditsCategorySelector(int width, CreditsController controller) {
        width(width);
        heightRel(1f);
        List<CreditsCategory> categories = controller.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            child(new CategoryButton(controller, i));
        }
    }

    public static @NotNull CreditsCategorySelector create(int width, CreditsController controller) {
        return new CreditsCategorySelector(width, controller);
    }

    private static final class CategoryButton extends ButtonWidget<CategoryButton> {

        private final CreditsController controller;
        private final int index;

        private CategoryButton(CreditsController controller, int index) {
            this.controller = controller;
            this.index = index;
            widthRel(1.0f).height(SLOT_HEIGHT)
                .paddingLeft(4)
                .disableHoverBackground()
                .child(
                    new TextWidget<>(IKey.dynamic(() -> controller.getCategoryDisplayName(index)))
                        .color(() -> controller.getSelectedIndex() == index ? 0xFFFFFF : 0xAAAAAA)
                        .textAlign(Alignment.CenterLeft)
                        .full())
                .onMousePressed(mb -> {
                    controller.selectCategory(index);
                    return true;
                });
        }

        @Override
        public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            int w = getArea().width, h = getArea().height;
            if (controller.getSelectedIndex() == this.index) {
                GuiDraw.drawRect(0, 0, w, h, 0x30FFFFFF);
                GuiDraw.drawBorderInsideXYWH(0, 0, w, h, 0x80FFFFFF);
            } else if (isHovering()) {
                GuiDraw.drawRect(0, 0, w, h, 0x18FFFFFF);
            }
        }
    }
}
