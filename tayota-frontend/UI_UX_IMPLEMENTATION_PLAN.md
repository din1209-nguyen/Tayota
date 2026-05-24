# TAYOTA UI/UX Implementation Plan

Tai lieu nay la brief thiet ke va ke hoach trien khai giao dien cho frontend `tayota-frontend`. Muc tieu la giup Codex agent khac hieu dung ngu canh du an TAYOTA, thiet ke giao dien den/trang sang trong, responsive, va ket noi chuan voi backend hien co.

> Luu y: `DESIGN_CONTEXT.md` hien tai mo ta mot ban React/Vite cu. Source of truth cua frontend hien tai la Next.js App Router trong `src/app`.

## 1. Muc Tieu San Pham

- Thuong hieu: `TAYOTA`.
- Linh vuc: website xe hoi, catalogue xe, tu van mua xe, dat lich lai thu, dat lich dich vu, quan ly lich hen, chat AI.
- Dinh vi: sang trong, toi gian, chuyen nghiep, huong toi khach hang thuong luu.
- Cam giac giao dien: showroom cao cap, nhieu khoang tho, anh xe lon, thong tin ro rang, thao tac it buoc.
- Ngon ngu hien thi: tieng Viet co dau, tone lich thiep, tu tin, khong su dung ngon ngu giam gia qua dai tra.

## 2. Stack Hien Tai

Frontend:

- Framework: Next.js `16.2.3`.
- React: `19.2.4`.
- Router: Next App Router trong `src/app`.
- Styling: Tailwind CSS 4 qua `@import "tailwindcss"` trong `src/app/globals.css`.
- Entry files:
  - `src/app/layout.js`: root layout, metadata, font.
  - `src/app/page.js`: trang chu hien dang la trang debug API.
  - `src/app/globals.css`: global styles.

Backend:

- API Gateway: Spring Cloud Gateway, mac dinh port `8090`.
- Operation service: Spring Boot, mac dinh port `8091`.
- AI service: FastAPI, mac dinh port `8094`.
- Gateway route prefixes:
  - `/user/**` -> operation-service, `StripPrefix=1`.
  - `/car/**` -> operation-service, `StripPrefix=1`.
  - `/operation/**` -> operation-service, `StripPrefix=1`.
  - `/ai/**` -> ai-service, `StripPrefix=1`.
  - `/user/chat/ws/**` -> websocket route, `StripPrefix=1`.

## 3. Huong Dan Ket Noi FE/BE

### Bien moi truong frontend

Tao `.env.local` trong `tayota-frontend` khi can chay local:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8090
```

Khi frontend chay trong Docker cung network voi gateway, dung URL noi bo phu hop voi compose, vi du:

```env
NEXT_PUBLIC_API_BASE_URL=http://api-gateway:8090
```

### Quy tac goi API

- Tat ca request tu frontend nen di qua API Gateway, khong goi truc tiep operation-service hoac ai-service.
- Khong duoc bo prefix gateway:
  - Auth: `/user/login`, `/user/register`, `/user/refresh-token`.
  - Catalog: `/car/catalog/car-versions`.
  - Appointment: `/operation/appointments/...`.
  - AI chat: `/ai/api/v1/chat`.
- Backend Spring tra response wrapper dang:

```json
{
  "success": true,
  "code": 200,
  "message": "Thong bao",
  "result": {},
  "timestamp": "..."
}
```

Trong Java field la `isSuccess`; JSON co the serialize thanh `success`. Khi viet helper, nen chap nhan ca `success` va `isSuccess`.

### Fetch helper de tranh loi ket noi

Nen tao helper o `src/lib/api.js`:

```js
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8090";

export async function apiFetch(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...(options.headers || {}),
    },
  });

  const data = await response.json().catch(() => null);
  const okByBody = data?.success ?? data?.isSuccess;

  if (!response.ok || okByBody === false) {
    throw new Error(data?.message || `Request failed: ${response.status}`);
  }

  return data?.result ?? data;
}
```

Voi login/refresh-token/logout phai giu `credentials: "include"` de browser nhan/gui HttpOnly refresh cookie.

## 4. API Mapping Cho Frontend

| Chuc nang | Method | Gateway path | Ghi chu UI |
|---|---:|---|---|
| Dang ky | POST | `/user/register` | Form khach hang, hien huong dan xac thuc email |
| Dang nhap | POST | `/user/login` | Luu access token trong memory/state hoac storage co kiem soat |
| Refresh token | POST | `/user/refresh-token` | Dung cookie HttpOnly |
| Dang xuat | POST | `/user/logout` | Xoa state client sau khi goi API |
| Ho so hien tai | GET | `/user/me` | Can Authorization header |
| Danh sach xe | GET | `/car/catalog/car-versions` | Query: `keyword`, `styleId`, `seriesId`, `modelYear`, `minPrice`, `maxPrice`, `page`, `size` |
| Kieu dang + phien ban | GET | `/car/catalog/car-styles-with-versions` | Dung cho filter catalog |
| Chi tiet xe | GET | `/car/catalog/car-versions/{carVersionId}` | Trang chi tiet xe |
| Thong so xe | GET | `/car/catalog/car-versions/{carVersionId}/specification` | Bang thong so |
| So sanh xe | GET | `/car/catalog/car-versions/compare?ids=a&ids=b` | Toi da 2-3 xe de UI gon |
| Khung gio trong | GET | `/operation/appointments/available-slots` | Query: `dealershipId`, `appointmentType`, `appointmentDate` |
| Dat lai thu guest | POST | `/operation/appointments/test-drive/guest` | Form khach chua dang nhap |
| Dat lai thu user | POST | `/operation/appointments/test-drive` | Can Authorization |
| Dat dich vu guest | POST | `/operation/appointments/service/guest` | Can VIN |
| Lich hen cua toi | GET | `/operation/appointments/my` | Can Authorization |
| Chat AI RAG | POST | `/ai/api/v1/chat` | Header bat buoc `X-AI-Session-Id`, body `{ "message": "..." }` |
| Lich su AI session | GET | `/ai/api/v1/sessions/{sessionId}/messages` | Dung khi khoi phuc chat |
| Health AI | GET | `/ai/health` | Chi dung debug/admin |

## 5. Visual System Den Trang Cao Cap

### Mau sac

Nen dung palette toi gian, uu tien den/trang/xam kim loai:

```css
:root {
  --background: #ffffff;
  --foreground: #09090b;
  --ink: #050505;
  --soft-black: #111111;
  --muted: #f4f4f5;
  --muted-foreground: #71717a;
  --border: #e4e4e7;
  --panel: #fafafa;
  --metal: #a1a1aa;
  --champagne: #d6c7a1;
}
```

Quy tac:

- Den la mau thuong hieu chinh, trang la khong gian cao cap.
- `champagne` chi dung lam accent rat tiet che cho gia, badge premium, focus state.
- Khong dung palette xanh duong cu trong `DESIGN_CONTEXT.md` neu dang lam ban sang trong moi.
- Khong dung gradient mau sac ruc ro; neu can gradient, chi dung den -> xam dam hoac anh that co overlay den.

### Typography

- Heading: font hien co Geist Sans co the giu, nhung nen dung can nang `600-700`, letter spacing `0`.
- Body: Geist Sans, line-height thoang.
- Khong scale font theo viewport width.
- Heading hero ngan, tap trung vao thuong hieu hoac dong xe:
  - `TAYOTA`
  - `Chuan muc moi cua chuyen dong sang trong`
  - `Trai nghiem lai thu rieng tu`

### Layout

- Container desktop: max width `1200px` den `1320px`, padding ngang `24px-40px`.
- Mobile: padding ngang `16px`, cac section cach nhau ro.
- Card radius toi da `8px`, border mong, shadow rat nhe.
- Khong long card trong card.
- Anh xe phai la tin hieu chinh trong first viewport, khong de hero chi co text.

## 6. Cau Truc Frontend De Xuat

Nen chuyen tu trang debug API sang cau truc ro rang:

```txt
src/
  app/
    layout.js
    page.js
    globals.css
    vehicles/
      page.js
      [id]/
        page.js
    compare/
      page.js
    appointments/
      test-drive/
        page.js
      service/
        page.js
    auth/
      login/
        page.js
      register/
        page.js
  components/
    layout/
      Header.js
      Footer.js
      MobileNav.js
    home/
      HeroSection.js
      FeaturedVehicles.js
      ConciergeTools.js
      OwnershipServices.js
    vehicles/
      VehicleCard.js
      VehicleFilters.js
      SpecificationTable.js
    appointments/
      AppointmentForm.js
      TimeSlotPicker.js
    chat/
      AiChatWidget.js
  lib/
    api.js
    format.js
    session.js
```

Neu chua can tach route ngay, van nen tach component trong `src/components` de `src/app/page.js` chi con compose layout.

## 7. Thiet Ke Cac Man Hinh Chinh

### Header

- Sticky top, nen trang hoac den tuy trang thai scroll.
- Logo text `TAYOTA` ro o trai, khong chi de nho trong nav.
- Desktop nav: `Dong xe`, `So sanh`, `Lai thu`, `Dich vu`, `AI tu van`, `Tai khoan`.
- Mobile: hamburger, menu full-height hoac sheet; touch target toi thieu `44px`.
- CTA phu hop khach cao cap: `Dat lich rieng`, `Tu van 1:1`.

### Home

First viewport:

- Hero full-bleed hoac gan full-bleed voi anh xe that/asset xe chat luong cao.
- Overlay den nhe de text doc duoc.
- CTA chinh: `Kham pha dong xe`.
- CTA phu: `Dat lai thu rieng`.
- Ben duoi hero phai lo mot phan section tiep theo tren moi viewport.

Section tiep theo:

- `Bo suu tap TAYOTA`: lay data tu `/car/catalog/car-versions`.
- `Dich vu ca nhan hoa`: lai thu, bao duong, tu van AI.
- `Trai nghiem so huu`: lich hen, thong bao, review dich vu.

### Vehicle Catalog

- Data bat buoc lay tu backend catalog, khong hard-code xe neu backend da san sang.
- Co loading skeleton, empty state, error state.
- Filter desktop la sidebar mong; mobile la button mo sheet/filter panel.
- Moi card xe hien:
  - Ten phien ban/dong xe.
  - Nam model.
  - Gia tu/thang neu backend co.
  - Kieu dang, nhien lieu, so cho neu DTO co.
  - CTA `Chi tiet`, `So sanh`, `Dat lai thu`.

### Vehicle Detail

- Hero anh xe lon, gia va CTA ro.
- Tabs/sections: tong quan, thong so, mau sac, phu kien, so sanh.
- Specification dung bang 2 cot tren desktop, accordion tren mobile.
- CTA sticky bottom tren mobile: `Dat lai thu` va `Tu van AI`.

### Appointment

- Hai luong rieng:
  - Lai thu: `/operation/appointments/test-drive/guest` hoac `/operation/appointments/test-drive`.
  - Dich vu: `/operation/appointments/service/guest` hoac `/operation/appointments/service`.
- Truoc khi submit phai goi `/operation/appointments/available-slots`.
- Neu user chua dang nhap, hien guest form gom thong tin lien he. Neu da dang nhap, reuse thong tin profile.
- Sau khi submit thanh cong, hien ma lich hen tu `result` va trang thai cho xac nhan.

### AI Chat

- Floating widget goc duoi phai, khong che CTA mobile.
- Khi gui message toi AI service:
  - Tao `session_id` o client neu chua co.
  - Header `X-AI-Session-Id: <session_id>`.
  - Neu co user: them `X-User-Id`.
  - Body `{ "message": input }`.
- Hien typing state, error retry, va nguon tham khao neu `sources` khong rong.
- Khong fake bot response khi API that da co.

## 8. Responsive Checklist

- Mobile `320px-480px`: khong text overflow trong button/card.
- Tablet `768px`: grid 2 cot khi noi dung du rong.
- Desktop `1024px+`: nav day du, catalog 3 cot.
- Wide desktop `1440px+`: container gioi han max width, khong keo text qua dai.
- Form fields full width tren mobile, 2 cot tren desktop.
- Hero khong qua cao: desktop nen de thay duoc dau section tiep theo.
- Chat widget mobile: width `calc(100vw - 32px)`, max height `70vh`.

## 9. Giai Doan Trien Khai

### Giai doan 0: Chuan hoa nen tang

- Cap nhat metadata trong `src/app/layout.js`: title `TAYOTA`, description dung cho xe va dich vu.
- Thay `src/app/page.js` debug API bang trang chu that.
- Them `src/lib/api.js` voi `apiFetch`.
- Tao `.env.local.example` neu can huong dan bien `NEXT_PUBLIC_API_BASE_URL`.
- Dam bao tieng Viet trong file moi luu UTF-8.

Kiem tra xong giai doan:

- `npm run lint`.
- Mo trang chu khong con goi `/auth/register` debug khi render.

### Giai doan 1: Design tokens va layout shell

- Mo rong `globals.css` voi token den/trang, focus state, base body.
- Tao Header/Footer responsive.
- Tao container utility/class pattern thong nhat.
- Dam bao header mobile va desktop khong overlap.

Kiem tra xong giai doan:

- Test viewport `375px`, `768px`, `1440px`.
- Kiem tra contrast text tren nen den/trang.

### Giai doan 2: Home page premium

- Tao HeroSection co anh xe lon va CTA.
- Tao FeaturedVehicles doc data catalog tu backend.
- Tao ConciergeTools cho so sanh, lai thu, dich vu, AI tu van.
- Them loading/error state khi backend chua san sang.

Kiem tra xong giai doan:

- Khi gateway down, UI hien loi than thien va khong crash.
- Khi gateway up, xe hien dung tu `/car/catalog/car-versions`.

### Giai doan 3: Catalogue va chi tiet xe

- Tao `/vehicles` voi filter query.
- Tao `/vehicles/[id]` voi detail va specification.
- Tao flow `So sanh` dua vao `/car/catalog/car-versions/compare`.

Kiem tra xong giai doan:

- Query params khong bi mat khi reload.
- Empty state khi filter khong co xe.
- Detail 404/error state ro rang.

### Giai doan 4: Dat lich lai thu va dich vu

- Tao form lai thu va form dich vu rieng.
- Goi available slots truoc khi hien chon gio.
- Submit dung endpoint guest/user theo trang thai dang nhap.
- Xu ly validation client can ban truoc khi submit.

Kiem tra xong giai doan:

- Guest flow dat lich thanh cong khi body hop le.
- Loi 400/409 tu backend hien thanh message doc duoc.
- Button submit co disabled/loading de tranh double submit.

### Giai doan 5: Auth va profile

- Tao login/register.
- Login doc `result.accessToken` va gui `Authorization: Bearer ...` cho API can auth.
- Refresh/logout dung cookie HttpOnly qua `credentials: include`.
- Tao `me/profile` khi can.

Kiem tra xong giai doan:

- Login thanh cong, refresh cookie duoc set boi backend.
- Reload trang khong lam UI vao trang thai vo nghia; co loading auth state.

### Giai doan 6: AI Chat

- Tao chat widget that, ket noi `/ai/api/v1/chat`.
- Luu `session_id` trong localStorage hoac cookie client.
- Hien answer, intent/stage neu can debug an trong dev, sources neu co.

Kiem tra xong giai doan:

- Thieu `X-AI-Session-Id` se khong xay ra.
- AI service down thi hien retry va khong mat lich su UI hien tai.

### Giai doan 7: QA, accessibility, polish

- Kiem tra keyboard navigation, focus ring, aria-label cho icon button.
- Kiem tra mobile menu, filter panel, form date/time.
- Dinh dang gia tien VND thong nhat.
- Them skeleton thay vi layout shift.
- Chay lint/build truoc khi ban giao.

Kiem tra xong giai doan:

- `npm run lint`.
- `npm run build`.
- Smoke test cac flow: home, catalog, detail, appointment, login, chat.

## 10. Quy Tac Cho Codex Agent

- Doc file nay truoc khi sua UI.
- Khong dua lai palette xanh duong cu neu task dang theo huong TAYOTA den/trang cao cap.
- Khong hard-code data xe khi backend catalog co endpoint tuong ung.
- Khong goi backend bo qua gateway.
- Khong sua prefix endpoint neu chua doc `api-gateway/src/main/resources/application.yml`.
- Moi request co auth phai gui `Authorization: Bearer <accessToken>`.
- Request lien quan cookie refresh phai co `credentials: "include"`.
- Moi API async phai co loading, empty, error state.
- Moi form submit phai chong double submit.
- UI phai responsive truoc khi coi la hoan tat.
- Sau thay doi frontend lon, chay `npm run lint` va `npm run build` neu moi truong cho phep.

## 11. Definition Of Done

Mot giai doan chi duoc xem la xong khi:

- UI dung tone den/trang sang trong, khong mot mau va khong loi layout mobile.
- Khong con trang debug API tren route nguoi dung.
- Du lieu dong di qua helper API chung.
- Endpoint dung prefix gateway.
- Loi backend/network duoc hien thi than thien.
- Lint/build khong loi, hoac neu khong chay duoc phai ghi ro ly do.
