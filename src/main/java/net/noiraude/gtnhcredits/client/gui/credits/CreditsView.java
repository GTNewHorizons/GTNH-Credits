package net.noiraude.gtnhcredits.client.gui.credits;

import net.noiraude.gtnhcredits.client.credits.CreditsController;
import net.noiraude.gtnhcredits.client.credits.CreditsController.FilterMethod;

import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
class CreditsView extends ScrollWidget<CreditsView> {

    private final CachedRichTextWidget richTextWidget;
    private final CreditsController controller;
    private int lastSelectedIndex = -1;
    private String lastFilter = "";
    private FilterMethod lastFilterMethod = null;

    private static final int PADDING_TOP = 18;
    private static final int PADDING_BOTTOM = 9;
    private static final int PADDING_H = 12;

    CreditsView(CreditsController controller) {
        super(new VerticalScrollData());
        this.controller = controller;
        getScrollArea().getPadding()
            .left(PADDING_H)
            .right(PADDING_H)
            .top(PADDING_TOP)
            .bottom(PADDING_BOTTOM);

        this.richTextWidget = new CachedRichTextWidget().widthRel(1f)
            .titleText(() -> controller.getCategoryDisplayName(controller.getSelectedIndex()))
            .textColor(Color.GREY.main)
            .textBuilder((rt, w) -> CreditsContentRenderer.render(rt, controller, w));
        child(this.richTextWidget);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        int currentIndex = this.controller.getSelectedIndex();
        if (currentIndex != this.lastSelectedIndex) {
            this.lastSelectedIndex = currentIndex;
            getScrollArea().getScrollY()
                .scrollTo(getScrollArea(), 0);
            this.richTextWidget.markDirty();
        }

        String currentFilter = this.controller.getPersonFilter();
        if (!currentFilter.equals(this.lastFilter)) {
            this.lastFilter = currentFilter;
            this.richTextWidget.markDirty();
        }

        CreditsController.FilterMethod currentMethod = this.controller.getFilterMethod();
        if (currentMethod != this.lastFilterMethod) {
            this.lastFilterMethod = currentMethod;
            this.richTextWidget.markDirty();
        }

        int h = this.richTextWidget.getLastHeight();
        if (h > 0) {
            getScrollArea().getScrollY()
                .setScrollSize(h + PADDING_TOP + PADDING_BOTTOM);
            this.richTextWidget.getArea()
                .h(h);
        }
    }
}
