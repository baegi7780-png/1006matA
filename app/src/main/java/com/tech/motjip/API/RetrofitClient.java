package com.tech.motjip.API;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.tech.motjip.Config.AppConfig;
import com.tech.motjip.Dto.RequestDto.RefreshRequestDto;
import com.tech.motjip.Dto.ResponseDto.TokenResponseDto;
import com.tech.motjip.MainActivity;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String TAG =
            "RetrofitClientDebug";

    public static final String BASE_URL =
            AppConfig.BASE_URL + "/";

    private static final String PREF_NAME =
            "AppPrefs";

    private static final String ACCESS_TOKEN =
            "ACCESS_TOKEN";

    private static final String REFRESH_TOKEN =
            "REFRESH_TOKEN";

    public static final String AUTH_EXPIRED_MESSAGE =
            "AUTH_EXPIRED_MESSAGE";

    private static Retrofit retrofit = null;

    private static ApiService apiService = null;

    public static ApiService getApiService(
            Context context
    ) {

        if (apiService == null || retrofit == null) {

            Context appContext =
                    context.getApplicationContext();

            HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor();

            loggingInterceptor.setLevel(
                    HttpLoggingInterceptor.Level.BASIC
            );

            Interceptor authInterceptor = chain -> {

                SharedPreferences prefs =
                        appContext.getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );

                String accessToken =
                        prefs.getString(
                                ACCESS_TOKEN,
                                null
                        );

                String refreshToken =
                        prefs.getString(
                                REFRESH_TOKEN,
                                null
                        );

                Log.d(
                        TAG,
                        "accessToken exists = "
                                + (accessToken != null
                                && !accessToken.trim().isEmpty())
                );

                if (accessToken != null
                        && accessToken.length() > 20) {

                    Log.d(
                            TAG,
                            "accessToken prefix = "
                                    + accessToken.substring(
                                    0,
                                    20
                            )
                    );
                }

                Log.d(
                        TAG,
                        "refreshToken exists = "
                                + (refreshToken != null
                                && !refreshToken.trim().isEmpty())
                );

                if (refreshToken != null
                        && refreshToken.length() > 20) {

                    Log.d(
                            TAG,
                            "refreshToken prefix = "
                                    + refreshToken.substring(
                                    0,
                                    20
                            )
                    );
                }

                Request originalRequest =
                        chain.request();

                String originalPath =
                        originalRequest
                                .url()
                                .encodedPath();

                Log.d(
                        TAG,
                        "요청 URL = "
                                + originalPath
                );

                boolean isRefreshRequest =
                        originalPath.contains(
                                "/api/v1/auth/refresh"
                        );

                Log.d(
                        TAG,
                        "isRefreshRequest = "
                                + isRefreshRequest
                );

                Request.Builder requestBuilder =
                        originalRequest.newBuilder();

                if (!isRefreshRequest
                        && accessToken != null
                        && !accessToken.trim().isEmpty()) {

                    Log.d(
                            TAG,
                            "Authorization Header 추가"
                    );

                    requestBuilder.header(
                            "Authorization",
                            "Bearer " + accessToken
                    );

                } else {

                    Log.d(
                            TAG,
                            "Authorization Header 추가 안함"
                    );
                }

                Response response =
                        chain.proceed(
                                requestBuilder.build()
                        );

                Log.d(
                        TAG,
                        "응답 code = "
                                + response.code()
                                + ", path = "
                                + originalPath
                );

                if (response.code() == 401
                        && !isRefreshRequest) {

                    Log.w(
                            TAG,
                            "AccessToken 만료 또는 인증 실패"
                    );

                    response.close();

                    if (refreshToken == null
                            || refreshToken.trim().isEmpty()) {

                        Log.d(
                                TAG,
                                "RefreshToken 없음 → 로그인 화면 이동"
                        );

                        moveToLoginWithExpiredMessage(
                                appContext,
                                prefs
                        );

                        throw new IOException(
                                "RefreshToken is empty"
                        );
                    }

                    Log.d(
                            TAG,
                            "RefreshToken 있음 → 토큰 재발급 시도"
                    );

                    String newAccessToken =
                            refreshAccessToken(
                                    appContext,
                                    refreshToken
                            );

                    if (newAccessToken == null) {

                        Log.e(
                                TAG,
                                "토큰 재발급 실패 → 자동 로그아웃"
                        );

                        moveToLoginWithExpiredMessage(
                                appContext,
                                prefs
                        );

                        throw new IOException(
                                "Token refresh failed"
                        );
                    }

                    Log.d(
                            TAG,
                            "토큰 재발급 성공 → 원래 요청 재시도"
                    );

                    if (newAccessToken.length() > 20) {

                        Log.d(
                                TAG,
                                "retry newAccessToken prefix = "
                                        + newAccessToken.substring(
                                        0,
                                        20
                                )
                        );
                    }

                    Request retryRequest =
                            originalRequest
                                    .newBuilder()
                                    .header(
                                            "Authorization",
                                            "Bearer " + newAccessToken
                                    )
                                    .build();

                    Response retryResponse =
                            chain.proceed(
                                    retryRequest
                            );

                    Log.d(
                            TAG,
                            "재시도 응답 code = "
                                    + retryResponse.code()
                                    + ", path = "
                                    + originalPath
                    );

                    return retryResponse;
                }

                return response;
            };

            OkHttpClient httpClient =
                    new OkHttpClient.Builder()
                            .connectTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )
                            .readTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )
                            .writeTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )
                            .addInterceptor(
                                    authInterceptor
                            )
                            .addInterceptor(
                                    loggingInterceptor
                            )
                            .build();

            retrofit =
                    new Retrofit.Builder()
                            .baseUrl(
                                    BASE_URL
                            )
                            .client(
                                    httpClient
                            )
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();

            apiService =
                    retrofit.create(
                            ApiService.class
                    );
        }

        return apiService;
    }

    private static String refreshAccessToken(
            Context context,
            String refreshToken
    ) {

        try {

            Log.d(
                    TAG,
                    "refreshAccessToken 시작"
            );

            if (refreshToken != null
                    && refreshToken.length() > 20) {

                Log.d(
                        TAG,
                        "refreshAccessToken refreshToken prefix = "
                                + refreshToken.substring(
                                0,
                                20
                        )
                );
            }

            OkHttpClient refreshClient =
                    new OkHttpClient.Builder()
                            .connectTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )
                            .readTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )
                            .writeTimeout(
                                    60,
                                    TimeUnit.SECONDS
                            )
                            .build();

            Retrofit refreshRetrofit =
                    new Retrofit.Builder()
                            .baseUrl(
                                    BASE_URL
                            )
                            .client(
                                    refreshClient
                            )
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();

            ApiService refreshApiService =
                    refreshRetrofit.create(
                            ApiService.class
                    );

            retrofit2.Response<TokenResponseDto> refreshResponse =
                    refreshApiService
                            .refreshToken(
                                    new RefreshRequestDto(
                                            refreshToken
                                    )
                            )
                            .execute();

            Log.d(
                    TAG,
                    "refreshResponse code = "
                            + refreshResponse.code()
            );

            Log.d(
                    TAG,
                    "refreshResponse body = "
                            + refreshResponse.body()
            );

            if (!refreshResponse.isSuccessful()
                    && refreshResponse.errorBody() != null) {

                String errorBody =
                        refreshResponse
                                .errorBody()
                                .string();

                Log.e(
                        TAG,
                        "refresh error = "
                                + errorBody
                );
            }

            if (refreshResponse.isSuccessful()
                    && refreshResponse.body() != null) {

                TokenResponseDto tokenResponse =
                        refreshResponse.body();

                String newAccessToken =
                        tokenResponse.getAccessToken();

                String newRefreshToken =
                        tokenResponse.getRefreshToken();

                Log.d(
                        TAG,
                        "newAccessToken exists = "
                                + (newAccessToken != null
                                && !newAccessToken.trim().isEmpty())
                );

                Log.d(
                        TAG,
                        "newRefreshToken exists = "
                                + (newRefreshToken != null
                                && !newRefreshToken.trim().isEmpty())
                );

                if (newAccessToken != null
                        && newAccessToken.length() > 20) {

                    Log.d(
                            TAG,
                            "newAccessToken prefix = "
                                    + newAccessToken.substring(
                                    0,
                                    20
                            )
                    );
                }

                if (newRefreshToken != null
                        && newRefreshToken.length() > 20) {

                    Log.d(
                            TAG,
                            "newRefreshToken prefix = "
                                    + newRefreshToken.substring(
                                    0,
                                    20
                            )
                    );
                }

                if (newAccessToken == null
                        || newAccessToken.trim().isEmpty()
                        || newRefreshToken == null
                        || newRefreshToken.trim().isEmpty()) {

                    Log.e(
                            TAG,
                            "재발급 응답에 토큰 값이 없음"
                    );

                    return null;
                }

                SharedPreferences prefs =
                        context.getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );

                prefs.edit()
                        .putString(
                                ACCESS_TOKEN,
                                newAccessToken
                        )
                        .putString(
                                REFRESH_TOKEN,
                                newRefreshToken
                        )
                        .apply();

                Log.d(
                        TAG,
                        "새 토큰 저장 완료"
                );

                return newAccessToken;
            }

        } catch (IOException e) {

            Log.e(
                    TAG,
                    "refreshAccessToken IOException",
                    e
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "refreshAccessToken Exception",
                    e
            );
        }

        return null;
    }

    private static void moveToLoginWithExpiredMessage(
            Context context,
            SharedPreferences prefs
    ) {

        Log.d(
                TAG,
                "moveToLoginWithExpiredMessage 실행"
        );

        clearTokens(
                prefs
        );

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        intent.putExtra(
                AUTH_EXPIRED_MESSAGE,
                "다시 로그인해 주세요."
        );

        context.startActivity(
                intent
        );
    }

    private static void clearTokens(
            SharedPreferences prefs
    ) {

        Log.d(
                TAG,
                "토큰 삭제 실행"
        );

        prefs.edit()
                .remove(
                        ACCESS_TOKEN
                )
                .remove(
                        REFRESH_TOKEN
                )
                .apply();

        Log.d(
                TAG,
                "토큰 삭제 완료"
        );
    }
}