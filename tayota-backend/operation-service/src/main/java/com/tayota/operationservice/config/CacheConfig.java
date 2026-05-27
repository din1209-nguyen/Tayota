package com.tayota.operationservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CacheConfig implements CachingConfigurer {

    // Khai báo cache manager in-memory cho nhóm dữ liệu xe để cấu hình TTL theo từng nhóm cache.
    @Bean
    public CacheManager cacheManager() {
        // Tạo danh sách cache Caffeine dùng cho catalog xe, phụ kiện và xe vật lý.
        List<CaffeineCache> caches = new ArrayList<>();

        // Lưu cây kiểu dáng/dòng xe/phiên bản trong 2 giờ để trang catalog trả nhanh.
        caches.add(createCache("catalogStylesWithVersions", Duration.ofHours(2)));

        // Lưu chi tiết phiên bản trong 1 giờ để trang giới thiệu xe phản hồi nhanh hơn.
        caches.add(createCache("catalogVersionDetail", Duration.ofHours(1)));

        // Lưu thông số kỹ thuật trong 6 giờ vì dữ liệu này ít thay đổi.
        caches.add(createCache("catalogSpecification", Duration.ofHours(6)));

        // Lưu kết quả tìm kiếm ngắn hạn để tránh cache dữ liệu lọc quá lâu.
        caches.add(createCache("catalogVersionSearch", Duration.ofMinutes(15)));
        caches.add(createCache("accessorySearch", Duration.ofMinutes(15)));

        // Lưu chi tiết phụ kiện trong 30 phút để cân bằng tốc độ và độ mới dữ liệu.
        caches.add(createCache("accessoryDetail", Duration.ofMinutes(30)));

        // Lưu tìm kiếm xe vật lý rất ngắn vì trạng thái xe có thể thay đổi nhanh.
        caches.add(createCache("physicalCarSearch", Duration.ofMinutes(2)));

        // Lưu dữ liệu quản trị danh mục xe theo TTL hiện có.
        caches.add(createCache("carStyleList", Duration.ofHours(2)));
        caches.add(createCache("carStyleDetail", Duration.ofHours(2)));
        caches.add(createCache("carSeriesList", Duration.ofHours(2)));
        caches.add(createCache("carSeriesDetail", Duration.ofHours(2)));
        caches.add(createCache("carVersionList", Duration.ofMinutes(30)));

        // Đăng ký các cache Caffeine vào SimpleCacheManager.
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        // Trả về handler để lỗi cache không làm hỏng luồng nghiệp vụ chính.
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                try {
                    // Xóa entry lỗi để request sau có thể đọc lại từ database.
                    cache.evictIfPresent(key);
                }
                catch (RuntimeException ignored) {
                    // Bỏ qua lỗi xóa cache để request tiếp tục đọc dữ liệu từ database.
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                // Bỏ qua lỗi ghi cache để cache hệ thống không làm hỏng luồng nghiệp vụ chính.
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                // Bỏ qua lỗi xóa cache đơn lẻ khi cache tạm thời không sẵn sàng.
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                // Bỏ qua lỗi xóa toàn bộ cache khi cache tạm thời không sẵn sàng.
            }
        };
    }

    // Tạo cache in-memory theo TTL truyền vào.
    private CaffeineCache createCache(String name, Duration ttl) {
        // Cấu hình cache Caffeine với TTL ghi và giới hạn số lượng entry.
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build(), false);
    }
}
