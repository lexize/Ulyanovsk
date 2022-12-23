package org.lexize.ulyanovsk.models;

import net.md_5.bungee.api.chat.BaseComponent;
import org.lexize.ulyanovsk.annotations.SQLValue;

public abstract class DataElement {
    @SQLValue("invokerUUID")
    private String _invokerUUID;
    @SQLValue("creation_time")
    private long _creationTime;
    protected DataElement() {}
    public DataElement(String invokerUUID, long creationTime) {
        _invokerUUID = invokerUUID;
        _creationTime = creationTime;
    }

    public String getInvokerUUID() {
        return _invokerUUID != null ? _invokerUUID : "CONSOLE";
    }

    public long getCreationTime() {
        return _creationTime;
    }

    public abstract BaseComponent getShortMessage(int element_id);
    public abstract BaseComponent getMessage(int element_id);
}
