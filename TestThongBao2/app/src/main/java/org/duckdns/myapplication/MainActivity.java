package org.duckdns.myapplication;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "notification_channel";
    private String lastNotificationId = "";

    // Định nghĩa dữ liệu thông báo
    public class NotificationData {
        public String id;
        public String title;
        public String message;
    }

    // Định nghĩa interface của Retrofit
    public interface ApiService {
        @GET("api/node-red")
        Call<NotificationData> getNotification();
    }

    // Khởi tạo Retrofit
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://tranghome.duckdns.org/") // URL cơ sở của API
            .addConverterFactory(GsonConverterFactory.create()) // Thêm converter cho Gson
            .build();

    ApiService apiService = retrofit.create(ApiService.class); // Tạo đối tượng ApiService

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cấu hình giao diện (padding cho systemBars)
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo Retrofit và gọi API
        Button notifyButton = findViewById(R.id.notifyButton);
        notifyButton.setOnClickListener(v -> fetchNotificationData());

        // Tạo kênh thông báo
        createNotificationChannel();

        // Lập lịch gọi API mỗi 10 giây (hoặc thời gian tùy chọn khác)
        startPollingForNotifications();
    }

    // Phương thức gọi API
    private void fetchNotificationData() {
        apiService.getNotification().enqueue(new Callback<NotificationData>() {
            @Override
            public void onResponse(Call<NotificationData> call, Response<NotificationData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NotificationData data = response.body();
                    if (!data.id.equals(lastNotificationId)) {
                        // Nếu ID khác, hiển thị thông báo mới
                        lastNotificationId = data.id;
                        showNotification(data.title, data.message);
                    }
                } else {
                    // Xử lý lỗi API
                }
            }

            @Override
            public void onFailure(Call<NotificationData> call, Throwable t) {
                // Xử lý lỗi khi không thể kết nối API
            }
        });
    }

    // Phương thức hiển thị thông báo
    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Icon nhỏ cho thông báo
                .setContentTitle(title)       // Tiêu đề thông báo từ API
                .setContentText(message)      // Nội dung thông báo từ API
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Đặt mức độ ưu tiên
                .setAutoCancel(true); // Tự động đóng thông báo khi nhấn

        // Intent để mở Activity khi nhấn vào thông báo
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingIntent); // Gắn Intent vào thông báo

        // Gửi thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build()); // ID của thông báo là 1
    }

    // Phương thức tạo kênh thông báo (Android 8.0 trở lên yêu cầu kênh)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Thông báo",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Kênh thông báo của ứng dụng");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Lập lịch gọi API mỗi 10 giây để kiểm tra thông báo mới
    private void startPollingForNotifications() {
        final int POLL_INTERVAL = 1000; // 10 giây
        final android.os.Handler handler = new android.os.Handler();
        Runnable pollingRunnable = new Runnable() {
            @Override
            public void run() {
                fetchNotificationData();
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };
        handler.post(pollingRunnable);
    }
}
