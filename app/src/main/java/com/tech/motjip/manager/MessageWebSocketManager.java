package com.tech.motjip.manager;

import android.util.Log;

import com.tech.motjip.MessageActivity;
import com.tech.motjip.Model.Message;
import com.tech.motjip.manager.socket.SocketManager;

import org.json.JSONObject;

import ua.naiksoftware.stomp.StompClient;

public class MessageWebSocketManager {

    public interface MessageCallback {

        void onMessageReceived(
                Message message
        );
    }

    public interface ReadCallback {

        void onReadReceived(
                JSONObject jsonObject
        );
    }

    public interface RoomStateCallback {

        void onConnected();
    }

    private static final String TAG =
            "MessageWebSocketManager";

    private final MessageActivity activity;
    private final long roomId;

    private final SocketManager socketManager =
            SocketManager.getInstance();

    private MessageCallback messageCallback;
    private ReadCallback readCallback;
    private RoomStateCallback roomStateCallback;

    private final String messageSubscribeKey;
    private final String readSubscribeKey;
    private final String connectedCallbackKey;

    private boolean disconnected =
            false;

    public MessageWebSocketManager(
            MessageActivity activity,
            long roomId
    ) {

        this.activity =
                activity;

        this.roomId =
                roomId;

        messageSubscribeKey =
                "message_room_" + roomId;

        readSubscribeKey =
                "read_room_" + roomId;

        connectedCallbackKey =
                "connected_room_" + roomId;

        Log.e(
                "CHAT_REALTIME",
                "WS_MANAGER_CREATE roomId="
                        + roomId
                        + ", messageKey="
                        + messageSubscribeKey
                        + ", readKey="
                        + readSubscribeKey
                        + ", connectedKey="
                        + connectedCallbackKey
        );
    }

    public void setMessageCallback(
            MessageCallback messageCallback
    ) {

        this.messageCallback =
                messageCallback;

        Log.e(
                "CHAT_REALTIME",
                "WS_SET_MESSAGE_CALLBACK roomId="
                        + roomId
                        + ", callbackNull="
                        + (messageCallback == null)
        );
    }

    public void setReadCallback(
            ReadCallback readCallback
    ) {

        this.readCallback =
                readCallback;

        Log.e(
                "CHAT_REALTIME",
                "WS_SET_READ_CALLBACK roomId="
                        + roomId
                        + ", callbackNull="
                        + (readCallback == null)
        );
    }

    public void setRoomStateCallback(
            RoomStateCallback roomStateCallback
    ) {

        this.roomStateCallback =
                roomStateCallback;

        Log.e(
                "CHAT_REALTIME",
                "WS_SET_ROOM_STATE_CALLBACK roomId="
                        + roomId
                        + ", callbackNull="
                        + (roomStateCallback == null)
        );
    }

    public StompClient getStompClient() {

        return socketManager.getStompClient();
    }

    public void connect() {

        disconnected =
                false;

        Log.e(
                "CHAT_REALTIME",
                "WS_CONNECT_REQUEST roomId="
                        + roomId
                        + ", socketConnected="
                        + socketManager.isConnected()
        );

        socketManager.addConnectedCallback(
                connectedCallbackKey,
                () -> {

                    Log.e(
                            "CHAT_REALTIME",
                            "WEBSOCKET_CONNECTED roomId="
                                    + roomId
                                    + ", messageKey="
                                    + messageSubscribeKey
                    );

                    subscribeMessageTopic();

                    subscribeReadTopic();

                    if (roomStateCallback != null) {

                        Log.e(
                                "CHAT_REALTIME",
                                "ROOM_STATE_CALLBACK_EXECUTE roomId="
                                        + roomId
                        );

                        roomStateCallback.onConnected();

                    } else {

                        Log.e(
                                "CHAT_REALTIME",
                                "ROOM_STATE_CALLBACK_NULL roomId="
                                        + roomId
                        );
                    }
                }
        );

        socketManager.connect(
                activity
        );
    }

    private void subscribeMessageTopic() {

        Log.e(
                "CHAT_REALTIME",
                "SUBSCRIBE_MESSAGE_TOPIC roomId="
                        + roomId
                        + ", key="
                        + messageSubscribeKey
                        + ", topic=/sub/chat/room/"
                        + roomId
        );

        socketManager.subscribe(
                messageSubscribeKey,
                "/sub/chat/room/" + roomId,
                payload -> {

                    try {

                        Log.e(
                                "CHAT_REALTIME",
                                "RAW_PAYLOAD="
                                        + payload
                        );

                        JSONObject jsonObject =
                                new JSONObject(
                                        payload
                                );

                        Long senderId =
                                null;

                        if (jsonObject.has(
                                "senderId"
                        )
                                && !jsonObject.isNull(
                                "senderId"
                        )) {

                            senderId =
                                    jsonObject.optLong(
                                            "senderId"
                                    );
                        }

                        String text =
                                jsonObject.optString(
                                        "messageContent",
                                        ""
                                );

                        String nickname =
                                jsonObject.optString(
                                        "senderNickname",
                                        "익명"
                                );

                        String sentAt =
                                jsonObject.optString(
                                        "sentAt",
                                        ""
                                );

                        String messageType =
                                jsonObject.optString(
                                        "messageType",
                                        "TEXT"
                                );

                        String fileUrl =
                                null;

                        if (jsonObject.has(
                                "fileUrl"
                        )
                                && !jsonObject.isNull(
                                "fileUrl"
                        )) {

                            fileUrl =
                                    jsonObject.optString(
                                            "fileUrl",
                                            null
                                    );
                        }

                        int unreadCount =
                                jsonObject.optInt(
                                        "unreadCount",
                                        0
                                );

                        Message receivedMsg =
                                new Message(
                                        text,
                                        senderId,
                                        nickname,
                                        unreadCount
                                );

                        if (jsonObject.has(
                                "id"
                        )
                                && !jsonObject.isNull(
                                "id"
                        )) {

                            receivedMsg.setId(
                                    jsonObject.optLong(
                                            "id"
                                    )
                            );
                        }

                        String senderProfileImage =
                                null;

                        if (jsonObject.has(
                                "senderProfileImage"
                        )
                                && !jsonObject.isNull(
                                "senderProfileImage"
                        )) {

                            senderProfileImage =
                                    jsonObject.optString(
                                            "senderProfileImage",
                                            null
                                    );
                        }

                        receivedMsg.setSenderProfileImage(
                                senderProfileImage
                        );

                        receivedMsg.setRoomId(
                                roomId
                        );

                        receivedMsg.setSentAt(
                                sentAt
                        );

                        receivedMsg.setMessageType(
                                messageType
                        );

                        receivedMsg.setFileUrl(
                                fileUrl
                        );

                        receivedMsg.setUnreadCount(
                                unreadCount
                        );

                        Log.e(
                                "CHAT_REALTIME",
                                "MESSAGE_PARSED id="
                                        + receivedMsg.getId()
                                        + ", senderId="
                                        + receivedMsg.getSenderId()
                                        + ", type="
                                        + receivedMsg.getMessageType()
                                        + ", content="
                                        + receivedMsg.getMessageContent()
                                        + ", unreadCount="
                                        + receivedMsg.getUnreadCount()
                                        + ", profile="
                                        + receivedMsg.getSenderProfileImage()
                        );

                        if (messageCallback != null) {

                            Log.e(
                                    "CHAT_REALTIME",
                                    "CALLBACK_EXECUTE id="
                                            + receivedMsg.getId()
                                            + ", content="
                                            + receivedMsg.getMessageContent()
                            );

                            messageCallback.onMessageReceived(
                                    receivedMsg
                            );

                        } else {

                            Log.e(
                                    "CHAT_REALTIME",
                                    "MESSAGE_CALLBACK_NULL id="
                                            + receivedMsg.getId()
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "WebSocket 메시지 처리 실패",
                                e
                        );

                        Log.e(
                                "CHAT_REALTIME",
                                "MESSAGE_PARSE_ERROR",
                                e
                        );
                    }
                }
        );
    }

    private void subscribeReadTopic() {

        Log.e(
                "CHAT_REALTIME",
                "SUBSCRIBE_READ_TOPIC roomId="
                        + roomId
                        + ", key="
                        + readSubscribeKey
                        + ", topic=/sub/chat/room/"
                        + roomId
                        + "/read"
        );

        socketManager.subscribe(
                readSubscribeKey,
                "/sub/chat/room/" + roomId + "/read",
                payload -> {

                    try {

                        Log.e(
                                "CHAT_REALTIME",
                                "READ_RAW_PAYLOAD="
                                        + payload
                        );

                        JSONObject jsonObject =
                                new JSONObject(
                                        payload
                                );

                        if (readCallback != null) {

                            Log.e(
                                    "CHAT_REALTIME",
                                    "READ_CALLBACK_EXECUTE roomId="
                                            + roomId
                            );

                            readCallback.onReadReceived(
                                    jsonObject
                            );

                        } else {

                            Log.e(
                                    "CHAT_REALTIME",
                                    "READ_CALLBACK_NULL roomId="
                                            + roomId
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "읽음 이벤트 payload 파싱 실패",
                                e
                        );

                        Log.e(
                                "CHAT_REALTIME",
                                "READ_PARSE_ERROR",
                                e
                        );
                    }
                }
        );
    }

    public void scheduleReconnect() {

        Log.e(
                "CHAT_REALTIME",
                "WS_SCHEDULE_RECONNECT roomId="
                        + roomId
        );

        socketManager.connect(
                activity
        );
    }

    public void disconnect() {

        if (disconnected) {

            Log.e(
                    "CHAT_REALTIME",
                    "WS_ROOM_DISCONNECT_SKIP_ALREADY roomId="
                            + roomId
            );

            return;
        }

        disconnected =
                true;

        Log.e(
                "CHAT_REALTIME",
                "WS_ROOM_DISCONNECT_START roomId="
                        + roomId
        );

        socketManager.unsubscribe(
                messageSubscribeKey
        );

        socketManager.unsubscribe(
                readSubscribeKey
        );

        socketManager.removeConnectedCallback(
                connectedCallbackKey
        );

        Log.e(
                "CHAT_REALTIME",
                "WS_ROOM_UNSUBSCRIBE_CLEAR roomId="
                        + roomId
        );
    }

    public boolean isConnected() {

        boolean connected =
                socketManager.isConnected();

        Log.e(
                "CHAT_REALTIME",
                "WS_IS_CONNECTED roomId="
                        + roomId
                        + ", connected="
                        + connected
        );

        return connected;
    }
}