const HomePage = async () => {
    const targetUrl = `http://${process.env.SERVER_HOST || 'localhost'}:${process.env.SERVER_PORT || '8090'}/auth/register`;

    let debugInfo = {
        statusCode: null,
        statusText: null,
        data: null,
        error: null
    };

    try {
        const response = await fetch(targetUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: "user_test@gmail", password: "123sdD@dskfdslk " }),
            cache: 'no-store'
        });

        // 1. Lấy thông tin từ Header của Response
        debugInfo.statusCode = response.status;     // Ví dụ: 200, 201, 400, 500
        debugInfo.statusText = response.statusText; // Ví dụ: OK, Created, Bad Request

        // 2. Parse nội dung Body
        const result = await response.json();
        debugInfo.data = result;

        if (!response.ok) {
            debugInfo.error = result.message || "Đã xảy ra lỗi từ phía Server";
        }
    } catch (err) {
        console.log("error: ", err);
        debugInfo.error = "Lỗi kết nối (Network Error / CORS / Docker down)";
    }

    return (
        <div style={{ padding: '20px', fontFamily: 'monospace' }}>
            <h2>Kết quả gọi API</h2>

            <div style={{ marginBottom: '10px' }}>
                <strong>Status Code: </strong>
                <span style={{ color: debugInfo.statusCode >= 400 ? 'red' : 'green' }}>
                    {debugInfo.statusCode} {debugInfo.statusText}
                </span>
            </div>

            {/* MESSAGE LỖI (NẾU CÓ) */}
            {debugInfo.error && (
                <div style={{ color: 'red', marginBottom: '10px' }}>
                    <strong>Message: </strong> {debugInfo.error}
                </div>
            )}

            {/* FULL DATA (DÙNG ĐỂ DEBUG) */}
            <div>
                <strong>Full Response Body:</strong>
                <pre style={{ background: '#f4f4f4', padding: '10px', marginTop: '10px', borderRadius: '4px' }}>
                    {JSON.stringify(debugInfo.data, null, 2)}
                </pre>
            </div>
        </div>
    );
};

export default HomePage;