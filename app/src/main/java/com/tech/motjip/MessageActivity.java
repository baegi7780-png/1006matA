package com.tech.motjip;

import android.graphics.Rect;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tech.motjip.API.ApiService;
import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Adapter.ParticipantAdapter;
import com.tech.motjip.Controller.MessageController;
import com.tech.motjip.Model.ChatRoom;
import com.tech.motjip.Model.Participant;


import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    private static final String TAG =
            "MessageActivity";

    private View rootView;
    private LinearLayout layoutChatBody;
    private RecyclerView recyclerView;
    private LinearLayout layoutInput;
    private EditText etMessage;
    private Button btnSend;
    private ImageView btnSelectImage;

    private ImageView btnChatMenu;
    private LinearLayout layoutMemberPanel;
    private View viewMemberPanelDim;
    private RecyclerView rvMemberPanelList;
    private TextView tvMemberLoading;
    private ImageView btnCloseMemberPanel;
    private Button btnInviteFromMemberPanel;
    private Button btnShareInviteLink;
    private Button btnLeaveChatRoom;
    private TextView tvTitle;

    private MessageController messageController;

    private long roomId = -1L;
    private Long myMemberId = -1L;

    private boolean isControllerStarted = false;
    private boolean reopenMemberPanelAfterInvite = false;
    private boolean isMemberPanelAnimating = false;
    private boolean skipNextResumeMemberReload = false;

    private int defaultRecyclerBottomPadding = 16;

    private SharedPreferences activeRoomPrefs;

    private final ActivityResultLauncher<Intent> inviteLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK) {

                            boolean reloadMembers =
                                    result.getData() != null
                                            && result.getData()
                                            .getBooleanExtra(
                                                    "reloadMembers",
                                                    false
                                            );

                            Toast.makeText(
                                    this,
                                    "채팅방 참가자 갱신 완료",
                                    Toast.LENGTH_SHORT
                            ).show();

                            if (messageController != null) {

                                messageController.forceReloadChatHistory();
                            }

                            if (reloadMembers) {

                                if (reopenMemberPanelAfterInvite) {

                                    reopenMemberPanelAfterInvite = false;
                                    skipNextResumeMemberReload = true;

                                    openMemberPanel();

                                } else {

                                    loadRoomMembers();
                                }

                            } else if (layoutMemberPanel.getVisibility()
                                    == View.VISIBLE) {

                                loadRoomMembers();
                            }
                        }
                    }
            );

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null
                                && messageController != null) {

                            String mimeType =
                                    getContentResolver()
                                            .getType(uri);

                            if (mimeType != null
                                    && mimeType.startsWith("video")) {

                                messageController
                                        .uploadAndSendVideo(
                                                uri
                                        );

                            } else {

                                messageController
                                        .uploadAndSendImage(
                                                uri
                                        );
                            }
                        }
                    }
            );





    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        /*
         * 키보드 처리는 WindowInsetsCompat에서 직접 처리한다.
         *
         * adjustResize가 기기별로 다르게 동작할 수 있으므로,
         * layout_chat_body 전체에 ime/systemBars bottom 값을 직접 적용한다.
         */
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        );

        setContentView(
                R.layout.activity_message
        );


        rootView =
                findViewById(
                        R.id.root_message_layout
                );

        layoutChatBody =
                findViewById(
                        R.id.layout_chat_body
                );

        ImageView btnBack =
                findViewById(
                        R.id.btn_custom_back
                );

        btnChatMenu =
                findViewById(
                        R.id.btn_chat_menu
                );

        tvTitle =
                findViewById(
                        R.id.tv_room_title
                );

        recyclerView =
                findViewById(
                        R.id.rv_message_list
                );

        layoutInput =
                findViewById(
                        R.id.layout_input
                );

        etMessage =
                findViewById(
                        R.id.et_message
                );

        btnSend =
                findViewById(
                        R.id.btn_send
                );

        btnSelectImage =
                findViewById(
                        R.id.btn_select_image
                );



        layoutMemberPanel =
                findViewById(
                        R.id.layout_member_panel
                );

        viewMemberPanelDim =
                findViewById(
                        R.id.view_member_panel_dim
                );

        rvMemberPanelList =
                findViewById(
                        R.id.rv_member_panel_list
                );

        tvMemberLoading =
                findViewById(
                        R.id.tv_member_loading
                );

        btnCloseMemberPanel =
                findViewById(
                        R.id.btn_close_member_panel
                );

        btnInviteFromMemberPanel =
                findViewById(
                        R.id.btn_invite_from_member_panel
                );

        btnShareInviteLink =
                findViewById(
                        R.id.btn_share_invite_link
                );

        btnLeaveChatRoom =
                findViewById(
                        R.id.btn_leave_chat_room
                );

        if (rootView == null
                || layoutChatBody == null
                || recyclerView == null
                || layoutInput == null
                || etMessage == null
                || btnSend == null
                || btnSelectImage == null
                || btnBack == null
                || btnChatMenu == null
                || tvTitle == null
                || layoutMemberPanel == null
                || viewMemberPanelDim == null
                || rvMemberPanelList == null
                || tvMemberLoading == null
                || btnCloseMemberPanel == null
                || btnInviteFromMemberPanel == null
                || btnShareInviteLink == null
                || btnLeaveChatRoom == null) {

            Toast.makeText(
                    this,
                    "채팅 화면을 불러올 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        defaultRecyclerBottomPadding =
                recyclerView.getPaddingBottom();

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this
                );

        layoutManager.setStackFromEnd(
                true
        );

        recyclerView.setLayoutManager(
                layoutManager
        );

        rvMemberPanelList.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        setupKeyboardInsets();

        Log.e(
                TAG,
                "setupKeyboardInsets 실행 직전"
        );

        setupKeyboardInsets();

        SharedPreferences prefs =
                getSharedPreferences(
                        "auth",
                        MODE_PRIVATE
                );

        activeRoomPrefs =
                getSharedPreferences(
                        "chat_state",
                        MODE_PRIVATE
                );

        myMemberId =
                prefs.getLong(
                        "memberId",
                        -1L
                );



        Intent intent =
                getIntent();

        Uri deepLinkData =
                intent.getData();

        if (deepLinkData != null) {

            handleInviteLink();

        } else {

            if (!initializeRoomFromIntent(
                    intent
            )) {

                return;
            }

            if (roomId > 0) {

                messageController =
                        new MessageController(
                                this,
                                recyclerView,
                                etMessage,
                                roomId
                        );

                messageController.start();

                isControllerStarted = true;
            }
        }

        btnBack.setOnClickListener(v -> {

            if (layoutMemberPanel.getVisibility()
                    == View.VISIBLE) {

                closeMemberPanel();

            } else {

                finish();
            }
        });

        btnChatMenu.setOnClickListener(v ->
                openMemberPanel()
        );

        btnCloseMemberPanel.setOnClickListener(v ->
                closeMemberPanel()
        );

        viewMemberPanelDim.setOnClickListener(v ->
                closeMemberPanel()
        );

        btnInviteFromMemberPanel.setOnClickListener(v -> {

            reopenMemberPanelAfterInvite = true;

            closeMemberPanel();

            openInviteFriendActivity();
        });

        btnShareInviteLink.setOnClickListener(v ->
                shareInviteLink()
        );

        btnLeaveChatRoom.setOnClickListener(v ->
                showLeaveChatRoomDialog()
        );





        btnSend.setOnClickListener(v -> {

            if (messageController == null) {

                Toast.makeText(
                        this,
                        "채팅방을 준비 중입니다.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }



            messageController.sendMessageFromInput();
        });

        btnSelectImage.setOnClickListener(v -> {

            if (messageController == null) {

                Toast.makeText(
                        this,
                        "채팅방을 준비 중입니다.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("파일 선택")
                    .setItems(
                            new String[]{
                                    "사진",
                                    "동영상"
                            },
                            (dialog, which) -> {

                                if (which == 0) {

                                    imagePickerLauncher.launch(
                                            "image/*"
                                    );

                                } else {

                                    imagePickerLauncher.launch(
                                            "video/*"
                                    );
                                }
                            }
                    )
                    .show();
        });





        recyclerView.postDelayed(() -> {

            if (messageController != null) {

                Log.d(
                        TAG,
                        "최초 진입 안정화 재동기화 실행"
                );

                messageController.forceReloadChatHistory();
            }

        }, 700);
    }

    @Override
    protected void onNewIntent(
            Intent intent
    ) {

        super.onNewIntent(
                intent
        );

        setIntent(
                intent
        );

        Log.d(
                TAG,
                "onNewIntent 호출"
        );

        Uri data =
                intent.getData();

        if (data != null) {

            handleInviteLink();

        } else {

            handleNotificationIntent(
                    intent
            );
        }
    }


    private boolean initializeRoomFromIntent(
            Intent intent
    ) {

        if (intent == null) {

            Toast.makeText(
                    this,
                    "채팅방 정보를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return false;
        }

        roomId =
                intent.getLongExtra(
                        "roomId",
                        -1L
                );

        if (roomId <= 0) {

            Toast.makeText(
                    this,
                    "채팅방 정보를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return false;
        }

        String roomName =
                intent.getStringExtra(
                        "roomName"
                );

        applyRoomTitle(
                roomName
        );

        Log.d(
                TAG,
                "초기 채팅방 설정 roomId="
                        + roomId
                        + ", roomName="
                        + roomName
        );

        return true;
    }

    private void handleNotificationIntent(
            Intent intent
    ) {

        if (intent == null) {

            Log.e(
                    TAG,
                    "handleNotificationIntent intent null"
            );

            return;
        }

        long newRoomId =
                intent.getLongExtra(
                        "roomId",
                        -1L
                );

        if (newRoomId <= 0) {

            Log.e(
                    TAG,
                    "푸시 roomId 없음"
            );

            return;
        }

        if (newRoomId == roomId) {

            Log.d(
                    TAG,
                    "이미 같은 채팅방 roomId="
                            + newRoomId
            );

            if (messageController != null) {

                messageController.forceReloadChatHistory();
            }

            return;
        }

        Log.d(
                TAG,
                "푸시 채팅방 이동 oldRoomId="
                        + roomId
                        + ", newRoomId="
                        + newRoomId
        );

        clearActiveChatRoom();

        roomId =
                newRoomId;

        String roomName =
                intent.getStringExtra(
                        "roomName"
                );

        applyRoomTitle(
                roomName
        );

        if (layoutMemberPanel != null
                && layoutMemberPanel.getVisibility()
                == View.VISIBLE) {

            closeMemberPanel();
        }

        if (messageController != null) {

            messageController.onDestroy();

            messageController =
                    null;
        }

        messageController =
                new MessageController(
                        this,
                        recyclerView,
                        etMessage,
                        roomId
                );

        messageController.start();

        isControllerStarted =
                true;

        saveActiveChatRoom();

        recyclerView.postDelayed(() -> {

            if (messageController != null) {

                Log.d(
                        TAG,
                        "푸시 이동 후 안정화 재동기화 실행"
                );

                messageController.forceReloadChatHistory();
            }

        }, 700);

        scrollRecyclerViewToBottom();

        Log.d(
                TAG,
                "새 채팅방으로 교체 완료 roomId="
                        + roomId
        );
    }

    private void applyRoomTitle(
            String roomName
    ) {

        if (tvTitle == null) {

            return;
        }

        if (roomName != null
                && !roomName.trim().isEmpty()) {

            tvTitle.setText(
                    roomName
            );

        } else {

            tvTitle.setText(
                    "채팅방"
            );
        }
    }

    private void shareInviteLink() {

        if (roomId <= 0
                || myMemberId == null
                || myMemberId <= 0) {

            Toast.makeText(
                    this,
                    "채팅방 정보를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        apiService.getMyRooms(
                        myMemberId
                )
                .enqueue(new Callback<List<ChatRoom>>() {

                    @Override
                    public void onResponse(
                            Call<List<ChatRoom>> call,
                            Response<List<ChatRoom>> response
                    ) {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            Toast.makeText(
                                    MessageActivity.this,
                                    "초대 링크를 불러오지 못했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        ChatRoom targetRoom =
                                null;

                        for (ChatRoom room : response.body()) {

                            if (room != null
                                    && room.getRoomId() != null
                                    && room.getRoomId().equals(
                                    roomId
                            )) {

                                targetRoom =
                                        room;

                                break;
                            }
                        }

                        if (targetRoom == null
                                || targetRoom.getInviteUrl() == null
                                || targetRoom.getInviteUrl()
                                .trim()
                                .isEmpty()) {

                            Toast.makeText(
                                    MessageActivity.this,
                                    "초대 링크가 없습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        String inviteUrl =
                                targetRoom.getInviteUrl();

                        ClipboardManager clipboard =
                                (ClipboardManager)
                                        getSystemService(
                                                CLIPBOARD_SERVICE
                                        );

                        ClipData clip =
                                ClipData.newPlainText(
                                        "invite_link",
                                        inviteUrl
                                );

                        clipboard.setPrimaryClip(
                                clip
                        );

                        Toast.makeText(
                                MessageActivity.this,
                                "초대 링크가 복사되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();


                    }

                    @Override
                    public void onFailure(
                            Call<List<ChatRoom>> call,
                            Throwable t
                    ) {

                        Log.e(
                                "CHAT_INVITE_LINK",
                                "초대 링크 조회 실패",
                                t
                        );

                        Toast.makeText(
                                MessageActivity.this,
                                "서버 연결 실패",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void openMemberPanel() {

        if (isMemberPanelAnimating
                || layoutMemberPanel.getVisibility()
                == View.VISIBLE) {

            return;
        }

        isMemberPanelAnimating =
                true;

        layoutMemberPanel.setVisibility(
                View.VISIBLE
        );

        viewMemberPanelDim.setVisibility(
                View.VISIBLE
        );

        layoutMemberPanel.post(() -> {

            ObjectAnimator animator =
                    ObjectAnimator.ofFloat(
                            layoutMemberPanel,
                            "translationX",
                            layoutMemberPanel.getWidth(),
                            0f
                    );

            animator.setDuration(
                    220
            );

            animator.addListener(
                    new android.animation.AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(
                                android.animation.Animator animation
                        ) {

                            isMemberPanelAnimating =
                                    false;
                        }

                        @Override
                        public void onAnimationCancel(
                                android.animation.Animator animation
                        ) {

                            isMemberPanelAnimating =
                                    false;
                        }
                    }
            );

            animator.start();
        });

        loadRoomMembers();
    }

    private void closeMemberPanel() {

        if (isMemberPanelAnimating
                || layoutMemberPanel.getVisibility()
                != View.VISIBLE) {

            return;
        }

        isMemberPanelAnimating =
                true;

        ObjectAnimator animator =
                ObjectAnimator.ofFloat(
                        layoutMemberPanel,
                        "translationX",
                        0f,
                        layoutMemberPanel.getWidth()
                );

        animator.setDuration(
                220
        );

        animator.addListener(
                new android.animation.AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(
                            android.animation.Animator animation
                    ) {

                        layoutMemberPanel.setVisibility(
                                View.GONE
                        );

                        viewMemberPanelDim.setVisibility(
                                View.GONE
                        );

                        isMemberPanelAnimating =
                                false;
                    }

                    @Override
                    public void onAnimationCancel(
                            android.animation.Animator animation
                    ) {

                        isMemberPanelAnimating =
                                false;
                    }
                }
        );

        animator.start();
    }

    private void loadRoomMembers() {

        if (roomId <= 0) {

            tvMemberLoading.setVisibility(
                    View.VISIBLE
            );

            tvMemberLoading.setText(
                    "채팅방 정보를 찾을 수 없습니다."
            );

            rvMemberPanelList.setAdapter(
                    null
            );

            return;
        }

        tvMemberLoading.setVisibility(
                View.VISIBLE
        );

        tvMemberLoading.setText(
                "참여자 목록을 불러오는 중..."
        );

        rvMemberPanelList.setAdapter(
                null
        );

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        apiService.getRoomMembers(
                        roomId
                )
                .enqueue(new Callback<List<Participant>>() {

                    @Override
                    public void onResponse(
                            Call<List<Participant>> call,
                            Response<List<Participant>> response
                    ) {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            tvMemberLoading.setVisibility(
                                    View.VISIBLE
                            );

                            tvMemberLoading.setText(
                                    "참여자 정보를 불러오지 못했습니다."
                            );

                            rvMemberPanelList.setAdapter(
                                    null
                            );

                            return;
                        }

                        List<Participant> participants =
                                response.body();

                        if (participants.isEmpty()) {

                            tvMemberLoading.setVisibility(
                                    View.VISIBLE
                            );

                            tvMemberLoading.setText(
                                    "참여자가 없습니다."
                            );

                            rvMemberPanelList.setAdapter(
                                    null
                            );

                            return;
                        }

                        tvMemberLoading.setVisibility(
                                View.GONE
                        );

                        ParticipantAdapter adapter =
                                new ParticipantAdapter(
                                        participants
                                );

                        rvMemberPanelList.setAdapter(
                                adapter
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<Participant>> call,
                            Throwable t
                    ) {

                        tvMemberLoading.setVisibility(
                                View.VISIBLE
                        );

                        tvMemberLoading.setText(
                                "참여자 목록 로딩 실패"
                        );

                        rvMemberPanelList.setAdapter(
                                null
                        );
                    }
                });
    }

    private void showLeaveChatRoomDialog() {

        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("정말 채팅방을 나가시겠습니까?")
                .setPositiveButton(
                        "나가기",
                        (dialog, which) ->
                                leaveChatRoom()
                )
                .setNegativeButton(
                        "취소",
                        null
                )
                .show();
    }

    private void leaveChatRoom() {

        if (roomId <= 0
                || myMemberId == null
                || myMemberId <= 0) {

            Toast.makeText(
                    this,
                    "채팅방 정보를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        apiService.leaveRoom(
                        roomId,
                        myMemberId
                )
                .enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(
                            Call<String> call,
                            Response<String> response
                    ) {

                        if (response.isSuccessful()) {

                            Toast.makeText(
                                    MessageActivity.this,
                                    "채팅방에서 나갔습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearActiveChatRoom();

                            Intent resultIntent =
                                    new Intent();

                            resultIntent.putExtra(
                                    "roomLeft",
                                    true
                            );

                            setResult(
                                    RESULT_OK,
                                    resultIntent
                            );

                            finish();

                        } else {

                            Toast.makeText(
                                    MessageActivity.this,
                                    "채팅방 나가기 실패",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<String> call,
                            Throwable t
                    ) {

                        Toast.makeText(
                                MessageActivity.this,
                                "서버 연결 실패",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void openInviteFriendActivity() {

        if (roomId <= 0) {

            Toast.makeText(
                    this,
                    "채팅방 정보를 불러오는 중입니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent inviteIntent =
                new Intent(
                        MessageActivity.this,
                        InviteFriendActivity.class
                );

        inviteIntent.putExtra(
                "roomId",
                roomId
        );

        inviteLauncher.launch(
                inviteIntent
        );
    }

    private void setupKeyboardInsets() {

        Log.e(
                TAG,
                "setupKeyboardInsets 호출됨"
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                rootView,
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    Insets ime =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.ime()
                            );

                    Log.d(
                            TAG,
                            "IME 확인 "
                                    + "imeVisible="
                                    + insets.isVisible(
                                    WindowInsetsCompat.Type.ime()
                            )
                                    + ", imeBottom="
                                    + ime.bottom
                                    + ", systemBottom="
                                    + systemBars.bottom
                    );

                    Rect visibleFrame =
                            new Rect();

                    rootView.getWindowVisibleDisplayFrame(
                            visibleFrame
                    );

                    int screenHeight =
                            rootView.getRootView().getHeight();

                    int fallbackKeyboardHeight =
                            screenHeight - visibleFrame.bottom;

                    Log.d(
                            TAG,
                            "Fallback 확인 "
                                    + "screenHeight="
                                    + screenHeight
                                    + ", visibleBottom="
                                    + visibleFrame.bottom
                                    + ", fallbackKeyboardHeight="
                                    + fallbackKeyboardHeight
                    );

                    boolean keyboardVisible =
                            insets.isVisible(
                                    WindowInsetsCompat.Type.ime()
                            );

                    int bottomInset;

                    if (keyboardVisible) {

                        bottomInset =
                                ime.bottom;

                    } else {

                        bottomInset =
                                systemBars.bottom;
                    }

                    layoutChatBody.setPadding(
                            layoutChatBody.getPaddingLeft(),
                            layoutChatBody.getPaddingTop(),
                            layoutChatBody.getPaddingRight(),
                            bottomInset
                    );

                    layoutInput.setTranslationY(
                            0f
                    );

                    recyclerView.setPadding(
                            recyclerView.getPaddingLeft(),
                            recyclerView.getPaddingTop(),
                            recyclerView.getPaddingRight(),
                            defaultRecyclerBottomPadding
                    );

                    if (keyboardVisible) {

                        recyclerView.postDelayed(
                                this::scrollRecyclerViewToBottom,
                                50
                        );

                        recyclerView.postDelayed(
                                this::scrollRecyclerViewToBottom,
                                150
                        );
                    }

                    Log.d(
                            TAG,
                            "Insets 적용 keyboardVisible="
                                    + keyboardVisible
                                    + ", systemBottom="
                                    + systemBars.bottom
                                    + ", imeBottom="
                                    + ime.bottom
                                    + ", bottomInset="
                                    + bottomInset
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(
                rootView
        );
    }

    private void scrollRecyclerViewToBottom() {

        if (recyclerView == null) {

            return;
        }

        recyclerView.postDelayed(() -> {

            if (recyclerView == null) {

                return;
            }

            RecyclerView.Adapter<?> currentAdapter =
                    recyclerView.getAdapter();

            if (currentAdapter == null) {

                return;
            }

            int itemCount =
                    currentAdapter.getItemCount();

            if (itemCount <= 0) {

                return;
            }

            recyclerView.smoothScrollToPosition(
                    itemCount - 1
            );

        }, 60);
    }

    private void saveActiveChatRoom() {

        if (activeRoomPrefs == null
                || roomId <= 0) {

            return;
        }

        activeRoomPrefs.edit()
                .putLong(
                        "activeChatRoomId",
                        roomId
                )
                .apply();

        Log.d(
                TAG,
                "현재 활성 채팅방 저장 roomId="
                        + roomId

        );
    }

    private void clearActiveChatRoom() {

        if (activeRoomPrefs == null) {

            return;
        }

        long activeRoomId =
                activeRoomPrefs.getLong(
                        "activeChatRoomId",
                        -1L
                );

        if (activeRoomId == roomId) {

            activeRoomPrefs.edit()
                    .remove(
                            "activeChatRoomId"
                    )
                    .apply();

            Log.d(
                    TAG,
                    "활성 채팅방 제거 roomId="
                            + roomId
            );
        }
    }

    @Override
    public void onBackPressed() {

        if (layoutMemberPanel != null
                && layoutMemberPanel.getVisibility()
                == View.VISIBLE) {

            closeMemberPanel();

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (roomId > 0) {

            saveActiveChatRoom();
        }



        if (messageController != null
                && isControllerStarted) {

            messageController.onResume();

            messageController.forceReloadChatHistory();
        }

        if (skipNextResumeMemberReload) {

            skipNextResumeMemberReload = false;

            return;
        }

        if (layoutMemberPanel != null
                && layoutMemberPanel.getVisibility()
                == View.VISIBLE
                && !isMemberPanelAnimating) {

            loadRoomMembers();
        }
    }

    @Override
    protected void onPause() {

        clearActiveChatRoom();

        if (messageController != null) {

            messageController.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        clearActiveChatRoom();

        if (messageController != null) {

            messageController.onDestroy();

            messageController = null;
        }

        super.onDestroy();
    }

    private void handleInviteLink() {

        Intent intent =
                getIntent();

        Uri data =
                intent.getData();

        if (data == null) {

            return;
        }

        List<String> segments =
                data.getPathSegments();

        if (segments.size() < 3) {

            return;
        }

        String inviteCode =
                segments.get(2);

        loadRoomAndJoin(
                inviteCode
        );
    }
    private void joinRoomByInviteCode(
            String inviteCode
    ) {

        if (myMemberId == null
                || myMemberId <= 0) {

            Toast.makeText(
                    this,
                    "로그인 정보가 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ApiService api =
                RetrofitClient.getApiService(
                        this
                );

        api.joinRoomByInviteCode(
                inviteCode,
                myMemberId
        ).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    Call<ResponseBody> call,
                    Response<ResponseBody> response
            ) {

                if (response.isSuccessful()) {

                    Toast.makeText(
                            MessageActivity.this,
                            "채팅방에 참여했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            MessageActivity.this,
                            "채팅방 참여 실패",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<ResponseBody> call,
                    Throwable t
            ) {

                Toast.makeText(
                        MessageActivity.this,
                        "서버 연결 실패",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadRoomAndJoin(
            String inviteCode
    ) {

        ApiService api =
                RetrofitClient.getApiService(
                        this
                );

        api.getRoomByInviteCode(
                inviteCode
        ).enqueue(new Callback<ChatRoom>() {

            @Override
            public void onResponse(
                    Call<ChatRoom> call,
                    Response<ChatRoom> response
            ) {

                if (!response.isSuccessful()
                        || response.body() == null) {

                    Toast.makeText(
                            MessageActivity.this,
                            "채팅방 정보를 찾을 수 없습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                ChatRoom room =
                        response.body();

                roomId =
                        room.getRoomId();

                applyRoomTitle(
                        room.getRoomName()
                );

                joinRoomByInviteCode(
                        inviteCode
                );

                if (messageController != null) {

                    messageController.onDestroy();

                    messageController = null;

                }

                if (roomId > 0) {

                    messageController =
                            new MessageController(
                                    MessageActivity.this,
                                    recyclerView,
                                    etMessage,
                                    roomId
                            );

                    messageController.start();

                    isControllerStarted = true;

                    saveActiveChatRoom();
                }



            }

            @Override
            public void onFailure(
                    Call<ChatRoom> call,
                    Throwable t
            ) {

                Toast.makeText(
                        MessageActivity.this,
                        "채팅방 조회 실패",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


}