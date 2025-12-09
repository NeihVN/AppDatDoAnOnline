package necom.eduvn.neihvn.fragments.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.adapters.NotificationAdapter;
import necom.eduvn.neihvn.databinding.FragmentSellerNotificationsBinding;
import necom.eduvn.neihvn.models.Notification;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class SellerNotificationsFragment extends Fragment {
    private FragmentSellerNotificationsBinding binding;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationList = new ArrayList<>();

        setupRecyclerView();
        loadNotifications();

        binding.btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList, new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                // Handle notification click - could navigate to related item
                Toast.makeText(getContext(), "Thông báo: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMarkAsRead(Notification notification) {
                markAsRead(notification);
            }
        });

        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String sellerId = FirebaseUtil.getCurrentUserId();

        FirebaseUtil.getFirestore().collection("notifications")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((value, error) -> {
                    if (binding == null) return; // Fragment destroyed
                    
                    binding.progressBar.setVisibility(View.GONE);
                    
                    // Handle error - but continue if we have data
                    if (error != null) {
                        // Check if error is about missing index (common with orderBy)
                        String errorMessage = error.getMessage();
                        if (errorMessage != null && errorMessage.contains("index")) {
                            // Index error - still try to process data if available
                            // Don't show error toast for index issues as data might still load
                        } else {
                            // Real error - show message only if no data was returned
                            if (value == null || value.isEmpty()) {
                                Toast.makeText(getContext(), "Lỗi tải thông báo: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    if (value != null && !value.isEmpty()) {
                        notificationList.clear();
                        notificationList.addAll(value.toObjects(Notification.class));
                        
                        // Sort by createdAt descending (newest first) on client side
                        notificationList.sort((n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt()));
                        
                        adapter.notifyDataSetChanged();

                        // Show/hide empty state
                        binding.layoutEmptyState.setVisibility(View.GONE);
                        binding.recyclerViewNotifications.setVisibility(View.VISIBLE);

                        // Show/hide mark all read button
                        boolean hasUnread = notificationList.stream().anyMatch(n -> !n.isRead());
                        binding.btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                    } else {
                        // No data - show empty state
                        notificationList.clear();
                        adapter.notifyDataSetChanged();
                        binding.layoutEmptyState.setVisibility(View.VISIBLE);
                        binding.recyclerViewNotifications.setVisibility(View.GONE);
                        binding.btnMarkAllRead.setVisibility(View.GONE);
                    }
                });
    }

    private void markAsRead(Notification notification) {
        FirebaseUtil.getFirestore().collection("notifications")
                .document(notification.getNotificationId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    notification.setRead(true);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void markAllAsRead() {
        String sellerId = FirebaseUtil.getCurrentUserId();
        
        FirebaseUtil.getFirestore().collection("notifications")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().update("read", true);
                    }
                    Toast.makeText(getContext(), "Đã đánh dấu tất cả thông báo là đã đọc", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
