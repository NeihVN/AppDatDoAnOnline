package necom.eduvn.neihvn.fragments.buyer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.activities.CheckoutActivity;
import necom.eduvn.neihvn.adapters.CartAdapter;
import necom.eduvn.neihvn.databinding.FragmentBuyerCartBinding;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.utils.CartManager;
import necom.eduvn.neihvn.utils.CurrencyFormatter;

import java.util.List;
import java.util.Locale;

public class BuyerCartFragment extends Fragment {
    private FragmentBuyerCartBinding binding;
    private CartAdapter adapter;
    private CartManager cartManager;
    private ActivityResultLauncher<Intent> checkoutLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartManager = CartManager.getInstance();

        checkoutLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                boolean paymentSuccess = data != null && data.getBooleanExtra("paymentSuccess", false);
                String message = data != null ? data.getStringExtra("paymentMessage") : null;
                if (paymentSuccess) {
                    updateCart();
                    showPaymentResultDialog(true, message);
                }
            } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                Intent data = result.getData();
                if (data != null && data.getBooleanExtra("paymentSuccess", false)) {
                    // Treat as success even if RESULT_CANCELED (edge cases)
                    updateCart();
                    showPaymentResultDialog(true, data.getStringExtra("paymentMessage"));
                } else {
                    String message = data != null ? data.getStringExtra("paymentMessage") : null;
                    if (message != null && !message.isEmpty()) {
                        showPaymentResultDialog(false, message);
                    }
                }
            }
        });

        setupRecyclerView();
        updateCart();

        binding.btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CheckoutActivity.class);
            checkoutLauncher.launch(intent);
        });

        binding.btnClearCart.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Xóa giỏ hàng")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ giỏ hàng không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        cartManager.clearCart();
                        updateCart();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(cartManager.getCartItems(), new CartAdapter.OnCartActionListener() {
            @Override
            public void onQuantityChanged(OrderItem item, int newQuantity) {
                cartManager.updateQuantity(item.getFoodId(), newQuantity);
                updateCart();
            }

            @Override
            public void onRemove(OrderItem item) {
                cartManager.removeItem(item.getFoodId());
                updateCart();
            }
        });

        binding.recyclerViewCart.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCart.setAdapter(adapter);
    }

    private void updateCart() {
        List<OrderItem> items = cartManager.getCartItems();
        adapter.updateItems(items);

        if (items.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.layoutCart.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.layoutCart.setVisibility(View.VISIBLE);

            double subtotal = cartManager.getTotalAmount();
            double deliveryFee = 2.00;
            double total = subtotal + deliveryFee;

            binding.tvSubtotal.setText(CurrencyFormatter.format(subtotal));
            binding.tvDeliveryFee.setText(CurrencyFormatter.format(deliveryFee));
            binding.tvTotal.setText(CurrencyFormatter.format(total));
            binding.btnCheckout.setText(String.format(Locale.getDefault(), "Thanh toán (%s)", CurrencyFormatter.format(total)));
        }
    }

    private void showPaymentResultDialog(boolean success, @Nullable String message) {
        if (!isAdded()) return;

        String title = success ? "Thanh toán thành công" : "Thanh toán thất bại";
        String content = message != null && !message.isEmpty()
                ? message
                : (success ? "Đơn hàng của bạn đã được đặt thành công." : "Có lỗi xảy ra trong quá trình thanh toán.");

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Đóng", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}