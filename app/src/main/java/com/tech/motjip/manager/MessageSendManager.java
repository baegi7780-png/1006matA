package com.tech.motjip.manager;

import android.util.Log;
import android.widget.Toast;

import com.tech.motjip.MessageActivity;

import org.json.JSONObject;

import ua.naiksoftware.stomp.StompClient;

public class MessageSendManager {

    private static final String TAG =
            "MessageSendManager";

    private static final String CHAT_REALTIME =
            "CHAT_REALTIME";

    private final MessageActivity activity;
    private final long roomId;

    public MessageSendManager(
            MessageActivity activity,
            long roomId
    ) {

        this.activity =
                activity;

        this.roomId =
                roomId;
    }

    public void sendMessage(
            StompClient stompClient,
            Long currentUserId,
            String currentNickname,
            String content,
            String messageType,
            String fileUrl,
            Runnable onDisconnected
    ) {

        try {

            Log.e(
                    CHAT_REALTIME,
                    "SEND_MANAGER_START roomId="
                            + roomId
                            + ", userId="
                            + currentUserId
                            + ", content="
                            + content
            );

            if (currentUserId == null
                    || currentUserId <= 0) {

                Toast.makeText(
                        activity,
                        "사용자 정보를 불러오는 중입니다.",
                        Toast.LENGTH_SHORT
                ).show();

                Log.e(
                        CHAT_REALTIME,
                        "SEND_MANAGER_FAIL_USER_NULL"
                );

                return;
            }

            if (stompClient == null
                    || !stompClient.isConnected()) {

                Toast.makeText(
                        activity,
                        "서버와 연결이 끊겨 메시지를 보낼 수 없습니다.",
                        Toast.LENGTH_SHORT
                ).show();

                Log.e(
                        CHAT_REALTIME,
                        "SEND_MANAGER_FAIL_SOCKET_DISCONNECTED"
                );

                if (onDisconnected != null) {

                    onDisconnected.run();
                }

                return;
            }

            JSONObject payload =
                    new JSONObject();

            payload.put(
                    "roomId",
                    roomId
            );

            payload.put(
                    "senderId",
                    currentUserId
            );

            payload.put(
                    "senderNickname",
                    currentNickname
            );

            payload.put(
                    "messageContent",
                    content
            );

            payload.put(
                    "messageType",
                    messageType
            );

            if (fileUrl != null) {

                payload.put(
                        "fileUrl",
                        fileUrl
                );
            }

            Log.e(
                    CHAT_REALTIME,
                    "SEND_MANAGER_PAYLOAD="
                            + payload
            );

            stompClient.send(
                    "/pub/chat/message",
                    payload.toString()
            ).subscribe(
                    () -> {

                        Log.e(
                                CHAT_REALTIME,
                                "SEND_MANAGER_SUCCESS destination=/pub/chat/message"
                        );
                    },
                    throwable -> {

                        Log.e(
                                TAG,
                                "메시지 전송 실패",
                                throwable
                        );

                        Log.e(
                                CHAT_REALTIME,
                                "SEND_MANAGER_ERROR",
                                throwable
                        );

                        if (onDisconnected != null) {

                            onDisconnected.run();
                        }
                    }
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "메시지 전송 실패",
                    e
            );

            Log.e(
                    CHAT_REALTIME,
                    "SEND_MANAGER_EXCEPTION",
                    e
            );

            if (onDisconnected != null) {

                onDisconnected.run();
            }
        }
    }
}