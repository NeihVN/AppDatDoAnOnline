package necom.eduvn.neihvn.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import necom.eduvn.neihvn.adapters.UserAdapter;
import necom.eduvn.neihvn.databinding.FragmentAdminUsersBinding;
import necom.eduvn.neihvn.models.User;
import necom.eduvn.neihvn.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersFragment extends Fragment {
    private FragmentAdminUsersBinding binding;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> filteredList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupRecyclerView();
        setupSearchView();
        loadUsers();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(filteredList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                // Show edit dialog
                showEditUserDialog(user);
            }

            @Override
            public void onDelete(User user) {
                deleteUser(user);
            }

            @Override
            public void onToggleActive(User user) {
                toggleUserActive(user);
            }
        });

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewUsers.setAdapter(adapter);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUtil.getFirestore().collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                        return;
                    }

                    if (value != null) {
                        userList.clear();
                        userList.addAll(value.toObjects(User.class));
                        filteredList.clear();
                        filteredList.addAll(userList);
                        adapter.notifyDataSetChanged();

                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmptyState.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    private void filterUsers(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(userList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(lowerQuery) ||
                        user.getEmail().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
        if (binding != null) {
            binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showEditUserDialog(User user) {
        // Implementation placeholder
        if (getContext() != null) {
            Toast.makeText(getContext(),
                    String.format(Locale.getDefault(), "Chỉnh sửa %s sẽ sớm được hỗ trợ.", user.getName()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteUser(User user) {
        if (getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa người dùng")
                .setMessage(String.format(Locale.getDefault(),
                        "Bạn có chắc chắn muốn xóa %s?\nHành động này không thể hoàn tác.", user.getName()))
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseUtil.getFirestore().collection("users")
                            .document(user.getUserId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleUserActive(User user) {
        boolean newStatus = !user.isActive();

        FirebaseUtil.getFirestore().collection("users")
                .document(user.getUserId())
                .update("active", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), newStatus ? "Đã kích hoạt người dùng" : "Đã vô hiệu hóa người dùng",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}