package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivityRestaurantFormBinding;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class RestaurantFormActivity extends AppCompatActivity {
    private ActivityRestaurantFormBinding binding;
    private String restaurantId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestaurantFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        checkExistingRestaurant();

        binding.btnSave.setOnClickListener(v -> saveRestaurant());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Thông tin nhà hàng");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkExistingRestaurant() {
        String userId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        isEditMode = true;
                        Restaurant restaurant = queryDocumentSnapshots.getDocuments().get(0).toObject(Restaurant.class);
                        if (restaurant != null) {
                            restaurantId = restaurant.getRestaurantId();
                            loadRestaurantData(restaurant);
                        }
                    }
                });
    }

    private void loadRestaurantData(Restaurant restaurant) {
        binding.etRestaurantName.setText(restaurant.getName());
        binding.etDescription.setText(restaurant.getDescription());
        binding.etAddress.setText(restaurant.getAddress());
        binding.etPhone.setText(restaurant.getPhone());
        binding.etImageUrl.setText(restaurant.getImageUrl());
        binding.btnSave.setText("Cập nhật nhà hàng");
        
        // Load categories
        loadRestaurantCategories(restaurant.getCategories());
    }
    
    private void loadRestaurantCategories(List<String> categories) {
        if (categories != null) {
            for (String category : categories) {
                String normalized = category != null ? category.trim() : "";
                checkCategory(binding.cbPizza, normalized, "Pizza");
                checkCategory(binding.cbBurger, normalized, "Burger");
                checkCategory(binding.cbAsian, normalized, "Asian Food");
                checkCategory(binding.cbSeafood, normalized, "Seafood");
                checkCategory(binding.cbDessert, normalized, "Dessert");
                checkCategory(binding.cbDrinks, normalized, "Drinks");
                checkCategory(binding.cbHealthy, normalized, "Healthy Food");
                checkCategory(binding.cbVegetarian, normalized, "Vegetarian");
            }
        }
    }

    private void checkCategory(CheckBox checkBox, String categoryValue, String legacyValue) {
        String viLabel = checkBox.getText().toString().trim();
        if (categoryValue.equalsIgnoreCase(viLabel) || categoryValue.equalsIgnoreCase(legacyValue)) {
            checkBox.setChecked(true);
        }
    }

    private void saveRestaurant() {
        String name = binding.etRestaurantName.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String imageUrl = binding.etImageUrl.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etRestaurantName.setError("Vui lòng nhập tên nhà hàng");
            return;
        }

        if (address.isEmpty()) {
            binding.etAddress.setError("Vui lòng nhập địa chỉ");
            return;
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            binding.etPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        if (!ValidationUtil.isValidUrl(imageUrl)) {
            binding.etImageUrl.setError("URL không hợp lệ");
            return;
        }
        
        // Get selected categories
        List<String> selectedCategories = getSelectedCategories();
        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một danh mục cho nhà hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnSave.setEnabled(false);

        if (isEditMode) {
            updateRestaurant(name, description, address, phone, imageUrl, selectedCategories);
        } else {
            createRestaurant(name, description, address, phone, imageUrl, selectedCategories);
        }
    }

    private void createRestaurant(String name, String description, String address, String phone, String imageUrl, List<String> categories) {
        String newRestaurantId = FirebaseUtil.getFirestore().collection("restaurants").document().getId();
        String userId = FirebaseUtil.getCurrentUserId();

        Restaurant restaurant = new Restaurant(newRestaurantId, userId, name, address);
        restaurant.setDescription(description);
        restaurant.setPhone(phone);
        restaurant.setImageUrl(imageUrl);
        restaurant.setCategories(categories);

        FirebaseUtil.getFirestore().collection("restaurants")
                .document(newRestaurantId)
                .set(restaurant)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "🏪 Đã tạo nhà hàng! Vui lòng chờ quản trị viên phê duyệt.\nDanh mục đã chọn: " + String.join(", ", categories), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnSave.setEnabled(true);
                });
    }

    private void updateRestaurant(String name, String description, String address, String phone, String imageUrl, List<String> categories) {
        FirebaseUtil.getFirestore().collection("restaurants")
                .document(restaurantId)
                .update("name", name,
                        "description", description,
                        "address", address,
                        "phone", phone,
                        "imageUrl", imageUrl,
                        "categories", categories)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Cập nhật nhà hàng thành công!\nDanh mục: " + String.join(", ", categories), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.btnSave.setEnabled(true);
                });
    }
    
    private List<String> getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();
        
        if (binding.cbPizza.isChecked()) selectedCategories.add(binding.cbPizza.getText().toString());
        if (binding.cbBurger.isChecked()) selectedCategories.add(binding.cbBurger.getText().toString());
        if (binding.cbAsian.isChecked()) selectedCategories.add(binding.cbAsian.getText().toString());
        if (binding.cbSeafood.isChecked()) selectedCategories.add(binding.cbSeafood.getText().toString());
        if (binding.cbDessert.isChecked()) selectedCategories.add(binding.cbDessert.getText().toString());
        if (binding.cbDrinks.isChecked()) selectedCategories.add(binding.cbDrinks.getText().toString());
        if (binding.cbHealthy.isChecked()) selectedCategories.add(binding.cbHealthy.getText().toString());
        if (binding.cbVegetarian.isChecked()) selectedCategories.add(binding.cbVegetarian.getText().toString());
        
        return selectedCategories;
    }
}