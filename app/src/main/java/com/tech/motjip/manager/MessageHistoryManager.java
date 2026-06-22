package com.tech.motjip.manager;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.MessageActivity;
import com.tech.motjip.Model.Message;
import com.tech.motjip.db.AppDatabase;
import com.tech.motjip.db.entity.MessageEntity;
import com.tech.motjip.db.mapper.MessageMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageHistoryManager {

    private static final String TAG =
            "MessageHistory";

    private final MessageActivity activity;

    private final RecyclerView recyclerView;

    private final MessageSyncManager syncManager;

    private final MessageRoomStateManager roomStateManager;

    private final long roomId;

    private final AppDatabase database;

    private boolean isLoading =
            false;

    public MessageHistoryManager(
            MessageActivity activity,
            RecyclerView recyclerView,
            MessageSyncManager syncManager,
            MessageRoomStateManager roomStateManager,
            long roomId
    ) {

        this.activity =
                activity;

        this.recyclerView =
                recyclerView;

        this.syncManager =
                syncManager;

        this.roomStateManager =
                roomStateManager;

        this.roomId =
                roomId;

        database =
                AppDatabase.getInstance(
                        activity
                );
    }

    public void loadLocalMessagesFirst(
            Long currentUserId
    ) {

        new Thread(() -> {

            try {

                List<MessageEntity> entities =
                        database.messageDao()
                                .getMessagesByRoomId(
                                        roomId
                                );

                List<Message> localMessages =
                        MessageMapper.toModelList(
                                entities,
                                currentUserId
                        );

                activity.runOnUiThread(() -> {

                    if (localMessages.isEmpty()) {
                        return;
                    }

                    syncManager.mergeMessages(
                            localMessages
                    );

                    recyclerView.postDelayed(
                            this::scrollToBottom,
                            100
                    );

                    Log.d(
                            TAG,
                            "Room 캐시 메시지 병합 로드 roomId="
                                    + roomId
                                    + ", size="
                                    + localMessages.size()
                    );
                });

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Room 캐시 로드 실패",
                        e
                );
            }

        }).start();
    }

    public void loadChatHistory(
            Long currentUserId
    ) {

        if (isLoading) {

            Log.d(
                    TAG,
                    "이미 로딩 중"
            );

            return;
        }

        isLoading =
                true;

        Log.d(
                TAG,
                "loadChatHistory 시작 roomId = "
                        + roomId
        );

        RetrofitClient.getApiService(activity)
                .getChatMessages(roomId)
                .enqueue(new Callback<List<Message>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<Message>> call,
                            @NonNull Response<List<Message>> response
                    ) {

                        isLoading =
                                false;

                        Log.d(
                                TAG,
                                "getChatMessages 응답 code = "
                                        + response.code()
                        );

                        if (response.isSuccessful()
                                && response.body() != null) {

                            List<Message> historyList =
                                    response.body();

                            List<Message> newList =
                                    normalizeAndDeduplicateMessages(
                                            historyList,
                                            currentUserId
                                    );

                            syncManager.mergeMessages(
                                    newList
                            );

                            saveMessagesToRoom(
                                    newList
                            );

                            recyclerView.postDelayed(
                                    MessageHistoryManager.this::scrollToBottom,
                                    100
                            );

                            Log.d(
                                    TAG,
                                    "서버 채팅 내역 병합 로드 완료 size = "
                                            + newList.size()
                            );

                        } else {

                            Log.e(
                                    TAG,
                                    "채팅 내역 응답 실패 code = "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<Message>> call,
                            @NonNull Throwable t
                    ) {

                        isLoading =
                                false;

                        Log.e(
                                TAG,
                                "채팅 내역 로드 실패",
                                t
                        );
                    }
                });
    }

    private List<Message> normalizeAndDeduplicateMessages(
            List<Message> sourceList,
            Long currentUserId
    ) {

        List<Message> newList =
                new ArrayList<>();

        Set<Long> messageIdSet =
                new HashSet<>();

        if (sourceList == null) {
            return newList;
        }

        for (Message msg : sourceList) {

            if (msg == null) {
                continue;
            }

            Long senderId =
                    msg.getSenderId();

            if ("SYSTEM".equalsIgnoreCase(
                    msg.getMessageType()
            )) {

                msg.setViewType(
                        2
                );

            } else if (senderId != null
                    && senderId.equals(
                    currentUserId
            )) {

                msg.setViewType(
                        0
                );

            } else {

                msg.setViewType(
                        1
                );
            }

            Long messageId =
                    msg.getId();

            if (messageId != null) {

                if (messageIdSet.contains(
                        messageId
                )) {

                    continue;
                }

                messageIdSet.add(
                        messageId
                );
            }

            newList.add(
                    msg
            );
        }

        return newList;
    }

    private void saveMessagesToRoom(
            List<Message> messages
    ) {

        new Thread(() -> {

            try {

                List<MessageEntity> entities =
                        MessageMapper.toEntityList(
                                messages
                        );

                database.messageDao()
                        .insertMessages(
                                entities
                        );

                Log.d(
                        TAG,
                        "Room 메시지 저장 완료 roomId="
                                + roomId
                                + ", size="
                                + entities.size()
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Room 메시지 저장 실패",
                        e
                );
            }

        }).start();
    }

    public void saveSingleMessage(
            Message message
    ) {

        if (message == null) {
            return;
        }

        new Thread(() -> {

            try {

                MessageEntity entity =
                        MessageMapper.toEntity(
                                message
                        );

                if (entity == null
                        || entity.getId() <= 0) {

                    return;
                }

                database.messageDao()
                        .insertMessage(
                                entity
                        );

                Log.d(
                        TAG,
                        "실시간 메시지 Room 저장 완료 id="
                                + entity.getId()
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "실시간 메시지 Room 저장 실패",
                        e
                );
            }

        }).start();
    }

    public void appendMessage(
            Message message
    ) {

        if (message == null) {
            return;
        }

        syncManager.addOrUpdateMessage(
                message
        );

        saveSingleMessage(
                message
        );

        scrollToBottom();
    }

    public void scrollToBottom() {

        recyclerView.post(() -> {

            if (recyclerView.getAdapter() == null) {
                return;
            }

            int count =
                    recyclerView.getAdapter()
                            .getItemCount();

            if (count > 0) {

                recyclerView.scrollToPosition(
                        count - 1
                );
            }
        });
    }
}