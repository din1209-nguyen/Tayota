# Tổng hợp lựa chọn công nghệ AI Service và mô hình RAG

## 1. Công nghệ cốt lõi mà đề tài xây dựng trên đó

Công nghệ cốt lõi của AI Service trong đề tài Tayota là mô hình RAG (Retrieval-Augmented Generation). Đây là hướng kết hợp giữa truy xuất tri thức từ tài liệu nội bộ và khả năng sinh câu trả lời của mô hình ngôn ngữ lớn.

### 1.1 Vấn đề từ đề tài

Đề tài cần xây dựng một trợ lý tư vấn xe Toyota có thể trả lời câu hỏi tự nhiên của người dùng, tư vấn theo nhu cầu như ngân sách, số chỗ, mục đích sử dụng và cung cấp thông tin có căn cứ từ tài liệu của hệ thống.

Nếu chỉ dùng chatbot thông thường hoặc LLM thuần, hệ thống dễ gặp các vấn đề:

- Câu trả lời có thể không đúng với dữ liệu Toyota hiện có trong hệ thống.
- LLM có thể bịa thông tin về giá, thông số, phiên bản hoặc chính sách.
- Khó cập nhật tri thức khi tài liệu xe thay đổi.
- Không trả được nguồn tham khảo để người dùng hoặc admin kiểm chứng.
- Không xử lý tốt hội thoại nhiều lượt, ví dụ người dùng đã nêu ngân sách ở lượt trước rồi hỏi tiếp ở lượt sau.

RAG được dùng để giải quyết các vấn đề này bằng cách cho hệ thống tìm đúng đoạn tài liệu liên quan trước, sau đó mới đưa đoạn tài liệu đó vào LLM để sinh câu trả lời.

### 1.2 Yêu cầu từ đề tài

Công nghệ cốt lõi cần đáp ứng các tiêu chí:

- Trả lời được bằng tiếng Việt và phù hợp ngữ cảnh tư vấn xe Toyota.
- Truy xuất thông tin theo ngữ nghĩa, không chỉ dựa vào từ khóa.
- Dựa trên tài liệu nội bộ, hạn chế việc LLM tự suy diễn.
- Cho phép trả về nguồn tham khảo như tên tài liệu, trang, điểm liên quan và mã chunk.
- Cập nhật được kho tri thức khi admin upload, index lại hoặc xóa tài liệu PDF.
- Tích hợp được với backend/frontend thông qua API.
- Lưu được trạng thái hội thoại, lịch sử chat và nhu cầu đã trích xuất.
- Có khả năng mở rộng, thay thế từng thành phần như embedding model, vector database hoặc LLM provider.

### 1.3 Lựa chọn công nghệ nào

Đề tài chọn RAG tự xây dựng thay vì chỉ dùng LLM thuần, tìm kiếm từ khóa, rule-based chatbot hoặc fine-tuning model.

Lý do chọn RAG:

- RAG phù hợp với bài toán cần trả lời dựa trên tài liệu thay đổi thường xuyên.
- Không cần huấn luyện lại mô hình mỗi khi có tài liệu Toyota mới.
- Có thể kiểm soát nguồn tri thức được đưa vào câu trả lời.
- Dễ kết hợp với nghiệp vụ tư vấn xe như phân loại intent, trích xuất ngân sách, số chỗ, mục đích sử dụng.
- Phù hợp quy mô đề tài vì có thể triển khai bằng các thành phần độc lập: API service, vector database, document store và LLM.

So sánh với các lựa chọn khác:

| Lựa chọn | Ưu điểm | Hạn chế so với RAG trong đề tài |
| --- | --- | --- |
| LLM thuần | Trả lời tự nhiên, dễ tích hợp | Dễ bịa thông tin, không bám sát tài liệu nội bộ, khó trả nguồn |
| Tìm kiếm từ khóa | Đơn giản, nhanh | Không hiểu tốt câu hỏi tự nhiên, kém hiệu quả khi người dùng diễn đạt khác tài liệu |
| Rule-based chatbot | Kiểm soát tốt, ít phụ thuộc AI | Khó bao phủ nhiều cách hỏi, hội thoại kém linh hoạt |
| Fine-tuning | Có thể tùy biến mô hình theo miền dữ liệu | Tốn chi phí, cần dữ liệu huấn luyện, phải huấn luyện lại khi tài liệu thay đổi |
| LangChain/LlamaIndex | Có nhiều thành phần RAG có sẵn | Dư thừa với đề tài, khó kiểm soát sâu các rule nghiệp vụ riêng |

Vì vậy, đề tài chọn hướng RAG tự xây: chủ động pipeline, bám sát nghiệp vụ tư vấn xe Toyota và dễ kiểm soát nguồn dữ liệu. Đổi lại, hệ thống phải tự xử lý các phần như chunking, retrieval, rerank và metadata.

Trong hệ thống hiện tại, RAG được triển khai bằng các module chính:

- `rag.py`: điều phối pipeline hỏi đáp.
- `chunking.py`: chia tài liệu thành các chunk nhỏ.
- `embed.py`: tạo embedding cho tài liệu và câu hỏi.
- `vector_database.py`: lưu và truy xuất vector trong Qdrant.
- `conversation_state_manager.py`: lưu trạng thái hội thoại.
- `mongo_storage.py`: lưu file PDF, metadata tài liệu và job index.

### 1.4 Mô hình RAG được dùng như thế nào trong thiết kế hệ thống

Trong thiết kế hệ thống Tayota, RAG nằm ở AI Service và đóng vai trò lớp tri thức cho chatbot tư vấn xe. Frontend/backend chỉ cần gọi API chat, còn AI Service chịu trách nhiệm tìm tài liệu liên quan, đưa ngữ cảnh vào LLM và trả câu trả lời kèm nguồn tham khảo.

Ở mức tổng quan, RAG được dùng trong hai nhóm chức năng:

- Quản lý tri thức: tài liệu PDF Toyota được upload, tách nội dung, tạo embedding và lưu vào vector database.
- Hỏi đáp tư vấn: câu hỏi người dùng được phân tích, truy xuất các đoạn tài liệu liên quan, sau đó LLM sinh câu trả lời dựa trên context đã tìm được.

Nhờ vậy, chatbot vừa trả lời tự nhiên, vừa bám vào tài liệu Toyota đang được quản lý trong hệ thống. Chi tiết luồng vận hành có thể tách sang file mô tả mô hình vận hành riêng.

## 2. Công nghệ phụ

Các công nghệ phụ là những thành phần hỗ trợ để mô hình RAG vận hành được trong hệ thống. Mỗi công nghệ đảm nhiệm một phần cụ thể của pipeline.

### 2.1 FastAPI

- Vấn đề giải quyết: Cần một service API riêng để frontend, gateway và backend gọi chức năng chat, upload tài liệu, xem lịch sử và kiểm tra health.
- Yêu cầu: Nhẹ, dễ viết REST API, validate dữ liệu tốt, có OpenAPI/Swagger.
- Lựa chọn: Chọn FastAPI vì phù hợp Python AI stack hơn Flask/Django/Spring Boot cho một microservice nhỏ.
- Vai trò trong RAG: Là lớp giao tiếp và điều phối của AI Service, nhận yêu cầu từ gateway, validate dữ liệu bằng Pydantic, chuyển câu hỏi hoặc tài liệu vào các module RAG phù hợp, chuẩn hóa response và xử lý lỗi để frontend không phụ thuộc trực tiếp vào logic truy xuất, embedding hay LLM.

### 2.2 MongoDB và GridFS

- Vấn đề giải quyết: Cần lưu session, lịch sử chat, metadata tài liệu, job index và file PDF gốc.
- Yêu cầu: Lưu dữ liệu linh hoạt, hỗ trợ document dạng JSON, quản lý file PDF kèm metadata.
- Lựa chọn: Chọn MongoDB/GridFS vì phù hợp dữ liệu hội thoại và tài liệu hơn Redis hoặc file system đơn thuần.
- Vai trò trong RAG: Lưu trạng thái hội thoại, file PDF gốc, trạng thái index và log câu trả lời.

### 2.3 Qdrant

- Vấn đề giải quyết: Cần tìm các đoạn tài liệu liên quan theo ngữ nghĩa.
- Yêu cầu: Lưu vector embedding, tìm kiếm cosine similarity, hỗ trợ filter metadata theo nguồn tài liệu.
- Lựa chọn: Chọn Qdrant vì chuyên cho vector search và dễ triển khai hơn việc tự quản lý FAISS local; có filter tốt hơn cho metadata so với giải pháp đơn giản.
- Vai trò trong RAG: Lưu vector của chunk tài liệu và trả về các chunk gần nghĩa nhất với câu hỏi.

### 2.4 Sentence Transformers

- Vấn đề giải quyết: Cần chuyển câu hỏi và nội dung tài liệu thành vector để tìm kiếm ngữ nghĩa.
- Yêu cầu: Hỗ trợ tiếng Việt, chạy được local, tốc độ tốt, không quá nặng.
- Lựa chọn: Chọn `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` vì là model đa ngữ, nhẹ và tạo vector 384 chiều. Các model lớn như `multilingual-e5-large` có thể tốt hơn nhưng nặng hơn.
- Vai trò trong RAG: Tạo embedding cho chunk tài liệu khi index và cho câu hỏi khi truy vấn.

### 2.5 Groq/Llama

- Vấn đề giải quyết: Cần sinh câu trả lời tự nhiên từ context đã truy xuất.
- Yêu cầu: Trả lời tiếng Việt tốt, tốc độ nhanh, dễ gọi qua API.
- Lựa chọn: Chọn Groq với model `llama-3.3-70b-versatile` vì tốc độ phản hồi nhanh và đủ mạnh cho tư vấn hội thoại. Ollama local chủ động hơn nhưng cần tài nguyên máy chủ lớn; OpenAI/Gemini cũng tốt nhưng phụ thuộc provider và chi phí khác nhau.
- Vai trò trong RAG: Nhận context từ Qdrant và sinh câu trả lời cuối cùng cho người dùng.

### 2.6 PyMuPDF

- Vấn đề giải quyết: Cần đọc nội dung từ tài liệu PDF Toyota.
- Yêu cầu: Trích xuất text nhanh, giữ được thông tin theo trang để trả nguồn.
- Lựa chọn: Chọn PyMuPDF vì nhanh và phù hợp trích xuất text theo trang. PyPDF2 đơn giản nhưng kém ổn định hơn; OCR chỉ cần thiết nếu PDF là ảnh scan.
- Vai trò trong RAG: Chuyển PDF thành text đầu vào cho bước chunking.

### 2.7 Docker

- Vấn đề giải quyết: Cần chạy AI Service ổn định cùng MongoDB, Qdrant và các service backend khác.
- Yêu cầu: Đóng gói môi trường, dễ triển khai, giảm lỗi khác biệt giữa các máy.
- Lựa chọn: Chọn Docker vì phù hợp kiến trúc microservice và dễ chạy qua Docker Compose.
- Vai trò trong RAG: Đóng gói AI Service, dependencies Python và môi trường chạy.

## 3. Kết luận

Đề tài chọn RAG làm công nghệ cốt lõi vì bài toán tư vấn xe Toyota cần trả lời dựa trên tài liệu nội bộ, có khả năng cập nhật tri thức và hạn chế hallucination. Các công nghệ phụ như FastAPI, MongoDB/GridFS, Qdrant, Sentence Transformers, Groq/Llama, PyMuPDF và Docker được chọn để hỗ trợ từng bước trong pipeline: nhận câu hỏi, lưu tài liệu, tạo embedding, truy xuất context, sinh câu trả lời và triển khai service.
