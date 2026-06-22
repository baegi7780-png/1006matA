

package com.tech.motjip.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tech.motjip.API.RetrofitClient;
import com.tech.motjip.Model.Participant;
import com.tech.motjip.R;

import java.util.List;

public class ParticipantAdapter
        extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private final List<Participant> participantList;

    private final OnParticipantClickListener listener;

    public interface OnParticipantClickListener {

        void onParticipantClick(
                Participant participant
        );
    }

    public ParticipantAdapter(
            List<Participant> participantList
    ) {

        this.participantList =
                participantList;

        this.listener =
                null;
    }

    public ParticipantAdapter(
            List<Participant> participantList,
            OnParticipantClickListener listener
    ) {

        this.participantList =
                participantList;

        this.listener =
                listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_participant,
                                parent,
                                false
                        );

        return new ViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Participant participant =
                participantList.get(
                        position
                );

        String nickname =
                participant.getNickname();

        if (nickname == null
                || nickname.trim().isEmpty()) {

            nickname =
                    "사용자";
        }

        Log.d(
                "PARTICIPANT_TEST",
                nickname
        );

        if (participant.isOwner()) {

            holder.tvNickname.setText(
                    nickname + " 👑"
            );

        } else {

            holder.tvNickname.setText(
                    nickname
            );
        }

        String profileUrl =
                buildImageUrl(
                        participant.getProfileImgUrl()
                );

        Glide.with(
                        holder.itemView.getContext()
                )
                .load(
                        profileUrl
                )
                .placeholder(
                        R.drawable.people
                )
                .error(
                        R.drawable.people
                )
                .circleCrop()
                .into(
                        holder.ivProfile
                );

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {

                listener.onParticipantClick(
                        participant
                );
            }
        });
    }

    @Override
    public int getItemCount() {

        if (participantList == null) {

            return 0;
        }

        return participantList.size();
    }

    private String buildImageUrl(
            String imageUrl
    ) {

        if (imageUrl == null
                || imageUrl.trim().isEmpty()) {

            return null;
        }

        String trimmedUrl =
                imageUrl.trim();

        if (trimmedUrl.startsWith("http://")
                || trimmedUrl.startsWith("https://")) {

            return trimmedUrl;
        }

        return RetrofitClient.BASE_URL.replaceAll(
                "/$",
                ""
        )
                + "/"
                + trimmedUrl.replaceAll(
                "^/",
                ""
        );
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvNickname;

        ImageView ivProfile;

        public ViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );

            ivProfile =
                    itemView.findViewById(
                            R.id.iv_participant_profile
                    );

            tvNickname =
                    itemView.findViewById(
                            R.id.tv_participant_nickname
                    );
        }
    }
}






