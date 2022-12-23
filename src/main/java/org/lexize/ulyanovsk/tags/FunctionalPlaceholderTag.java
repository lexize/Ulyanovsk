package org.lexize.ulyanovsk.tags;

import org.lexize.lomponent.LomponentReader;
import org.lexize.lomponent.LomponentSerializer;
import org.lexize.lomponent.components.GroupComponent;
import org.lexize.lomponent.components.TextComponent;
import org.lexize.lomponent.tags.Tag;
import org.lexize.lomponent.tags.context.TagContext;

import java.util.function.Supplier;

public class FunctionalPlaceholderTag extends Tag<TagContext> {
    private String _name;
    private Supplier<String> _stringSupplier;
    public FunctionalPlaceholderTag(String name, Supplier<String> stringSupplier) {
        _name = name;
        _stringSupplier = stringSupplier;
    }
    @Override
    public String getTagName() {
        return _name;
    }

    @Override
    public void onFound(GroupComponent groupComponent, LomponentSerializer lomponentSerializer, LomponentReader lomponentReader, TagContext tagContext) {
        groupComponent.add(new TextComponent(_stringSupplier.get()));
    }

    @Override
    public TagContext onMatch(LomponentReader.ReaderEvent readerEvent) {
        if (readerEvent.getTagName().equals(getTagName())) {
            TagContext ctx = new TagContext();
            ctx.applyFromEvent(readerEvent);
            return ctx;
        }
        return null;
    }
}
