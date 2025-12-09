package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityAdminMainBinding;
import necom.eduvn.neihvn.fragments.admin.AdminDashboardFragment;
import necom.eduvn.neihvn.fragments.admin.AdminFoodsFragment;
import necom.eduvn.neihvn.fragments.admin.AdminProfileFragment;
import necom.eduvn.neihvn.fragments.admin.AdminRestaurantsFragment;
import necom.eduvn.neihvn.fragments.admin.AdminUsersFragment;
import com.google.android.material.navigation.NavigationBarView;

public class AdminMainActivity extends AppCompatActivity {
    private ActivityAdminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadFragment(new AdminDashboardFragment());

        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_admin_dashboard) {
                    fragment = new AdminDashboardFragment();
                } else if (itemId == R.id.nav_admin_users) {
                    fragment = new AdminUsersFragment();
                } else if (itemId == R.id.nav_admin_restaurants) {
                    fragment = new AdminRestaurantsFragment();
                } else if (itemId == R.id.nav_admin_foods) {
                    fragment = new AdminFoodsFragment();
                } else if (itemId == R.id.nav_admin_profile) {
                    fragment = new AdminProfileFragment();
                }

                return loadFragment(fragment);
            }
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}