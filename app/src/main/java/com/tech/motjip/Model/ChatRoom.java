package com.tech.motjip.Model;

import java.util.List;

public class ChatRoom {

    private Long roomId;

    private String roomName;

    // true = 사용자가 직접 입력한 채팅방 이름
    // false = 참여자 닉네임 기반 동적 제목
    private boolean customRoomName;

    private String roomType;

    private String opponentNickname;

    private String lastMessage;

    private String lastMessageType;

    private String time;

    private long unreadCount;

    private String inviteCode;

    private String inviteUrl;

    private List<String> participantProfileImages;

    public ChatRoom() {
    }

    public ChatRoom(
            Long roomId,
            String roomName,
            String lastMessage,
            String time
    ) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.lastMessage = lastMessage;
        this.time = time;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isCustomRoomName() {
        return customRoomName;
    }

    public void setCustomRoomName(boolean customRoomName) {
        this.customRoomName = customRoomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getOpponentNickname() {
        return opponentNickname;
    }

    public void setOpponentNickname(String opponentNickname) {
        this.opponentNickname = opponentNickname;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public void setInviteUrl(String inviteUrl) {
        this.inviteUrl = inviteUrl;
    }

    public List<String> getParticipantProfileImages() {

        return participantProfileImages;
    }

    public void setParticipantProfileImages(
            List<String> participantProfileImages
    ) {

        this.participantProfileImages =
                participantProfileImages;
    }
}