package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivitySellerMainBinding;
import necom.eduvn.neihvn.fragments.seller.SellerMenuFragment;
import necom.eduvn.neihvn.fragments.seller.SellerNotificationsFragment;
import necom.eduvn.neihvn.fragments.seller.SellerOrdersFragment;
import necom.eduvn.neihvn.fragments.seller.SellerProfileFragment;
import necom.eduvn.neihvn.fragments.seller.SellerStatisticsFragment;
import necom.eduvn.neihvn.fragments.seller.SellerVoucherFragment;
import com.google.android.material.navigation.NavigationBarView;

public class SellerMainActivity extends AppCompatActivity {
    private ActivitySellerMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadFragment(new SellerStatisticsFragment());
        binding.bottomNavigation.setSelectedItemId(R.id.nav_seller_statistics);

        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_seller_statistics) {
                    fragment = new SellerStatisticsFragment();
                } else if (itemId == R.id.nav_seller_menu) {
                    fragment = new SellerMenuFragment();
                } else if (itemId == R.id.nav_seller_orders) {
                    fragment = new SellerOrdersFragment();
                } else if (itemId == R.id.nav_seller_vouchers) {
                    fragment = new SellerVoucherFragment();
                } else if (itemId == R.id.nav_seller_profile) {
                    fragment = new SellerProfileFragment();
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