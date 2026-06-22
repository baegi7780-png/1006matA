package com.tech.motjip.Adapter.binder;

import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tech.motjip.Config.AppConfig;
import com.tech.motjip.Model.Message;
import com.tech.motjip.R;
import com.tech.motjip.Utils.ImagePreviewDialog;
import com.tech.motjip.Utils.TimeUtil;

public class MessageBindHelper {

    private static final String MESSAGE_TYPE_UNREAD_DIVIDER =
            "__UNREAD_DIVIDER__";

    private static final String BASE_IMAGE_URL =
            AppConfig.BASE_URL;

    private MessageBindHelper() {
    }

    public static void bindTextOrImageMessage(
            Message message,
            TextView tvMessageContent,
            ImageView ivMessageImage
    ) {

        boolean isImageMessage =
                Message.TYPE_IMAGE.equalsIgnoreCase(
                        message.getMessageType()
                )
                        && message.getFileUrl() != null
                        && !message.getFileUrl().trim().isEmpty();

        boolean isVideoMessage =
                Message.TYPE_VIDEO.equalsIgnoreCase(
                        message.getMessageType()
                )
                        && message.getFileUrl() != null
                        && !message.getFileUrl().trim().isEmpty();

        if (isImageMessage) {

            if (tvMessageContent != null) {
                tvMessageContent.setVisibility(View.GONE);
            }

            if (ivMessageImage != null) {

                ivMessageImage.setVisibility(View.VISIBLE);

                String imageUrl =
                        buildImageUrl(
                                message.getFileUrl()
                        );

                Glide.with(ivMessageImage.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.default_profile)
                        .fallback(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .centerCrop()
                        .dontAnimate()
                        .into(ivMessageImage);

                ivMessageImage.setOnClickListener(v ->
                        ImagePreviewDialog.show(
                                ivMessageImage.getContext(),
                                imageUrl
                        )
                );
            }

        } else if (isVideoMessage) {

            if (tvMessageContent != null) {
                tvMessageContent.setVisibility(View.GONE);
            }

            if (ivMessageImage != null) {

                ivMessageImage.setVisibility(View.VISIBLE);

                String videoUrl =
                        buildImageUrl(
                                message.getFileUrl()
                        );

                Glide.with(ivMessageImage.getContext())
                        .load(videoUrl)
                        .thumbnail(0.1f)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .centerCrop()
                        .into(ivMessageImage);

                ivMessageImage.setOnClickListener(v -> {

                    android.content.Intent intent =
                            new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW
                            );

                    intent.setDataAndType(
                            android.net.Uri.parse(videoUrl),
                            "video/*"
                    );

                    ivMessageImage.getContext()
                            .startActivity(intent);
                });
            }

        } else {

            if (ivMessageImage != null) {

                Glide.with(ivMessageImage.getContext())
                        .clear(ivMessageImage);

                ivMessageImage.setOnClickListener(null);

                ivMessageImage.setVisibility(View.GONE);
            }

            if (tvMessageContent != null) {

                tvMessageContent.setVisibility(View.VISIBLE);

                tvMessageContent.setText(
                        message.getMessageContent() != null
                                ? message.getMessageContent()
                                : ""
                );

                Linkify.addLinks(
                        tvMessageContent,
                        Linkify.WEB_URLS
                );

                tvMessageContent.setLinksClickable(true);
            }
        }
    }

    public static void bindProfileAndSenderName(
            Message message,
            ImageView ivProfile,
            TextView tvSenderName,
            boolean isContinuousFromPrevious
    ) {

        if (isContinuousFromPrevious) {

            if (ivProfile != null) {
                ivProfile.setVisibility(View.INVISIBLE);
            }

            if (tvSenderName != null) {
                tvSenderName.setVisibility(View.GONE);
            }

            return;
        }

        if (ivProfile != null) {

            ivProfile.setVisibility(View.VISIBLE);

            Glide.with(ivProfile.getContext())
                    .clear(ivProfile);

            String profileUrl =
                    message.getSenderProfileImage();

            String fullProfileUrl =
                    buildImageUrl(
                            profileUrl
                    );

            Log.d(
                    "CHAT_PROFILE",
                    "messageId="
                            + message.getId()
                            + ", senderId="
                            + message.getSenderId()
                            + ", nickname="
                            + message.getSenderNickname()
                            + ", profileUrl="
                            + profileUrl
                            + ", fullProfileUrl="
                            + fullProfileUrl
            );

            Glide.with(ivProfile.getContext())
                    .load(fullProfileUrl)
                    .placeholder(R.drawable.default_profile)
                    .fallback(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .centerCrop()
                    .dontAnimate()
                    .into(ivProfile);
        }

        if (tvSenderName != null) {

            tvSenderName.setVisibility(View.VISIBLE);

            tvSenderName.setText(
                    message.getSenderNickname() != null
                            ? message.getSenderNickname()
                            : "상대방"
            );
        }
    }

    public static void bindTime(
            Message message,
            TextView tvTime,
            boolean shouldHideTime
    ) {

        if (tvTime == null) {
            return;
        }

        if (shouldHideTime) {
            tvTime.setVisibility(View.GONE);
            return;
        }

        tvTime.setVisibility(View.VISIBLE);

        tvTime.setText(
                TimeUtil.formatMessageTime(
                        message.getSentAt()
                )
        );
    }

    public static void bindUnreadCount(
            Message message,
            TextView tvUnreadCount,
            boolean shouldHideUnread
    ) {

        if (tvUnreadCount == null) {
            return;
        }

        int unreadCount =
                message != null
                        ? message.getUnreadCount()
                        : 0;

        Log.d(
                "CHAT_TEST",
                "BIND_UNREAD messageId="
                        + (message != null ? message.getId() : null)
                        + ", senderId="
                        + (message != null ? message.getSenderId() : null)
                        + ", unreadCount="
                        + unreadCount
                        + ", shouldHideUnread="
                        + shouldHideUnread
        );

        if (unreadCount > 0) {

            tvUnreadCount.setVisibility(View.VISIBLE);
            tvUnreadCount.setText(String.valueOf(unreadCount));
            tvUnreadCount.bringToFront();

        } else {

            tvUnreadCount.setVisibility(View.GONE);
        }
    }

    public static boolean isContinuousMessage(
            Message currentMessage,
            Message previousMessage
    ) {

        if (currentMessage == null
                || previousMessage == null) {
            return false;
        }

        if (isUnreadDivider(currentMessage)
                || isUnreadDivider(previousMessage)) {
            return false;
        }

        if (currentMessage.isDateHeader()
                || previousMessage.isDateHeader()) {
            return false;
        }

        if (currentMessage.isSystemMessage()
                || previousMessage.isSystemMessage()) {
            return false;
        }

        if (currentMessage.getSenderId() == null
                || previousMessage.getSenderId() == null) {
            return false;
        }

        return currentMessage.getSenderId()
                .equals(previousMessage.getSenderId());
    }

    public static void applyMessageGroupSpacing(
            View itemView,
            boolean isContinuous
    ) {

        ViewGroup.LayoutParams layoutParams =
                itemView.getLayoutParams();

        if (!(layoutParams instanceof RecyclerView.LayoutParams)) {
            return;
        }

        RecyclerView.LayoutParams params =
                (RecyclerView.LayoutParams) layoutParams;

        int topMarginDp =
                isContinuous
                        ? 3
                        : 10;

        params.topMargin =
                dpToPx(
                        itemView,
                        topMarginDp
                );

        itemView.setLayoutParams(params);
    }

    public static int dpToPx(
            View view,
            int dp
    ) {

        return (int) (
                dp
                        * view.getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    private static boolean isUnreadDivider(
            Message message
    ) {

        return message != null
                && MESSAGE_TYPE_UNREAD_DIVIDER.equals(
                message.getMessageType()
        );
    }

    private static String buildImageUrl(
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

        if (trimmedUrl.startsWith("/")) {
            return BASE_IMAGE_URL + trimmedUrl;
        }

        return BASE_IMAGE_URL + "/" + trimmedUrl;
    }
}