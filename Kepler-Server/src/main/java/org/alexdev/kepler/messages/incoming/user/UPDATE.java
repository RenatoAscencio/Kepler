package org.alexdev.kepler.messages.incoming.user;

import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPDATE implements MessageEvent {
    private static final Logger log = LoggerFactory.getLogger(UPDATE.class);

    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        if (!player.isLoggedIn()) {
            return;
        }

        var registerValues = PlayerManager.getInstance().getRegisterValues();

        while (reader.remainingBytes().length > 0) {
            var valueId = reader.readBase64();

            if (!registerValues.containsKey(valueId)) {
                log.info("Stopped profile update parse for user {} at unknown register field {}",
                        player.getDetails().getId(), valueId);
                break;
            }

            var value = registerValues.get(valueId);

            switch (value.getDataType()) {
                case STRING:
                {
                    value.setValue(reader.readString());
                    break;
                }
                case BOOLEAN:
                {
                    value.setFlag(reader.readBytes(1)[0] == 'A');
                    break;
                }
            }
        }

        Object directMail = PlayerManager.getInstance().getRegisterValue(registerValues, "directMail");
        boolean updatedDirectMail = false;
        if (directMail != null) {
            player.getDetails().setReceiveNews((boolean) directMail);
            PlayerDao.saveReceiveMail(player.getDetails());
            updatedDirectMail = true;
        }

        Object motto = PlayerManager.getInstance().getRegisterValue(registerValues, "customData");
        boolean updatedMotto = false;
        if (motto != null) {
            player.getDetails().setMotto((String) motto);
            updatedMotto = true;
        }

        Object figure = PlayerManager.getInstance().getRegisterValue(registerValues, "figure");
        boolean updatedFigure = false;
        if (figure != null) {
            player.getDetails().setFigure((String) figure);
            updatedFigure = true;
        }

        Object sex = PlayerManager.getInstance().getRegisterValue(registerValues, "sex");
        boolean updatedSex = false;
        if (sex != null && ((String) sex).length() > 0) {
            player.getDetails().setSex(Character.toUpperCase(((String) sex).toCharArray()[0]));
            updatedSex = true;
        }

        PlayerDao.saveDetails(player.getDetails());
        PlayerDao.saveMotto(player.getDetails());

        log.info("Saved profile update for user {} (figure={}, motto={}, sex={}, directMail={})",
                player.getDetails().getId(), updatedFigure, updatedMotto, updatedSex, updatedDirectMail);

        new GET_INFO().handle(player, null);

        if (player.getRoomUser().getRoom() != null) {
            player.getRoomUser().refreshAppearance();
        }
    }
}
