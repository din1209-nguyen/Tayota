# Tayota Backend AI Coding Guide

Tài liệu này dùng để đưa cho AI/agent khi cần code tiếp trong dự án `tayota-backend`. Mục tiêu là giữ code mới giống cấu trúc, phong cách và quy ước hiện có của `operation-service`, `api-gateway`.

## Tổng quan kiến trúc

- Repo là dạng multi-service, mỗi service có thư mục riêng: `operation-service`, `api-gateway`, `chat-service`, `ai-service`.
- Các service Java dùng Spring Boot, Maven, Java 21.
- Mã dùng chung nằm trực tiếp trong `operation-service` dưới package `com.tayota.commoncore`:
  - DTO response chuẩn: `ApiResponse<T>`
  - exception chuẩn: `CustomException`, `GlobalExceptionHandler`, `ErrorCode`
  - security header filter: `HeaderAuthenticationFilter`
  - util dùng chung như `SecurityContextUtil`, `OtpUtil`
  - config tự động qua `AutoConfiguration`
- `api-gateway` dùng Spring Cloud Gateway WebFlux:
  - Verify JWT access token.
  - Gắn header `X-User-Id`, `X-User-Role` xuống service con.
  - Strip prefix route như `/user/** -> operation-service`, `/car/** -> operation-service`.
- Service con không tự decode JWT nếu dùng common security. Service con đọc user hiện tại từ security context đã được tạo bởi header filter.

## Cấu trúc package chuẩn cho service Java

Ví dụ với `com.tayota.operationservice`:

```text
src/main/java/com/tayota/<service>/
  config/
  controller/
  dto/
    Request/
    Response/
  entity/
  enums/
  grpc/
  mapper/
  object/
  repository/
    projection/
  service/
  util/
  <Service>Application.java
```

Không phải service nào cũng có đủ mọi package. Chỉ tạo package khi thật sự cần.

## Quy ước đặt tên

- Class controller: `<Domain>Controller`, ví dụ `AuthController`, `CarVersionController`.
- Class service: `<Domain>Service`.
- Repository: `<Entity>Repository extends JpaRepository<Entity, UUID>`.
- DTO request: `<Action><Domain>RequestDTO`, đặt trong `dto.Request`.
- DTO response: `<Action/Domain>ResponseDTO`, đặt trong `dto.Response`.
- Entity: danh từ domain, ví dụ `User`, `CarVersion`, `CarSpecification`.
- Mapper: `<Domain>Mapper`, dùng static method nếu mapping đơn giản.
- Enum: `<Something>Type`, ví dụ `StatusType`, `ProviderType`, `RoleType`.
- Object/helper data class không phải entity/DTO đặt trong `object`.

## Controller style

Controller chỉ làm việc mỏng:

- Nhận request, validate bằng `@Valid`, `@Validated`.
- Parse tham số đơn giản nếu cần.
- Gọi service.
- Trả về `ApiResponse.success(code, message, result)`.
- Không viết business logic dài trong controller.
- Dùng `@PreAuthorize` cho quyền ở endpoint.
- Response message hiện tại dùng tiếng Việt.

Mẫu:

```java
@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarVersionController {
    private final CarVersionService carVersionService;

    @PostMapping("/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<CreateCarVersionResponseDTO> createCarVersion(@Valid @RequestBody CreateCarVersionRequestDTO request) {
        CreateCarVersionResponseDTO response = carVersionService.createCarVersion(request);
        return ApiResponse.success(201, "Thêm phiên bản xe thành công!", response);
    }
}
```

Khi endpoint nhận `@RequestParam` hoặc `@PathVariable` cần validate, dùng:

- `@Validated` trên controller nếu validate tham số rời.
- `@NotBlank`, `@Email`, `@Size`, v.v.
- Với lỗi parse thủ công, ném `new CustomException(400, "Thông báo lỗi")`.

## Service style

Service chứa business logic chính:

- Annotate `@Service`, `@RequiredArgsConstructor`.
- Inject dependency bằng `private final`.
- Dùng `@Transactional` cho method ghi dữ liệu.
- Dùng `@Transactional(readOnly = true)` cho truy vấn đọc.
- Validate nghiệp vụ bằng `CustomException`.
- Repository trả `Optional` thì dùng `orElseThrow`.
- Redis/Kafka/gRPC xử lý trong service hoặc helper riêng.
- Helper private đặt cuối class.

Mẫu:

```java
@Service
@RequiredArgsConstructor
public class CarVersionService {
    private final CarVersionRepository carVersionRepository;
    private final CarSeriesRepository carSeriesRepository;

    @Transactional
    public CreateCarVersionResponseDTO createCarVersion(CreateCarVersionRequestDTO request) {
        UUID carSeriesId = parseUuid(request.getCarSeriesId());
        CarSeries carSeries = carSeriesRepository.findById(carSeriesId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy dòng xe"));

        String version = request.getVersion().trim();

        if (carVersionRepository.existsByCarSeriesIdAndVersion(carSeriesId, version)) {
            throw new CustomException(409, "Phiên bản xe đã tồn tại");
        }

        CarVersion carVersion = CarVersion.builder()
                .carSeries(carSeries)
                .version(version)
                .build();

        CarVersion savedCarVersion = carVersionRepository.save(carVersion);
        return new CreateCarVersionResponseDTO(savedCarVersion.getId(), "Thêm phiên bản xe thành công.");
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(400, "Id không hợp lệ");
        }
    }
}
```

## Response và exception

Luôn dùng `ApiResponse<T>` từ mã dùng chung cho API response.

Success:

```java
return ApiResponse.success(200, "Lấy dữ liệu thành công!", data);
```

Không có body:

```java
return ApiResponse.success(200, "Xử lý thành công!", null);
```

Lỗi nghiệp vụ:

```java
throw new CustomException(404, "Không tìm thấy dữ liệu");
throw new CustomException(ErrorCode.USER_NOT_FOUND);
```

Không tự try/catch lỗi validation trong controller. `GlobalExceptionHandler` của mã dùng chung đã xử lý:

- JSON body rỗng/sai format.
- `MethodArgumentNotValidException`.
- `ConstraintViolationException`.
- `AccessDeniedException`.
- `CustomException`.
- Exception chưa dự đoán.

## DTO style

- Request DTO đặt trong `dto.Request`.
- Response DTO đặt trong `dto.Response`.
- Request DTO ưu tiên dùng Lombok `@Getter`; chỉ thêm `@Setter` khi có nhu cầu mutate rõ ràng.
- Dùng constructor DTO response khi response cố định.
- Validate request bằng Jakarta Validation và luôn khai báo `message` tiếng Việt cho annotation validation.
- Request body dùng camelCase theo tên field Java, giống request DTO của `operation-service`; không dùng `@JsonProperty` trong request DTO chỉ để đổi sang snake_case.
- `@JsonProperty` chỉ dùng khi thật sự cần giữ contract bên ngoài hoặc cho response/event đã có format cố định.

Mẫu request:

```java
@Getter
public class CreateCarVersionRequestDTO {
    @NotBlank(message = "Dòng xe không được để trống")
    private String carSeriesId;

    @NotBlank(message = "Phiên bản xe không được để trống")
    @Size(max = 50, message = "Phiên bản xe không được vượt quá 50 ký tự")
    private String version;

    @Valid
    @NotNull(message = "Thông số kỹ thuật không được để trống")
    private CreateCarSpecificationRequestDTO specification;
}
```

Mẫu response:

```java
@Getter
@Setter
public class AccessTokenResponseDTO {
    private String accessToken;

    public AccessTokenResponseDTO(String accessToken) {
        this.accessToken = accessToken;
    }
}
```

## Entity style

Entity dùng JPA annotation, Lombok, UUID:

- `@Entity`
- `@Table(name = "\"TABLE_NAME\"")` nếu table viết hoa hoặc cần quote.
- `@Id`
- `@GeneratedValue(strategy = GenerationType.UUID)`
- Enum dùng `@Enumerated(EnumType.STRING)`.
- Quan hệ dùng lazy fetch nếu phù hợp: `@ManyToOne(fetch = FetchType.LAZY)`.
- Timestamp có thể dùng `Instant.now()` hoặc `@CreationTimestamp`, theo style file gần nhất.
- Với entity mới trong nhóm API xe, có thể dùng `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

Mẫu:

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"CAR_VERSION\"")
public class CarVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_series_id", nullable = false)
    private CarSeries carSeries;
}
```

## Repository style

- Dùng Spring Data JPA derived query khi đủ rõ.
- Dùng `@Query` với JPQL text block khi query phức tạp.
- Dùng projection interface trong `repository.projection` cho list view.
- Dùng `@EntityGraph` để tránh N+1 khi cần load quan hệ.

Mẫu:

```java
@Repository
public interface CarVersionRepository extends JpaRepository<CarVersion, UUID> {
    boolean existsByCarSeriesIdAndVersion(UUID carSeriesId, String version);

    @EntityGraph(attributePaths = {"carSeries", "carSeries.carStyle"})
    Optional<CarVersion> findWithCarSeriesById(UUID id);
}
```

## Mapper style

- Nếu mapping đơn giản, dùng class mapper với static method.
- Mapper không inject repository/service.
- Chuẩn hoá string trong mapper nếu chỉ là conversion đơn giản, ví dụ `trimToNull`.
- Mapping nghiệp vụ cần query hoặc validate nên để trong service.

Mẫu:

```java
public class CarVersionMapper {
    public static CarVersionItemResponseDTO toResponse(CarVersionListProjection projection) {
        return new CarVersionItemResponseDTO(
                projection.getId(),
                projection.getVersion(),
                projection.getSeries()
        );
    }
}
```

## Security và auth flow

Gateway là nơi xác thực JWT:

- Public URL đưa vào whitelist trong `api-gateway` `AuthenticationFilter`.
- Request protected phải có `Authorization: Bearer <token>`.
- Gateway parse JWT bằng `JwtUtil`.
- Gateway xoá header giả mạo `X-User-Id`, `X-User-Role`, sau đó set lại từ claims.
- Service con nhận header qua `HeaderAuthenticationFilter` trong mã dùng chung.

Trong service con:

- Dùng `@PreAuthorize("hasRole('ADMIN')")` hoặc `hasAnyRole`.
- Lấy user hiện tại qua `SecurityContextUtil` hoặc util tương ứng đang dùng trong service.
- Không tin `X-User-*` từ client trực tiếp. Chỉ gateway được set header này.
- Nếu service cần tự config security riêng, set `common.security.enabled=false`.

## Cache, Redis, event

Redis:

- Dùng `RedisTemplate<String, Object>`.
- Key nên có prefix domain rõ ràng, ví dụ `auth:refresh:`, `cars:versions:page:%d:size:%d`.
- TTL dùng `Duration`.
- Với cache không critical, catch `RuntimeException`, log warn và tiếp tục xử lý DB.

Kafka/event:

- Object event đặt trong `object`.
- Producer/service gửi event sau khi action nghiệp vụ thành công.
- Với log hành vi, dùng action string constant như `VIEW_CAR_VERSION_ACTION`.

## API Gateway route style

Route khai báo trong `api-gateway/src/main/resources/application.yml`:

```yaml
- id: car-route
  uri: http://${OPERATION_SERVICE_HOST:operation-service}:${OPERATION_SERVICE_PORT:8091}
  predicates:
    - Path=/car/**
  filters:
    - StripPrefix=1
```

Nếu thêm service mới:

- Thêm route trong `application.yml`.
- Thêm biến host/port trong Docker Compose nếu cần.
- Cập nhật whitelist trong `AuthenticationFilter` nếu endpoint public.

## Maven và dependency

- Service Java dùng parent `org.springframework.boot:spring-boot-starter-parent:4.0.5`.
- Java version: `21`.
- Mã dùng chung đã nằm trực tiếp trong `operation-service`, không thêm dependency nội bộ riêng cho module dùng chung cũ.
- Nếu tách thêm service mới sau này, cân nhắc tách lại package chung thành dependency riêng trước khi dùng lại.
- Lombok scope `provided`, compiler plugin có annotation processor.
- gRPC chỉ thêm ở service thật sự cần proto/client/server.

## Formatting và phong cách code

- Indent 4 spaces.
- Method chaining xuống dòng theo style Spring hiện có.
- Import rõ ràng, không dùng wildcard trừ khi file hiện hữu đã dùng.
- Constant đặt đầu class: `private static final`.
- Field dependency đặt sau constant.
- Comment tiếng Việt được dùng khá nhiều để giải thích nghiệp vụ. Thêm comment khi logic không hiển nhiên, không comment boilerplate.
- Comment `// ...` nên bắt đầu bằng động từ, mô tả hành động đang làm. Ví dụ: `// Lấy thông tin user từ token`, `// Kiểm tra email đã tồn tại`, `// Gửi email xác thực qua gRPC`, `// Xoá token khỏi Redis`.
- Tránh comment dạng nhãn/danh từ cụt như `// User`, `// Validate`, `// Redis key`. Nếu cần, đổi thành câu hành động: `// Validate dữ liệu đầu vào`, `// Tạo key Redis cho token`.
- Message lỗi/thành công nên viết tiếng Việt, ngắn, đúng ngữ cảnh.
- Dùng `StringUtils.hasText` khi check string từ request/token nếu đã có Spring util.
- Trim input string trước khi lưu nếu field là text người dùng nhập.
- Không trả entity trực tiếp ra API. Luôn trả response DTO.

## Checklist trước khi code xong

- Endpoint trả `ApiResponse<T>` đúng code/message/result.
- Request DTO có validation đầy đủ.
- Service ném `CustomException` cho lỗi nghiệp vụ.
- Method ghi DB có `@Transactional`.
- Không decode JWT trong service con nếu thông tin đã có từ gateway và mã dùng chung.
- Không để business logic lớn trong controller.
- Không trả entity JPA trực tiếp.
- Query list có phân trang nếu dữ liệu có thể lớn.
- Cache/event nếu thêm mới phải có key/topic rõ tên domain.
- Nếu thêm public endpoint, cập nhật whitelist gateway.
- Nếu thêm route/service mới, cập nhật gateway và Docker Compose khi cần.
