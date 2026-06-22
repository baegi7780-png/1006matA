package com.tech.motjip.API;

import com.tech.motjip.Dto.RequestDto.CreateRoomRequestDto;
import com.tech.motjip.Dto.RequestDto.DeleteNotificationRequestDto;
import com.tech.motjip.Dto.RequestDto.FcmTokenRequestDto;
import com.tech.motjip.Dto.RequestDto.KakaoSdkLoginRequestDto;
import com.tech.motjip.Dto.RequestDto.LogoutRequestDto;
import com.tech.motjip.Dto.RequestDto.NicknameUpdateRequestDto;
import com.tech.motjip.Dto.RequestDto.RefreshRequestDto;
import com.tech.motjip.Dto.RequestDto.StatusUpdateRequestDto;
import com.tech.motjip.Dto.RequestDto.UpdateLocationRequestDto;
import com.tech.motjip.Dto.RequestDto.UpdateMyNicknameRequestDto;
import com.tech.motjip.Dto.RequestDto.UpdateMyStatusSettingRequestDto;

import com.tech.motjip.Dto.ResponseDto.CommunityPostPageResponse;
import com.tech.motjip.Dto.ResponseDto.FriendRecommendationResponseDto;
import com.tech.motjip.Dto.ResponseDto.FriendResponseDto;
import com.tech.motjip.Dto.ResponseDto.FriendStatusResponseDto;
import com.tech.motjip.Dto.ResponseDto.LoginResponseDto;
import com.tech.motjip.Dto.ResponseDto.MyStatusSettingResponseDto;
import com.tech.motjip.Dto.ResponseDto.TokenResponseDto;

import com.tech.motjip.Model.ChatRoom;
import com.tech.motjip.Model.CommunityMember;
import com.tech.motjip.Model.CommunityPost;
import com.tech.motjip.Model.Message;
import com.tech.motjip.Model.NotificationItem;
import com.tech.motjip.Model.Participant;
import com.tech.motjip.Model.RecommendedPlace;
import com.tech.motjip.Model.Review;
import com.tech.motjip.Model.UploadResponse;
import com.tech.motjip.Model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @PATCH("/api/v1/auth/nickname")
    Call<LoginResponseDto> updateNickname(
            @Body NicknameUpdateRequestDto request
    );

    @PATCH("/api/v1/auth/me/nickname")
    Call<LoginResponseDto> updateMyNickname(
            @Body UpdateMyNicknameRequestDto request
    );

    @Multipart
    @PATCH("/api/v1/auth/me/profile-image")
    Call<LoginResponseDto> uploadProfileImage(
            @Part MultipartBody.Part image
    );

    @GET("/api/v1/user/me")
    Call<LoginResponseDto> getCurrentUser();

    @GET("/api/v1/user/users")
    Call<List<User>> getUsers();

    @POST("/api/v1/auth/refresh")
    Call<TokenResponseDto> refreshToken(
            @Body RefreshRequestDto request
    );

    @POST("/api/v1/auth/logout")
    Call<Void> logout(
            @Body LogoutRequestDto request
    );

    @PATCH("/api/v1/auth/me/fcm-token")
    Call<Void> updateFcmToken(
            @Body FcmTokenRequestDto request
    );

    @POST("/api/v1/auth/kakao")
    Call<LoginResponseDto> loginWithKakaoSdk(
            @Body KakaoSdkLoginRequestDto requestDto
    );

    @PATCH("/api/v1/auth/me/status")
    Call<Void> updateMyStatus(
            @Body StatusUpdateRequestDto request
    );

    @PATCH("/api/v1/auth/me/location")
    Call<Void> updateMyLocation(
            @Body UpdateLocationRequestDto request
    );

    @GET("/api/v1/member-status")
    Call<MyStatusSettingResponseDto> getMyStatusSetting();

    @PATCH("/api/v1/member-status")
    Call<MyStatusSettingResponseDto> updateMyStatusSetting(
            @Body UpdateMyStatusSettingRequestDto requestDto
    );

    @GET("/api/chat/rooms")
    Call<List<ChatRoom>> getChatRoomList();

    @GET("/api/chat/my-rooms/{memberId}")
    Call<List<ChatRoom>> getMyRooms(
            @Path("memberId") Long memberId
    );

    @GET("/api/chat/messages/{roomId}")
    Call<List<Message>> getChatMessages(
            @Path("roomId") Long roomId
    );

    @POST("/api/chat/rooms")
    Call<ChatRoom> createChatRoom(
            @Body ChatRoom chatRoom
    );

    @POST("/api/chat/rooms")
    Call<ChatRoom> createRoom(
            @Body CreateRoomRequestDto request
    );

    @GET("/api/chat/invite/{inviteCode}")
    Call<ChatRoom> getRoomByInviteCode(
            @Path("inviteCode") String inviteCode
    );

    @POST("/api/chat/invite/{inviteCode}/join")
    Call<ResponseBody> joinRoomByInviteCode(
            @Path("inviteCode") String inviteCode,
            @Query("memberId") Long memberId
    );

    @PATCH("/api/chat/read/{roomId}/{memberId}")
    Call<Map<String, Object>> updateReadTime(
            @Path("roomId") Long roomId,
            @Path("memberId") Long memberId
    );

    @DELETE("/api/chat/room/{roomId}/leave")
    Call<String> leaveRoom(
            @Path("roomId") Long roomId,
            @Query("memberId") Long memberId
    );

    @GET("/api/chat/rooms/{roomId}/members")
    Call<List<Participant>> getRoomMembers(
            @Path("roomId") Long roomId
    );

    @GET("/api/chat/direct-room")
    Call<ChatRoom> findDirectRoom(
            @Query("member1") Long member1,
            @Query("member2") Long member2
    );

    @DELETE("/api/chat/message/{messageId}")
    Call<Void> deleteMessage(
            @Path("messageId") Long messageId
    );

    @GET("/api/chat/participants/{roomId}")
    Call<List<FriendResponseDto>> getParticipants(
            @Path("roomId") Long roomId
    );

    @POST("/api/chat/rooms/{roomId}/invite/{friendId}")
    Call<String> inviteFriendToRoom(
            @Path("roomId") Long roomId,
            @Path("friendId") Long friendId,
            @Query("inviterId") Long inviterId
    );

    @POST("/api/chat/enter")
    Call<Map<String, Object>> enterChatRoom(
            @Query("memberId") Long memberId,
            @Query("roomId") Long roomId
    );

    @POST("/api/chat/exit")
    Call<Void> exitChatRoom(
            @Query("memberId") Long memberId,
            @Query("roomId") Long roomId
    );

    @Multipart
    @POST("/api/files/upload")
    Call<UploadResponse> uploadImage(
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("/api/v1/community/posts")
    Call<Void> createCommunityPost(
            @Part("tag") RequestBody tag,
            @Part("region") RequestBody region,
            @Part("title") RequestBody title,
            @Part("location") RequestBody location,
            @Part("date") RequestBody date,
            @Part("chatLink") RequestBody chatLink,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image
    );

    @Multipart
    @PATCH("/api/v1/community/posts/{comId}")
    Call<Void> updateCommunityPost(
            @Path("comId") Long comId,
            @Part("tag") RequestBody tag,
            @Part("region") RequestBody region,
            @Part("title") RequestBody title,
            @Part("location") RequestBody location,
            @Part("date") RequestBody date,
            @Part("chatLink") RequestBody chatLink,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image
    );

    @GET("/api/v1/community/posts")
    Call<CommunityPostPageResponse> getCommunityPosts(
            @Query("title") String title,
            @Query("tag") String tag,
            @Query("region") String region,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/v1/community/posts/closed")
    Call<CommunityPostPageResponse> getClosedCommunityPosts(
            @Query("title") String title,
            @Query("tag") String tag,
            @Query("region") String region,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/v1/community/posts/my")
    Call<CommunityPostPageResponse> getMyCommunityPosts(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/v1/community/posts/my/closed")
    Call<CommunityPostPageResponse> getMyClosedCommunityPosts(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/v1/community/posts/{comId}/members")
    Call<List<CommunityMember>> getCommunityMembers(
            @Path("comId") Long comId
    );

    @DELETE("/api/v1/community/posts/{comId}")
    Call<Void> deleteCommunityPost(
            @Path("comId") Long comId
    );

    @POST("/api/v1/favorites/community/{comId}/toggle")
    Call<Boolean> toggleFavoriteCommunityPost(
            @Path("comId") Long comId
    );

    @POST("/api/v1/community/posts/{comId}/join")
    Call<Void> joinCommunity(
            @Path("comId") Long comId
    );

    @PATCH("/api/v1/community/posts/{comId}/cancel")
    Call<Void> cancelJoinCommunity(
            @Path("comId") Long comId
    );

    @PATCH("/api/v1/community/posts/{comId}/hide")
    Call<Void> hideMyClosedCommunity(
            @Path("comId") Long comId
    );

    @PATCH("/api/v1/community/posts/{comId}/members/{targetMemberId}/kick")
    Call<Void> kickCommunityMember(
            @Path("comId") Long comId,
            @Path("targetMemberId") Long targetMemberId
    );

    @GET("/api/v1/favorites/community")
    Call<List<CommunityPost>> getFavoriteCommunityPosts();

    @POST("/api/v1/friends/requests/{receiverId}")
    Call<Void> sendFriendRequest(
            @Path("receiverId") Long receiverId
    );

    @PATCH("/api/v1/friends/requests/{friendRequestId}")
    Call<Void> respondFriendRequest(
            @Path("friendRequestId") Long friendRequestId,
            @Query("status") String status
    );

    @PATCH("/api/v1/friends/requests/{friendRequestId}/cancel")
    Call<Void> cancelFriendRequest(
            @Path("friendRequestId") Long friendRequestId
    );

    @GET("/api/v1/friends/status/{targetMemberId}")
    Call<FriendStatusResponseDto> getFriendStatus(
            @Path("targetMemberId") Long targetMemberId
    );

    @GET("/api/v1/friends")
    Call<List<FriendResponseDto>> getMyFriends();

    @GET("/api/v1/friends/recommendations")
    Call<List<FriendRecommendationResponseDto>> getFriendRecommendations();

    @DELETE("/api/v1/friends/{friendMemberId}")
    Call<Void> deleteFriend(
            @Path("friendMemberId") Long friendMemberId
    );

    @POST("/api/friends/add")
    Call<Void> addFriend(
            @Query("memberId") Long memberId,
            @Query("friendId") Long friendId
    );

    @GET("/notifications")
    Call<List<NotificationItem>> getNotifications();

    @PATCH("/notifications/{notificationId}/read")
    Call<Void> markNotificationAsRead(
            @Path("notificationId") Long notificationId
    );

    @DELETE("/notifications/{notificationId}")
    Call<Void> deleteNotification(
            @Path("notificationId") Long notificationId
    );

    @HTTP(
            method = "DELETE",
            path = "/notifications",
            hasBody = true
    )
    Call<Void> deleteNotifications(
            @Body DeleteNotificationRequestDto requestDto
    );

    @POST("/api/v1/community-invites/{communityId}/{receiverId}")
    Call<Void> sendCommunityInvite(
            @Path("communityId") Long communityId,
            @Path("receiverId") Long receiverId
    );

    @POST("/api/v1/community-invites/{communityInviteId}/respond")
    Call<Void> respondCommunityInvite(
            @Path("communityInviteId") Long communityInviteId,
            @Query("status") String status
    );

    @POST("/api/v1/community-invites/{communityInviteId}/cancel")
    Call<Void> cancelCommunityInvite(
            @Path("communityInviteId") Long communityInviteId
    );

    /*
     * 후기 등록
     * 이미지가 없는 경우 image 파라미터는 null 가능
     */
    @Multipart
    @POST("/api/reviews")
    Call<Void> createReview(
            @Part("placeId") RequestBody placeId,
            @Part("placeName") RequestBody placeName,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("memberId") RequestBody memberId,
            @Part("rating") RequestBody rating,
            @Part("revisit") RequestBody revisit,
            @Part("content") RequestBody content,
            @Part("tags") RequestBody tags,
            @Part MultipartBody.Part image
    );

    /*
     * 특정 장소 후기 목록 조회
     * memberId:
     * 현재 로그인 사용자
     * mine 여부 판별용
     */
    @GET("/api/reviews/{placeId}")
    Call<List<Review>> getReviews(
            @Path("placeId") Long placeId,
            @Query("memberId") Long memberId
    );

    /*
     * 추천 장소 조회
     *
     * 조건:
     * - 평균 평점 4.5 이상
     * - 랜덤 사용자 프로필 포함
     * - 평균 평점 소수점 첫째 자리
     */
    @GET("/api/reviews/recommended")
    Call<List<RecommendedPlace>> getRecommendedPlaces();

    /*
     * 후기 수정
     */
    @Multipart
    @PATCH("/api/reviews/{reviewId}")
    Call<Void> updateReview(
            @Path("reviewId") Long reviewId,
            @Part("placeName") RequestBody placeName,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("memberId") RequestBody memberId,
            @Part("rating") RequestBody rating,
            @Part("revisit") RequestBody revisit,
            @Part("content") RequestBody content,
            @Part("tags") RequestBody tags,
            @Part MultipartBody.Part image
    );

    /*
     * 후기 삭제
     */
    @DELETE("/api/reviews/{reviewId}")
    Call<Void> deleteReview(
            @Path("reviewId") Long reviewId,
            @Query("memberId") Long memberId
    );

    @GET("/notifications/unread-count")
    Call<Long> getUnreadNotificationCount();
}