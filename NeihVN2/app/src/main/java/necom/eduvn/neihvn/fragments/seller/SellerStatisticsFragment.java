package necom.eduvn.neihvn.fragments.seller;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.FragmentSellerStatisticsBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.models.OrderItem;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

public class SellerStatisticsFragment extends Fragment {
    private static final String TAG = "SellerStatisticsFragment";
    private FragmentSellerStatisticsBinding binding;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadSellerData();
    }

    private void loadSellerData() {
        if (binding == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("Vui lòng đăng nhập để xem thống kê");
            return;
        }

        FirebaseUtil.getFirestore().collection("restaurants")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;

                    if (queryDocumentSnapshots.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Chưa có nhà hàng nào. Vui lòng tạo nhà hàng trước.");
                        return;
                    }

                    DocumentSnapshot restaurantDoc = queryDocumentSnapshots.getDocuments().get(0);
                    restaurantId = restaurantDoc.getId();

                    String restaurantName = restaurantDoc.getString("name");
                    Double rating = restaurantDoc.getDouble("rating");
                    Long totalReviewsLong = restaurantDoc.getLong("totalReviews");
                    Integer totalReviews = totalReviewsLong != null ? totalReviewsLong.intValue() : 0;
                    String address = restaurantDoc.getString("address");
                    String phone = restaurantDoc.getString("phone");
                    Boolean approved = restaurantDoc.getBoolean("approved");
                    Boolean active = restaurantDoc.getBoolean("active");

                    binding.tvRestaurantName.setText(restaurantName != null ? restaurantName : "Nhà hàng của tôi");
                    binding.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d)", rating != null ? rating : 0.0, totalReviews));
                    binding.tvRestaurantAddress.setText(address != null && !address.isEmpty() ? address : "Chưa cập nhật địa chỉ");
                    binding.tvRestaurantPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa cập nhật số điện thoại");

                    if (approved != null && approved && active != null && active) {
                        binding.tvRestaurantStatus.setText("Đã duyệt ✓");
                        binding.tvRestaurantStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_success));
                    } else if (approved != null && !approved && active != null && !active) {
                        binding.tvRestaurantStatus.setText("Bị từ chối ✗");
                        binding.tvRestaurantStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                    } else {
                        binding.tvRestaurantStatus.setText("Đang chờ duyệt");
                        binding.tvRestaurantStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning));
                    }

                    loadStatistics();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading restaurant: " + e.getMessage(), e);
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Lỗi tải dữ liệu: " + e.getMessage());
                    }
                });
    }

    private void loadStatistics() {
        if (binding == null || restaurantId == null) return;

        FirebaseUtil.getFirestore().collection("orders")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;

                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            orders.add(order);
                        }
                    }

                    binding.progressBar.setVisibility(View.GONE);

                    if (orders.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Chưa có đơn hàng nào");
                        binding.scrollView.setVisibility(View.GONE);
                        return;
                    }

                    binding.tvEmptyState.setVisibility(View.GONE);
                    binding.scrollView.setVisibility(View.VISIBLE);

                    setupOrderStatusChart(orders);
                    setupRevenueByMonthChart(orders);
                    setupOrderCountChart(orders);
                    setupTopMenuItemsChart(orders);
                    setupSummaryStats(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders: " + e.getMessage(), e);
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Lỗi tải dữ liệu: " + e.getMessage());
                        binding.scrollView.setVisibility(View.GONE);
                    }
                });
    }

    private void setupOrderStatusChart(List<Order> orders) {
        if (binding == null) return;

        Map<String, Integer> statusCount = new HashMap<>();
        for (Order order : orders) {
            String status = order.getStatus();
            if (status == null) status = "Không xác định";
            String statusVN = translateStatus(status);
            statusCount.put(statusVN, statusCount.getOrDefault(statusVN, 0) + 1);
        }

        if (statusCount.isEmpty()) {
            binding.pieChartOrderStatus.setVisibility(View.GONE);
            return;
        }

        binding.pieChartOrderStatus.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(requireContext(), R.color.primary_orange));
        colors.add(ContextCompat.getColor(requireContext(), R.color.accent_teal));
        colors.add(ContextCompat.getColor(requireContext(), R.color.status_success));
        colors.add(ContextCompat.getColor(requireContext(), R.color.status_error));
        colors.add(ContextCompat.getColor(requireContext(), R.color.status_warning));

        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            if (colorIndex >= colors.size() - 1) colorIndex = 0;
            colorIndex++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        binding.pieChartOrderStatus.setData(pieData);
        binding.pieChartOrderStatus.getDescription().setEnabled(false);
        binding.pieChartOrderStatus.setCenterText("Trạng thái đơn hàng");
        binding.pieChartOrderStatus.setCenterTextSize(14f);
        binding.pieChartOrderStatus.setEntryLabelTextSize(12f);
        binding.pieChartOrderStatus.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        binding.pieChartOrderStatus.animateY(1000);
        binding.pieChartOrderStatus.invalidate();
    }

    private void setupRevenueByMonthChart(List<Order> orders) {
        if (binding == null) return;

        Map<String, Double> monthlyRevenue = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        for (Order order : orders) {
            if (order.getCreatedAt() > 0 && order.getTotalAmount() > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(order.getCreatedAt());
                String month = monthFormat.format(cal.getTime());
                monthlyRevenue.put(month, monthlyRevenue.getOrDefault(month, 0.0) + order.getTotalAmount());
            }
        }

        if (monthlyRevenue.isEmpty()) {
            binding.barChartSpending.setVisibility(View.GONE);
            return;
        }

        binding.barChartSpending.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(monthlyRevenue.keySet());
        labels.sort(String::compareTo);

        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, monthlyRevenue.get(labels.get(i)).floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (₫)");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_orange));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyFormatter.format(value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        binding.barChartSpending.setData(barData);
        binding.barChartSpending.getDescription().setText("Doanh thu theo tháng");
        binding.barChartSpending.getDescription().setTextSize(12f);
        binding.barChartSpending.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartSpending.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChartSpending.getXAxis().setGranularity(1f);
        binding.barChartSpending.getXAxis().setLabelRotationAngle(-45);
        binding.barChartSpending.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyFormatter.format(value);
            }
        });
        binding.barChartSpending.getAxisRight().setEnabled(false);
        binding.barChartSpending.getLegend().setEnabled(false);
        binding.barChartSpending.animateY(1000);
        binding.barChartSpending.invalidate();
    }

    private void setupOrderCountChart(List<Order> orders) {
        if (binding == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -6);

        Map<String, Integer> dailyCount = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            dailyCount.put(day, 0);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long startTime = calendar.getTimeInMillis();

        for (Order order : orders) {
            if (order.getCreatedAt() >= startTime) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(order.getCreatedAt());
                String day = dayFormat.format(cal.getTime());
                dailyCount.put(day, dailyCount.getOrDefault(day, 0) + 1);
            }
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(dailyCount.keySet());
        labels.sort(String::compareTo);

        for (int i = 0; i < labels.size(); i++) {
            entries.add(new Entry(i, dailyCount.get(labels.get(i))));
        }

        if (entries.isEmpty()) {
            binding.lineChartOrderCount.setVisibility(View.GONE);
            return;
        }

        binding.lineChartOrderCount.setVisibility(View.VISIBLE);

        LineDataSet dataSet = new LineDataSet(entries, "Đơn hàng");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.accent_teal));
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.accent_teal));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent_teal));
        dataSet.setFillAlpha(100);

        LineData lineData = new LineData(dataSet);
        binding.lineChartOrderCount.setData(lineData);
        binding.lineChartOrderCount.getDescription().setText("Đơn hàng (7 ngày qua)");
        binding.lineChartOrderCount.getDescription().setTextSize(12f);
        binding.lineChartOrderCount.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChartOrderCount.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.lineChartOrderCount.getXAxis().setGranularity(1f);
        binding.lineChartOrderCount.getXAxis().setLabelRotationAngle(-45);
        binding.lineChartOrderCount.getAxisLeft().setGranularity(1f);
        binding.lineChartOrderCount.getAxisRight().setEnabled(false);
        binding.lineChartOrderCount.getLegend().setEnabled(false);
        binding.lineChartOrderCount.animateX(1000);
        binding.lineChartOrderCount.invalidate();
    }

    private void setupTopMenuItemsChart(List<Order> orders) {
        if (binding == null) return;

        Map<String, Integer> itemQuantityMap = new HashMap<>();

        for (Order order : orders) {
            List<OrderItem> items = order.getItems();
            if (items == null) continue;
            for (OrderItem item : items) {
                if (item.getFoodName() == null) continue;
                itemQuantityMap.put(item.getFoodName(), itemQuantityMap.getOrDefault(item.getFoodName(), 0) + item.getQuantity());
            }
        }

        if (itemQuantityMap.isEmpty()) {
            binding.barChartTopRestaurants.setVisibility(View.GONE);
            return;
        }

        binding.barChartTopRestaurants.setVisibility(View.VISIBLE);

        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemQuantityMap.entrySet());
        sortedItems.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        int topCount = Math.min(5, sortedItems.size());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < topCount; i++) {
            Map.Entry<String, Integer> entry = sortedItems.get(i);
            entries.add(new BarEntry(i, entry.getValue()));
            String name = entry.getKey();
            if (name.length() > 15) {
                name = name.substring(0, 15) + "...";
            }
            labels.add(name);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lượng");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_orange));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        binding.barChartTopRestaurants.setData(barData);
        binding.barChartTopRestaurants.getDescription().setText("Món bán chạy");
        binding.barChartTopRestaurants.getDescription().setTextSize(12f);
        binding.barChartTopRestaurants.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartTopRestaurants.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChartTopRestaurants.getXAxis().setGranularity(1f);
        binding.barChartTopRestaurants.getXAxis().setLabelRotationAngle(-45);
        binding.barChartTopRestaurants.getAxisLeft().setGranularity(1f);
        binding.barChartTopRestaurants.getAxisRight().setEnabled(false);
        binding.barChartTopRestaurants.getLegend().setEnabled(false);
        binding.barChartTopRestaurants.animateY(1000);
        binding.barChartTopRestaurants.invalidate();
    }

    private void setupSummaryStats(List<Order> orders) {
        if (binding == null) return;

        int totalOrders = orders.size();
        double totalRevenue = 0;
        int completedOrders = 0;
        double averageOrderValue = 0;
        double todayRevenue = 0;
        int todayOrders = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        for (Order order : orders) {
            totalRevenue += order.getTotalAmount();
            String status = order.getStatus();
            if (status != null && (status.equals("Completed") || status.equals("Hoàn thành"))) {
                completedOrders++;
            }

            if (order.getCreatedAt() >= startOfDay) {
                todayOrders++;
                todayRevenue += order.getTotalAmount();
            }
        }

        if (totalOrders > 0) {
            averageOrderValue = totalRevenue / totalOrders;
        }

        binding.tvTotalOrders.setText(String.valueOf(totalOrders));
        binding.tvTotalRevenue.setText(CurrencyFormatter.format(totalRevenue));
        binding.tvCompletedOrders.setText(String.valueOf(completedOrders));
        binding.tvAverageOrderValue.setText(CurrencyFormatter.format(averageOrderValue));
        binding.tvTodayRevenue.setText(CurrencyFormatter.format(todayRevenue));
        binding.tvTodayOrders.setText(String.valueOf(todayOrders));
    }

    private String translateStatus(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "Processing":
                return "Đang xử lý";
            case "Pending Payment":
                return "Chờ thanh toán";
            case "Delivering":
                return "Đang giao";
            case "Completed":
                return "Hoàn thành";
            case "Cancelled":
                return "Đã hủy";
            default:
                return status;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


