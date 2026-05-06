package org.alexdev.kepler.server.rcon.messages;

public enum RconHeader {
    REFRESH_LOOKS("refresh_looks"),
    HOTEL_ALERT("hotel_alert"),
    REFRESH_CLUB("refresh_club"),
    REFRESH_HAND("refresh_hand"),
    REFRESH_CREDITS("refresh_credits"),
    DISCONNECT("disconnect"),
    REFRESH_CATALOGUE("refresh_catalogue"),
    REFRESH_CATALOGUE_FRONTPAGE("refresh_catalogue_frontpage"),
    REFRESH_TRADE("refresh_trade"),
    REFRESH_ADS("refresh_ads"),
    REFRESH_GAME_SETTINGS("refresh_game_settings"),
    MUTE_USER("mute_user"),
    UNMUTE_USER("unmute_user"),
    ROOM_MUTE("room_mute"),
    REFRESH_WORDFILTER("refresh_wordfilter"),
    REFRESH_NAVIGATOR("refresh_navigator"),
    GIVE_BADGE("give_badge"),
    REMOVE_BADGE("remove_badge");

    private final String rawHeader;

    RconHeader(String rawHeader) {
        this.rawHeader = rawHeader;
    }

    public String getRawHeader() {
        return rawHeader;
    }

    public static RconHeader getByHeader(String header) {
        for (var rconHeader : values()) {
            if (rconHeader.getRawHeader().equalsIgnoreCase(header)) {
                return rconHeader;
            }
        }

        return null;
    }
}
