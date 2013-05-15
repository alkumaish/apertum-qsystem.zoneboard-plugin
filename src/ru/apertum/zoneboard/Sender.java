/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.zoneboard;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Scanner;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.cmd.AJsonRPC20;
import ru.apertum.qsystem.common.cmd.JsonRPC20Error;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.model.INetProperty;

/**
 * Копия NetCommander. Нужен для распараллелевания рассылки на зональные сервера
 * @author Evgeniy Egorov
 */
public class Sender {
    
    public String sendRpc(INetProperty netProperty, AJsonRPC20 jsonRpc) throws QException {
        final String message;
        Gson gson = GsonPool.getInstance().borrowGson();
        try {
            message = gson.toJson(jsonRpc);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        QLog.l().logger().trace("Задание \"" + jsonRpc.getMethod() + "\" на " + netProperty.getAddress().getHostAddress() + ":" + netProperty.getPort() + "#\n" + message);
        final String data;
        try {
            final PrintWriter writer;
            final Scanner in;
            try (Socket socket = new Socket(netProperty.getAddress(), netProperty.getPort())) {
                QLog.l().logger().trace("Создали Socket.");
                writer = new PrintWriter(socket.getOutputStream());
                writer.print(URLEncoder.encode(message, "utf-8"));
                QLog.l().logger().trace("Высылаем задание.");
                writer.flush();
                QLog.l().logger().trace("Читаем ответ ...");
                StringBuilder sb = new StringBuilder();
                in = new Scanner(socket.getInputStream());
                while (in.hasNextLine()) {
                    sb = sb.append(in.nextLine()).append("\n");
                }
                data = URLDecoder.decode(sb.toString(), "utf-8");
            }
            writer.close();
            in.close();
            QLog.l().logger().trace("Ответ:\n" + data);
        } catch (IOException ex) {
            throw new QException("Невозможно получить ответ от сервера. ", ex);
        }
        gson = GsonPool.getInstance().borrowGson();
        try {
            final JsonRPC20Error rpc = gson.fromJson(data, JsonRPC20Error.class);
            if (rpc == null) {
                throw new QException("Ошибка на сервере не позволила сформировать ответ.");
            }
            if (rpc.getError() != null) {
                throw new QException("Выполнение задания произошло с ошибкой. " + rpc.getError().getCode() + ":" + rpc.getError().getMessage());
            }
        } catch (JsonParseException ex) {
            throw new QException("Не возможно интерпритировать ответ.\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return data;
    }
    
}
