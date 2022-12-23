package org.lexize.ulyanovsk.components;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.chat.hover.content.TextSerializer;
import org.lexize.lomponent.LomponentSerializer;
import org.lexize.lomponent.LomponentStyleContainer;
import org.lexize.lomponent.LomponentUtils;
import org.lexize.lomponent.components.GroupComponent;
import org.lexize.ulyanovsk.Ulyanovsk;

import java.io.IOException;
import java.io.StringReader;

public class HoverComponent extends GroupComponent {
    public enum HoverType {
        SHOW_TEXT(1,1),
        SHOW_ITEM(1,3),
        SHOW_ENTITY(2,3);
        private int _neededArgs;
        private int _maxArgs;
        HoverType(int neededArgsCount, int maxArgs) {
            _neededArgs = neededArgsCount;
            _maxArgs = maxArgs;
        }

        public boolean matchArgsCount(int c) {
            return c >= _neededArgs && c <= _maxArgs;
        }

        public static HoverType getByString(String stringID) {
            return switch (stringID.toLowerCase()) {
                case "show_text" -> SHOW_TEXT;
                case "show_item" -> SHOW_ITEM;
                case "show_entity" -> SHOW_ENTITY;
                default -> null;
            };
        }


    }
    private HoverEvent _event;
    public HoverComponent(HoverType type, String[] args, LomponentSerializer sourceSerializer) {
        switch (type) {
            case SHOW_TEXT -> {
                StringReader sr = new StringReader(args[0].trim());
                String lomponentString;
                try {
                    lomponentString = LomponentUtils.readString(sr);
                } catch (IOException e) {
                    lomponentString = "string read exception";
                }
                sr.close();
                GroupComponent gc = sourceSerializer.parse(lomponentString);
                BaseComponent bc = Ulyanovsk.Utils.FromLomponentToBaseComponent(gc);
                _event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[]{bc}));
            }
            case SHOW_ITEM -> {
                int count = 1;
                if (args.length > 1) count = Math.max(1, Integer.parseInt(args[1]));
                ItemTag tag;
                if (args.length > 2) {
                    StringReader sr = new StringReader(args[2].trim());
                    String tagString;
                    try {
                        tagString = LomponentUtils.readString(sr);
                    } catch (IOException e) {
                        tagString = "";
                    }
                    sr.close();
                    tag = ItemTag.ofNbt(tagString);
                }
                else tag = ItemTag.ofNbt("");
                _event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Item(args[0], count, tag));
            }
        }
    }
    @Override
    public void onStyleGet(LomponentStyleContainer style, float relativePos) {
        style.addStyle("hover", _event);
    }
}
