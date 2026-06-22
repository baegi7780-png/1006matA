package com.tech.motjip.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tech.motjip.Config.AppConfig;
import com.tech.motjip.Dto.ResponseDto.FriendRecommendationResponseDto;
import com.tech.motjip.Dto.ResponseDto.FriendResponseDto;
import com.tech.motjip.R;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter
        extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public static final int MODE_FRIEND = 0;

    public static final int MODE_RECOMMEND = 1;

    private static final int COLOR_CHAT_TEXT =
            Color.parseColor("#A45A2A");

    public interface OnFriendClickListener {

        void onChatClick(
                FriendResponseDto friend
        );

        void onInviteClick(
                FriendResponseDto friend
        );

        void onRecommendAddClick(
                FriendResponseDto friend
        );

        void onDeleteFriendClick(
                FriendResponseDto friend
        );
    }

    private final Context context;

    private final OnFriendClickListener listener;

    private final List<FriendResponseDto> friends =
            new ArrayList<>();

    private int mode =
            MODE_FRIEND;

    public FriendAdapter(
            Context context,
            OnFriendClickListener listener
    ) {

        this.context =
                context;

        this.listener =
                listener;
    }

    public void setMode(
            int mode
    ) {

        this.mode =
                mode;

        notifyDataSetChanged();
    }

    public void setFriends(
            List<FriendResponseDto> friendList
    ) {

        mode =
                MODE_FRIEND;

        friends.clear();

        if (friendList != null) {

            friends.addAll(
                    friendList
            );
        }

        notifyDataSetChanged();
    }

    public void setRecommendedFriends(
            List<FriendRecommendationResponseDto> recommendationList
    ) {

        mode =
                MODE_RECOMMEND;

        friends.clear();

        if (recommendationList != null) {

            for (FriendRecommendationResponseDto recommendation : recommendationList) {

                FriendResponseDto friend =
                        new FriendResponseDto(
                                recommendation.getMemberId(),
                                recommendation.getNickname(),
                                null,
                                recommendation.getProfileImgUrl(),
                                recommendation.getDistanceKm(),
                                recommendation.getStatus()
                        );

                friends.add(
                        friend
                );
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.item_friend,
                                parent,
                                false
                        );

        return new FriendViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull FriendViewHolder holder,
            int position
    ) {

        FriendResponseDto friend =
                friends.get(
                        position
                );

        holder.tvNickname.setText(
                friend.getNickname() != null
                        && !friend.getNickname().isEmpty()
                        ? friend.getNickname()
                        : "닉네임 없음"
        );

        bindProfileImage(
                holder,
                friend
        );

        bindDistanceAndStatus(
                holder,
                friend
        );

        bindButtons(
                holder,
                friend
        );
    }

    private void bindProfileImage(
            FriendViewHolder holder,
            FriendResponseDto friend
    ) {

        String profileImgUrl =
                friend.getProfileImgUrl();

        if (profileImgUrl != null
                && !profileImgUrl.trim().isEmpty()) {

            String imageUrl =
                    buildImageUrl(
                            profileImgUrl
                    );

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.ivProfile);

        } else {

            holder.ivProfile.setImageResource(
                    R.drawable.default_profile
            );
        }
    }

    private String buildImageUrl(
            String imageUrl
    ) {

        if (imageUrl == null
                || imageUrl.trim().isEmpty()) {

            return "";
        }

        String trimmedUrl =
                imageUrl.trim();

        if (trimmedUrl.startsWith("http://")
                || trimmedUrl.startsWith("https://")) {

            return trimmedUrl;
        }

        if (trimmedUrl.startsWith("/")) {

            return AppConfig.BASE_URL + trimmedUrl;
        }

        return AppConfig.BASE_URL + "/" + trimmedUrl;
    }

    private void bindDistanceAndStatus(
            FriendViewHolder holder,
            FriendResponseDto friend
    ) {

        if (mode == MODE_RECOMMEND) {

            holder.tvStatus.setVisibility(
                    View.VISIBLE
            );

            String status =
                    friend.getStatus();

            if ("PENDING".equals(status)) {

                holder.tvStatus.setText(
                        "요청 대기중"
                );

            } else {

                if (friend.getDistanceKm() != null) {

                    holder.tvStatus.setText(
                            String.format(
                                    "추천 친구 · %.1fkm",
                                    friend.getDistanceKm()
                            )
                    );

                } else {

                    holder.tvStatus.setText(
                            "추천 친구"
                    );
                }
            }

        } else {

            holder.tvStatus.setVisibility(
                    View.GONE
            );
        }
    }

    private void bindButtons(
            FriendViewHolder holder,
            FriendResponseDto friend
    ) {

        if (mode == MODE_FRIEND) {

            holder.btnChat.setVisibility(
                    View.VISIBLE
            );

            setButtonStyle(
                    holder.btnChat,
                    "1:1채팅",
                    R.drawable.bg_chat_outline_round,
                    COLOR_CHAT_TEXT,
                    true
            );

            holder.btnInvite.setVisibility(
                    View.VISIBLE
            );

            setButtonStyle(
                    holder.btnInvite,
                    "모임초대",
                    R.drawable.bg_orange_fill_round,
                    Color.WHITE,
                    true
            );

            holder.btnDeleteFriend.setVisibility(
                    View.VISIBLE
            );

            setButtonStyle(
                    holder.btnDeleteFriend,
                    "친구삭제",
                    R.drawable.bg_red_round,
                    Color.WHITE,
                    true
            );

        } else {

            holder.btnChat.setVisibility(
                    View.VISIBLE
            );

            setButtonStyle(
                    holder.btnChat,
                    "1:1채팅",
                    R.drawable.bg_chat_outline_round,
                    COLOR_CHAT_TEXT,
                    true
            );

            holder.btnInvite.setVisibility(
                    View.VISIBLE
            );

            String status =
                    friend.getStatus();

            if ("PENDING".equals(status)) {

                setButtonStyle(
                        holder.btnInvite,
                        "요청중",
                        R.drawable.bg_gray_round,
                        Color.DKGRAY,
                        false
                );

            } else {

                setButtonStyle(
                        holder.btnInvite,
                        "추가",
                        R.drawable.bg_orange_fill_round,
                        Color.WHITE,
                        true
                );
            }

            holder.btnDeleteFriend.setVisibility(
                    View.GONE
            );

            holder.btnDeleteFriend.setEnabled(
                    false
            );
        }

        holder.btnChat.setOnClickListener(v -> {

            if (listener != null) {

                listener.onChatClick(
                        friend
                );
            }
        });

        holder.btnInvite.setOnClickListener(v -> {

            if (listener == null
                    || !holder.btnInvite.isEnabled()) {

                return;
            }

            if (mode == MODE_FRIEND) {

                listener.onInviteClick(
                        friend
                );

            } else {

                setButtonStyle(
                        holder.btnInvite,
                        "요청중",
                        R.drawable.bg_gray_round,
                        Color.DKGRAY,
                        false
                );

                friend.setStatus(
                        "PENDING"
                );

                holder.tvStatus.setVisibility(
                        View.VISIBLE
                );

                holder.tvStatus.setText(
                        "요청 대기중"
                );

                listener.onRecommendAddClick(
                        friend
                );
            }
        });

        holder.btnDeleteFriend.setOnClickListener(v -> {

            Toast.makeText(
                    context,
                    "삭제 버튼 클릭",
                    Toast.LENGTH_SHORT
            ).show();

            if (listener != null
                    && mode == MODE_FRIEND) {

                listener.onDeleteFriendClick(
                        friend
                );
            }
        });
    }

    private void setButtonStyle(
            TextView button,
            String text,
            int backgroundRes,
            int textColor,
            boolean enabled
    ) {

        button.setText(
                text
        );

        button.setBackgroundResource(
                backgroundRes
        );

        button.setTextColor(
                textColor
        );

        button.setEnabled(
                enabled
        );

        button.setAlpha(
                enabled ? 1.0f : 0.65f
        );
    }

    @Override
    public int getItemCount() {

        return friends.size();
    }

    static class FriendViewHolder
            extends RecyclerView.ViewHolder {

        ImageView ivProfile;

        TextView tvNickname;

        TextView tvStatus;

        TextView btnChat;

        TextView btnInvite;

        TextView btnDeleteFriend;

        public FriendViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );

            ivProfile =
                    itemView.findViewById(
                            R.id.ivProfile
                    );

            tvNickname =
                    itemView.findViewById(
                            R.id.tvNickname
                    );

            tvStatus =
                    itemView.findViewById(
                            R.id.tvStatus
                    );

            btnChat =
                    itemView.findViewById(
                            R.id.btnChat
                    );

            btnInvite =
                    itemView.findViewById(
                            R.id.btnInvite
                    );

            btnDeleteFriend =
                    itemView.findViewById(
                            R.id.btnDeleteFriend
                    );
        }
    }
}