package com.tech.motjip.Config;

public final class AppConfig {

    private AppConfig() {
    }

    public static final String BASE_URL =
            "https://spout-distant-cost.ngrok-free.dev";

    public static final String GOOGLE_REDIRECT_URI =
            BASE_URL + "/login/oauth2/code/google";

    public static final String KAKAO_REDIRECT_URI =
            BASE_URL + "/login/oauth2/code/kakao";

    public static final String UPLOAD_URL =
            BASE_URL + "/uploads";

    public static final String WS_URL =
            "wss://spout-distant-cost.ngrok-free.dev/ws/chat/websocket";
}