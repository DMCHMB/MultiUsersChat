package server;

import connection.Connection;
import connection.Message;
import connection.MessageType;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Server {
    private ServerSocket serverSocket;
    private static ModelGuiServer model;
    private static volatile boolean isServerStart = false;

    // метод, запускающий сервер
    protected void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isServerStart = true;
            System.out.println("Сервер запущен.");
        } catch (Exception e) {
            System.out.println("Не удалось запустить сервер.");
        }
    }

    // метод останавливающий сервер
    protected void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
                    user.getValue().close();
                }
                serverSocket.close();
                model.getAllUsersMultiChat().clear();
                System.out.println("Сервер остановлен.");
            } else {
                System.out.println("Сервер не запущен - останавливать нечего!");
            }
        } catch (Exception e) {
            System.out.println("Остановить сервер не удалось.");
        }
    }

    // метод, в котором в бесконечном цикле сервер принимает новое сокетное подключение от клиента
    protected void acceptServer() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
            } catch (Exception e) {
                System.out.println("Связь с сервером потеряна.");
                break;
            }
        }
    }

    // метод, рассылающий заданное сообщение всем клиентам из мапы
    protected void sendMessageAllUsers(Message message) {
        for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
            try {
                user.getValue().send(message);
            } catch (Exception e) {
                System.out.println("Ошибка отправки сообщения всем пользователям!");
            }
        }
    }

    // точка входа для приложения сервера
    public static void main(String[] args) {
        Server server = new Server();
        model = new ModelGuiServer();

        // порт, который будет использоваться сервером
        int port = 1111;

        // запуск сервера в фоновом режиме
        server.startServer(port);
        server.acceptServer();
    }

    // класс-поток, который запускается при принятии сервером нового сокетного соединения с клиентом
    private class ServerThread extends Thread {
        private Socket socket;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        private String requestAndAddingUser(Connection connection) {
            while (true) {
                try {
                    connection.send(new Message(MessageType.REQUEST_NAME_USER));
                    Message responseMessage = connection.receive();
                    String userName = responseMessage.getTextMessage();

                    if (responseMessage.getTypeMessage() == MessageType.USER_NAME && userName != null && !userName.isEmpty() && !model.getAllUsersMultiChat().containsKey(userName)) {
                        model.addUser(userName, connection);
                        Set<String> listUsers = new HashSet<>();
                        for (Map.Entry<String, Connection> users : model.getAllUsersMultiChat().entrySet()) {
                            listUsers.add(users.getKey());
                        }
                        connection.send(new Message(MessageType.NAME_ACCEPTED, listUsers));
                        sendMessageAllUsers(new Message(MessageType.USER_ADDED, userName));
                        return userName;
                    } else {
                        connection.send(new Message(MessageType.NAME_USED));
                    }
                } catch (Exception e) {
                    System.out.println("Возникла ошибка при запросе и добавлении нового пользователя");
                }
            }
        }

        private void messagingBetweenUsers(Connection connection, String userName) {
            while (true) {
                try {
                    Message message = connection.receive();
                    if (message.getTypeMessage() == MessageType.TEXT_MESSAGE) {
                        String textMessage = String.format("%s: %s\n", userName, message.getTextMessage());
                        sendMessageAllUsers(new Message(MessageType.TEXT_MESSAGE, textMessage));
                    }
                    if (message.getTypeMessage() == MessageType.DISABLE_USER) {
                        sendMessageAllUsers(new Message(MessageType.REMOVED_USER, userName));
                        model.removeUser(userName);
                        connection.close();
                        System.out.printf("Пользователь с удаленным доступом %s отключился.\n", socket.getRemoteSocketAddress());
                        break;
                    }
                } catch (Exception e) {
                    System.out.printf("Произошла ошибка при рассылке сообщения от пользователя %s, либо отключился!\n", userName);
                    break;
                }
            }
        }

        @Override
        public void run() {
            System.out.printf("Подключился новый пользователь с удаленным сокетом - %s.\n", socket.getRemoteSocketAddress());
            try {
                Connection connection = new Connection(socket);
                String nameUser = requestAndAddingUser(connection);
                messagingBetweenUsers(connection, nameUser);
            } catch (Exception e) {
                System.out.println("Произошла ошибка при рассылке сообщения от пользователя!");
            }
        }
    }
}