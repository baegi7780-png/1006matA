package com.tech.motjip;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tech.motjip.Adapter.CommunityPostAdapter;
import com.tech.motjip.Controller.FavoriteController;
import com.tech.motjip.Model.CommunityPost;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteActivity
        extends AppCompatActivity {

    private TextView tvFavoriteEmpty;

    private RecyclerView recyclerViewFavorite;

    private CommunityPostAdapter adapter;

    private FavoriteController favoriteController;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_favorite
        );

        tvFavoriteEmpty =
                findViewById(R.id.tvFavoriteEmpty);

        recyclerViewFavorite =
                findViewById(R.id.recyclerViewFavorite);

        favoriteController =
                new FavoriteController(this);

        adapter =
                new CommunityPostAdapter(
                        this,
                        post -> {

                            Intent intent =
                                    new Intent(
                                            FavoriteActivity.this,
                                            CommunityDetailActivity.class
                                    );

                            intent.putExtra(
                                    "communityPost",
                                    post
                            );

                            startActivity(intent);
                        },
                        null
                );

        recyclerViewFavorite.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerViewFavorite.setAdapter(
                adapter
        );

        loadFavoriteCommunityPosts();
    }

    private void loadFavoriteCommunityPosts() {

        tvFavoriteEmpty.setVisibility(
                View.VISIBLE
        );

        tvFavoriteEmpty.setText(
                "불러오는 중입니다..."
        );

        favoriteController.getFavoriteCommunityPosts(
                new Callback<List<CommunityPost>>() {

                    @Override
                    public void onResponse(
                            Call<List<CommunityPost>> call,
                            Response<List<CommunityPost>> response
                    ) {

                        if (!response.isSuccessful()) {

                            adapter.setPosts(null);

                            tvFavoriteEmpty.setVisibility(
                                    View.VISIBLE
                            );

                            tvFavoriteEmpty.setText(
                                    "모임 즐겨찾기 조회 실패: "
                                            + response.code()
                            );

                            return;
                        }

                        List<CommunityPost> posts =
                                response.body();

                        if (posts == null
                                || posts.isEmpty()) {

                            adapter.setPosts(null);

                            tvFavoriteEmpty.setVisibility(
                                    View.VISIBLE
                            );

                            tvFavoriteEmpty.setText(
                                    "즐겨찾기한 모임이 없습니다."
                            );

                            return;
                        }

                        adapter.setPosts(posts);

                        tvFavoriteEmpty.setVisibility(
                                View.GONE
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<CommunityPost>> call,
                            Throwable t
                    ) {

                        adapter.setPosts(null);

                        tvFavoriteEmpty.setVisibility(
                                View.VISIBLE
                        );

                        tvFavoriteEmpty.setText(
                                "서버 연결 실패: "
                                        + t.getMessage()
                        );

                        Toast.makeText(
                                FavoriteActivity.this,
                                "서버 연결 실패",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }
}