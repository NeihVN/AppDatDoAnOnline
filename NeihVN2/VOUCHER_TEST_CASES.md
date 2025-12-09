# 🧪 Test Cases cho Voucher Feature

## 📋 Voucher Test Data (từ screenshot)

**Mã voucher:** `SUMMER`  
**Loại giảm giá:** Phần trăm (%)  
**Giá trị giảm:** 10% (discountValue = 10.0)  
**Đơn hàng tối thiểu:** $50.0  
**Giảm tối đa:** $500.0  
**Giới hạn sử dụng:** -1 (không giới hạn)  
**Thời gian:** 08/11/2025 - 08/12/2025  
**Trạng thái:** Kích hoạt ✅

---

## ✅ Test Cases - PASS Scenarios

### Test 1: Voucher hợp lệ - Đơn hàng $100
**Input:**
- Order subtotal: $100.00
- Voucher code: `SUMMER`

**Expected Result:**
- ✅ Voucher được áp dụng
- Discount: $10.00 (10% của $100)
- Toast: "✅ Áp dụng voucher thành công! Giảm 10% Tiết kiệm: $10.00"
- Order Summary:
  - Subtotal: $100.00
  - Delivery: $2.00
  - Discount: -$10.00
  - **Total: $92.00**

**Calculation:**
```
Discount = $100 * 10% = $10.00
Max discount = $500 (not reached)
Final discount = $10.00
```

---

### Test 2: Voucher hợp lệ - Đơn hàng $50 (minimum)
**Input:**
- Order subtotal: $50.00
- Voucher code: `summer` (lowercase - should work)

**Expected Result:**
- ✅ Voucher được áp dụng
- Discount: $5.00 (10% của $50)
- Order Summary:
  - Subtotal: $50.00
  - Delivery: $2.00
  - Discount: -$5.00
  - **Total: $47.00**

---

### Test 3: Voucher hợp lệ - Đơn hàng $5000 (test max discount)
**Input:**
- Order subtotal: $5000.00
- Voucher code: `SUMMER`

**Expected Result:**
- ✅ Voucher được áp dụng
- Discount: $500.00 (capped at max discount)
- Order Summary:
  - Subtotal: $5000.00
  - Delivery: $2.00
  - Discount: -$500.00
  - **Total: $4502.00**

**Calculation:**
```
Discount = $5000 * 10% = $500
Max discount = $500 (reached!)
Final discount = $500.00
```

---

## ❌ Test Cases - FAIL Scenarios

### Test 4: Đơn hàng không đủ minimum
**Input:**
- Order subtotal: $30.00
- Voucher code: `SUMMER`

**Expected Result:**
- ❌ Dialog hiển thị lỗi:
  - Title: "Không thể áp dụng voucher"
  - Badge: `SUMMER` (màu đỏ)
  - Message: 
    ```
    Đơn hàng tối thiểu: $50.00
    Đơn hàng hiện tại: $30.00
    Thiếu: $20.00
    ```
  - Button: "Đã hiểu"

---

### Test 5: Mã voucher không tồn tại
**Input:**
- Order subtotal: $100.00
- Voucher code: `INVALID123`

**Expected Result:**
- ❌ Dialog hiển thị lỗi:
  - Message: "Mã voucher không tồn tại hoặc không áp dụng cho nhà hàng này"

---

### Test 6: Voucher đã hết hạn (test sau 08/12/2025)
**Input:**
- Current date: 09/12/2025
- Order subtotal: $100.00
- Voucher code: `SUMMER`

**Expected Result:**
- ❌ Dialog hiển thị lỗi:
  - Message: "Voucher đã hết hạn vào: 08/12/2025"

---

### Test 7: Voucher chưa hiệu lực (test trước 08/11/2025)
**Input:**
- Current date: 07/11/2025
- Order subtotal: $100.00
- Voucher code: `SUMMER`

**Expected Result:**
- ❌ Dialog hiển thị lỗi:
  - Message: "Voucher chưa có hiệu lực.\nCó thể sử dụng từ: 08/11/2025"

---

### Test 8: Mã voucher rỗng
**Input:**
- Order subtotal: $100.00
- Voucher code: `   ` (empty/spaces)

**Expected Result:**
- ❌ Toast: "Vui lòng nhập mã voucher"

---

## 🔍 Debugging - Logcat Output

Khi test, xem logcat với filter `CheckoutActivity` và `VoucherManager`:

```
D/CheckoutActivity: Validating voucher: SUMMER
D/CheckoutActivity: Restaurant ID: abc123
D/CheckoutActivity: Order subtotal: $100.00

D/VoucherManager: === Validating Voucher ===
D/VoucherManager: Code: SUMMER
D/VoucherManager: Restaurant ID: abc123
D/VoucherManager: Order Amount: $100.00
D/VoucherManager: Query returned 1 results
D/VoucherManager: Voucher found:
D/VoucherManager:   Type: percentage
D/VoucherManager:   Value: 10.0
D/VoucherManager:   Min Order: $50.0
D/VoucherManager:   Max Discount: $500.0
D/VoucherManager:   Usage: 0/-1
D/VoucherManager: Discount calculated: $10.00
D/VoucherManager: Voucher validated successfully!

D/CheckoutActivity: Voucher valid! Discount: $10.00
D/CheckoutActivity: Voucher type: percentage
D/CheckoutActivity: Voucher value: 10.0

D/CheckoutActivity: Recalculating discount in displayOrderSummary:
D/CheckoutActivity:   Subtotal: $100.00
D/CheckoutActivity:   Voucher type: percentage
D/CheckoutActivity:   Voucher value: 10.0
D/CheckoutActivity:   Calculated discount: $10.00

D/CheckoutActivity: Order Summary: Subtotal=$100.00, Delivery=$2.00, Discount=$10.00, Total=$92.00
```

---

## 📊 Expected Behaviors

### ✅ Success Flow
1. User nhập mã voucher
2. Validate voucher (check active, dates, usage, min order)
3. Calculate discount
4. Update UI (show discount row)
5. Show success toast
6. Close bottom sheet

### ❌ Error Flow
1. User nhập mã voucher
2. Validate voucher - FAIL
3. Show custom error dialog with:
   - Voucher code badge
   - Detailed error message
   - "Đã hiểu" button
4. Keep bottom sheet open
5. User có thể nhập lại

---

## 🎯 Key Validation Points

1. **Restaurant Match:** Voucher phải thuộc đúng restaurant
2. **Active Status:** `active = true`
3. **Date Range:** `startDate <= now <= endDate`
4. **Min Order:** `orderAmount >= minOrderAmount`
5. **Usage Limit:** `usedCount < usageLimit` (if usageLimit > 0)
6. **Discount Calculation:**
   - Percentage: `discount = orderAmount * (value / 100)`
   - Cap at maxDiscount if set
   - Fixed: `discount = value`

---

## 🐛 Common Issues to Check

1. ❌ **Discount = 0:** Check if orderAmount < minOrderAmount
2. ❌ **Voucher not found:** Check restaurantId matches
3. ❌ **Wrong discount amount:** Check discountType (percentage vs fixed)
4. ❌ **Max discount not applied:** Check maxDiscount > 0
5. ❌ **Total negative:** Should be capped at 0

---

## 📱 UI Components to Verify

### Checkout Screen
- ✅ Voucher card hiển thị "Chọn hoặc nhập mã"
- ✅ Click vào card → mở bottom sheet
- ✅ Sau apply → hiển thị mã và discount text
- ✅ Discount row xuất hiện trong order summary
- ✅ Total được tính đúng

### Error Dialog
- ✅ Icon voucher màu đỏ
- ✅ Title rõ ràng
- ✅ Voucher code badge
- ✅ Error message chi tiết
- ✅ Button "Đã hiểu"

### Success Toast
- ✅ Icon checkmark
- ✅ Hiển thị discount type và amount
- ✅ Hiển thị số tiền tiết kiệm

---

## ✨ Edge Cases

1. **Multiple vouchers:** User không thể apply nhiều voucher cùng lúc
2. **Remove voucher:** Discount = 0, UI reset về "Chọn hoặc nhập mã"
3. **Cart changes:** Nếu cart thay đổi, re-validate voucher
4. **Case insensitive:** `summer`, `SUMMER`, `SuMmEr` đều work
5. **Whitespace:** `  SUMMER  ` → auto trim

