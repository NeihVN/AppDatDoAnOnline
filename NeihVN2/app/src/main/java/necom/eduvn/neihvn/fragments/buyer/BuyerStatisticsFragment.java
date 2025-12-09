package necom.eduvn.neihvn.fragments.buyer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
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

import necom.eduvn.neihvn.R;
import necom.eduvn.neihvn.databinding.FragmentBuyerStatisticsBinding;
import necom.eduvn.neihvn.models.Order;
import necom.eduvn.neihvn.utils.CurrencyFormatter;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BuyerStatisticsFragment extends Fragment {
    private static final String TAG = "BuyerStatisticsFragment";
    private FragmentBuyerStatisticsBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBuyerStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadStatistics();
    }

    private void loadStatistics() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseUtil.getCurrentUserId();

        if (userId == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("Vui lòng đăng nhập để xem thống kê");
            return;
        }

        FirebaseUtil.getFirestore().collection("orders")
                .whereEqualTo("buyerId", userId)
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

                    if (orders.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setText("Chưa có đơn hàng nào");
                        binding.scrollView.setVisibility(View.GONE);
                        return;
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                    binding.scrollView.setVisibility(View.VISIBLE);

                    // Setup all charts
                    setupOrderStatusChart(orders);
                    setupSpendingByMonthChart(orders);
                    setupOrderCountChart(orders);
                    setupTopRestaurantsChart(orders);
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
            // Translate status to Vietnamese
            String statusVN = translateStatus(status);
            statusCount.put(statusVN, statusCount.getOrDefault(statusVN, 0) + 1);
        }

        if (statusCount.isEmpty()) {
            binding.pieChartOrderStatus.setVisibility(View.GONE);
            return;
        }

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

    private void setupSpendingByMonthChart(List<Order> orders) {
        if (binding == null) return;

        Map<String, Double> monthlySpending = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        for (Order order : orders) {
            if (order.getCreatedAt() > 0 && order.getTotalAmount() > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(order.getCreatedAt());
                String month = monthFormat.format(cal.getTime());
                monthlySpending.put(month, monthlySpending.getOrDefault(month, 0.0) + order.getTotalAmount());
            }
        }

        if (monthlySpending.isEmpty()) {
            binding.barChartSpending.setVisibility(View.GONE);
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(monthlySpending.keySet());
        labels.sort(String::compareTo);

        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, monthlySpending.get(labels.get(i)).floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu (₫)");
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
        binding.barChartSpending.getDescription().setText("Chi tiêu theo tháng");
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

        // Last 7 days
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
        binding.lineChartOrderCount.getDescription().setText("Số đơn hàng (7 ngày qua)");
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

    private void setupTopRestaurantsChart(List<Order> orders) {
        if (binding == null) return;

        Map<String, Integer> restaurantOrderCount = new HashMap<>();
        Map<String, String> restaurantNames = new HashMap<>();

        for (Order order : orders) {
            String restaurantId = order.getRestaurantId();
            if (restaurantId != null) {
                restaurantOrderCount.put(restaurantId, restaurantOrderCount.getOrDefault(restaurantId, 0) + 1);
            }
        }

        // Get restaurant names
        List<String> restaurantIds = new ArrayList<>(restaurantOrderCount.keySet());
        if (restaurantIds.isEmpty()) {
            binding.barChartTopRestaurants.setVisibility(View.GONE);
            return;
        }

        FirebaseUtil.getFirestore().collection("restaurants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        restaurantNames.put(doc.getId(), doc.getString("name"));
                    }

                    // Sort by order count
                    restaurantIds.sort((id1, id2) -> 
                        Integer.compare(restaurantOrderCount.get(id2), restaurantOrderCount.get(id1)));

                    // Get top 5
                    int topCount = Math.min(5, restaurantIds.size());
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    for (int i = 0; i < topCount; i++) {
                        String restaurantId = restaurantIds.get(i);
                        entries.add(new BarEntry(i, restaurantOrderCount.get(restaurantId)));
                        String name = restaurantNames.getOrDefault(restaurantId, "Không xác định");
                        if (name.length() > 15) {
                            name = name.substring(0, 15) + "...";
                        }
                        labels.add(name);
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Số đơn");
                    dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary_orange));
                    dataSet.setValueTextSize(10f);
                    dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

                    BarData barData = new BarData(dataSet);
                    barData.setBarWidth(0.5f);
                    binding.barChartTopRestaurants.setData(barData);
                    binding.barChartTopRestaurants.getDescription().setText("Nhà hàng yêu thích");
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
                });
    }

    private void setupSummaryStats(List<Order> orders) {
        if (binding == null) return;

        int totalOrders = orders.size();
        double totalSpending = 0;
        int completedOrders = 0;
        double averageOrderValue = 0;

        for (Order order : orders) {
            totalSpending += order.getTotalAmount();
            String status = order.getStatus();
            if (status != null && (status.equals("Completed") || status.equals("Hoàn thành"))) {
                completedOrders++;
            }
        }

        if (totalOrders > 0) {
            averageOrderValue = totalSpending / totalOrders;
        }

        binding.tvTotalOrders.setText(String.valueOf(totalOrders));
        binding.tvTotalSpending.setText(CurrencyFormatter.format(totalSpending));
        binding.tvCompletedOrders.setText(String.valueOf(completedOrders));
        binding.tvAverageOrderValue.setText(CurrencyFormatter.format(averageOrderValue));
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

