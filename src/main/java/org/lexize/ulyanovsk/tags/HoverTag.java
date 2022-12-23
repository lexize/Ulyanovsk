package org.lexize.ulyanovsk.tags;

import org.lexize.lomponent.LomponentReader;
import org.lexize.lomponent.LomponentSerializer;
import org.lexize.lomponent.components.GroupComponent;
import org.lexize.lomponent.components.TextComponent;
import org.lexize.lomponent.tags.Tag;
import org.lexize.lomponent.tags.context.TagContext;
import org.lexize.ulyanovsk.components.HoverComponent;

import java.util.Arrays;

public class HoverTag extends Tag<TagContext> {
    @Override
    public String getTagName() {
        return "hover";
    }

    @Override
    public void onFound(GroupComponent groupComponent, LomponentSerializer lomponentSerializer, LomponentReader lomponentReader, TagContext tagContext) {
        String[] args = tagContext.getTagArgs();
        if (args.length >= 2) {
            HoverComponent.HoverType hoverType = HoverComponent.HoverType.getByString(args[0]);
            if (hoverType.matchArgsCount(args.length-1)) {
                HoverComponent component = new HoverComponent(hoverType, Arrays.copyOfRange(args, 1, args.length), lomponentSerializer);
                groupComponent.add(component);
                readToClosingTag(tagContext, component, lomponentSerializer, lomponentReader);
                return;
            }
        }
        groupComponent.add(new TextComponent(tagContext.getTagSourceText()));
    }

    @Override
    public TagContext onMatch(LomponentReader.ReaderEvent readerEvent) {
        if (readerEvent.getTagName().equals(getTagName())) {
            return TagContext.fromReaderEvent(readerEvent, TagContext::new);
        }
        return null;
    }
}
