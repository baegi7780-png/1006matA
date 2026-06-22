package com.tech.motjip;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tech.motjip.Fragment.ChatFragment;
import com.tech.motjip.Fragment.CommunityFragment;
import com.tech.motjip.Fragment.HomeFragment;
import com.tech.motjip.Fragment.ProfileFragment;

public class MapActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private HomeFragment homeFragment;
    private CommunityFragment communityFragment;
    private ChatFragment chatFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        homeFragment = new HomeFragment();
        communityFragment = new CommunityFragment();
        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();

        bottomNavigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                replaceFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_community) {
                replaceFragment(communityFragment);
                return true;
            } else if (itemId == R.id.nav_chat) {
                replaceFragment(chatFragment);
                return true;
            } else if (itemId == R.id.nav_person) {
                replaceFragment(profileFragment);
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_community);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}