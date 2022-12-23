package org.lexize.ulyanovsk.exceptions;

import java.util.UUID;

public class PlayerAlreadyInJail extends Exception{
    private UUID _playerUUID;
    public PlayerAlreadyInJail(UUID player_uuid) {
        _playerUUID = player_uuid;
    }

    @Override
    public String getMessage() {
        return "Player with UUID %s is already in jail".formatted(_playerUUID.toString());
    }
}
