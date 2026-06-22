package com.tech.motjip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Dto.RequestDto.LogoutRequestDto;
import com.tech.motjip.Dto.ResponseDto.LoginResponseDto;
import com.tech.motjip.Utils.DialogUtil;
import com.tech.motjip.manager.socket.SocketManager;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyInfoActivity extends AppCompatActivity {

    private Button btnEditNickname;
    private Button btnEditProfileImage;
    private Button btnMyStatusSetting;
    private Button btnLogout;
    private Button btnDeleteAccount;

    private static final String PREF_NAME = "AppPrefs";

    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    private static final String LOGIN_STATUS = "LOGIN_STATUS";

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_info);

        btnEditNickname = findViewById(R.id.btnEditNickname);
        btnEditProfileImage = findViewById(R.id.btnEditProfileImage);
        btnMyStatusSetting = findViewById(R.id.btnMyStatusSetting);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        initImagePicker();

        initCropImageLauncher();

        btnEditNickname.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MyInfoActivity.this,
                            EditNicknameActivity.class
                    );

            startActivity(intent);
        });

        btnEditProfileImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnMyStatusSetting.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MyInfoActivity.this,
                            MyStatusSettingActivity.class
                    );

            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());

        btnDeleteAccount.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "회원탈퇴 기능",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void logout() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String refreshToken =
                preferences.getString(
                        REFRESH_TOKEN,
                        null
                );

        if (refreshToken == null
                || refreshToken.trim().isEmpty()) {

            clearTokensAndMoveToMain();

            return;
        }

        LogoutRequestDto requestDto =
                new LogoutRequestDto(refreshToken);

        RetrofitClient.getApiService(this)
                .logout(requestDto)
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        clearTokensAndMoveToMain();
                    }

                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        clearTokensAndMoveToMain();
                    }
                });
    }

    private void clearTokensAndMoveToMain() {

        SocketManager.getInstance().disconnectAll();

        SharedPreferences preferences =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        preferences.edit()
                .remove(ACCESS_TOKEN)
                .remove(REFRESH_TOKEN)
                .putInt(LOGIN_STATUS, 0)
                .apply();

        getSharedPreferences(
                "chat_state",
                MODE_PRIVATE
        ).edit().clear().apply();

        Intent intent =
                new Intent(
                        MyInfoActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void initImagePicker() {

        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            if (uri != null) {

                                startCrop(uri);
                            }
                        }
                );
    }

    private void initCropImageLauncher() {

        cropImageLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode() == RESULT_OK
                                    && result.getData() != null) {

                                Uri croppedImageUri =
                                        UCrop.getOutput(
                                                result.getData()
                                        );

                                if (croppedImageUri != null) {

                                    uploadProfileImage(
                                            croppedImageUri
                                    );
                                }

                            } else if (result.getResultCode()
                                    == UCrop.RESULT_ERROR) {

                                DialogUtil.showMessageDialog(
                                        this,
                                        R.drawable.fail,
                                        "이미지 오류",
                                        "이미지 자르기에 실패했습니다.",
                                        null
                                );
                            }
                        }
                );
    }

    private void startCrop(Uri sourceUri) {

        String fileName =
                "profile_"
                        + System.currentTimeMillis()
                        + ".jpg";

        Uri destinationUri =
                Uri.fromFile(
                        new File(
                                getCacheDir(),
                                fileName
                        )
                );

        UCrop.Options options =
                new UCrop.Options();

        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCompressionQuality(90);
        options.setHideBottomControls(true);

        UCrop uCrop =
                UCrop.of(
                                sourceUri,
                                destinationUri
                        )
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(512, 512)
                        .withOptions(options);

        cropImageLauncher.launch(
                uCrop.getIntent(this)
        );
    }

    private void uploadProfileImage(
            Uri croppedImageUri
    ) {

        try {

            File imageFile =
                    uriToFile(croppedImageUri);

            RequestBody requestBody =
                    RequestBody.create(
                            MediaType.parse("image/jpeg"),
                            imageFile
                    );

            MultipartBody.Part image =
                    MultipartBody.Part.createFormData(
                            "image",
                            imageFile.getName(),
                            requestBody
                    );

            RetrofitClient.getApiService(this)
                    .uploadProfileImage(image)
                    .enqueue(new Callback<LoginResponseDto>() {

                        @Override
                        public void onResponse(
                                Call<LoginResponseDto> call,
                                Response<LoginResponseDto> response
                        ) {

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                DialogUtil.showMessageDialog(
                                        MyInfoActivity.this,
                                        R.drawable.success,
                                        "수정 완료",
                                        "프로필 이미지가 성공적으로 변경되었습니다.",
                                        () -> finish()
                                );

                            } else {

                                DialogUtil.showMessageDialog(
                                        MyInfoActivity.this,
                                        R.drawable.fail,
                                        "수정 실패",
                                        "프로필 이미지 수정 중 오류가 발생했습니다.",
                                        null
                                );
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<LoginResponseDto> call,
                                Throwable t
                        ) {

                            DialogUtil.showMessageDialog(
                                    MyInfoActivity.this,
                                    R.drawable.fail,
                                    "연결 오류",
                                    "서버와의 통신이 원활하지 않습니다.",
                                    null
                            );
                        }
                    });

        } catch (Exception e) {

            DialogUtil.showMessageDialog(
                    this,
                    R.drawable.fail,
                    "이미지 오류",
                    "이미지 처리 중 오류가 발생했습니다.",
                    null
            );
        }
    }

    private File uriToFile(Uri uri)
            throws Exception {

        File file =
                new File(
                        getCacheDir(),
                        "upload_profile_"
                                + System.currentTimeMillis()
                                + ".jpg"
                );

        InputStream inputStream =
                getContentResolver()
                        .openInputStream(uri);

        FileOutputStream outputStream =
                new FileOutputStream(file);

        byte[] buffer =
                new byte[1024];

        int length;

        while ((length = inputStream.read(buffer)) > 0) {

            outputStream.write(
                    buffer,
                    0,
                    length
            );
        }

        outputStream.close();
        inputStream.close();

        return file;
    }
}