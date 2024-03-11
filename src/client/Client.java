package client;

import connection.Connection;
import connection.Message;
import connection.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    private Connection connection;
    private static ModelGuiClient model;
    private static ViewGuiClient gui;
    private volatile boolean isConnect = false; //���� ������������ ��������� ����������� �������  �������

    private String userName;

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

    //����� ����� � ���������� ����������
    public static void main(String[] args) {
        Client client = new Client();
        model = new ModelGuiClient();
        gui = new ViewGuiClient(client);
        gui.initFrameClient();
        while (true) {
            if (client.isConnect()) {
                client.nameUserRegistration();
                client.receiveMessageFromServer();
                client.setConnect(false);
            }
        }
    }

    //����� ����������� �������  �������
    protected void connectToServer() {
        //���� ������ �� ���������  ������� ��..
        if (!isConnect) {
            while (true) {
                try {
                    //�������� ���� ����� ������, ����� �������
                    String addressServer = gui.getServerAddressFromOptionPane();
                    int port = gui.getPortServerFromOptionPane();
                    //������� ����� � ������ connection
                    Socket socket = new Socket(addressServer, port);
                    connection = new Connection(socket);
                    isConnect = true;
                    gui.addMessage("��������� ���������: �� ������������ � �������.\n");
                    break;
                } catch (Exception e) {
                    gui.errorDialogWindow("��������� ������! �������� �� ����� �� ������ ����� ������� ��� ����. ���������� ��� ���");
                    break;
                }
            }
        } else gui.errorDialogWindow("�� ��� ����������!");
    }

    //�����, ����������� ����������� ����� ������������ �� ������� ����������� ����������
    protected void nameUserRegistration() {
        while (true) {
            try {
                Message message = connection.receive();
                //������� �� ������� ���������, ���� ��� ������ �����, �� �������� ���� ����� �����, ���������� �� ������ ���
                if (message.getTypeMessage() == MessageType.REQUEST_NAME_USER) {
                    String nameUser = gui.getNameUser();
                    this.userName = nameUser;
                    connection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //���� ��������� - ��� ��� ������������, ������� ��������������� ���� � ������, ��������� ���� �����
                if (message.getTypeMessage() == MessageType.NAME_USED) {
                    gui.errorDialogWindow("������ ��� ��� ������������, ������� ������");
                    String nameUser = gui.getNameUser();
                    connection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //���� ��� �������, �������� ��������� ���� �������������� �������������, ������� �� �����
                if (message.getTypeMessage() == MessageType.NAME_ACCEPTED) {
                    gui.addMessage("��������� ���������: ���� ��� �������!\n");
                    model.setUsers(message.getListUsers());
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                gui.errorDialogWindow("��������� ������ ��� ����������� �����. ���������� ����������������");
                try {
                    connection.close();
                    isConnect = false;
                    break;
                } catch (IOException ex) {
                    gui.errorDialogWindow("������ ��� �������� ����������");
                }
            }

        }
    }

    //����� �������� ��������� ���������������� ��� ������ ������������� �� ������
    protected void sendMessageOnServer(String text) {
        try {
            connection.send(new Message(MessageType.TEXT_MESSAGE, text));
        } catch (Exception e) {
            gui.errorDialogWindow("������ ��� �������� ���������");
        }
    }

    //����� ����������� � ������� �������� �� ������ ��������
    protected void receiveMessageFromServer() {
        while (isConnect) {
            try {
                Message message = connection.receive();
                //���� ��� TEXT_MESSAGE, �� ��������� ����� ��������� � ���� ���������
                if (message.getTypeMessage() == MessageType.TEXT_MESSAGE) {
                    gui.addMessage(message.getTextMessage());
                    if (!this.userName.equals(extractNameFromMessage(message.getTextMessage()))) {
                        String cmd = "msg * " + "\"" + message.getTextMessage() + "\"";
                        Runtime.getRuntime().exec(cmd);
                    }
                }
                //���� ��������� � ���� USER_ADDED ��������� ��������� � ���� ��������� � ����� ������������
                if (message.getTypeMessage() == MessageType.USER_ADDED) {
                    model.addUser(message.getTextMessage());
                    gui.refreshListUsers(model.getUsers());
                    gui.addMessage(String.format("��������� ���������: ������������ %s ������������� � ����.\n", message.getTextMessage()));
                }
                //���������� ��� ���������� ������ �������������
                if (message.getTypeMessage() == MessageType.REMOVED_USER) {
                    model.removeUser(message.getTextMessage());
                    gui.refreshListUsers(model.getUsers());
                    gui.addMessage(String.format("��������� ���������: ������������ %s ������� ���.\n", message.getTextMessage()));
                }
            } catch (Exception e) {
                gui.errorDialogWindow("������ ��� ������ ��������� �� �������.");
                setConnect(false);
                gui.refreshListUsers(model.getUsers());
                break;
            }
        }
    }

    //����� ����������� ���������� ������ ������� �� ����
    protected void disableClient() {
        try {
            if (isConnect) {
                connection.send(new Message(MessageType.DISABLE_USER));
                model.getUsers().clear();
                isConnect = false;
                gui.refreshListUsers(model.getUsers());
            } else gui.errorDialogWindow("�� ��� ���������.");
        } catch (Exception e) {
            gui.errorDialogWindow("��������� ���������: ��������� ������ ��� ����������.");
        }
    }

    private String extractNameFromMessage (String message) {
        return message.split(":")[0];
    }
}
