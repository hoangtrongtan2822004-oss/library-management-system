# 💳 Payment Gateway Integration - VNPay Sandbox

## 🎯 Mục tiêu

User có thể thanh toán phạt quá hạn trực tuyến qua VNPay (không cần đến thư viện).

## 📋 Use Cases

### User Flow:

1. User vào trang "Phạt của tôi" → Thấy 50,000đ phạt quá hạn
2. Click "Thanh toán online"
3. Chuyển sang trang VNPay
4. Quét mã QR hoặc nhập thẻ
5. Thanh toán thành công → Quay về website
6. Hệ thống tự động cập nhật trạng thái phạt: PAID ✅

## 🛠️ Tech Stack

**VNPay Sandbox:**

- FREE, không cần đăng ký doanh nghiệp
- Test card: https://sandbox.vnpayment.vn/merchantv2/

**Backend:**

- Spring Boot REST API
- HMAC SHA-512 signature

## 📝 Implementation Steps

### Step 1: Đăng ký VNPay Sandbox (10 phút)

1. Truy cập: https://sandbox.vnpayment.vn/
2. Đăng ký tài khoản test
3. Lấy thông tin:
   ```
   TMN Code: YOUR_TMN_CODE
   Hash Secret: YOUR_HASH_SECRET
   Payment URL: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
   ```

### Step 2: Backend - VNPay Config (20 phút)

**File:** `application.properties`

```properties
# VNPay Config
vnpay.tmn-code=${VNPAY_TMN_CODE:YOUR_TMN_CODE}
vnpay.hash-secret=${VNPAY_HASH_SECRET:YOUR_HASH_SECRET}
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=${APP_URL:http://localhost:4200}/payment/callback
```

### Step 3: Payment Service (40 phút)

**File:** `lms-backend/src/main/java/com/ibizabroker/lms/service/PaymentService.java`

```java
package com.ibizabroker.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpayUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    public String createPaymentUrl(Integer fineId, Long amount, String ipAddress) {
        Map<String, String> vnpParams = new HashMap<>();

        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay: VNĐ * 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", "FINE_" + fineId + "_" + System.currentTimeMillis());
        vnpParams.put("vnp_OrderInfo", "Thanh toan phat: " + fineId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Sort params
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                     .append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String vnpSecureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return vnpayUrl + "?" + query.toString();
    }

    public boolean verifyPayment(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        return calculatedHash.equals(vnpSecureHash);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac", e);
        }
    }
}
```

### Step 4: Payment Controller (30 phút)

**File:** `lms-backend/src/main/java/com/ibizabroker/lms/controller/PaymentController.java`

```java
package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.service.PaymentService;
import com.ibizabroker.lms.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final FineService fineService;

    @PostMapping("/vnpay/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> createPayment(
            @RequestParam Integer fineId,
            HttpServletRequest request) {

        Fine fine = fineService.getFineById(fineId);

        String ipAddress = request.getRemoteAddr();
        String paymentUrl = paymentService.createPaymentUrl(
            fineId,
            fine.getAmount(),
            ipAddress
        );

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<Map<String, Object>> paymentCallback(
            @RequestParam Map<String, String> params) {

        boolean isValid = paymentService.verifyPayment(params);

        if (isValid && "00".equals(params.get("vnp_ResponseCode"))) {
            // Thanh toán thành công
            String txnRef = params.get("vnp_TxnRef");
            Integer fineId = extractFineId(txnRef); // Parse FINE_123_timestamp

            fineService.markAsPaid(fineId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thanh toán thành công!"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Thanh toán thất bại!"
            ));
        }
    }

    private Integer extractFineId(String txnRef) {
        // FINE_123_1234567890 -> 123
        String[] parts = txnRef.split("_");
        return Integer.parseInt(parts[1]);
    }
}
```

### Step 5: Frontend - Payment Button (40 phút)

**File:** `lms-frontend/src/app/my-fines/my-fines.component.html`

```html
<div class="card">
  <div class="card-body">
    <h5>Phạt của tôi</h5>

    <table class="table">
      <tr *ngFor="let fine of fines">
        <td>{{ fine.reason }}</td>
        <td>{{ fine.amount | currency:'VND' }}</td>
        <td>
          <span *ngIf="fine.status === 'PAID'" class="badge bg-success">
            Đã thanh toán
          </span>
          <button
            *ngIf="fine.status === 'UNPAID'"
            class="btn btn-primary btn-sm"
            (click)="payOnline(fine.id)"
          >
            💳 Thanh toán online
          </button>
        </td>
      </tr>
    </table>
  </div>
</div>
```

**File:** `lms-frontend/src/app/my-fines/my-fines.component.ts`

```typescript
import { Component } from "@angular/core";
import { HttpClient } from "@angular/common/http";

@Component({
  selector: "app-my-fines",
  templateUrl: "./my-fines.component.html",
})
export class MyFinesComponent {
  fines: any[] = [];

  constructor(private http: HttpClient) {
    this.loadFines();
  }

  loadFines() {
    this.http
      .get<any[]>("/api/user/fines")
      .subscribe((data) => (this.fines = data));
  }

  payOnline(fineId: number) {
    this.http
      .post<any>(`/api/payment/vnpay/create?fineId=${fineId}`, {})
      .subscribe((response) => {
        // Redirect to VNPay
        window.location.href = response.paymentUrl;
      });
  }
}
```

### Step 6: Payment Callback Page (20 phút)

**File:** `lms-frontend/src/app/payment-callback/payment-callback.component.ts`

```typescript
import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";

@Component({
  template: `
    <div class="container text-center mt-5">
      <div *ngIf="success" class="alert alert-success">
        <h3>✅ Thanh toán thành công!</h3>
        <p>Phạt của bạn đã được thanh toán.</p>
      </div>
      <div *ngIf="!success" class="alert alert-danger">
        <h3>❌ Thanh toán thất bại!</h3>
        <p>Vui lòng thử lại.</p>
      </div>
      <button class="btn btn-primary" (click)="goHome()">Về trang chủ</button>
    </div>
  `,
})
export class PaymentCallbackComponent implements OnInit {
  success = false;

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.success = params["vnp_ResponseCode"] === "00";
    });
  }

  goHome() {
    this.router.navigate(["/my-account"]);
  }
}
```

## ⏱️ Time Estimate

- VNPay setup: 10 phút
- Backend: 1.5 giờ
- Frontend: 1 giờ
- Testing: 30 phút
- **Total: 3 giờ**

## 🎯 Demo Value

- ⭐⭐⭐⭐⭐ Tính năng "đắt giá" cho startup
- Payment gateway = Professional system
- VNPay = Trusted in Vietnam

## 🧪 Test Cards (Sandbox)

```
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Tên: NGUYEN VAN A
Ngày hết hạn: 07/15
OTP: 123456
```

## 📚 Resources

- [VNPay Sandbox](https://sandbox.vnpayment.vn/apis/docs/huong-dan-tich-hop/)
- [VNPay Integration Guide](https://sandbox.vnpayment.vn/merchantv2/)
