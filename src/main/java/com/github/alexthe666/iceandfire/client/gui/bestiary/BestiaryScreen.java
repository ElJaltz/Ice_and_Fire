package com.github.alexthe666.iceandfire.client.gui.bestiary;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data-driven bestiary screen. Loads entries and widgets from json, reads text files per language,
 * performs pixel-accurate wrapping, and renders per-spread (two-page) layout.
 *
 * This screen is introduced in parallel to the legacy GuiBestiary. Integration (opening this screen)
 * can be wired subsequently.
 */
public class BestiaryScreen extends Screen {

    // Layout constants (matching legacy bestiary background)
    protected static final int X = 390;
    protected static final int Y = 245;

    private static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire:textures/gui/bestiary/bestiary.png");

    private static final int PAGE_SIZE_IN_LINES = 19; // per column
    private static final int TEXT_MAX_LINE_WIDTH_PX = 92; // soft wrap width per column

    private final List<BestiaryEntry> entries = new ArrayList<>();
    private final List<IndexPageButton> indexButtons = new ArrayList<>();

    private BestiaryEntry currentEntry = null;
    private int currentSpread = 0; // spread index (two pages per spread)
    private int indexPage = 0;

    private ChangePageButton previousPage;
    private ChangePageButton nextPage;

    private boolean indexMode = true;

    protected Font font = getFont();

    public BestiaryScreen() {
        super(Component.translatable("bestiary_gui"));
    }

    private static Font getFont() {
        if (IafConfig.useVanillaFont || !Minecraft.getInstance().options.languageCode.equalsIgnoreCase("en_us")) {
            return Minecraft.getInstance().font;
        } else {
            return (Font) IceAndFire.PROXY.getFontRenderer();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.indexButtons.clear();

        if (entries.isEmpty()) {
            BestiaryLoader.loadIndex(entries);
            // Sort entries by title key for deterministic index
            entries.sort(Comparator.comparing(BestiaryEntry::getTranslatableTitle));
        }

        int centerX = (width - X) / 2;
        int centerY = (height - Y) / 2;

        this.previousPage = new ChangePageButton(centerX + 15, centerY + 215, false, 0, (btn) -> {
            if (indexMode) {
                if (indexPage > 0) indexPage--;
            } else {
                if (currentSpread > 0) {
                    currentSpread--;
                } else {
                    indexMode = true;
                }
            }
        });
        this.addRenderableWidget(previousPage);

        this.nextPage = new ChangePageButton(centerX + 357, centerY + 215, true, 0, (btn) -> {
            if (indexMode) {
                int pages = (int) Math.ceil(entries.size() / 10D);
                if (indexPage < pages - 1) indexPage++;
            } else if (currentEntry != null) {
                if (currentSpread < currentEntry.getSpreadCount() - 1) currentSpread++;
            }
        });
        this.addRenderableWidget(this.nextPage);

        // Build index buttons (paginate 10 per index page, two columns of 5 each)
        for (int i = 0; i < entries.size(); i++) {
            int pageOfThis = i / 10;
            int withinPage = i % 10; // 0..9
            int col = withinPage / 5; // 0..1
            int row = withinPage % 5; // 0..4

            int x = centerX + 25 + col * 180;
            int y = centerY + 20 + row * 20;
            BestiaryEntry entry = entries.get(i);

            IndexPageButton button = new IndexPageButton(x, y,
                    Component.translatable(entry.getTranslatableTitle()), (b) -> {
                this.indexMode = false;
                this.currentEntry = entry;
                this.currentSpread = 0;
            });
            button.visible = pageOfThis == indexPage;
            this.indexButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);

        for (Renderable widget : this.renderables) {
            if (widget instanceof IndexPageButton) {
                IndexPageButton button = (IndexPageButton) widget;
                button.active = indexMode;
                // update visibility based on current index page
                if (indexMode) {
                    int idx = indexButtons.indexOf(button);
                    int pageOfThis = idx / 10;
                    button.visible = pageOfThis == indexPage;
                } else {
                    button.visible = false;
                }
            }
        }

        int cornerX = (width - X) / 2;
        int cornerY = (height - Y) / 2;
        ms.blit(TEXTURE, cornerX, cornerY, 0, 0, X, Y, 390, 390);

        super.render(ms, mouseX, mouseY, partialTicks);

        ms.pose().pushPose();
        ms.pose().translate(cornerX, cornerY, 0.0F);

        if (!indexMode && currentEntry != null) {
            renderSpread(ms, currentEntry, currentSpread);

            // Page numbers (left/right)
            int pageLeft = currentSpread * 2 + 1;
            int pageRight = pageLeft + 1;
            // left number near left page top
            font.drawInBatch(String.valueOf(pageLeft), 20, 8, 0X303030, false, ms.pose().last().pose(), ms.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
            // right number near right page top
            font.drawInBatch(String.valueOf(pageRight), 200 + 20, 8, 0X303030, false, ms.pose().last().pose(), ms.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        }

        ms.pose().popPose();

        this.renderables.forEach((widget -> widget.render(ms, mouseX, mouseY, partialTicks)));
    }

    private void renderSpread(GuiGraphics ms, BestiaryEntry entry, int spread) {
        // title
        ms.pose().pushPose();
        String s = Component.translatable(entry.getTranslatableTitle()).getString();
        float scale = font.width(s) <= 100 ? 2.0F : font.width(s) * 0.0125F;
        ms.pose().scale(scale, scale, scale);
        font.drawInBatch(s, 10, 2, 0X7A756A, false, ms.pose().last().pose(), ms.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        ms.pose().popPose();

        // text
        drawTextColumns(ms, entry, spread);

        // widgets for this spread
        for (BookWidget w : entry.getWidgets()) {
            if (w.spread == spread) {
                w.render(ms);
            }
        }
    }

    private void drawTextColumns(GuiGraphics ms, BestiaryEntry entry, int spread) {
        int minLine = spread * PAGE_SIZE_IN_LINES * 2;
        int maxLine = Math.min(minLine + PAGE_SIZE_IN_LINES * 2, entry.entryText.size());

        int line = minLine;
        // Left column
        for (int i = 0; i < PAGE_SIZE_IN_LINES && line < maxLine; i++, line++) {
            drawLine(ms, entry.entryText.get(line), 15, 20 + i * 10);
        }
        // Right column
        for (int i = 0; i < PAGE_SIZE_IN_LINES && line < maxLine; i++, line++) {
            drawLine(ms, entry.entryText.get(line), 220, 20 + i * 10);
        }
    }

    private void drawLine(GuiGraphics ms, String text, int x, int y) {
        ms.pose().pushPose();
        if (font == Minecraft.getInstance().font) {
            ms.pose().scale(0.945F, 0.945F, 0.945F);
            ms.pose().translate(0, 5.5F, 0);
        }
        font.drawInBatch(text, x, y, 0X303030, false, ms.pose().last().pose(), ms.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        ms.pose().popPose();
    }

    // ---------- Data Model & Loader ----------

    public static class BestiaryEntry {
        @Expose
        private String title;
        @Expose private String parent;
        @Expose private String text; // text file name, e.g., "introduction_0.txt"
        @Expose private String required_progression;
        @Expose private BookWidget[] widgets = new BookWidget[0];

        // runtime
        private List<String> entryText = new ArrayList<>();
        private int spreadCount = 1;

        public String getTranslatableTitle() {
            return title;
        }
        public String getParent() { return parent; }
        public BookWidget[] getWidgets() { return widgets; }
        public int getSpreadCount() { return spreadCount; }

        public void init() {
            this.entryText = getRawTextFromFile(text);
            int lines = entryText.size();
            this.spreadCount = Math.max(1, (int) Math.ceil(lines / (float) (PAGE_SIZE_IN_LINES * 2)));
            // Ensure widgets' max spread considered
            for (BookWidget w : widgets) {
                this.spreadCount = Math.max(this.spreadCount, w.spread + 1);
            }
        }

        private List<String> getRawTextFromFile(String fileName) {
            List<String> strings = new ArrayList<>();
            try {
                String lang = getSelectedLanguageCode();
                ResourceLocation fileRes = new ResourceLocation(getBookFileDirectory() + lang + "/" + fileName);
                Optional<Resource> res;
                res = Minecraft.getInstance().getResourceManager().getResource(fileRes);
                if (res.isEmpty()) {
                    fileRes = new ResourceLocation(getBookFileDirectory() + "en_us/" + fileName);
                    res = Minecraft.getInstance().getResourceManager().getResource(fileRes);
                }
                if (res.isPresent()) {
                    try (BufferedReader bufferedReader = Minecraft.getInstance().getResourceManager().openAsReader(fileRes)) {
                        List<String> readIn = IOUtils.readLines(bufferedReader);
                        Font font = getFont();
                        Pattern pattern = Pattern.compile("\\{.*?\\}"); // inline link pattern reserved (not yet clickable)
                        for (String raw : readIn) {
                            String readString = raw;
                            // Strip inline patterns but keep visible text before we implement links
                            Matcher m = pattern.matcher(readString);
                            while (m.find()) {
                                String[] found = m.group().split("\\|");
                                if (found.length >= 1) {
                                    String display = found[0].substring(1); // text inside {}
                                    readString = m.replaceFirst(display);
                                    m = pattern.matcher(readString);
                                }
                            }
                            if (readString.isEmpty()) {
                                strings.add("");
                                continue;
                            }
                            // wrap by pixel width
                            while (font.width(readString) > TEXT_MAX_LINE_WIDTH_PX) {
                                int spaceScanIndex = 0;
                                int lastSpace = -1;
                                while (spaceScanIndex < readString.length()) {
                                    if (readString.charAt(spaceScanIndex) == ' ' && font.width(readString.substring(0, spaceScanIndex)) > TEXT_MAX_LINE_WIDTH_PX) {
                                        lastSpace = spaceScanIndex;
                                        break;
                                    }
                                    spaceScanIndex++;
                                }
                                int cutIndex = lastSpace == -1 ? Math.min(readString.length(), clampByGlyphs(font, readString, TEXT_MAX_LINE_WIDTH_PX)) : lastSpace;
                                strings.add(readString.substring(0, cutIndex));
                                readString = readString.substring(cutIndex);
                                if (readString.startsWith(" ")) {
                                    readString = readString.substring(1);
                                }
                            }
                            if (!readString.isEmpty()) {
                                strings.add(readString);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return strings;
        }
    }

    public static abstract class BookWidget {
        @Expose public int spread = 0; // spread index this widget appears on
        @Expose public int x;
        @Expose public int y;
        @Expose public float scale = 1.0F;

        public abstract void render(GuiGraphics ms);

        public enum Type {
            IMAGE(ImageWidget.class),
            ITEM(ItemWidget.class);
            private final Class<? extends BookWidget> widgetClass;
            Type(Class<? extends BookWidget> widgetClass) { this.widgetClass = widgetClass; }
            public Class<? extends BookWidget> getWidgetClass() { return widgetClass; }
        }
    }

    public static class ImageWidget extends BookWidget {
        @Expose public String texture;
        @Expose public int u;
        @Expose public int v;
        @Expose public int w;
        @Expose public int h;

        @Override
        public void render(GuiGraphics ms) {
            ms.pose().pushPose();
            ResourceLocation tex = new ResourceLocation(texture);
            net.minecraft.client.renderer.texture.TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.bindForSetup(tex);
            float s = scale;
            ms.pose().scale(s, s, s);
            ms.blit(tex, (int)(x / s), (int)(y / s), u, v, w, h, 512, 512);
            ms.pose().popPose();
        }
    }

    public static class ItemWidget extends BookWidget {
        @Expose public String item;

        @Override
        public void render(GuiGraphics ms) {
            ItemStack stack = ItemStack.EMPTY;
            try {
                Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));
                if (it != null) stack = new ItemStack(it);
            } catch (Exception ignored) {}
            ms.pose().pushPose();
            ms.pose().scale(scale, scale, scale);
            ms.renderItem(stack, x, y);
            ms.pose().popPose();
        }
    }

    public static class BestiaryLoader {
        private static final Gson GSON = (new GsonBuilder())
                .registerTypeAdapter(BookWidget.class, new WidgetDeserializer())
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        public static void loadIndex(List<BestiaryEntry> sink) {
            try {
                ResourceLocation indexLoc = new ResourceLocation(getBookRoot() + "index.json");
                Optional<Resource> indexRes = Minecraft.getInstance().getResourceManager().getResource(indexLoc);
                if (indexRes.isEmpty()) return;
                try (Reader reader = indexRes.get().openAsReader()) {
                    JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                    JsonArray arr = json.getAsJsonArray("entries");
                    for (JsonElement el : arr) {
                        String path = el.getAsString();
                        ResourceLocation entryLoc = new ResourceLocation(getBookRoot() + path);
                        Optional<Resource> eRes = Minecraft.getInstance().getResourceManager().getResource(entryLoc);
                        if (eRes.isEmpty()) continue;
                        try (Reader er = eRes.get().openAsReader()) {
                            BestiaryEntry entry = GsonHelper.fromJson(GSON, er, BestiaryEntry.class);
                            if (entry != null) {
                                entry.init();
                                sink.add(entry);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class WidgetDeserializer implements JsonDeserializer<BookWidget> {
        @Override
        public BookWidget deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            BookWidget.Type type = GsonHelper.getAsObject(obj, "type", context, BookWidget.Type.class);
            return GsonHelper.convertToObject(obj, "", context, type.getWidgetClass());
        }
    }

    // ---------- Helpers ----------

    private static String getSelectedLanguageCode() {
        try {
            return Minecraft.getInstance().options.languageCode.toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return "en_us";
        }
    }

    private static String getBookRoot() {
        // assets/iceandfire/bestiary/ -> ResourceLocation root "iceandfire:bestiary/"
        return "iceandfire:bestiary/";
    }

    private static String getBookFileDirectory() {
        return getBookRoot();
    }

    private static int clampByGlyphs(Font font, String str, int maxPx) {
        int i = 0;
        while (i < str.length() && font.width(str.substring(0, i + 1)) <= maxPx) {
            i++;
        }
        return Math.max(1, i);
    }

    // ---------- Dummy buttons to keep parity with existing screen API ----------

    public static class ChangePageButton extends net.minecraft.client.gui.components.Button {
        public ChangePageButton(int x, int y, boolean forward, int unused, OnPress onPress) {
            super(x, y, 20, 20, Component.literal(forward ? ">" : "<"), onPress, DEFAULT_NARRATION);
        }
    }

    public static class IndexPageButton extends net.minecraft.client.gui.components.Button {
        public IndexPageButton(int x, int y, Component title, OnPress onPress) {
            super(x, y, 160, 20, title, onPress, DEFAULT_NARRATION);
        }
    }
}
