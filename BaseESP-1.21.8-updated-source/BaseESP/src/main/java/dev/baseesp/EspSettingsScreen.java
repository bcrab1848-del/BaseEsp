package dev.baseesp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class EspSettingsScreen extends Screen {
    private TextFieldWidget input;
    private int page = 0;
    private String status = "Enter a block ID, e.g. minecraft:amethyst_cluster";
    EspSettingsScreen() { super(Text.literal("BaseESP Settings")); }
    @Override protected void init() {
        int x = width/2-155, y = height/2-115;
        addDrawableChild(ButtonWidget.builder(Text.literal("Range -"), b -> { BaseEsp.radius(-16); refresh(); }).dimensions(x,y,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Range +"), b -> { BaseEsp.radius(16); refresh(); }).dimensions(x+105,y,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Opacity -"), b -> { BaseEsp.opacity(-.1f); refresh(); }).dimensions(x,y+24,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Opacity +"), b -> { BaseEsp.opacity(.1f); refresh(); }).dimensions(x+105,y+24,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Finder: " + (BaseEsp.config.finder ? "ON" : "OFF")), b -> { BaseEsp.config.finder = !BaseEsp.config.finder; BaseEsp.save(); refresh(); }).dimensions(x+210,y,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Threshold -"), b -> { BaseEsp.threshold(-1); refresh(); }).dimensions(x+210,y+24,48,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> { BaseEsp.threshold(1); refresh(); }).dimensions(x+263,y+24,47,20).build());
        input = new TextFieldWidget(textRenderer,x,y+53,225,20,Text.literal("Block ID"));
        input.setMaxLength(100); input.setPlaceholder(Text.literal("minecraft:amethyst_cluster")); addDrawableChild(input);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> { status = BaseEsp.addBlock(input.getText()) ? "Added block" : "Unknown block ID"; refresh(); }).dimensions(x+230,y+53,80,20).build());
        int start = page * 5;
        for (int i=start; i<Math.min(start+5,BaseEsp.config.blocks.size()); i++) {
            String id = BaseEsp.config.blocks.get(i);
            int row = i-start;
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> { BaseEsp.removeBlock(id); if (page*5 >= BaseEsp.config.blocks.size() && page>0) page--; refresh(); }).dimensions(x+235,y+82+row*22,75,20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0,page-1); refresh(); }).dimensions(x,y+198,40,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; refresh(); }).dimensions(x+45,y+198,40,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(x+210,y+198,100,20).build());
    }
    private void refresh() { clearAndInit(); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context,mouseX,mouseY,delta);
        int x=width/2-155,y=height/2-115;
        context.drawCenteredTextWithShadow(textRenderer,"BASE ESP  •  PURPLE EDITION",width/2,y-30,0xFFB88AFF);
        context.drawTextWithShadow(textRenderer,"Range: " + BaseEsp.config.radius + "  Opacity: " + Math.round(BaseEsp.config.opacity*100) + "%  Finder threshold: " + BaseEsp.config.threshold,x,y-13,0xFFE4D9FF);
        context.drawTextWithShadow(textRenderer,status,x,y+76,0xFFB88AFF);
        for (int i=page*5;i<Math.min(page*5+5,BaseEsp.config.blocks.size());i++) context.drawTextWithShadow(textRenderer,BaseEsp.config.blocks.get(i),x,y+88+(i-page*5)*22,0xFFE4D9FF);
        context.drawTextWithShadow(textRenderer,"Page " + (page+1),x+95,y+204,0xFFE4D9FF);
    }
    @Override public void close() { if(client != null) client.setScreen(null); }
}
