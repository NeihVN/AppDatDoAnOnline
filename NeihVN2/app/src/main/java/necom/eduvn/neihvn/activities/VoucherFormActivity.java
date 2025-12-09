package necom.eduvn.neihvn.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.ActivityVoucherFormBinding;
import necom.eduvn.neihvn.models.Voucher;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class VoucherFormActivity extends AppCompatActivity {
    
    private ActivityVoucherFormBinding binding;
    private String restaurantId;
    private String voucherId; // null for new voucher
    private Voucher existingVoucher;
    private long startDate = 0;
    private long endDate = 0;
    private String discountType = "percentage";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        restaurantId = getIntent().getStringExtra("restaurantId");
        voucherId = getIntent().getStringExtra("voucherId");
        
        setupToolbar();
        setupDiscountTypeRadio();
        setupDatePickers();
        
        if (voucherId != null) {
            // Edit mode
            loadVoucher();
        } else {
            // Create mode - set default dates
            setDefaultDates();
        }
        
        binding.btnSaveVoucher.setOnClickListener(v -> saveVoucher());
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(voucherId == null ? "Tạo voucher mới" : "Chỉnh sửa voucher");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupDiscountTypeRadio() {
        binding.radioGroupDiscountType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPercentage) {
                discountType = "percentage";
                binding.layoutMaxDiscount.setVisibility(View.VISIBLE);
                binding.etDiscountValue.setHint("Giá trị giảm (%)");
            } else {
                discountType = "fixed";
                binding.layoutMaxDiscount.setVisibility(View.GONE);
                binding.etDiscountValue.setHint("Giá trị giảm (₫)");
            }
        });
    }
    
    private void setupDatePickers() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        binding.etStartDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (startDate > 0) {
                calendar.setTimeInMillis(startDate);
            }
            
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth, 0, 0, 0);
                        startDate = calendar.getTimeInMillis();
                        binding.etStartDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
        
        binding.etEndDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (endDate > 0) {
                calendar.setTimeInMillis(endDate);
            }
            
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth, 23, 59, 59);
                        endDate = calendar.getTimeInMillis();
                        binding.etEndDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }
    
    private void setDefaultDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // Start date: today
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        startDate = calendar.getTimeInMillis();
        binding.etStartDate.setText(sdf.format(calendar.getTime()));
        
        // End date: 30 days from now
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        endDate = calendar.getTimeInMillis();
        binding.etEndDate.setText(sdf.format(calendar.getTime()));
    }
    
    private void loadVoucher() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        FirebaseUtil.getFirestore().collection("vouchers")
                .document(voucherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        existingVoucher = documentSnapshot.toObject(Voucher.class);
                        if (existingVoucher != null) {
                            populateForm(existingVoucher);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
    
    private void populateForm(Voucher voucher) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        binding.etVoucherCode.setText(voucher.getCode());
        binding.etDescription.setText(voucher.getDescription());
        
        if ("percentage".equals(voucher.getDiscountType())) {
            binding.radioPercentage.setChecked(true);
            binding.etMaxDiscount.setText(String.valueOf(voucher.getMaxDiscount()));
        } else {
            binding.radioFixed.setChecked(true);
        }
        
        binding.etDiscountValue.setText(String.valueOf(voucher.getDiscountValue()));
        binding.etMinOrderAmount.setText(String.valueOf(voucher.getMinOrderAmount()));
        binding.etUsageLimit.setText(String.valueOf(voucher.getUsageLimit()));
        
        startDate = voucher.getStartDate();
        endDate = voucher.getEndDate();
        binding.etStartDate.setText(sdf.format(startDate));
        binding.etEndDate.setText(sdf.format(endDate));
        
        binding.switchActive.setChecked(voucher.isActive());
        
        // Disable code editing for existing voucher
        binding.etVoucherCode.setEnabled(false);
    }
    
    private void saveVoucher() {
        // Validate inputs
        String code = binding.etVoucherCode.getText().toString().trim().toUpperCase();
        String description = binding.etDescription.getText().toString().trim();
        String discountValueStr = binding.etDiscountValue.getText().toString().trim();
        String minOrderStr = binding.etMinOrderAmount.getText().toString().trim();
        String usageLimitStr = binding.etUsageLimit.getText().toString().trim();
        String maxDiscountStr = binding.etMaxDiscount.getText().toString().trim();
        
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (description.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mô tả", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (discountValueStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giá trị giảm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startDate == 0 || endDate == 0) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu và kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (endDate <= startDate) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double discountValue = Double.parseDouble(discountValueStr);
        double minOrderAmount = minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr);
        int usageLimit = usageLimitStr.isEmpty() ? -1 : Integer.parseInt(usageLimitStr);
        double maxDiscount = 0;
        
        if ("percentage".equals(discountType)) {
            if (discountValue <= 0 || discountValue > 100) {
                Toast.makeText(this, "Giá trị giảm phải từ 1-100%", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!maxDiscountStr.isEmpty()) {
                maxDiscount = Double.parseDouble(maxDiscountStr);
            }
        } else {
            if (discountValue <= 0) {
                Toast.makeText(this, "Giá trị giảm phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        boolean active = binding.switchActive.isChecked();
        
        // Create or update voucher
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveVoucher.setEnabled(false);
        
        if (voucherId == null) {
            // Create new voucher
            createNewVoucher(code, description, discountValue, minOrderAmount, maxDiscount, usageLimit, active);
        } else {
            // Update existing voucher
            updateVoucher(code, description, discountValue, minOrderAmount, maxDiscount, usageLimit, active);
        }
    }
    
    private void createNewVoucher(String code, String description, double discountValue,
                                   double minOrderAmount, double maxDiscount, int usageLimit, boolean active) {
        
        // Check if code already exists for this restaurant
        FirebaseUtil.getFirestore().collection("vouchers")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSaveVoucher.setEnabled(true);
                        Toast.makeText(this, "Mã voucher đã tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create voucher
                    DocumentReference docRef = FirebaseUtil.getFirestore().collection("vouchers").document();
                    
                    Voucher voucher = new Voucher(
                            docRef.getId(),
                            restaurantId,
                            code,
                            description,
                            discountType,
                            discountValue,
                            minOrderAmount,
                            maxDiscount,
                            startDate,
                            endDate,
                            usageLimit
                    );
                    voucher.setActive(active);
                    voucher.setCreatedAt(System.currentTimeMillis());
                    
                    docRef.set(voucher)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Tạo voucher thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.btnSaveVoucher.setEnabled(true);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveVoucher.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateVoucher(String code, String description, double discountValue,
                                double minOrderAmount, double maxDiscount, int usageLimit, boolean active) {
        
        existingVoucher.setDescription(description);
        existingVoucher.setDiscountType(discountType);
        existingVoucher.setDiscountValue(discountValue);
        existingVoucher.setMinOrderAmount(minOrderAmount);
        existingVoucher.setMaxDiscount(maxDiscount);
        existingVoucher.setStartDate(startDate);
        existingVoucher.setEndDate(endDate);
        existingVoucher.setUsageLimit(usageLimit);
        existingVoucher.setActive(active);
        
        FirebaseUtil.getFirestore().collection("vouchers")
                .document(voucherId)
                .set(existingVoucher)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật voucher thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveVoucher.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

