package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityBuyerMainBinding;
import necom.eduvn.neihvn.fragments.buyer.BuyerCartFragment;
import necom.eduvn.neihvn.fragments.buyer.BuyerFavoritesFragment;
import necom.eduvn.neihvn.fragments.buyer.BuyerHomeFragment;
import necom.eduvn.neihvn.fragments.buyer.BuyerOrdersFragment;
import necom.eduvn.neihvn.fragments.buyer.BuyerProfileFragment;
import necom.eduvn.neihvn.fragments.buyer.BuyerStatisticsFragment;
import com.google.android.material.navigation.NavigationBarView;

public class BuyerMainActivity extends AppCompatActivity {
    private ActivityBuyerMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuyerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadFragment(new BuyerHomeFragment());

        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_buyer_home) {
                    fragment = new BuyerHomeFragment();
                } else if (itemId == R.id.nav_buyer_orders) {
                    fragment = new BuyerOrdersFragment();
                } else if (itemId == R.id.nav_buyer_cart) {
                    fragment = new BuyerCartFragment();
                } else if (itemId == R.id.nav_buyer_favorites) {
                    fragment = new BuyerFavoritesFragment();
                } else if (itemId == R.id.nav_buyer_statistics) {
                    fragment = new BuyerStatisticsFragment();
                } else if (itemId == R.id.nav_buyer_profile) {
                    fragment = new BuyerProfileFragment();
                }

                return loadFragment(fragment);
            }
        });

        updateCartBadge();
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

    public void updateCartBadge() {
        int count = necom.eduvn.neihvn.utils.CartManager.getInstance().getItemCount();
        if (count > 0) {
            binding.bottomNavigation.getOrCreateBadge(R.id.nav_buyer_cart).setNumber(count);
        } else {
            binding.bottomNavigation.removeBadge(R.id.nav_buyer_cart);
        }
    }
}