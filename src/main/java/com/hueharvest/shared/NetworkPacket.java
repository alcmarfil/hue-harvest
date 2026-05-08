package com.hueharvest.shared;

import java.io.Serializable;

public class NetworkPacket implements Serializable {
    public enum Type {
        JOIN,          // Client to Server: Request to join
        WELCOME,       // Server to Client: Assign Player ID
        START,         // Client to Server: Host requests game start
        MOVE,          // Client to Server: Directional move
        BURST,         // Client to Server: Trigger ink burst
        UPDATE,        // Server to Client: Full GameState update
        CHAT           // Client <-> Server: Chat message
    }

    public Type type;
    public int playerId;
    public Object data;

    public NetworkPacket(Type type, int playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
    }
}
