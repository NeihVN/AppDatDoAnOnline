package necom.eduvn.neihvn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.adapters.RestaurantBannerAdapter;
import necom.eduvn.neihvn.databinding.ActivityRestaurantListBinding;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RestaurantListActivity extends AppCompatActivity {
    private ActivityRestaurantListBinding binding;
    private RestaurantBannerAdapter adapter;
    private List<Restaurant> restaurantList;
    private List<Restaurant> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestaurantListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        restaurantList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupToolbar();
        setupRecyclerView();
        setupSearchView();
        loadRestaurants();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new RestaurantBannerAdapter(filteredList, restaurant -> {
            Intent intent = new Intent(RestaurantListActivity.this, RestaurantDetailActivity.class);
            intent.putExtra("restaurantId", restaurant.getRestaurantId());
            startActivity(intent);
        });
        
        binding.recyclerViewRestaurants.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewRestaurants.setAdapter(adapter);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRestaurants(newText);
                return true;
            }
        });
    }

    private void loadRestaurants() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("approved", true)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    restaurantList.clear();
                    restaurantList.addAll(queryDocumentSnapshots.toObjects(Restaurant.class));
                    filteredList.clear();
                    filteredList.addAll(restaurantList);
                    adapter.notifyDataSetChanged();

                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải danh sách nhà hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void filterRestaurants(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(restaurantList);
        } else {
            String lowerQuery = query.toLowerCase(Locale.US);
            for (Restaurant restaurant : restaurantList) {
                boolean matchesCategories = false;
                if (restaurant.getCategories() != null) {
                    String canonical = restaurant.getCategories().toString().toLowerCase(Locale.US);
                    String display = CategoryUtils.getDisplayNames(restaurant.getCategories()).toString().toLowerCase(Locale.US);
                    matchesCategories = canonical.contains(lowerQuery) || display.contains(lowerQuery);
                }

                if ((restaurant.getName() != null && restaurant.getName().toLowerCase(Locale.US).contains(lowerQuery)) ||
                    (restaurant.getAddress() != null && restaurant.getAddress().toLowerCase(Locale.US).contains(lowerQuery)) ||
                    matchesCategories) {
                    filteredList.add(restaurant);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewRestaurants.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewRestaurants.setVisibility(View.VISIBLE);
        }
    }
}
