package com.tech.motjip.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tech.motjip.Config.AppConfig;
import com.tech.motjip.Model.Review;
import com.tech.motjip.R;

import java.util.ArrayList;
import java.util.List;

public class PlaceReviewAdapter
        extends RecyclerView.Adapter<PlaceReviewAdapter.PlaceReviewViewHolder> {

    private final List<Review> reviewList =
            new ArrayList<>();

    private static final String BASE_URL =
            AppConfig.BASE_URL;

    public interface OnReviewActionListener {

        void onEditReview(
                Review review
        );

        void onDeleteReview(
                Review review
        );
    }

    private OnReviewActionListener reviewActionListener;

    public void setOnReviewActionListener(
            OnReviewActionListener listener
    ) {

        this.reviewActionListener =
                listener;
    }

    public void setReviewList(
            List<Review> reviews
    ) {

        reviewList.clear();

        if (reviews != null) {
            reviewList.addAll(
                    reviews
            );
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceReviewViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_place_review,
                                parent,
                                false
                        );

        return new PlaceReviewViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull PlaceReviewViewHolder holder,
            int position
    ) {

        Review review =
                reviewList.get(
                        position
                );

        // =========================
        // 프로필 이미지
        // =========================
        String profileImageUrl =
                review.getProfileImageUrl();

        if (profileImageUrl != null
                && !profileImageUrl.trim().isEmpty()) {

            Glide.with(
                            holder.itemView.getContext()
                    ).load(
                            buildUrl(
                                    profileImageUrl
                            )
                    ).circleCrop()
                    .placeholder(
                            R.drawable.ic_launcher_foreground
                    )
                    .error(
                            R.drawable.ic_launcher_foreground
                    )
                    .into(
                            holder.ivProfile
                    );

        } else {

            holder.ivProfile.setImageResource(
                    R.drawable.ic_launcher_foreground
            );
        }

        // =========================
        // 작성자 닉네임
        // =========================
        if (review.getNickname() != null
                && !review.getNickname().trim().isEmpty()) {

            holder.tvReviewWriter.setText(
                    review.getNickname()
            );

        } else {

            holder.tvReviewWriter.setText(
                    "사용자"
            );
        }

        // =========================
        // 내 후기일 때 점 세개 버튼 표시
        // =========================
        if (review.isMine()) {

            holder.btnReviewMore.setVisibility(
                    View.VISIBLE
            );

        } else {

            holder.btnReviewMore.setVisibility(
                    View.GONE
            );
        }

        holder.btnReviewMore.setOnClickListener(v -> {

            PopupMenu popupMenu =
                    new PopupMenu(
                            holder.itemView.getContext(),
                            holder.btnReviewMore
                    );

            popupMenu
                    .getMenu()
                    .add(
                            "수정"
                    );

            popupMenu
                    .getMenu()
                    .add(
                            "삭제"
                    );

            popupMenu.setOnMenuItemClickListener(item -> {

                String title =
                        item.getTitle()
                                .toString();

                if ("수정".equals(title)) {

                    if (reviewActionListener != null) {

                        reviewActionListener.onEditReview(
                                review
                        );

                    } else {

                        Toast.makeText(
                                holder.itemView.getContext(),
                                "수정 기능이 연결되지 않았습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }

                if ("삭제".equals(title)) {

                    if (reviewActionListener != null) {

                        reviewActionListener.onDeleteReview(
                                review
                        );

                    } else {

                        Toast.makeText(
                                holder.itemView.getContext(),
                                "삭제 기능이 연결되지 않았습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        // =========================
        // 별점
        // =========================
        holder.ratingBar.setRating(
                review.getRating()
        );

        // =========================
        // 후기 내용
        // =========================
        if (review.getContent() != null
                && !review.getContent().trim().isEmpty()) {

            holder.tvContent.setText(
                    review.getContent()
            );

        } else {

            holder.tvContent.setText(
                    ""
            );
        }

        // =========================
        // 작성일
        // 서버에서 한국식으로 내려줌
        // 예:
        // 2026년 05월 26일 16시 30분
        // =========================
        if (review.getCreatedAt() != null
                && !review.getCreatedAt().trim().isEmpty()) {

            holder.tvCreatedAt.setText(
                    review.getCreatedAt()
            );

        } else {

            holder.tvCreatedAt.setText(
                    ""
            );
        }

        // =========================
        // 태그
        // =========================
        String tags =
                review.getTags();

        if (tags != null
                && !tags.trim().isEmpty()) {

            holder.tvTags.setVisibility(
                    View.VISIBLE
            );

            holder.tvTags.setText(
                    "#" + tags.replace(",", " #")
            );

        } else {

            holder.tvTags.setVisibility(
                    View.GONE
            );
        }

        // =========================
        // 리뷰 이미지
        // =========================
        String imageUrl =
                review.getImageUrl();

        if (imageUrl != null
                && !imageUrl.trim().isEmpty()) {

            holder.ivReviewImage.setVisibility(
                    View.VISIBLE
            );

            Glide.with(
                    holder.itemView.getContext()
            ).load(
                    buildUrl(
                            imageUrl
                    )
            ).placeholder(
                    R.drawable.ic_launcher_foreground
            ).error(
                    R.drawable.ic_launcher_foreground
            ).into(
                    holder.ivReviewImage
            );

        } else {

            holder.ivReviewImage.setVisibility(
                    View.GONE
            );
        }
    }

    private String buildUrl(
            String url
    ) {

        if (url == null
                || url.trim().isEmpty()) {

            return "";
        }

        String trimmedUrl =
                url.trim();

        if (trimmedUrl.startsWith("http://")
                || trimmedUrl.startsWith("https://")) {

            return trimmedUrl;
        }

        if (trimmedUrl.startsWith("/")) {

            return BASE_URL + trimmedUrl;
        }

        return BASE_URL + "/" + trimmedUrl;
    }

    @Override
    public int getItemCount() {

        return reviewList.size();
    }

    static class PlaceReviewViewHolder
            extends RecyclerView.ViewHolder {

        ImageView ivProfile;
        ImageView ivReviewImage;

        RatingBar ratingBar;

        TextView tvReviewWriter;
        TextView tvCreatedAt;
        TextView tvContent;
        TextView tvTags;
        TextView btnReviewMore;

        public PlaceReviewViewHolder(
                @NonNull View itemView
        ) {
            super(
                    itemView
            );

            ivProfile =
                    itemView.findViewById(
                            R.id.iv_review_profile
                    );

            ivReviewImage =
                    itemView.findViewById(
                            R.id.ivReviewImage
                    );

            ratingBar =
                    itemView.findViewById(
                            R.id.ratingBar
                    );

            tvReviewWriter =
                    itemView.findViewById(
                            R.id.tv_review_writer
                    );

            tvCreatedAt =
                    itemView.findViewById(
                            R.id.tvCreatedAt
                    );

            tvContent =
                    itemView.findViewById(
                            R.id.tvContent
                    );

            tvTags =
                    itemView.findViewById(
                            R.id.tvTags
                    );

            btnReviewMore =
                    itemView.findViewById(
                            R.id.btn_review_more
                    );
        }
    }
}