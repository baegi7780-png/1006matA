package com.tech.motjip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tech.motjip.Auth.TokenManager;
import com.tech.motjip.Controller.NicknameController;
import com.tech.motjip.Dto.ResponseDto.LoginResponseDto;
import com.tech.motjip.Utils.DialogUtil;
import com.tech.motjip.Utils.LoginStateManager;

public class NicknameActivity extends AppCompatActivity
        implements NicknameController.NicknameControllerCallback {

    private static final String TAG =
            "NicknameActivityDebug";

    private Long memberId;
    private String nickname;

    private EditText etNickname;
    private Button btnSubmit;

    private NicknameController nicknameController;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_nickname
        );

        nicknameController =
                new NicknameController(
                        this,
                        this
                );

        Log.d(
                TAG,
                "onCreate() - NicknameActivity started"
        );

        try {

            memberId =
                    getIntent()
                            .getLongExtra(
                                    "member_id",
                                    -1L
                            );

            nickname =
                    getIntent()
                            .getStringExtra(
                                    "nickname"
                            );

            Log.d(
                    TAG,
                    "Intent memberId = " + memberId
            );

            Log.d(
                    TAG,
                    "Intent nickname = " + nickname
            );

            if (nickname != null
                    && !nickname.trim().isEmpty()) {

                Log.d(
                        TAG,
                        "이미 닉네임 존재 → MapActivity 이동"
                );

                LoginStateManager.setLoginStatus(
                        this,
                        LoginStateManager.LOGIN
                );

                Intent homeIntent =
                        new Intent(
                                this,
                                MapActivity.class
                        );

                /*
                 * 닉네임 화면을 뒤로가기 스택에 남기지 않기 위한 설정
                 */
                homeIntent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(
                        homeIntent
                );

                finish();

                return;
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Intent 처리 오류",
                    e
            );
        }

        etNickname =
                findViewById(
                        R.id.etNickname
                );

        btnSubmit =
                findViewById(
                        R.id.btnSubmit
                );

        btnSubmit.setOnClickListener(v -> {

            String inputNickname =
                    etNickname
                            .getText()
                            .toString()
                            .trim();

            Log.d(
                    TAG,
                    "닉네임 입력값 = " + inputNickname
            );

            if (memberId == null
                    || memberId == -1L) {

                Log.e(
                        TAG,
                        "memberId 없음"
                );

                Toast.makeText(
                        this,
                        "회원 정보가 없습니다. 다시 로그인해 주세요.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (inputNickname.isEmpty()) {

                Log.d(
                        TAG,
                        "닉네임 입력값 비어있음"
                );

                Toast.makeText(
                        this,
                        "닉네임을 입력해 주세요.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (inputNickname.length() < 2
                    || inputNickname.length() > 10) {

                Log.d(
                        TAG,
                        "닉네임 길이 오류 length = "
                                + inputNickname.length()
                );

                Toast.makeText(
                        this,
                        "닉네임은 2~10자 이내로 입력해 주세요.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            /*
             * 특수문자 제한 검사
             */
            if (!inputNickname.matches("^[a-zA-Z0-9가-힣]+$")) {

                Log.d(
                        TAG,
                        "닉네임 특수문자 포함"
                );

                DialogUtil.showMessageDialog(
                        this,
                        R.drawable.fail,
                        "닉네임 오류",
                        "닉네임에는 특수문자를 사용할 수 없습니다.",
                        null
                );

                return;
            }

            Log.d(
                    TAG,
                    "닉네임 변경 요청 시작 memberId = "
                            + memberId
                            + ", nickname = "
                            + inputNickname
            );

            nicknameController.updateNickname(
                    memberId,
                    inputNickname
            );
        });
    }

    @Override
    public void onNicknameSuccess(
            LoginResponseDto user
    ) {

        Log.d(
                TAG,
                "onNicknameSuccess 호출"
        );

        if (user == null) {

            Log.e(
                    TAG,
                    "onNicknameSuccess user null"
            );

            DialogUtil.showMessageDialog(
                    this,
                    R.drawable.fail,
                    "설정 실패",
                    "사용자 정보를 받지 못했습니다.",
                    null
            );

            return;
        }

        Log.d(
                TAG,
                "응답 memberId = " + user.getMemberId()
        );

        Log.d(
                TAG,
                "응답 nickname = " + user.getNickname()
        );

        Log.d(
                TAG,
                "accessToken exists = "
                        + (user.getAccessToken() != null
                        && !user.getAccessToken().trim().isEmpty())
        );

        Log.d(
                TAG,
                "refreshToken exists = "
                        + (user.getRefreshToken() != null
                        && !user.getRefreshToken().trim().isEmpty())
        );

        /*
         * 닉네임 설정 후 서버에서 새로 발급된 토큰 저장
         */
        if (user.getAccessToken() != null
                && !user.getAccessToken().trim().isEmpty()
                && user.getRefreshToken() != null
                && !user.getRefreshToken().trim().isEmpty()) {

            TokenManager tokenManager =
                    new TokenManager(
                            this
                    );

            tokenManager.saveTokens(
                    user.getAccessToken(),
                    user.getRefreshToken()
            );

            Log.d(
                    TAG,
                    "닉네임 설정 후 새 토큰 저장 완료"
            );

        } else {

            Log.e(
                    TAG,
                    "닉네임 설정 응답에 토큰 없음"
            );
        }

        /*
         * 로그인 상태 저장
         */
        LoginStateManager.setLoginStatus(
                this,
                LoginStateManager.LOGIN
        );

        Log.d(
                TAG,
                "로그인 상태 저장 완료"
        );

        DialogUtil.showMessageDialog(
                this,
                R.drawable.success,
                "설정 완료",
                "반가워요! 닉네임 설정이 완료되었습니다.",
                () -> {

                    Log.d(
                            TAG,
                            "MapActivity 이동"
                    );

                    Intent homeIntent =
                            new Intent(
                                    NicknameActivity.this,
                                    MapActivity.class
                            );

                    homeIntent.putExtra(
                            "LOGIN_USER_INFO",
                            user
                    );

                    /*
                     * 닉네임 설정 후 뒤로가기로 닉네임 화면에 돌아오지 않도록 처리
                     */
                    homeIntent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(
                            homeIntent
                    );

                    finish();
                }
        );
    }

    @Override
    public void onNicknameFail(
            String message
    ) {

        Log.e(
                TAG,
                "onNicknameFail message = "
                        + message
        );

        DialogUtil.showMessageDialog(
                this,
                R.drawable.fail,
                "설정 실패",
                message,
                null
        );
    }
}