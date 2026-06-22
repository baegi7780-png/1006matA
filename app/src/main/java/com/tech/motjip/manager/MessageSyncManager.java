package com.tech.motjip.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tech.motjip.Adapter.MessageAdapter;
import com.tech.motjip.Model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageSyncManager {

    private static final String TAG =
            "MessageSyncManager";

    private MessageAdapter messageAdapter;

    private List<Message> messageList =
            new ArrayList<>();

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    public MessageSyncManager(
            @NonNull MessageAdapter messageAdapter
    ) {

        this.messageAdapter =
                messageAdapter;
    }

    public void setAdapter(
            @NonNull MessageAdapter messageAdapter
    ) {

        this.messageAdapter =
                messageAdapter;

        submitList();

        Log.d(
                TAG,
                "Adapter 교체 후 메시지 재동기화 size="
                        + messageList.size()
        );
    }

    public void setInitialMessages(
            List<Message> serverMessages
    ) {

        List<Message> newList =
                new ArrayList<>();

        if (serverMessages != null) {

            for (Message message : serverMessages) {

                if (message != null) {

                    newList.add(
                            copyMessage(
                                    message
                            )
                    );
                }
            }
        }

        messageList =
                newList;

        submitList();

        Log.d(
                TAG,
                "초기 메시지 동기화 완료 size="
                        + messageList.size()
        );
    }

    public void mergeMessages(
            List<Message> serverMessages
    ) {

        List<Message> mergedList =
                copyMessageList(
                        messageList
                );

        if (serverMessages != null) {

            for (Message serverMessage : serverMessages) {

                if (serverMessage == null) {

                    continue;
                }

                int index =
                        findMessageIndexFromList(
                                mergedList,
                                serverMessage
                        );

                if (index >= 0) {

                    mergedList.set(
                            index,
                            copyMessage(
                                    serverMessage
                            )
                    );

                    Log.d(
                            TAG,
                            "서버 메시지 병합 갱신 id="
                                    + serverMessage.getId()
                    );

                } else {

                    mergedList.add(
                            copyMessage(
                                    serverMessage
                            )
                    );

                    Log.d(
                            TAG,
                            "서버 메시지 병합 추가 id="
                                    + serverMessage.getId()
                    );
                }
            }
        }

        messageList =
                mergedList;

        submitList();

        Log.d(
                TAG,
                "메시지 병합 완료 size="
                        + messageList.size()
        );
    }

    public void clearMessages() {

        messageList =
                new ArrayList<>();

        submitList();

        Log.d(
                TAG,
                "메시지 목록 초기화 완료"
        );
    }

    public void addOrUpdateMessage(
            Message newMessage
    ) {

        if (newMessage == null) {

            return;
        }

        List<Message> newList =
                copyMessageList(
                        messageList
                );

        int index =
                findMessageIndexFromList(
                        newList,
                        newMessage
                );

        if (index >= 0) {

            newList.set(
                    index,
                    copyMessage(
                            newMessage
                    )
            );

            Log.d(
                    TAG,
                    "기존 메시지 갱신 id="
                            + newMessage.getId()
            );

        } else {

            newList.add(
                    copyMessage(
                            newMessage
                    )
            );

            Log.d(
                    TAG,
                    "신규 메시지 추가 id="
                            + newMessage.getId()
            );
        }

        messageList =
                newList;

        submitList();
    }

    public void updateReadCount(
            Long messageId,
            Integer unreadCount
    ) {

        if (messageId == null
                || unreadCount == null) {

            return;
        }

        List<Message> newList =
                copyMessageList(
                        messageList
                );

        for (int i = 0;
             i < newList.size();
             i++) {

            Message message =
                    newList.get(
                            i
                    );

            if (message == null
                    || message.getId() == null
                    || !message.getId()
                    .equals(
                            messageId
                    )) {

                continue;
            }

            if (message.getUnreadCount()
                    == unreadCount) {

                Log.d(
                        TAG,
                        "읽음 카운트 동일, 갱신 생략 messageId="
                                + messageId
                                + ", unreadCount="
                                + unreadCount
                );

                return;
            }

            Message copiedMessage =
                    copyMessage(
                            message
                    );

            copiedMessage.setUnreadCount(
                    unreadCount
            );

            newList.set(
                    i,
                    copiedMessage
            );

            messageList =
                    newList;

            Log.d(
                    TAG,
                    "읽음 카운트 갱신 messageId="
                            + messageId
                            + ", unreadCount="
                            + unreadCount
            );

            submitList();

            return;
        }

        Log.d(
                TAG,
                "읽음 카운트 갱신 대상 없음 messageId="
                        + messageId
        );
    }

    public void updateRoomReadCounts(
            List<Message> updatedMessages
    ) {

        if (updatedMessages == null
                || updatedMessages.isEmpty()) {

            return;
        }

        List<Message> newList =
                copyMessageList(
                        messageList
                );

        boolean changed =
                false;

        for (Message updated : updatedMessages) {

            if (updated == null
                    || updated.getId() == null) {

                continue;
            }

            int index =
                    findMessageIndexFromList(
                            newList,
                            updated
                    );

            if (index >= 0) {

                newList.set(
                        index,
                        copyMessage(
                                updated
                        )
                );

                changed =
                        true;
            }
        }

        if (changed) {

            messageList =
                    newList;

            submitList();

            Log.d(
                    TAG,
                    "방 전체 읽음 상태 동기화 완료"
            );
        }
    }

    public List<Message> getCurrentMessages() {

        return copyMessageList(
                messageList
        );
    }

    public int getMessageCount() {

        return messageList.size();
    }

    private int findMessageIndex(
            Message target
    ) {

        return findMessageIndexFromList(
                messageList,
                target
        );
    }

    private int findMessageIndexFromList(
            List<Message> targetList,
            Message target
    ) {

        if (targetList == null
                || target == null
                || target.getId() == null) {

            return -1;
        }

        for (int i = 0;
             i < targetList.size();
             i++) {

            Message message =
                    targetList.get(
                            i
                    );

            if (message != null
                    && message.getId() != null
                    && message.getId()
                    .equals(
                            target.getId()
                    )) {

                return i;
            }
        }

        return -1;
    }

    private List<Message> copyMessageList(
            List<Message> sourceList
    ) {

        List<Message> copiedList =
                new ArrayList<>();

        if (sourceList == null) {

            return copiedList;
        }

        for (Message message : sourceList) {

            if (message != null) {

                copiedList.add(
                        copyMessage(
                                message
                        )
                );
            }
        }

        return copiedList;
    }

    private Message copyMessage(
            Message original
    ) {

        Message copiedMessage =
                new Message();

        copiedMessage.setId(
                original.getId()
        );

        copiedMessage.setMessageContent(
                original.getMessageContent()
        );

        copiedMessage.setSenderId(
                original.getSenderId()
        );

        copiedMessage.setSenderNickname(
                original.getSenderNickname()
        );

        copiedMessage.setSenderProfileImage(
                original.getSenderProfileImage()
        );

        copiedMessage.setRoomId(
                original.getRoomId()
        );

        copiedMessage.setMessageType(
                original.getMessageType()
        );

        copiedMessage.setFileUrl(
                original.getFileUrl()
        );

        copiedMessage.setSentAt(
                original.getSentAt()
        );

        copiedMessage.setViewType(
                original.getViewType()
        );

        copiedMessage.setUnreadCount(
                original.getUnreadCount()
        );

        return copiedMessage;
    }

    private void submitList() {

        if (messageAdapter == null) {

            return;
        }

        List<Message> submitTarget =
                copyMessageList(
                        messageList
                );

        mainHandler.post(() -> {

            if (messageAdapter == null) {

                return;
            }

            messageAdapter.submitList(
                    submitTarget
            );

            Log.d(
                    TAG,
                    "submitList 실행 size="
                            + submitTarget.size()
            );
        });
    }
}