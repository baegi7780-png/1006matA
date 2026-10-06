package com.tech.motjip.API.KakaoMap;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.KakaoMapSdk;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.tech.motjip.API.KakaoMap.CallbackInterface.IMapStartCallback;
import com.tech.motjip.API.KakaoMap.Utils.MapHelper;
import com.tech.motjip.Model.MapPostionVO;

import lombok.NonNull;

// 카카오맵을 초기화하고 뷰에 표시합니다.
public class KakaoMapStarter extends MapLifeCycleCallback {

    // 콜백함수의 객체입니다.
    private final IMapStartCallback callback;
    // 카카오맵을 표시할 맵뷰입니다.
    private final MapView mapView;
    // 카카오맵에 사용될 네이티브 앱키 입니다.
    //private final String apiKey = "1c820c4c55f310c4d33afecc66a6e8d4";
    private final String apiKey = "fc96e5701f247062fd7a0af17ce8e526";

    //private final String apiKey = "c2f31aa92db76b6d090b77365cf633bd";

    public KakaoMapStarter(@NonNull MapView mapView, @NonNull IMapStartCallback callback, @NonNull Activity activity){
        this.mapView = mapView;
        this.callback = callback;
        KakaoMapSdk.init(activity, apiKey);
    }

    // 카카오맵을 시작하고 뷰에 표현합니다.
    public void start(){
        mapView.start(this, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap kakaoMap) {
                // 준비가 완료되면 카카오맵 객체를 반환합니다
                callback.onReady(kakaoMap);
            }

            // 맵 로드시 시작 좌표
            @NonNull
            public LatLng getPosition() {
                return getDefaultPosition();
            }
        });
    }

    // 카카오맵을 시작하고 뷰에 표현하고 좌표에 따른 맵 시작점을 설정합니다.
    public void start(LatLng startPosition){
        mapView.start(this, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap kakaoMap) {
                // 준비가 완료되면 카카오맵 객체를 반환합니다
                callback.onReady(kakaoMap);
            }

            // 맵 로드시 시작 좌표 내가주기
            @Override
            @NonNull
            public LatLng getPosition() {
                return startPosition;
            }
        });
    }

    // 카카오맵 API의 네이티브 앱키를 가져옵니다.
    public String getAppKey()
    {
        return KakaoMapSdk.INSTANCE.getAppKey();
    }

    // 안드로이드 앱의 해쉬키를 가져옵니다.
    public String getHashKey()
    {
        return KakaoMapSdk.INSTANCE.getHashKey();
    }

    // 기본 디폴트 위치를 반환합니다.
    private LatLng getDefaultPosition(){
        // 임시로 피자헛 목동점 좌표 넣어둠 나중에 내 현재 GPS 위치 가져오는 함수로 바꿈
        MapPostionVO vo = new MapPostionVO(37.53660174890449, 126.8819899200535);
        return MapHelper.getLatLng(vo);
    }

    @Override
    public void onMapDestroy() {
        // 맵 객체 꺼졌을떄 처리
    }

    // 맵 에러 발생시 콜백함수에 에러 반환
    @Override
    public void onMapError(Exception e) {
        Log.e("MapDebug", "onMapError 호출됨", e);
        callback.onError(e);
    }
}
