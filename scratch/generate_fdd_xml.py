import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def create_fdd_xml():
    root = ET.Element("mxfile", host="app.diagrams.net", modified="2026-11-02T16:15:00.000Z", agent="SecureFTPClientPro", version="24.0.0", type="device")
    diagram = ET.SubElement(root, "diagram", id="Functional_Decomposition_Diagram", name="Sơ đồ Phân rã Chức năng (Functional Decomposition Diagram - FDD)")
    model = ET.SubElement(diagram, "mxGraphModel", dx="1200", dy="800", grid="1", gridSize="10", guides="1", tooltips="1", connect="1", arrows="1", fold="1", page="1", pageScale="1", pageWidth="1169", pageHeight="827", background="#FFFFFF", math="0", shadow="0")
    root_cell = ET.SubElement(model, "root")
    
    ET.SubElement(root_cell, "mxCell", id="0")
    ET.SubElement(root_cell, "mxCell", id="1", parent="0")

    # Helper: Add Functional Box (Level 0, 1, 2)
    def add_box(bid, text, x, y, w, h, level=2, parent_id="1"):
        if level == 0:
            style = "rounded=0;whiteSpace=wrap;html=1;strokeWidth=2.2;strokeColor=#000000;fillColor=#F0F0F0;fontFamily=Segoe UI;fontSize=12;fontStyle=1;align=center;verticalAlign=middle;"
        elif level == 1:
            style = "rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.8;strokeColor=#000000;fillColor=#F8F8F8;fontFamily=Segoe UI;fontSize=11;fontStyle=1;align=center;verticalAlign=middle;"
        else:
            style = "rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.4;strokeColor=#000000;fillColor=#FFFFFF;fontFamily=Segoe UI;fontSize=10;fontStyle=0;align=center;verticalAlign=middle;spacing=2;"
            
        cell = ET.SubElement(root_cell, "mxCell", id=bid, value=text, style=style, vertex="1", parent=parent_id)
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # Helper: Add Tree Connector (no arrow)
    def add_connector(cid, sid, tid, exit_x=0.5, exit_y=1.0, entry_x=0.5, entry_y=0.0):
        style = f"edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=none;exitX={exit_x};exitY={exit_y};entryX={entry_x};entryY={entry_y};"
        edge = ET.SubElement(root_cell, "mxCell", id=cid, value="", style=style, edge="1", parent="1", source=sid, target=tid)
        ET.SubElement(edge, "mxGeometry", relative="1", **{"as": "geometry"})
        return edge

    # Level 0: Root Function
    add_box("f_root", "<b>HỆ THỐNG TRUYỀN TỆP TIN AN TOÀN</b><br/>(SECURE FTP CLIENT PRO &amp; SERVER)", 325, 45, 520, 52, level=0)

    # 5 Level 1 Groups
    groups = [
        ("g1", "1.0 QUẢN LÝ TÀI KHOẢN<br/>&amp; XÁC THỰC", 40, 140, 205, 48),
        ("g2", "2.0 QUẢN LÝ TỆP TIN<br/>&amp; THƯ MỤC", 261, 140, 205, 48),
        ("g3", "3.0 TRUYỀN TẢI DỮ LIỆU<br/>(STOR &amp; RETR)", 482, 140, 205, 48),
        ("g4", "4.0 CHIA SẺ TỆP<br/>&amp; DỊCH VỤ QUOTA", 703, 140, 205, 48),
        ("g5", "5.0 QUẢN TRỊ HỆ THỐNG<br/>&amp; KIỂM TOÁN", 924, 140, 205, 48),
    ]

    for gid, gname, gx, gy, gw, gh in groups:
        add_box(gid, f"<b>{gname}</b>", gx, gy, gw, gh, level=1)
        add_connector(f"e_root_{gid}", "f_root", gid)

    # Sub-functions per group (Level 2)
    # Group 1 (x = 40)
    g1_subs = [
        ("f_1_1", "1.1 Đăng ký tài khoản mới"),
        ("f_1_2", "1.2 Đăng nhập hệ thống<br/>(Xác thực mật khẩu &amp; trạng thái)"),
        ("f_1_3", "1.3 Đổi mật khẩu cá nhân"),
        ("f_1_4", "1.4 Quản lý giao diện Client<br/>(Đổi Theme giao diện đồ họa)"),
        ("f_1_5", "1.5 Đăng xuất &amp; Hủy phiên làm việc")
    ]
    cur_y = 220
    for sid, stext in g1_subs:
        h = 42 if "<br/>" in stext else 36
        add_box(sid, stext, 40, cur_y, 205, h, level=2)
        add_connector(f"e_g1_{sid}", "g1", sid)
        cur_y += h + 20

    # Group 2 (x = 261)
    g2_subs = [
        ("f_2_1", "2.1 Duyệt danh sách tệp &amp; thư mục<br/>(Lệnh LIST - Cập nhật bảng dữ liệu)"),
        ("f_2_2", "2.2 Tạo thư mục mới<br/>(Lệnh MKD trong Sandbox)"),
        ("f_2_3", "2.3 Xóa thư mục rỗng<br/>(Lệnh RMD)"),
        ("f_2_4", "2.4 Chuyển đổi thư mục làm việc<br/>(Lệnh CWD, CDUP, PWD)"),
        ("f_2_5", "2.5 Đổi tên tệp tin an toàn<br/>(Lệnh RNFR &amp; RNTO)"),
        ("f_2_6", "2.6 Xóa tệp tin &amp; Hoàn trả Quota<br/>(Lệnh DELE đồng bộ CSDL)")
    ]
    cur_y = 220
    for sid, stext in g2_subs:
        h = 42 if "<br/>" in stext else 36
        add_box(sid, stext, 261, cur_y, 205, h, level=2)
        add_connector(f"e_g2_{sid}", "g2", sid)
        cur_y += h + 18

    # Group 3 (x = 482)
    g3_subs = [
        ("f_3_1", "3.1 Tải tệp lên máy chủ (Upload)<br/>(Lệnh STOR qua luồng Socket)"),
        ("f_3_2", "3.2 Kiểm tra &amp; Đối soát Quota<br/>(Chặn ghi khi vượt hạn mức lưu trữ)"),
        ("f_3_3", "3.3 Tải tệp về máy trạm (Download)<br/>(Lệnh RETR lưu vào downloads/)"),
        ("f_3_4", "3.4 Giám sát tiến độ truyền tệp<br/>(Thanh Progress Bar thời gian thực)"),
        ("f_3_5", "3.5 Kiểm tra toàn vẹn Checksum<br/>(Xác thực mã băm SHA-256)"),
        ("f_3_6", "3.6 Phục hồi &amp; Dọn dẹp lỗi mạng<br/>(Tự động xóa tệp tạm .tmp/.part)")
    ]
    cur_y = 220
    for sid, stext in g3_subs:
        h = 42 if "<br/>" in stext else 36
        add_box(sid, stext, 482, cur_y, 205, h, level=2)
        add_connector(f"e_g3_{sid}", "g3", sid)
        cur_y += h + 18

    # Group 4 (x = 703)
    g4_subs = [
        ("f_4_1", "4.1 Tạo liên kết chia sẻ tệp<br/>(Sinh mã Token public ngẫu nhiên)"),
        ("f_4_2", "4.2 Thiết lập quyền truy cập chia sẻ<br/>(Quyền VIEW chỉ xem / DOWNLOAD)"),
        ("f_4_3", "4.3 Tải tệp chia sẻ công khai<br/>(Lệnh GETSHARED qua Token)"),
        ("f_4_4", "4.4 Quản lý &amp; Thu hồi liên kết<br/>(Xóa token hoặc hủy hiệu lực)"),
        ("f_4_5", "4.5 Xem danh mục gói cước Quota<br/>(Gói Free 50MB, VIP1 1GB, VIP2 5GB)"),
        ("f_4_6", "4.6 Đăng ký &amp; Thanh toán mô phỏng<br/>(Cập nhật tự động hạn mức Quota)")
    ]
    cur_y = 220
    for sid, stext in g4_subs:
        h = 42 if "<br/>" in stext else 36
        add_box(sid, stext, 703, cur_y, 205, h, level=2)
        add_connector(f"e_g4_{sid}", "g4", sid)
        cur_y += h + 18

    # Group 5 (x = 924)
    g5_subs = [
        ("f_5_1", "5.1 Quản trị tài khoản người dùng<br/>(Xem danh sách, tìm kiếm user)"),
        ("f_5_2", "5.2 Khóa / Mở khóa tài khoản<br/>(Chặn đăng nhập tài khoản vi phạm)"),
        ("f_5_3", "5.3 Phân quyền vai trò &amp; Quản trị viên<br/>(Phân bổ quyền USER / ADMIN)"),
        ("f_5_4", "5.4 Điều chỉnh hạn mức Quota thủ công<br/>(Cấp thêm dung lượng lưu trữ)"),
        ("f_5_5", "5.5 Quản lý đơn hàng &amp; Giao dịch<br/>(Duyệt và kiểm tra biên lai đơn hàng)"),
        ("f_5_6", "5.6 Kiểm toán nhật ký hệ thống<br/>(Truy vấn bảng transfer_logs)")
    ]
    cur_y = 220
    for sid, stext in g5_subs:
        h = 42 if "<br/>" in stext else 36
        add_box(sid, stext, 924, cur_y, 205, h, level=2)
        add_connector(f"e_g5_{sid}", "g5", sid)
        cur_y += h + 18

    # Format pretty XML
    xml_str = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    
    os.makedirs("docs", exist_ok=True)
    with open("docs/functional_decomposition_diagram.drawio", "w", encoding="utf-8") as f:
        f.write(xml_str)
    with open("docs/functional_decomposition_diagram.xml", "w", encoding="utf-8") as f:
        f.write(xml_str)
    print("Generated docs/functional_decomposition_diagram.drawio & .xml successfully!")

if __name__ == "__main__":
    create_fdd_xml()

