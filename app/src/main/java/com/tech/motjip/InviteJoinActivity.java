package com.tech.motjip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tech.motjip.API.ApiService;
import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Model.ChatRoom;

import java.io.IOException;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InviteJoinActivity extends AppCompatActivity {

    private static final String TAG =
            "InviteJoinActivity";

    private ApiService apiService;

    private String inviteCode;

    private Long memberId;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        apiService =
                RetrofitClient.getApiService(
                        this
                );

        handleInviteDeepLink(
                getIntent()
        );
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

        handleInviteDeepLink(
                intent
        );
    }

    private void handleInviteDeepLink(
            Intent intent
    ) {

        if (intent == null) {

            showErrorAndGoMain(
                    "초대 링크 정보가 없습니다."
            );

            return;
        }

        Uri uri =
                intent.getData();

        if (uri == null) {

            showErrorAndGoMain(
                    "초대 링크가 올바르지 않습니다."
            );

            return;
        }

        Log.d(
                TAG,
                "딥링크 URI = "
                        + uri
        );

        inviteCode =
                uri.getQueryParameter(
                        "code"
                );

        if (inviteCode == null
                || inviteCode.trim().isEmpty()) {

            showErrorAndGoMain(
                    "초대 코드가 없습니다."
            );

            return;
        }

        inviteCode =
                inviteCode.trim();

        Log.d(
                TAG,
                "초대 코드 = "
                        + inviteCode
        );

        memberId =
                getLoginMemberId();

        if (memberId == null) {

            Toast.makeText(
                    this,
                    "로그인 후 초대 링크를 다시 열어주세요.",
                    Toast.LENGTH_SHORT
            ).show();

            Intent mainIntent =
                    new Intent(
                            InviteJoinActivity.this,
                            MainActivity.class
                    );

            mainIntent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(
                    mainIntent
            );

            finish();

            return;
        }

        Log.d(
                TAG,
                "로그인 memberId = "
                        + memberId
        );

        joinRoomByInviteCode();
    }

    private void joinRoomByInviteCode() {

        apiService.joinRoomByInviteCode(
                inviteCode,
                memberId
        ).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    Call<ResponseBody> call,
                    Response<ResponseBody> response
            ) {

                if (!response.isSuccessful()) {

                    String errorMessage =
                            parseErrorMessage(
                                    response.errorBody()
                            );

                    Log.e(
                            TAG,
                            "초대 참여 실패 code = "
                                    + response.code()
                                    + ", message = "
                                    + errorMessage
                    );

                    if (errorMessage == null
                            || errorMessage.trim().isEmpty()) {

                        errorMessage =
                                "채팅방 참여에 실패했습니다.";
                    }

                    showErrorAndGoMain(
                            errorMessage
                    );

                    return;
                }

                Log.d(
                        TAG,
                        "초대 참여 성공 code = "
                                + response.code()
                );

                loadRoomByInviteCode();
            }

            @Override
            public void onFailure(
                    Call<ResponseBody> call,
                    Throwable t
            ) {

                Log.e(
                        TAG,
                        "초대 참여 API 오류",
                        t
                );

                showErrorAndGoMain(
                        "서버 연결에 실패했습니다."
                );
            }
        });
    }

    private void loadRoomByInviteCode() {

        apiService.getRoomByInviteCode(
                inviteCode
        ).enqueue(new Callback<ChatRoom>() {

            @Override
            public void onResponse(
                    Call<ChatRoom> call,
                    Response<ChatRoom> response
            ) {

                if (!response.isSuccessful()
                        || response.body() == null) {

                    String errorMessage =
                            parseErrorMessage(
                                    response.errorBody()
                            );

                    Log.e(
                            TAG,
                            "초대 방 조회 실패 code = "
                                    + response.code()
                                    + ", message = "
                                    + errorMessage
                    );

                    if (errorMessage == null
                            || errorMessage.trim().isEmpty()) {

                        errorMessage =
                                "채팅방 정보를 불러오지 못했습니다.";
                    }

                    showErrorAndGoMain(
                            errorMessage
                    );

                    return;
                }

                ChatRoom chatRoom =
                        response.body();

                if (chatRoom.getRoomId() == null) {

                    Log.e(
                            TAG,
                            "roomId 없음"
                    );

                    showErrorAndGoMain(
                            "채팅방 ID가 없습니다."
                    );

                    return;
                }

                openMessageActivity(
                        chatRoom
                );
            }

            @Override
            public void onFailure(
                    Call<ChatRoom> call,
                    Throwable t
            ) {

                Log.e(
                        TAG,
                        "초대 방 조회 API 오류",
                        t
                );

                showErrorAndGoMain(
                        "채팅방 정보를 불러오지 못했습니다."
                );
            }
        });
    }

    private String parseErrorMessage(
            ResponseBody errorBody
    ) {

        if (errorBody == null) {

            return null;
        }

        try {

            String error =
                    errorBody.string();

            Log.e(
                    TAG,
                    "서버 errorBody = "
                            + error
            );

            return error;

        } catch (IOException e) {

            Log.e(
                    TAG,
                    "errorBody parse 실패",
                    e
            );
        }

        return null;
    }

    private void openMessageActivity(
            ChatRoom chatRoom
    ) {

        Toast.makeText(
                this,
                "채팅방에 참여했습니다.",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent =
                new Intent(
                        InviteJoinActivity.this,
                        MessageActivity.class
                );

        intent.putExtra(
                "roomId",
                chatRoom.getRoomId()
        );

        if (chatRoom.getRoomName() != null) {

            intent.putExtra(
                    "roomName",
                    chatRoom.getRoomName()
            );
        }

        if (chatRoom.getRoomType() != null) {

            intent.putExtra(
                    "roomType",
                    chatRoom.getRoomType()
            );
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        startActivity(
                intent
        );

        finish();
    }

    private Long getLoginMemberId() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "auth",
                        MODE_PRIVATE
                );

        long savedMemberId =
                preferences.getLong(
                        "memberId",
                        -1L
                );

        if (savedMemberId == -1L) {

            return null;
        }

        return savedMemberId;
    }

    private void showErrorAndGoMain(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

        Intent intent =
                new Intent(
                        InviteJoinActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        startActivity(
                intent
        );

        finish();
    }
}