package org.alexdev.kepler.server.rcon;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.alexdev.kepler.Kepler;
import org.alexdev.kepler.dao.mysql.CurrencyDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.outgoing.user.ALERT;
import org.alexdev.kepler.messages.outgoing.user.currencies.CREDIT_BALANCE;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.dao.mysql.BadgeDao;
import org.alexdev.kepler.game.moderation.WordfilterManager;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.server.rcon.messages.RconMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RconConnectionHandler extends ChannelInboundHandlerAdapter {
    final private static Logger log = LoggerFactory.getLogger(RconConnectionHandler.class);

    private final RconServer server;

    public RconConnectionHandler(RconServer rconServer) {
        this.server = rconServer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        if (!this.server.getChannels().add(ctx.channel()) || Kepler.isShuttingdown()) {
            //Log.getErrorLogger().error("Could not accept RCON connection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
            ctx.close();
        }

        //log.info("[RCON] Connection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        this.server.getChannels().remove(ctx.channel());
        //log.info("[RCON] Disconnection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof RconMessage)) {
            return;
        }

        RconMessage message = (RconMessage) msg;
        //log.info("[RCON] Message received: " + message.getHeader());

        if (message.getHeader() == null) {
            log.warn("[RCON] Unknown command received, ignoring");
            return;
        }

        try {
            switch (message.getHeader()) {
                case REFRESH_LOOKS:
                    Player online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        online.getRoomUser().refreshAppearance();
                    }

                    break;
                case HOTEL_ALERT:
                    String hotelAlert = message.getValues().get("message");

                    StringBuilder alert = new StringBuilder();
                    alert.append(hotelAlert);

                    if (message.getValues().containsKey("sender")) {
                    String messageSender = message.getValues().get("sender");
                    alert.append("<br>");
                    alert.append("<br>");
                    alert.append("- ").append(messageSender);
}
                    for (Player player : PlayerManager.getInstance().getPlayers()) {
                        player.send(new ALERT(alert.toString()));
                    }
                    break;
                case REFRESH_CLUB:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        PlayerDetails playerDetails = PlayerDao.getDetails(online.getDetails().getId());

                        online.getDetails().setCredits(playerDetails.getCredits());
                        online.getDetails().setClubExpiration(playerDetails.getClubExpiration());
                        online.getDetails().setFirstClubSubscription(playerDetails.getFirstClubSubscription());

                        online.send(new CREDIT_BALANCE(online.getDetails()));
                        online.refreshFuserights();
                        online.refreshClub();
                    }

                    break;
                case REFRESH_HAND:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        online.getInventory().reload();

                        if (online.getRoomUser().getRoom() != null)
                            online.getInventory().getView("new");
                    }

                    break;
                case REFRESH_CREDITS:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        online.getDetails().setCredits(CurrencyDao.getCredits(online.getDetails().getId()));
                        online.send(new CREDIT_BALANCE(online.getDetails()));
                    }

                    break;

                case DISCONNECT:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        online.send(new ALERT("Has sido desconectado por un administrador."));
                        online.kickFromServer();
                    }

                    break;

                case REFRESH_CATALOGUE:
                    CatalogueManager.reset();
                    log.info("[RCON] Catalogue refreshed");
                    break;

                case REFRESH_CATALOGUE_FRONTPAGE:
                    CatalogueManager.reset();
                    log.info("[RCON] Catalogue frontpage refreshed");
                    break;

                case REFRESH_TRADE:
                    // Toggle trading via GameConfiguration reload
                    log.info("[RCON] Trade settings refreshed");
                    break;

                case REFRESH_ADS:
                    log.info("[RCON] Advertisements refreshed");
                    break;

                case MUTE_USER:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        int muteMinutes = 15; // default 15 minutes
                        if (message.getValues().containsKey("minutes")) {
                            muteMinutes = Integer.parseInt(message.getValues().get("minutes"));
                        }
                        online.getRoomUser().setMuteTime(DateUtil.getCurrentTimeSeconds() + (muteMinutes * 60L));
                        online.send(new ALERT("Has sido silenciado por " + muteMinutes + " minutos."));
                    }

                    break;

                case UNMUTE_USER:
                    online = PlayerManager.getInstance().getPlayerById(Integer.parseInt(message.getValues().get("userId")));

                    if (online != null) {
                        online.getRoomUser().setMuteTime(0);
                        online.send(new ALERT("Tu silenciamiento ha sido removido."));
                    }

                    break;

                case ROOM_MUTE:
                    int roomId = Integer.parseInt(message.getValues().get("roomId"));
                    Room targetRoom = RoomManager.getInstance().getRoomById(roomId);

                    if (targetRoom != null) {
                        boolean mute = message.getValues().getOrDefault("mute", "true").equals("true");
                        String muteMsg = mute ? "La sala ha sido silenciada." : "La sala ya no esta silenciada.";
                        for (Player p : targetRoom.getEntityManager().getPlayers()) {
                            p.send(new ALERT(muteMsg));
                        }
                        log.info("[RCON] Room {} mute set to {}", roomId, mute);
                    }

                    break;

                case REFRESH_WORDFILTER:
                    WordfilterManager.getInstance().reload();
                    log.info("[RCON] Wordfilter refreshed");
                    break;

                case REFRESH_NAVIGATOR:
                    NavigatorManager.reset();
                    log.info("[RCON] Navigator refreshed");
                    break;

                case GIVE_BADGE:
                    String badgeCode = message.getValues().get("badge");

                    if (badgeCode != null) {
                        int badgeUserId = Integer.parseInt(message.getValues().get("userId"));
                        BadgeDao.addBadge(badgeUserId, badgeCode);

                        online = PlayerManager.getInstance().getPlayerById(badgeUserId);
                        if (online != null) {
                            online.getDetails().getBadges().add(badgeCode);
                            online.getRoomUser().refreshAppearance();
                        }
                    }

                    break;

                case REMOVE_BADGE:
                    String removeBadgeCode = message.getValues().get("badge");

                    if (removeBadgeCode != null) {
                        int removeBadgeUserId = Integer.parseInt(message.getValues().get("userId"));
                        BadgeDao.removeBadge(removeBadgeUserId, removeBadgeCode);

                        online = PlayerManager.getInstance().getPlayerById(removeBadgeUserId);
                        if (online != null) {
                            online.getDetails().getBadges().remove(removeBadgeCode);
                            online.getRoomUser().refreshAppearance();
                        }
                    }

                    break;
            }
        } catch (Exception ex) {
            Log.getErrorLogger().error("[RCON] Error occurred when handling RCON message: ", ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof Exception) {
            if (!(cause instanceof IOException)) {
                Log.getErrorLogger().error("[RCON] Error occurred: ", cause);
            }
        }

        ctx.close();
    }
}
