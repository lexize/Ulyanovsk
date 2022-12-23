package org.lexize.ulyanovsk;

import org.lexize.ulyanovsk.models.UlyanovskTeleportPosition;

import java.util.HashMap;
import java.util.Map;

public class UlyanovskConfig {
    public String WorldName = "jail";
    public String CommandNamespace = "ulyanovsk";
    public UlyanovskTeleportPosition TeleportPosition = new UlyanovskTeleportPosition();
    public Map<String, String[]> TimestampComponentAliases = new HashMap<>(){{
        put("sec", new String[]{"s"});
        put("min", new String[]{"m"});
        put("hours", new String[]{"h"});
        put("days", new String[]{"d"});
        put("months", new String[]{"M"});
    }};
    public String TimestampNotMatchExceptionMessage = "Specified timestamp not matches pattern";
}
