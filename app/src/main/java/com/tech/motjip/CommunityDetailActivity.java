package com.tech.motjip;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.button.MaterialButton;

import com.tech.motjip.API.ApiService;
import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Config.AppConfig;
import com.tech.motjip.Controller.CommunityDetailController;
import com.tech.motjip.Dto.ResponseDto.LoginResponseDto;
import com.tech.motjip.Model.ChatRoom;
import com.tech.motjip.Model.CommunityPost;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityDetailActivity
        extends AppCompatActivity {

    private static final String CHAT_LINK_DEBUG =
            "CHAT_LINK_DEBUG";

    private ImageView ivPostImage;

    private ImageView ivFavorite;
    private ImageView ivMore;

    private TextView tvTitle;
    private TextView tvTagRegion;
    private TextView tvPlace;
    private TextView tvDate;
    private TextView tvContent;
    private TextView tvMemberCount;

    private Button btnJoin;

    private Button btnOpenChatLink;

    private MaterialButton btnMemberList;

    private CommunityDetailController detailController;

    private CommunityPost post;

    private Long comId;

    private ActivityResultLauncher<Intent> editPostLauncher;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_community_detail
        );

        Log.e(
                CHAT_LINK_DEBUG,
                "CommunityDetailActivity onCreate"
        );

        editPostLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() == RESULT_OK) {

                                setResult(RESULT_OK);

                                finish();
                            }
                        }
                );

        detailController =
                new CommunityDetailController(this);

        ivPostImage =
                findViewById(R.id.ivPostImage);

        ivFavorite =
                findViewById(R.id.ivFavorite);

        ivMore =
                findViewById(R.id.ivMore);

        tvTitle =
                findViewById(R.id.tvTitle);

        tvTagRegion =
                findViewById(R.id.tvTagRegion);

        tvPlace =
                findViewById(R.id.tvPlace);

        tvDate =
                findViewById(R.id.tvDate);

        tvContent =
                findViewById(R.id.tvContent);

        tvMemberCount =
                findViewById(R.id.tvMemberCount);

        btnJoin =
                findViewById(R.id.btnJoin);

        btnOpenChatLink =
                findViewById(R.id.btnOpenChatLink);

        btnMemberList =
                findViewById(R.id.btnMemberList);

        post =
                (CommunityPost) getIntent()
                        .getSerializableExtra(
                                "communityPost"
                        );

        if (post == null || post.getComId() == null) {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "post null 또는 comId null"
            );

            Toast.makeText(
                    this,
                    "게시글 정보가 올바르지 않습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        Log.e(
                CHAT_LINK_DEBUG,
                "post loaded comId="
                        + post.getComId()
                        + ", isMine="
                        + post.isMine()
                        + ", isJoined="
                        + post.isJoined()
                        + ", chatLink="
                        + post.getChatLink()
        );

        comId =
                post.getComId();

        setupPostData();

        setupTopButtons();

        setupJoinedState();

        setupJoinButton();

        setupChatLinkButton();

        setupMemberListButton();

        ivPostImage.setOnClickListener(v -> {
            showImagePreviewDialog();
        });
    }

    private void setupTopButtons() {

        if (post.isMine()) {

            ivFavorite.setVisibility(
                    View.GONE
            );

            ivMore.setVisibility(
                    View.VISIBLE
            );

        } else {

            ivFavorite.setVisibility(
                    View.VISIBLE
            );

            ivMore.setVisibility(
                    View.GONE
            );
        }

        ivFavorite.setColorFilter(
                detailController.getFavoriteColor(
                        post.isFavorite()
                )
        );

        ivMore.setOnClickListener(v -> {

            PopupMenu popupMenu =
                    new PopupMenu(
                            CommunityDetailActivity.this,
                            ivMore
                    );

            popupMenu.getMenu().add(
                    "수정하기"
            );

            popupMenu.getMenu().add(
                    "삭제하기"
            );

            popupMenu.setOnMenuItemClickListener(item -> {

                String title =
                        item.getTitle().toString();

                if (title.equals("수정하기")) {

                    Intent intent =
                            new Intent(
                                    CommunityDetailActivity.this,
                                    WriteActivity.class
                            );

                    intent.putExtra(
                            "isEditMode",
                            true
                    );

                    intent.putExtra(
                            "communityPost",
                            post
                    );

                    editPostLauncher.launch(intent);

                } else if (title.equals("삭제하기")) {

                    showDeleteConfirmDialog();
                }

                return true;
            });

            popupMenu.show();
        });

        ivFavorite.setOnClickListener(v -> {

            detailController.toggleFavorite(
                    post.getComId(),
                    new CommunityDetailController.FavoriteCallback() {

                        @Override
                        public void onSuccess(
                                boolean isFavorite
                        ) {

                            post.setFavorite(
                                    isFavorite
                            );

                            ivFavorite.setColorFilter(
                                    detailController.getFavoriteColor(
                                            isFavorite
                                    )
                            );

                            if (isFavorite) {

                                Toast.makeText(
                                        CommunityDetailActivity.this,
                                        "즐겨찾기에 추가되었습니다.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        CommunityDetailActivity.this,
                                        "즐겨찾기가 취소되었습니다.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            setResult(RESULT_OK);
                        }

                        @Override
                        public void onError(
                                String message
                        ) {

                            Toast.makeText(
                                    CommunityDetailActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        });
    }

    private void showDeleteConfirmDialog() {

        new AlertDialog.Builder(this)
                .setTitle("모임 삭제")
                .setMessage("정말 이 모임을 삭제하시겠습니까?")
                .setNegativeButton(
                        "취소",
                        null
                )
                .setPositiveButton(
                        "삭제",
                        (dialog, which) -> deleteCommunity()
                )
                .show();
    }

    private void setupPostData() {

        tvTitle.setText(
                post.getTitle()
        );

        tvTagRegion.setText(
                post.getTag()
                        + " · "
                        + post.getRegion()
        );

        tvPlace.setText(
                post.getPlaceName()
        );

        tvDate.setText(
                detailController.formatMeetingAt(
                        post.getMeetingAt()
                )
        );

        tvContent.setText(
                post.getContent()
        );

        tvMemberCount.setText(
                post.getMemberCount()
                        + "명 참여중"
        );

        detailController.loadPostImage(
                ivPostImage,
                post.getImageUrl()
        );
    }

    private void showImagePreviewDialog() {

        if (post == null
                || post.getImageUrl() == null
                || post.getImageUrl().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "확대할 이미지가 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PhotoView previewImageView =
                new PhotoView(this);

        int width =
                (int) (getResources()
                        .getDisplayMetrics()
                        .widthPixels * 0.92);

        int height =
                (int) (getResources()
                        .getDisplayMetrics()
                        .heightPixels * 0.65);

        previewImageView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        width,
                        height
                )
        );

        previewImageView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        previewImageView.setBackgroundColor(
                Color.BLACK
        );

        previewImageView.setPadding(
                12,
                12,
                12,
                12
        );

        detailController.loadPostImage(
                previewImageView,
                post.getImageUrl()
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(previewImageView)
                        .create();

        previewImageView.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {

            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(
                            Color.parseColor("#DD000000")
                    )
            );
        }
    }

    private void setupJoinedState() {

        boolean closed =
                detailController.isMeetingClosed(
                        post.getMeetingAt()
                );

        if (closed) {

            btnJoin.setVisibility(
                    View.VISIBLE
            );

            btnJoin.setEnabled(true);

            btnJoin.setAlpha(1f);

            if (post.isMine()) {

                btnJoin.setText(
                        "모임 삭제"
                );

            } else {

                btnJoin.setText(
                        "목록 숨기기"
                );
            }

            return;
        }

        if (post.isMine()) {

            btnJoin.setVisibility(
                    View.GONE
            );

            return;
        }

        if (post.isJoined()) {

            btnJoin.setText(
                    "참여 취소"
            );

        } else {

            btnJoin.setText(
                    "모임 참여하기"
            );
        }

        btnJoin.setEnabled(true);

        btnJoin.setAlpha(1f);
    }

    private void setupJoinButton() {

        btnJoin.setOnClickListener(v -> {

            if (detailController.isMeetingClosed(
                    post.getMeetingAt()
            )) {

                btnJoin.setEnabled(false);

                if (post.isMine()) {

                    showDeleteConfirmDialog();

                } else {

                    hideClosedCommunity();
                }

                return;
            }

            btnJoin.setEnabled(false);

            if (post.isJoined()) {

                cancelJoin();

            } else {

                joinCommunity();
            }
        });
    }

    private void setupChatLinkButton() {

        Log.e(
                CHAT_LINK_DEBUG,
                "setupChatLinkButton isMine="
                        + post.isMine()
                        + ", isJoined="
                        + post.isJoined()
                        + ", chatLink="
                        + post.getChatLink()
        );

        if (!post.isJoined() && !post.isMine()) {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "채팅 링크 버튼 숨김: 참여자 아님"
            );

            btnOpenChatLink.setVisibility(
                    View.GONE
            );

            return;
        }

        String chatLink =
                post.getChatLink();

        if (chatLink == null
                || chatLink.trim().isEmpty()) {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "채팅 링크 버튼 숨김: chatLink 없음"
            );

            btnOpenChatLink.setVisibility(
                    View.GONE
            );

            return;
        }

        String finalChatLink =
                chatLink.trim();

        String lowerLink =
                finalChatLink.toLowerCase();

        btnOpenChatLink.setVisibility(
                View.VISIBLE
        );

        boolean isMotjipLink =
                isMotjipChatLink(
                        finalChatLink
                );

        Log.e(
                CHAT_LINK_DEBUG,
                "finalChatLink="
                        + finalChatLink
                        + ", isMotjipLink="
                        + isMotjipLink
        );

        if (isMotjipLink) {

            btnOpenChatLink.setText(
                    "앱 채팅방 참여하기"
            );

        } else if (lowerLink.contains("open.kakao.com")) {

            btnOpenChatLink.setText(
                    "오픈채팅 참여하기"
            );

        } else if (lowerLink.contains("discord.gg")
                || lowerLink.contains("discord.com")) {

            btnOpenChatLink.setText(
                    "디스코드 참여하기"
            );

        } else {

            btnOpenChatLink.setText(
                    "링크 열기"
            );
        }

        btnOpenChatLink.setOnClickListener(v -> {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "채팅 링크 버튼 클릭 link="
                            + finalChatLink
            );

            if (isMotjipChatLink(finalChatLink)) {

                openMotjipChatLink(
                        finalChatLink
                );

            } else {

                openExternalLink(
                        finalChatLink
                );
            }
        });

        btnOpenChatLink.setOnLongClickListener(v -> {

            try {

                ClipboardManager clipboardManager =
                        (ClipboardManager) getSystemService(
                                CLIPBOARD_SERVICE
                        );

                ClipData clipData =
                        ClipData.newPlainText(
                                "chat_link",
                                finalChatLink
                        );

                if (clipboardManager != null) {

                    clipboardManager.setPrimaryClip(
                            clipData
                    );

                    Toast.makeText(
                            CommunityDetailActivity.this,
                            "링크가 복사되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return true;
                }

            } catch (Exception e) {

                Log.e(
                        CHAT_LINK_DEBUG,
                        "링크 복사 실패",
                        e
                );

                Toast.makeText(
                        CommunityDetailActivity.this,
                        "링크 복사에 실패했습니다.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return false;
        });
    }

    private boolean isMotjipChatLink(
            String link
    ) {

        if (link == null
                || link.trim().isEmpty()) {

            return false;
        }

        String trimmedLink =
                link.trim();

        String lowerLink =
                trimmedLink.toLowerCase();

        if (lowerLink.startsWith("motjip://")
                && lowerLink.contains("invite")) {

            return true;
        }

        String appBaseUrl =
                AppConfig.BASE_URL.toLowerCase();

        return lowerLink.startsWith(appBaseUrl)
                && lowerLink.contains("/invite/");
    }

    private String extractInviteCode(
            String link
    ) {

        if (link == null
                || link.trim().isEmpty()) {

            return null;
        }

        try {

            Uri uri =
                    Uri.parse(
                            link.trim()
                    );

            if (uri.getPathSegments() == null
                    || uri.getPathSegments().isEmpty()) {

                return null;
            }

            Log.e(
                    CHAT_LINK_DEBUG,
                    "extractInviteCode pathSegments="
                            + uri.getPathSegments()
            );

            for (int i = 0; i < uri.getPathSegments().size(); i++) {

                String segment =
                        uri.getPathSegments().get(i);

                if ("invite".equalsIgnoreCase(segment)
                        && i + 1 < uri.getPathSegments().size()) {

                    String inviteCode =
                            uri.getPathSegments().get(i + 1);

                    if (inviteCode != null
                            && !inviteCode.trim().isEmpty()
                            && !"join".equalsIgnoreCase(inviteCode)) {

                        return inviteCode.trim();
                    }
                }
            }

            String lastSegment =
                    uri.getLastPathSegment();

            if (lastSegment != null
                    && !lastSegment.trim().isEmpty()
                    && !"join".equalsIgnoreCase(lastSegment)) {

                return lastSegment.trim();
            }

        } catch (Exception e) {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "extractInviteCode 실패",
                    e
            );

            return null;
        }

        return null;
    }

    private void openMotjipChatLink(
            String link
    ) {

        Log.e(
                CHAT_LINK_DEBUG,
                "openMotjipChatLink start link="
                        + link
        );

        String inviteCode =
                extractInviteCode(
                        link
                );

        Log.e(
                CHAT_LINK_DEBUG,
                "openMotjipChatLink inviteCode="
                        + inviteCode
        );

        if (inviteCode == null
                || inviteCode.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "채팅방 초대코드를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        Log.e(
                CHAT_LINK_DEBUG,
                "getCurrentUser 호출 시작"
        );

        apiService.getCurrentUser()
                .enqueue(new Callback<LoginResponseDto>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponseDto> call,
                            Response<LoginResponseDto> response
                    ) {

                        Log.e(
                                CHAT_LINK_DEBUG,
                                "getCurrentUser response code="
                                        + response.code()
                                        + ", success="
                                        + response.isSuccessful()
                                        + ", body="
                                        + response.body()
                        );

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().getMemberId() <= 0) {

                            Toast.makeText(
                                    CommunityDetailActivity.this,
                                    "로그인 정보를 확인할 수 없습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        Long memberId =
                                response.body()
                                        .getMemberId();

                        Log.e(
                                CHAT_LINK_DEBUG,
                                "getCurrentUser memberId="
                                        + memberId
                        );

                        joinMotjipChatRoom(
                                inviteCode,
                                memberId
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponseDto> call,
                            Throwable t
                    ) {

                        Log.e(
                                CHAT_LINK_DEBUG,
                                "getCurrentUser failure",
                                t
                        );

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "로그인 정보 조회 실패",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void joinMotjipChatRoom(
            String inviteCode,
            Long memberId
    ) {

        Log.e(
                CHAT_LINK_DEBUG,
                "joinMotjipChatRoom start inviteCode="
                        + inviteCode
                        + ", memberId="
                        + memberId
        );

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        apiService.joinRoomByInviteCode(
                inviteCode,
                memberId
        ).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    Call<ResponseBody> call,
                    Response<ResponseBody> response
            ) {

                Log.e(
                        CHAT_LINK_DEBUG,
                        "joinRoomByInviteCode response code="
                                + response.code()
                                + ", success="
                                + response.isSuccessful()
                );

                if (response.isSuccessful()) {

                    loadRoomByInviteCodeAndMove(
                            inviteCode
                    );

                } else {

                    Toast.makeText(
                            CommunityDetailActivity.this,
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

                Log.e(
                        CHAT_LINK_DEBUG,
                        "joinRoomByInviteCode failure",
                        t
                );

                Toast.makeText(
                        CommunityDetailActivity.this,
                        "서버 연결 실패",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadRoomByInviteCodeAndMove(
            String inviteCode
    ) {

        Log.e(
                CHAT_LINK_DEBUG,
                "loadRoomByInviteCodeAndMove start inviteCode="
                        + inviteCode
        );

        ApiService apiService =
                RetrofitClient.getApiService(
                        this
                );

        apiService.getRoomByInviteCode(
                inviteCode
        ).enqueue(new Callback<ChatRoom>() {

            @Override
            public void onResponse(
                    Call<ChatRoom> call,
                    Response<ChatRoom> response
            ) {

                Log.e(
                        CHAT_LINK_DEBUG,
                        "getRoomByInviteCode response code="
                                + response.code()
                                + ", success="
                                + response.isSuccessful()
                                + ", body="
                                + response.body()
                );

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().getRoomId() == null) {

                    Toast.makeText(
                            CommunityDetailActivity.this,
                            "채팅방 정보를 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                ChatRoom room =
                        response.body();

                Log.e(
                        CHAT_LINK_DEBUG,
                        "getRoomByInviteCode result roomId="
                                + room.getRoomId()
                                + ", roomName="
                                + room.getRoomName()
                                + ", roomType="
                                + room.getRoomType()
                );

                Intent intent =
                        new Intent(
                                CommunityDetailActivity.this,
                                MessageActivity.class
                        );

                intent.putExtra(
                        "roomId",
                        room.getRoomId()
                );

                intent.putExtra(
                        "roomName",
                        room.getRoomName()
                );

                intent.putExtra(
                        "roomType",
                        room.getRoomType()
                );

                Log.e(
                        CHAT_LINK_DEBUG,
                        "MessageActivity 이동 roomId="
                                + room.getRoomId()
                );

                startActivity(
                        intent
                );
            }

            @Override
            public void onFailure(
                    Call<ChatRoom> call,
                    Throwable t
            ) {

                Log.e(
                        CHAT_LINK_DEBUG,
                        "getRoomByInviteCode failure",
                        t
                );

                Toast.makeText(
                        CommunityDetailActivity.this,
                        "채팅방 정보 조회 실패",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void openExternalLink(
            String link
    ) {

        try {

            String openLink =
                    link;

            if (!openLink.startsWith("http://")
                    && !openLink.startsWith("https://")) {

                openLink =
                        "https://" + openLink;
            }

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(openLink)
                    );

            if (intent.resolveActivity(getPackageManager()) != null) {

                startActivity(intent);

            } else {

                Toast.makeText(
                        CommunityDetailActivity.this,
                        "링크를 열 수 있는 앱이 없습니다.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {

            Log.e(
                    CHAT_LINK_DEBUG,
                    "외부 링크 열기 실패",
                    e
            );

            Toast.makeText(
                    CommunityDetailActivity.this,
                    "링크를 열 수 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setupMemberListButton() {

        btnMemberList.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            CommunityDetailActivity.this,
                            CommunityMemberListActivity.class
                    );

            intent.putExtra(
                    "comId",
                    comId
            );

            startActivity(intent);
        });
    }

    private void joinCommunity() {

        detailController.joinCommunity(
                comId,
                new CommunityDetailController.JoinCallback() {

                    @Override
                    public void onSuccess() {

                        post.setJoined(true);

                        post.setMemberCount(
                                post.getMemberCount() + 1
                        );

                        tvMemberCount.setText(
                                post.getMemberCount()
                                        + "명 참여중"
                        );

                        setupChatLinkButton();

                        setResult(RESULT_OK);

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "모임 참여 완료",
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setText(
                                "참여 취소"
                        );

                        btnJoin.setEnabled(true);
                    }

                    @Override
                    public void onAlreadyJoined() {

                        post.setJoined(true);

                        setupChatLinkButton();

                        setResult(RESULT_OK);

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "이미 참여한 모임입니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setText(
                                "참여 취소"
                        );

                        btnJoin.setEnabled(true);
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setEnabled(true);
                    }
                }
        );
    }

    private void cancelJoin() {

        detailController.cancelJoinCommunity(
                comId,
                new CommunityDetailController.BasicCallback() {

                    @Override
                    public void onSuccess() {

                        post.setJoined(false);

                        if (post.getMemberCount() > 0) {

                            post.setMemberCount(
                                    post.getMemberCount() - 1
                            );
                        }

                        tvMemberCount.setText(
                                post.getMemberCount()
                                        + "명 참여중"
                        );

                        setupChatLinkButton();

                        setResult(RESULT_OK);

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "모임 참여가 취소되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setText(
                                "모임 참여하기"
                        );

                        btnJoin.setEnabled(true);
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setEnabled(true);
                    }
                }
        );
    }

    private void hideClosedCommunity() {

        detailController.hideClosedCommunity(
                comId,
                new CommunityDetailController.BasicCallback() {

                    @Override
                    public void onSuccess() {

                        setResult(RESULT_OK);

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "목록에서 숨김 처리되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setEnabled(true);
                    }
                }
        );
    }

    private void deleteCommunity() {

        detailController.deleteCommunity(
                comId,
                new CommunityDetailController.BasicCallback() {

                    @Override
                    public void onSuccess() {

                        setResult(RESULT_OK);

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                "모임이 삭제되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        Toast.makeText(
                                CommunityDetailActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        btnJoin.setEnabled(true);
                    }
                }
        );
    }
}