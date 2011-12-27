/*
 * Copyright (C) 2011 Evgeniy Egorov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.zoneboard.plugins.events;

import java.net.InetAddress;
import java.net.UnknownHostException;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.cmd.RpcToZoneServer;
import ru.apertum.qsystem.common.model.INetProperty;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IChangeCustomerStateEvent;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.zoneboard.plugins.IZoneBoardSenderPluginUID;
import ru.apertum.zoneboard.plugins.pinger.PingResult;

/**
 * Плагин во время смены статуса клиенту отсылает статистику в сервер
 * отображений зональных табло по сети
 * @author Evgeniy Egorov
 */
public class EventSender implements IChangeCustomerStateEvent, IZoneBoardSenderPluginUID {

    @Override
    public void change(QCustomer qc, CustomerState cs, Long newServiceId) {
        //System.out.println("ZoneBoardSenderPlugin: IChangeCustomerStateEvent");
        if (!PingResult.getInstance().isReady()) {
           // QLog.l().logger().error("Версия плагина \"ZoneboardPlugin\" не сообветствует версии зональго сервера ввыда инфмации.");
            return;
        }
        // Создаем событие
        // Отсылаем событие
        String cmdName = "kill";
        switch (cs) {
            case STATE_INVITED:
                cmdName = "show";
                break;
            case STATE_INVITED_SECONDARY:
                cmdName = "show";
                break;
            case STATE_WORK:
                cmdName = "work";
                break;
            case STATE_WORK_SECONDARY:
                cmdName = "work";
                break;
            case STATE_DEAD:
                break;
            case STATE_FINISH:
                break;    
            case STATE_POSTPONED:
                break;   
            case STATE_REDIRECT:
                break;        
            default:// нужная вещь. чтобы отсечь состояния, которые не при чем в зональном табло
                return;

        }

        final RpcToZoneServer params = new RpcToZoneServer(new RpcToZoneServer.Data(qc.getUser().getName(), qc.getUser().getPoint(), qc.getPrefix(), qc.getNumber(), qc.getUser().getAdressRS()));
        params.setMethod(cmdName);
        try {
            NetCommander.sendRpc(netProperty, params);
        } catch (Exception ex) {// вывод исключений
          //  QLog.l().logger().error("Проблема с командой. ", ex);
        }
    }
    final public INetProperty netProperty = new INetProperty() {

        @Override
        public Integer getPort() {
            return ServerProps.getInstance().getProps().getZoneBoardServPort();
        }

        @Override
        public InetAddress getAddress() {
            try {
                return InetAddress.getByName(ServerProps.getInstance().getProps().getZoneBoardServAddr());
            } catch (UnknownHostException ex) {
              //  QLog.l().logger().error("Проблема с getAddress(). ", ex);
                throw new RuntimeException(ex);
            }
        }
    };

    @Override
    public String getDescription() {
        return "Плагин \"ZoneboardPlugin\" во время смены статуса клиенту отсылает статистику в зональный сервер отображения данных через сеть";
    }

    @Override
    public long getUID() {
        return UID;
    }
}
