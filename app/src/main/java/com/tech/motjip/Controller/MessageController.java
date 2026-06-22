package com.tech.motjip.Controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Adapter.MessageAdapter;
import com.tech.motjip.Dto.ResponseDto.LoginResponseDto;
import com.tech.motjip.Handler.PreferenceManager;
import com.tech.motjip.MessageActivity;
import com.tech.motjip.Model.Message;
import com.tech.motjip.manager.MessageCacheManager;
import com.tech.motjip.manager.MessageHistoryManager;
import com.tech.motjip.manager.MessageImageManager;
import com.tech.motjip.manager.MessageReadManager;
import com.tech.motjip.manager.MessageRoomStateManager;
import com.tech.motjip.manager.MessageSendManager;
import com.tech.motjip.manager.MessageSyncManager;
import com.tech.motjip.manager.MessageWebSocketManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageController {

    private final MessageActivity activity;
    private final RecyclerView recyclerView;
    private final EditText etMessage;
    private final long roomId;

    private MessageAdapter adapter;
    private MessageSyncManager syncManager;

    private Long currentUserId;
    private String currentNickname;

    private final MessageRoomStateManager roomStateManager;
    private final MessageHistoryManager historyManager;
    private final MessageImageManager imageManager;
    private final MessageCacheManager cacheManager;
    private final MessageReadManager readManager;
    private final MessageSendManager sendManager;
    private final MessageWebSocketManager webSocketManager;

    private final String TAG = "MessageController";

    private boolean historyLoaded = false;
    private boolean shouldAutoScroll = true;

    public MessageController(
            MessageActivity activity,
            RecyclerView recyclerView,
            EditText etMessage,
            long roomId
    ) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        this.etMessage = etMessage;
        this.roomId = roomId;

        Log.e(
                "CHAT_REALTIME",
                "MESSAGE_CONTROLLER_CREATE roomId=" + roomId
        );

        roomStateManager =
                new MessageRoomStateManager(
                        activity,
                        roomId
                );

        cacheManager =
                new MessageCacheManager(
                        roomId
                );

        adapter =
                new MessageAdapter(
                        0L
                );

        syncManager =
                new MessageSyncManager(
                        adapter
                );

        recyclerView.setAdapter(
                adapter
        );

        disableRecyclerViewChangeAnimation();

        historyManager =
                new MessageHistoryManager(
                        activity,
                        recyclerView,
                        syncManager,
                        roomStateManager,
                        roomId
                );

        imageManager =
                new MessageImageManager(
                        activity
                );

        readManager =
                new MessageReadManager(
                        syncManager
                );

        sendManager =
                new MessageSendManager(
                        activity,
                        roomId
                );

        webSocketManager =
                new MessageWebSocketManager(
                        activity,
                        roomId
                );

        setupWebSocketCallbacks();
        setupKeyboardScrollListener();
        setupRecyclerScrollListener();
    }

    public void start() {

        Log.e(
                "CHAT_REALTIME",
                "MESSAGE_CONTROLLER_START roomId=" + roomId
        );

        fetchUserInfoAndStartChat();
    }

    private void disableRecyclerViewChangeAnimation() {

        RecyclerView.ItemAnimator animator =
                recyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {

            ((SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(
                            false
                    );
        }
    }

    private void setupWebSocketCallbacks() {

        Log.e(
                "CHAT_REALTIME",
                "SETUP_WEBSOCKET_CALLBACKS roomId=" + roomId
        );

        webSocketManager.setMessageCallback(message -> {

            Log.e(
                    "CHAT_REALTIME",
                    "CALLBACK_FROM_WS_BEFORE_UI messageNull="
                            + (message == null)
            );

            activity.runOnUiThread(() -> {

                if (message == null) {

                    Log.e(
                            "CHAT_REALTIME",
                            "UI_THREAD_MESSAGE_NULL"
                    );

                    return;
                }

                Log.e(
                        "CHAT_REALTIME",
                        "UI_THREAD_MESSAGE_RECEIVED id="
                                + message.getId()
                                + ", senderId="
                                + message.getSenderId()
                                + ", currentUserId="
                                + currentUserId
                                + ", type="
                                + message.getMessageType()
                                + ", content="
                                + message.getMessageContent()
                );

                if (message.getSenderId() != null
                        && currentUserId != null
                        && message.getSenderId().equals(
                        currentUserId
                )) {

                    message.setViewType(
                            0
                    );

                    Log.e(
                            "CHAT_REALTIME",
                            "MESSAGE_IS_MINE id="
                                    + message.getId()
                    );

                    if (replacePendingLocalMessageIfNeeded(
                            message
                    )) {

                        Log.e(
                                "CHAT_REALTIME",
                                "PENDING_LOCAL_MESSAGE_REPLACED id="
                                        + message.getId()
                        );

                        historyManager.saveSingleMessage(
                                message
                        );

                        saveCurrentMessagesToCache();

                        scrollToBottomAfterSend();

                        refreshRoomTitleIfMemberChanged(
                                message
                        );

                        return;
                    }

                } else {

                    message.setViewType(
                            "SYSTEM".equals(message.getMessageType())
                                    ? 2
                                    : 1
                    );

                    Log.e(
                            "CHAT_REALTIME",
                            "MESSAGE_IS_OTHER_OR_SYSTEM id="
                                    + message.getId()
                                    + ", viewType="
                                    + message.getViewType()
                    );
                }

                addMessageAndRefresh(
                        message
                );

                historyManager.saveSingleMessage(
                        message
                );

                refreshRoomTitleIfMemberChanged(
                        message
                );
            });
        });

        webSocketManager.setReadCallback(jsonObject -> {

            Log.e(
                    "CHAT_REALTIME",
                    "READ_CALLBACK_RECEIVED json="
                            + jsonObject
            );

            readManager.handleReadPayload(
                    jsonObject
            );
        });

        webSocketManager.setRoomStateCallback(() -> {

            Log.e(
                    "CHAT_REALTIME",
                    "ROOM_STATE_CONNECTED_CALLBACK currentUserId="
                            + currentUserId
                            + ", roomId="
                            + roomId
            );

            if (currentUserId != null
                    && currentUserId > 0) {

                roomStateManager.enterChatRoom(
                        currentUserId
                );

                Log.e(
                        "CHAT_REALTIME",
                        "ROOM_STATE_CONNECTED_ENTER_ONLY"
                );
            }
        });
    }

    private void refreshRoomTitleIfMemberChanged(
            Message message
    ) {

        if (message == null) {

            return;
        }

        if (!"SYSTEM".equals(
                message.getMessageType()
        )) {

            return;
        }

        String content =
                message.getMessageContent();

        if (content == null
                || content.trim().isEmpty()) {

            return;
        }

        boolean memberChanged =
                content.contains("참여")
                        || content.contains("초대")
                        || content.contains("나갔")
                        || content.contains("입장");

        if (!memberChanged) {

            return;
        }

        Log.e(
                "CHAT_REALTIME",
                "SYSTEM_MEMBER_CHANGED_REFRESH_TITLE content="
                        + content
        );

        recyclerView.postDelayed(() -> {

            try {

                activity.refreshCurrentRoomTitle();

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "채팅방 제목 갱신 실패",
                        e
                );
            }

        }, 300);
    }

    private void setupKeyboardScrollListener() {

        etMessage.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {

                        shouldAutoScroll =
                                true;

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                100
                        );

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                250
                        );

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                450
                        );
                    }
                }
        );

        etMessage.setOnClickListener(v -> {

            shouldAutoScroll =
                    true;

            recyclerView.postDelayed(
                    this::scrollToBottomSafely,
                    100
            );

            recyclerView.postDelayed(
                    this::scrollToBottomSafely,
                    250
            );

            recyclerView.postDelayed(
                    this::scrollToBottomSafely,
                    450
            );
        });

        recyclerView.addOnLayoutChangeListener(
                (v,
                 left,
                 top,
                 right,
                 bottom,
                 oldLeft,
                 oldTop,
                 oldRight,
                 oldBottom) -> {

                    boolean keyboardVisible =
                            oldBottom > bottom;

                    if (keyboardVisible) {

                        shouldAutoScroll =
                                true;

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                100
                        );

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                250
                        );

                        recyclerView.postDelayed(
                                this::scrollToBottomSafely,
                                450
                        );
                    }
                }
        );
    }

    private void setupRecyclerScrollListener() {

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {

                        super.onScrolled(
                                recyclerView,
                                dx,
                                dy
                        );

                        shouldAutoScroll =
                                !recyclerView.canScrollVertically(
                                        1
                                );
                    }
                }
        );
    }

    private void scrollToBottomSafely() {

        if (adapter == null
                || recyclerView == null) {

            Log.e(
                    "CHAT_REALTIME",
                    "SCROLL_SAFE_SKIP adapterOrRecyclerNull"
            );

            return;
        }

        int lastPosition =
                adapter.getItemCount() - 1;

        Log.e(
                "CHAT_REALTIME",
                "SCROLL_SAFE itemCount="
                        + adapter.getItemCount()
                        + ", lastPosition="
                        + lastPosition
        );

        if (lastPosition < 0) {

            return;
        }

        recyclerView.scrollToPosition(
                lastPosition
        );
    }

    private void smoothScrollToBottomSafely() {

        if (adapter == null
                || recyclerView == null) {

            Log.e(
                    "CHAT_REALTIME",
                    "SMOOTH_SCROLL_SKIP adapterOrRecyclerNull"
            );

            return;
        }

        int lastPosition =
                adapter.getItemCount() - 1;

        Log.e(
                "CHAT_REALTIME",
                "SMOOTH_SCROLL itemCount="
                        + adapter.getItemCount()
                        + ", lastPosition="
                        + lastPosition
        );

        if (lastPosition < 0) {

            return;
        }

        recyclerView.smoothScrollToPosition(
                lastPosition
        );
    }

    private void scrollToBottomAfterSend() {

        Log.e(
                "CHAT_REALTIME",
                "SCROLL_TO_BOTTOM_AFTER_SEND"
        );

        shouldAutoScroll =
                true;

        recyclerView.post(
                this::smoothScrollToBottomSafely
        );

        recyclerView.postDelayed(
                this::smoothScrollToBottomSafely,
                80
        );

        recyclerView.postDelayed(
                this::smoothScrollToBottomSafely,
                180
        );
    }

    private void fetchUserInfoAndStartChat() {

        Log.e(
                "CHAT_REALTIME",
                "FETCH_USER_INFO_START roomId=" + roomId
        );

        RetrofitClient.getApiService(activity)
                .getCurrentUser()
                .enqueue(new Callback<LoginResponseDto>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<LoginResponseDto> call,
                            @NonNull Response<LoginResponseDto> response
                    ) {

                        Log.e(
                                "CHAT_REALTIME",
                                "FETCH_USER_INFO_RESPONSE code="
                                        + response.code()
                                        + ", success="
                                        + response.isSuccessful()
                        );

                        if (response.isSuccessful()
                                && response.body() != null) {

                            LoginResponseDto user =
                                    response.body();

                            currentUserId =
                                    user.getMemberId();

                            currentNickname =
                                    user.getNickname();

                            PreferenceManager.saveNickname(
                                    activity,
                                    currentNickname
                            );
                        }

                        prepareUserFallback();

                        initChatAfterUserReady();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<LoginResponseDto> call,
                            @NonNull Throwable t
                    ) {

                        Log.e(
                                TAG,
                                "유저 정보 로드 실패",
                                t
                        );

                        Log.e(
                                "CHAT_REALTIME",
                                "FETCH_USER_INFO_FAILURE",
                                t
                        );

                        prepareUserFallback();

                        initChatAfterUserReady();
                    }
                });
    }

    private void initChatAfterUserReady() {

        Log.e(
                "CHAT_REALTIME",
                "INIT_CHAT_AFTER_USER_READY roomId="
                        + roomId
                        + ", currentUserId="
                        + currentUserId
                        + ", currentNickname="
                        + currentNickname
        );

        adapter =
                new MessageAdapter(
                        currentUserId
                );

        syncManager.setAdapter(
                adapter
        );

        recyclerView.setAdapter(
                adapter
        );

        disableRecyclerViewChangeAnimation();

        Log.e(
                "CHAT_REALTIME",
                "WEBSOCKET_CONNECT_START roomId="
                        + roomId
        );

        webSocketManager.connect();

        if (currentUserId != null
                && currentUserId > 0) {

            Log.e(
                    "CHAT_REALTIME",
                    "ROOM_ENTER_AND_READ_START currentUserId="
                            + currentUserId
                            + ", roomId="
                            + roomId
            );

            roomStateManager.enterChatRoom(
                    currentUserId
            );

            roomStateManager.updateReadStatus(
                    currentUserId
            );
        }

        loadCachedMessagesFirst();

        historyManager.loadLocalMessagesFirst(
                currentUserId
        );

        loadChatHistory();
    }

    private void prepareUserFallback() {

        SharedPreferences prefs =
                activity.getSharedPreferences(
                        "auth",
                        Context.MODE_PRIVATE
                );

        if (currentUserId == null
                || currentUserId <= 0) {

            currentUserId =
                    prefs.getLong(
                            "memberId",
                            0L
                    );
        }

        if (currentUserId == null
                || currentUserId <= 0) {

            currentUserId =
                    0L;
        }

        if (currentNickname == null
                || currentNickname.trim().isEmpty()) {

            currentNickname =
                    prefs.getString(
                            "nickname",
                            null
                    );
        }

        if (currentNickname == null
                || currentNickname.trim().isEmpty()) {

            currentNickname =
                    PreferenceManager.getNickname(
                            activity
                    );
        }

        if (currentNickname == null
                || currentNickname.trim().isEmpty()) {

            currentNickname =
                    "익명";
        }

        Log.e(
                "CHAT_REALTIME",
                "PREPARE_USER_FALLBACK result currentUserId="
                        + currentUserId
                        + ", currentNickname="
                        + currentNickname
        );
    }

    public void loadChatHistory() {

        Log.e(
                "CHAT_REALTIME",
                "LOAD_CHAT_HISTORY_REQUEST historyLoaded="
                        + historyLoaded
                        + ", roomId="
                        + roomId
        );

        if (historyLoaded) {

            Log.e(
                    "CHAT_REALTIME",
                    "LOAD_CHAT_HISTORY_SKIP_ALREADY_LOADED"
            );

            return;
        }

        historyLoaded =
                true;

        historyManager.loadChatHistory(
                currentUserId
        );
    }

    public void forceReloadChatHistory() {

        Log.e(
                "CHAT_REALTIME",
                "FORCE_RELOAD_CHAT_HISTORY roomId="
                        + roomId
        );

        historyLoaded =
                false;

        cacheManager.clear();

        if (syncManager != null) {

            syncManager.clearMessages();
        }

        loadChatHistory();
    }

    private void loadCachedMessagesFirst() {

        List<Message> cachedMessages =
                cacheManager.getCachedMessages();

        Log.e(
                "CHAT_REALTIME",
                "LOAD_CACHE_MESSAGES size="
                        + cachedMessages.size()
        );

        if (cachedMessages.isEmpty()) {

            return;
        }

        if (syncManager != null) {

            syncManager.setInitialMessages(
                    cachedMessages
            );
        }

        recyclerView.post(
                this::scrollToBottomSafely
        );
    }

    private void saveCurrentMessagesToCache() {

        if (syncManager == null) {

            Log.e(
                    "CHAT_REALTIME",
                    "SAVE_CACHE_SKIP syncManagerNull"
            );

            return;
        }

        List<Message> currentMessages =
                syncManager.getCurrentMessages();

        Log.e(
                "CHAT_REALTIME",
                "SAVE_CURRENT_MESSAGES_TO_CACHE size="
                        + currentMessages.size()
        );

        cacheManager.saveMessages(
                currentMessages
        );
    }

    private void addMessageAndRefresh(
            Message message
    ) {

        if (message == null) {

            Log.e(
                    "CHAT_REALTIME",
                    "ADD_MESSAGE_SKIP messageNull"
            );

            return;
        }

        Log.e(
                "CHAT_REALTIME",
                "ADD_MESSAGE_START id="
                        + message.getId()
                        + ", senderId="
                        + message.getSenderId()
                        + ", viewType="
                        + message.getViewType()
                        + ", type="
                        + message.getMessageType()
                        + ", content="
                        + message.getMessageContent()
        );

        Message copiedMessage =
                copyMessage(
                        message
                );

        if (syncManager != null) {

            Log.e(
                    "CHAT_REALTIME",
                    "ADD_MESSAGE_BEFORE_SYNC size="
                            + syncManager.getCurrentMessages().size()
            );

            syncManager.addOrUpdateMessage(
                    copiedMessage
            );

            Log.e(
                    "CHAT_REALTIME",
                    "ADD_MESSAGE_AFTER_SYNC size="
                            + syncManager.getCurrentMessages().size()
            );

        } else {

            Log.e(
                    "CHAT_REALTIME",
                    "ADD_MESSAGE_SYNC_MANAGER_NULL"
            );
        }

        saveCurrentMessagesToCache();

        scrollToBottomAfterSend();
    }

    private boolean replacePendingLocalMessageIfNeeded(
            Message serverMessage
    ) {

        if (serverMessage == null
                || syncManager == null) {

            return false;
        }

        List<Message> currentMessages =
                syncManager.getCurrentMessages();

        if (currentMessages == null
                || currentMessages.isEmpty()) {

            return false;
        }

        List<Message> newMessages =
                new ArrayList<>();

        boolean replaced =
                false;

        for (Message existing : currentMessages) {

            if (!replaced
                    && isPendingLocalMessageMatch(
                    existing,
                    serverMessage
            )) {

                newMessages.add(
                        copyMessage(
                                serverMessage
                        )
                );

                replaced =
                        true;

            } else {

                newMessages.add(
                        copyMessage(
                                existing
                        )
                );
            }
        }

        if (replaced) {

            Log.e(
                    "CHAT_REALTIME",
                    "REPLACE_PENDING_LOCAL_SET_INITIAL id="
                            + serverMessage.getId()
            );

            syncManager.setInitialMessages(
                    newMessages
            );
        }

        return replaced;
    }

    private boolean isPendingLocalMessageMatch(
            Message localMessage,
            Message serverMessage
    ) {

        if (localMessage == null
                || serverMessage == null) {

            return false;
        }

        if (localMessage.getId() == null
                || localMessage.getId() >= 0) {

            return false;
        }

        if (localMessage.getSenderId() == null
                || serverMessage.getSenderId() == null
                || !localMessage.getSenderId().equals(
                serverMessage.getSenderId()
        )) {

            return false;
        }

        if (!safeEquals(
                localMessage.getMessageContent(),
                serverMessage.getMessageContent()
        )) {

            return false;
        }

        if (!safeEquals(
                localMessage.getMessageType(),
                serverMessage.getMessageType()
        )) {

            return false;
        }

        return safeEquals(
                localMessage.getFileUrl(),
                serverMessage.getFileUrl()
        );
    }

    private boolean safeEquals(
            Object a,
            Object b
    ) {

        if (a == null
                && b == null) {

            return true;
        }

        if (a == null
                || b == null) {

            return false;
        }

        return a.equals(
                b
        );
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

    public void sendMessageFromInput() {

        String content =
                etMessage.getText()
                        .toString()
                        .trim();

        Log.e(
                "CHAT_REALTIME",
                "SEND_MESSAGE_FROM_INPUT content="
                        + content
                        + ", roomId="
                        + roomId
        );

        if (!content.isEmpty()
                && roomId != -1) {

            sendTextMessage(
                    content
            );
        }
    }

    private void sendTextMessage(
            String content
    ) {

        etMessage.setText(
                ""
        );

        sendMessage(
                content,
                "TEXT",
                null
        );
    }

    private void sendImageMessage(
            String imageUrl
    ) {

        sendMessage(
                "📷 사진",
                "IMAGE",
                imageUrl
        );
    }

    private void sendVideoMessage(
            String videoUrl
    ) {

        sendMessage(
                "🎥 동영상",
                "VIDEO",
                videoUrl
        );
    }

    private void sendMessage(
            String content,
            String messageType,
            String fileUrl
    ) {

        if (currentUserId == null
                || currentUserId <= 0) {

            prepareUserFallback();
        }

        Log.e(
                "CHAT_REALTIME",
                "SEND_MESSAGE_START roomId="
                        + roomId
                        + ", senderId="
                        + currentUserId
                        + ", type="
                        + messageType
                        + ", content="
                        + content
        );

        Message localMessage =
                new Message();

        localMessage.setId(
                -System.currentTimeMillis()
        );

        localMessage.setSenderId(
                currentUserId
        );

        localMessage.setSenderNickname(
                currentNickname
        );

        localMessage.setRoomId(
                roomId
        );

        localMessage.setMessageContent(
                content
        );

        localMessage.setMessageType(
                messageType
        );

        localMessage.setFileUrl(
                fileUrl
        );

        localMessage.setUnreadCount(
                1
        );

        localMessage.setViewType(
                0
        );

        addMessageAndRefresh(
                localMessage
        );

        sendManager.sendMessage(
                webSocketManager.getStompClient(),
                currentUserId,
                currentNickname,
                content,
                messageType,
                fileUrl,
                webSocketManager::scheduleReconnect
        );
    }

    public void uploadAndSendImage(
            Uri imageUri
    ) {

        imageManager.uploadAndSendImage(
                imageUri,
                this::sendImageMessage
        );
    }

    public void uploadAndSendVideo(
            Uri videoUri
    ) {

        imageManager.uploadAndSendVideo(
                videoUri,
                this::sendVideoMessage
        );
    }

    public void onResume() {

        Log.e(
                "CHAT_REALTIME",
                "CONTROLLER_ON_RESUME currentUserId="
                        + currentUserId
                        + ", roomId="
                        + roomId
                        + ", wsConnected="
                        + webSocketManager.isConnected()
        );

        if (currentUserId == null
                || currentUserId <= 0) {

            prepareUserFallback();
        }

        if (currentUserId != null
                && currentUserId > 0) {

            roomStateManager.enterChatRoom(
                    currentUserId
            );

            Log.e(
                    "CHAT_REALTIME",
                    "CONTROLLER_ON_RESUME_ENTER_ONLY"
            );
        }

        if (!webSocketManager.isConnected()) {

            Log.e(
                    "CHAT_REALTIME",
                    "CONTROLLER_ON_RESUME_RECONNECT"
            );

            webSocketManager.connect();
        }
    }

    public void onPause() {

        Log.e(
                "CHAT_REALTIME",
                "CONTROLLER_ON_PAUSE roomId="
                        + roomId
        );

        saveCurrentMessagesToCache();

        if (currentUserId != null
                && currentUserId > 0
                && roomId > 0) {

            roomStateManager.exitChatRoom(
                    currentUserId,
                    roomId
            );
        }
    }

    public void onDestroy() {

        Log.e(
                "CHAT_REALTIME",
                "CONTROLLER_ON_DESTROY roomId="
                        + roomId
        );

        saveCurrentMessagesToCache();

        if (currentUserId != null
                && currentUserId > 0
                && roomId > 0) {

            roomStateManager.exitChatRoom(
                    currentUserId,
                    roomId
            );
        }

        webSocketManager.disconnect();
    }
}