package server;

import java.io.IOException;
import java.io.InputStream;

/**
 * PHASE 5 - Protocol Input Stream
 *
 * Đọc dữ liệu từ Socket không dùng BufferedReader.
 *
 * VẤN ĐỀ với BufferedReader:
 *   BufferedReader đọc trước (buffer ahead) một chunk lớn (thường 8KB).
 *   Khi ta gọi readLine() để đọc "SIZE 1024\n", BufferedReader có thể
 *   đã đọc luôn một phần bytes của FILE vào buffer nội bộ.
 *   Sau đó khi ta cố đọc bytes từ raw InputStream → mất dữ liệu!
 *
 *   Ví dụ minh họa:
 *     [SIZE 1024\n][byte1][byte2]...[byte1024]
 *     BufferedReader.readLine() → "SIZE 1024"
 *     Nhưng BufferedReader đã đọc [byte1][byte2]...[byteN] vào buffer!
 *     rawInputStream.read() → bắt đầu từ [byteN+1] → MẤT [byte1..byteN]!
 *
 * GIẢI PHÁP:
 *   Đọc từng byte một. Không buffer. Luôn nhất quán.
 *   readLine() đọc đến '\n' → dừng ĐÚNG tại đó.
 *   Bytes của file vẫn nguyên vẹn trong InputStream.
 */
public class ProtocolInputStream {

    // Raw InputStream của Socket — không bao bọc thêm gì
    private final InputStream in;

    /**
     * @param in  InputStream lấy trực tiếp từ socket.getInputStream()
     */
    public ProtocolInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * Đọc một dòng text, kết thúc bằng '\n'.
     *
     * Đọc từng byte một:
     *   - Gặp '\n' → trả về dòng đã đọc (không kèm '\n')
     *   - Gặp '\r' → bỏ qua (Windows line ending \r\n)
     *   - Gặp -1 (EOF) → client đã đóng kết nối → trả null
     *
     * @return  Dòng text (không có '\n'), hoặc null nếu kết nối đóng
     */
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int byteRead;

        while ((byteRead = in.read()) != -1) {
            if (byteRead == '\n') {
                // Kết thúc dòng — trả về nội dung đã đọc
                return sb.toString();
            }
            if (byteRead == '\r') {
                // Bỏ qua ký tự \r (Windows \r\n line ending)
                continue;
            }
            sb.append((char) byteRead);
        }

        // byteRead == -1 → EOF → client đóng kết nối
        return null;
    }

    /**
     * Trả về raw InputStream để đọc binary data (bytes của file).
     *
     * Gọi hàm này SAU KHI đã readLine() xong phần text.
     * Bytes ngay sau đó là dữ liệu binary của file.
     *
     * @return  InputStream gốc của Socket
     */
    public InputStream getRawInputStream() {
        return in;
    }
}
