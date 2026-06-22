package com.tech.motjip.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.tech.motjip.API.ApiService;
import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Adapter.ChatRoomAdapter;
import com.tech.motjip.GroupCreateActivity;
import com.tech.motjip.MessageActivity;
import com.tech.motjip.Model.ChatRoom;
import com.tech.motjip.MyFirebaseMessagingService;
import com.tech.motjip.R;
import com.tech.motjip.manager.socket.SocketManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText etChatSearch;
    private ChatRoomAdapter adapter;

    private final List<ChatRoom> roomList = new ArrayList<>();
    private final List<ChatRoom> allRoomList = new ArrayList<>();

    private String currentSearchKeyword = "";

    private final SocketManager socketManager = SocketManager.getInstance();

    private static final String ROOM_LIST_SUBSCRIBE_KEY = "room_list_update";
    private static final String ROOM_LIST_CONNECTED_KEY = "room_list_connected";

    private boolean isLoadingRooms = false;
    private boolean pendingRoomReload = false;
    private boolean isOpeningChatRoom = false;
    private boolean isRoomListReceiverRegistered = false;

    private final String TAG = "ChatFragment";

    private final BroadcastReceiver roomListUpdateReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    Log.d(TAG, "FCM 채팅방 리스트 갱신 브로드캐스트 수신");

                    loadChatRooms();
                }
            };

    public ChatFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        Log.d(TAG, "onCreateView 시작");

        View view = inflater.inflate(
                R.layout.fragment_chat,
                container,
                false
        );

        recyclerView = view.findViewById(R.id.rv_chat_list);
        etChatSearch = view.findViewById(R.id.et_chat_search);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new ChatRoomAdapter(
                roomList,
                this::openChatRoom
        );

        recyclerView.setAdapter(adapter);

        disableRecyclerViewChangeAnimation();

        setupChatRoomSearch();

        ImageView btnCreateRoom = view.findViewById(R.id.btn_create_room);

        if (btnCreateRoom != null) {

            btnCreateRoom.setOnClickListener(v -> {

                if (!isAdded() || getContext() == null) {
                    return;
                }

                Log.d(TAG, "그룹 채팅 생성 버튼 클릭");

                Intent intent = new Intent(
                        requireContext(),
                        GroupCreateActivity.class
                );

                startActivity(intent);
            });

        } else {

            Log.e(TAG, "btn_create_room 을 찾을 수 없습니다.");
        }

        loadChatRooms();

        connectRoomListSocket();

        return view;
    }

    private void setupChatRoomSearch() {

        if (etChatSearch == null) {

            Log.e(TAG, "et_chat_search 를 찾을 수 없습니다.");
            return;
        }

        etChatSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {

            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {

                currentSearchKeyword =
                        s == null
                                ? ""
                                : s.toString().trim();

                Log.d(
                        TAG,
                        "채팅방 검색어 변경 keyword=" + currentSearchKeyword
                );

                applyRoomSearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void applyRoomSearchFilter() {

        roomList.clear();

        if (currentSearchKeyword == null
                || currentSearchKeyword.trim().isEmpty()) {

            roomList.addAll(allRoomList);

        } else {

            String keyword =
                    currentSearchKeyword
                            .trim()
                            .toLowerCase();

            for (ChatRoom room : allRoomList) {

                if (room == null) {
                    continue;
                }

                String roomName =
                        ChatRoomAdapter.getDisplayRoomName(room);

                if (roomName != null
                        && roomName.toLowerCase().contains(keyword)) {

                    roomList.add(room);
                }
            }
        }

        Log.d(
                TAG,
                "검색 필터 적용 전체="
                        + allRoomList.size()
                        + ", 표시="
                        + roomList.size()
                        + ", keyword="
                        + currentSearchKeyword
        );

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void disableRecyclerViewChangeAnimation() {

        if (recyclerView == null) {
            return;
        }

        RecyclerView.ItemAnimator animator =
                recyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {

            ((SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(false);
        }
    }

    private Long getLoginMemberId() {

        if (!isAdded() || getContext() == null) {
            return 0L;
        }

        SharedPreferences authPrefs =
                requireContext().getSharedPreferences(
                        "auth",
                        Context.MODE_PRIVATE
                );

        return authPrefs.getLong("memberId", 0L);
    }

    private void loadChatRooms() {

        Log.d(TAG, "loadChatRooms 시작");

        if (!isAdded() || getContext() == null) {

            Log.e(TAG, "loadChatRooms 중단: Fragment attach 안됨");
            return;
        }

        if (isLoadingRooms) {

            Log.d(TAG, "loadChatRooms 중복 호출 감지 → pendingRoomReload 예약");

            pendingRoomReload = true;
            return;
        }

        Long memberId = getLoginMemberId();

        if (memberId == null || memberId <= 0) {

            Log.e(TAG, "memberId 없음");
            return;
        }

        isLoadingRooms = true;

        ApiService apiService =
                RetrofitClient.getApiService(requireContext());

        apiService.getMyRooms(memberId).enqueue(new Callback<List<ChatRoom>>() {

            @Override
            public void onResponse(
                    @NonNull Call<List<ChatRoom>> call,
                    @NonNull Response<List<ChatRoom>> response
            ) {

                isLoadingRooms = false;

                if (!isAdded() || getView() == null) {

                    Log.e(TAG, "응답 수신 후 Fragment detach 상태");
                    return;
                }

                Log.d(TAG, "getMyRooms 응답 코드: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    allRoomList.clear();
                    allRoomList.addAll(response.body());

                    for (ChatRoom room : allRoomList) {

                        if (room == null) {
                            continue;
                        }

                        Log.d(
                                "CHAT_ROOM_UNREAD",
                                "roomId="
                                        + room.getRoomId()
                                        + ", roomName="
                                        + room.getRoomName()
                                        + ", unreadCount="
                                        + room.getUnreadCount()
                        );
                    }

                    applyRoomSearchFilter();

                } else {

                    Log.e(TAG, "채팅방 응답 실패: " + response.code());
                }

                if (pendingRoomReload) {

                    pendingRoomReload = false;

                    if (recyclerView != null) {

                        recyclerView.postDelayed(
                                ChatFragment.this::loadChatRooms,
                                150
                        );
                    }
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<List<ChatRoom>> call,
                    @NonNull Throwable t
            ) {

                isLoadingRooms = false;

                if (!isAdded()) {
                    return;
                }

                Log.e(TAG, "채팅방 로드 실패", t);

                if (pendingRoomReload) {

                    pendingRoomReload = false;

                    if (recyclerView != null) {

                        recyclerView.postDelayed(
                                ChatFragment.this::loadChatRooms,
                                300
                        );
                    }
                }
            }
        });
    }

    private void connectRoomListSocket() {

        if (!isAdded() || getContext() == null) {
            return;
        }

        socketManager.addConnectedCallback(
                ROOM_LIST_CONNECTED_KEY,
                () -> {

                    subscribeRoomUpdate();

                    Log.e(
                            "CHAT_REALTIME",
                            "ROOM_UPDATE_SUBSCRIBED_FORCE"
                    );
                }
        );

        socketManager.connect(requireContext());
    }

    private void subscribeRoomUpdate() {

        Log.e(
                "CHAT_REALTIME",
                "SUBSCRIBE_ROOM_UPDATE_CALLED"
        );

        Long memberId = getLoginMemberId();

        if (memberId == null || memberId <= 0) {
            return;
        }

        String roomUpdateTopic =
                "/sub/chat/rooms/update/" + memberId;

        Log.e(
                "CHAT_REALTIME",
                "ROOM_UPDATE_SUBSCRIBE topic=" + roomUpdateTopic
        );

        socketManager.subscribe(
                ROOM_LIST_SUBSCRIBE_KEY,
                roomUpdateTopic,
                payload -> {

                    Log.e(
                            "CHAT_REALTIME",
                            "ROOM_UPDATE_RECEIVED topic="
                                    + roomUpdateTopic
                                    + ", payload="
                                    + payload
                    );

                    if (!isAdded() || getActivity() == null) {
                        return;
                    }

                    requireActivity().runOnUiThread(() -> {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        handleRoomUpdatePayload(payload);
                    });
                }
        );
    }

    private void handleRoomUpdatePayload(String payload) {

        try {

            if (payload == null || payload.trim().isEmpty()) {

                scheduleRoomReload(150);
                return;
            }

            JSONObject jsonObject = new JSONObject(payload);

            if (!jsonObject.has("roomId")
                    || jsonObject.isNull("roomId")) {

                scheduleRoomReload(150);
                return;
            }

            long updatedRoomId =
                    jsonObject.optLong("roomId", -1L);

            if (updatedRoomId <= 0) {

                scheduleRoomReload(150);
                return;
            }

            int position =
                    findRoomPositionById(updatedRoomId);

            if (position < 0) {

                scheduleRoomReload(200);
                return;
            }

            ChatRoom room =
                    allRoomList.get(position);

            if (room == null) {

                scheduleRoomReload(150);
                return;
            }

            boolean changed = false;

            if (jsonObject.has("unreadCount")
                    && !jsonObject.isNull("unreadCount")) {

                long unreadCount =
                        jsonObject.optLong(
                                "unreadCount",
                                room.getUnreadCount()
                        );

                if (room.getUnreadCount() != unreadCount) {

                    room.setUnreadCount(unreadCount);
                    changed = true;
                }
            }

            if (jsonObject.has("lastMessage")
                    && !jsonObject.isNull("lastMessage")) {

                String lastMessage =
                        jsonObject.optString(
                                "lastMessage",
                                room.getLastMessage()
                        );

                if (!lastMessage.equals(room.getLastMessage())) {

                    room.setLastMessage(lastMessage);
                    changed = true;
                }
            }

            if (jsonObject.has("lastMessageType")
                    && !jsonObject.isNull("lastMessageType")) {

                String lastMessageType =
                        jsonObject.optString(
                                "lastMessageType",
                                room.getLastMessageType()
                        );

                if (!lastMessageType.equals(room.getLastMessageType())) {

                    room.setLastMessageType(lastMessageType);
                    changed = true;
                }
            }

            if (jsonObject.has("time")
                    && !jsonObject.isNull("time")) {

                String time =
                        jsonObject.optString(
                                "time",
                                room.getTime()
                        );

                if (!time.equals(room.getTime())) {

                    room.setTime(time);
                    changed = true;
                }
            }

            if (changed && adapter != null) {

                ChatRoom updatedRoom =
                        allRoomList.remove(position);

                allRoomList.add(0, updatedRoom);

                applyRoomSearchFilter();

                if (recyclerView != null
                        && (currentSearchKeyword == null
                        || currentSearchKeyword.trim().isEmpty())) {

                    recyclerView.post(() -> {

                        if (recyclerView != null) {

                            recyclerView.scrollToPosition(0);
                        }
                    });
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "채팅방 업데이트 payload 처리 실패",
                    e
            );

            scheduleRoomReload(300);
        }
    }

    private void scheduleRoomReload(long delayMillis) {

        if (!isAdded() || recyclerView == null) {
            return;
        }

        if (isLoadingRooms) {

            pendingRoomReload = true;
            return;
        }

        recyclerView.postDelayed(
                this::loadChatRooms,
                delayMillis
        );
    }

    private int findRoomPositionById(long roomId) {

        for (int i = 0; i < allRoomList.size(); i++) {

            ChatRoom room = allRoomList.get(i);

            if (room != null
                    && room.getRoomId() != null
                    && room.getRoomId() == roomId) {

                return i;
            }
        }

        return -1;
    }

    private void openChatRoom(ChatRoom room) {

        if (isLoadingRooms || isOpeningChatRoom) {
            return;
        }

        if (!isAdded()
                || getContext() == null
                || getActivity() == null
                || recyclerView == null
                || adapter == null) {

            return;
        }

        if (room == null
                || room.getRoomId() == null
                || room.getRoomId() <= 0) {

            Toast.makeText(
                    requireContext(),
                    "채팅방 정보를 불러오는 중입니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String displayName =
                ChatRoomAdapter.getDisplayRoomName(room);

        Intent intent =
                new Intent(
                        requireContext(),
                        MessageActivity.class
                );

        intent.putExtra("roomId", room.getRoomId());
        intent.putExtra("roomName", displayName);
        intent.putExtra("roomType", room.getRoomType());

        isOpeningChatRoom = true;

        startActivity(intent);
    }

    private void registerRoomListUpdateReceiver() {

        if (!isAdded()
                || getContext() == null
                || isRoomListReceiverRegistered) {

            return;
        }

        IntentFilter filter =
                new IntentFilter(
                        MyFirebaseMessagingService.ACTION_CHAT_ROOM_LIST_UPDATE
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            requireContext().registerReceiver(
                    roomListUpdateReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );

        } else {

            requireContext().registerReceiver(
                    roomListUpdateReceiver,
                    filter
            );
        }

        isRoomListReceiverRegistered = true;
    }

    private void unregisterRoomListUpdateReceiver() {

        if (!isAdded()
                || getContext() == null
                || !isRoomListReceiverRegistered) {

            return;
        }

        try {

            requireContext().unregisterReceiver(
                    roomListUpdateReceiver
            );

        } catch (Exception e) {

            Log.e(TAG, "receiver 해제 실패", e);
        }

        isRoomListReceiverRegistered = false;
    }

    @Override
    public void onResume() {

        super.onResume();

        isOpeningChatRoom = false;

        registerRoomListUpdateReceiver();

        loadChatRooms();

        if (!socketManager.isConnected()) {

            connectRoomListSocket();

        } else {

            subscribeRoomUpdate();
        }
    }

    @Override
    public void onPause() {

        unregisterRoomListUpdateReceiver();

        super.onPause();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        socketManager.unsubscribe(ROOM_LIST_SUBSCRIBE_KEY);

        socketManager.removeConnectedCallback(ROOM_LIST_CONNECTED_KEY);

        unregisterRoomListUpdateReceiver();

        pendingRoomReload = false;
        isOpeningChatRoom = false;

        recyclerView = null;
        etChatSearch = null;
        adapter = null;
    }
}