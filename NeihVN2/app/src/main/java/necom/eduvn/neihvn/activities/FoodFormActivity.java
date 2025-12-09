package necom.eduvn.neihvn.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import necom.eduvn.neihvn.databinding.ActivityFoodFormBinding;
import necom.eduvn.neihvn.models.FoodItem;
import necom.eduvn.neihvn.models.Restaurant;
import necom.eduvn.neihvn.utils.CategoryUtils;
import necom.eduvn.neihvn.utils.FirebaseUtil;
import necom.eduvn.neihvn.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class FoodFormActivity extends AppCompatActivity {
    private ActivityFoodFormBinding binding;
    private String restaurantId;
    private String foodId;
    private boolean isEditMode = false;
    private List<String> restaurantCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        restaurantId = getIntent().getStringExtra("restaurantId");
        foodId = getIntent().getStringExtra("foodId");
        isEditMode = foodId != null;

        setupToolbar();
        loadRestaurantCategories();

        if (isEditMode) {
            loadFoodData();
            binding.btnSave.setText("Cập nhật món ăn");
        }

        binding.btnSave.setOnClickListener(v -> saveFood());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(isEditMode ? "Chỉnh sửa món ăn" : "Thêm món mới");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadRestaurantCategories() {
        restaurantCategories.clear();
        if (restaurantId != null) {
            FirebaseUtil.getFirestore().collection("restaurants")
                    .document(restaurantId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Restaurant restaurant = documentSnapshot.toObject(Restaurant.class);
                            if (restaurant != null && restaurant.getCategories() != null) {
                                for (String category : restaurant.getCategories()) {
                                    String canonical = CategoryUtils.getCanonicalCode(category);
                                    if (!canonical.isEmpty() && !restaurantCategories.contains(canonical)) {
                                        restaurantCategories.add(canonical);
                                    }
                                }
                            }
                        } else {
                            restaurantCategories.addAll(CategoryUtils.getCanonicalCodes());
                        }
                        if (restaurantCategories.isEmpty()) {
                            restaurantCategories.addAll(CategoryUtils.getCanonicalCodes());
                        }
                        setupCategorySpinner();
                    })
                    .addOnFailureListener(e -> {
                        if (restaurantCategories.isEmpty()) {
                            restaurantCategories.addAll(CategoryUtils.getCanonicalCodes());
                        }
                        setupCategorySpinner();
                        Toast.makeText(this, "Không thể tải danh mục nhà hàng. Sử dụng danh mục mặc định.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            restaurantCategories.addAll(CategoryUtils.getCanonicalCodes());
            setupCategorySpinner();
        }
    }

    private void setupCategorySpinner() {
        if (restaurantCategories.isEmpty()) {
            restaurantCategories.addAll(CategoryUtils.getCanonicalCodes());
        }

        List<String> displayCategories = CategoryUtils.getDisplayNames(restaurantCategories);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, displayCategories);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void loadFoodData() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        FirebaseUtil.getFirestore().collection("foods")
                .document(foodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        FoodItem food = documentSnapshot.toObject(FoodItem.class);
                        if (food != null) {
                            binding.etFoodName.setText(food.getName());
                            binding.etDescription.setText(food.getDescription());
                            binding.etPrice.setText(String.valueOf(food.getPrice()));
                            binding.etImageUrl.setText(food.getImageUrl());

                            // Set category selection from restaurant categories
                            String foodCategory = CategoryUtils.getCanonicalCode(food.getCategory());
                            for (int i = 0; i < restaurantCategories.size(); i++) {
                                if (restaurantCategories.get(i).equalsIgnoreCase(foodCategory)) {
                                    binding.spinnerCategory.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                    binding.progressBar.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(android.view.View.GONE);
                });
    }

    private void saveFood() {
        String name = binding.etFoodName.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String imageUrl = binding.etImageUrl.getText().toString().trim();
        int selectedIndex = binding.spinnerCategory.getSelectedItemPosition();
        String category = (selectedIndex >= 0 && selectedIndex < restaurantCategories.size())
                ? restaurantCategories.get(selectedIndex)
                : CategoryUtils.getCanonicalCode(binding.spinnerCategory.getSelectedItem().toString());

        if (name.isEmpty()) {
            binding.etFoodName.setError("Vui lòng nhập tên món");
            return;
        }

        if (!ValidationUtil.isValidPrice(priceStr)) {
            binding.etPrice.setError("Giá không hợp lệ");
            return;
        }

        if (!ValidationUtil.isValidUrl(imageUrl)) {
            binding.etImageUrl.setError("URL không hợp lệ");
            return;
        }

        double price = Double.parseDouble(priceStr);

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnSave.setEnabled(false);

        if (isEditMode) {
            // Update existing food
            FirebaseUtil.getFirestore().collection("foods")
                    .document(foodId)
                    .update("name", name,
                            "description", description,
                            "price", price,
                            "imageUrl", imageUrl,
                            "category", category)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật món ăn thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        binding.btnSave.setEnabled(true);
                    });
        } else {
            // Create new food
            String newFoodId = FirebaseUtil.getFirestore().collection("foods").document().getId();
            FoodItem food = new FoodItem(newFoodId, restaurantId, name, price, category);
            food.setDescription(description);
            food.setImageUrl(imageUrl);

            FirebaseUtil.getFirestore().collection("foods")
                    .document(newFoodId)
                    .set(food)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "🍽️ Thêm món thành công!\nChờ quản trị viên duyệt trước khi hiển thị cho khách hàng.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        binding.btnSave.setEnabled(true);
                    });
        }
    }
}