import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def create_retr_sequence_xml():
    root = ET.Element("mxfile", host="app.diagrams.net", modified="2026-11-02T15:00:00.000Z", agent="SecureFTPClientPro", version="24.0.0", type="device")
    diagram = ET.SubElement(root, "diagram", id="Sequence_Diagram_RETR", name="Sơ đồ Tuần tự (Sequence Diagram) - Chức năng Tải tệp (RETR)")
    model = ET.SubElement(diagram, "mxGraphModel", dx="1200", dy="800", grid="1", gridSize="10", guides="1", tooltips="1", connect="1", arrows="1", fold="1", page="1", pageScale="1", pageWidth="1169", pageHeight="827", background="#FFFFFF", math="0", shadow="0")
    root_cell = ET.SubElement(model, "root")
    
    ET.SubElement(root_cell, "mxCell", id="0")
    ET.SubElement(root_cell, "mxCell", id="1", parent="0")

    # 1. 5 Lifelines Header Boxes
    lifelines = [
        ("p_client", "&laquo;boundary&raquo;<br><b>Client</b><br>(FTP Client Pro)", 40, 55, 150, 48, 115),
        ("p_server", "&laquo;control&raquo;<br><b>FTP Server</b><br>(TCPServer / Handler)", 280, 55, 150, 48, 355),
        ("p_auth", "&laquo;service&raquo;<br><b>Auth Service</b><br>(Xác thực & Phân quyền)", 520, 55, 150, 48, 595),
        ("p_storage", "&laquo;subsystem&raquo;<br><b>File Storage</b><br>(Storage Sandbox)", 760, 55, 150, 48, 835),
        ("p_db", "&laquo;entity&raquo;<br><b>Database</b><br>(MySQL Database)", 980, 55, 150, 48, 1055),
    ]

    for pid, val, x, y, w, h, cx in lifelines:
        box = ET.SubElement(root_cell, "mxCell", id=pid, value=val, 
                            style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.8;strokeColor=#000000;fillColor=#FFFFFF;fontFamily=Segoe UI;fontSize=11;fontStyle=1;align=center;verticalAlign=middle;", 
                            vertex="1", parent="1")
        ET.SubElement(box, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        
        # Dashed Lifeline
        line = ET.SubElement(root_cell, "mxCell", id=f"line_{cx}", value="", 
                             style="endArrow=none;dashed=1;html=1;strokeWidth=1.5;strokeColor=#666666;", 
                             edge="1", parent="1")
        geo = ET.SubElement(line, "mxGeometry", relative="1", **{"as": "geometry"})
        ET.SubElement(geo, "mxPoint", x=str(cx), y="105", **{"as": "sourcePoint"})
        ET.SubElement(geo, "mxPoint", x=str(cx), y="780", **{"as": "targetPoint"})

    # 2. Activation Bars
    activations = [
        ("act_client", 108, 130, 14, 635),
        ("act_server", 348, 130, 14, 635),
        ("act_auth", 588, 180, 14, 215),
        ("act_db_read", 1048, 415, 14, 30),
        ("act_storage", 828, 455, 14, 145),
        ("act_db_log", 1048, 665, 14, 75),
    ]
    for aid, ax, ay, aw, ah in activations:
        act = ET.SubElement(root_cell, "mxCell", id=aid, value="", 
                            style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.5;strokeColor=#000000;fillColor=#FFFFFF;", 
                            vertex="1", parent="1")
        ET.SubElement(act, "mxGeometry", x=str(ax), y=str(ay), width=str(aw), height=str(ah), **{"as": "geometry"})

    # Helper function for messages
    def add_msg(mid, val, sx, sy, tx, ty, dashed=False, self_msg=False):
        style = "html=1;verticalAlign=bottom;strokeWidth=1.6;strokeColor=#000000;fontFamily=Segoe UI;fontSize=10;fontStyle=1;"
        if dashed:
            style += "endArrow=open;dashed=1;"
        elif self_msg:
            style += "endArrow=block;endFill=1;"
        else:
            style += "endArrow=block;endFill=1;"
            
        edge = ET.SubElement(root_cell, "mxCell", id=mid, value=val, style=style, edge="1", parent="1")
        geo = ET.SubElement(edge, "mxGeometry", relative="1", **{"as": "geometry"})
        ET.SubElement(geo, "mxPoint", x=str(sx), y=str(sy), **{"as": "sourcePoint"})
        ET.SubElement(geo, "mxPoint", x=str(tx), y=str(ty), **{"as": "targetPoint"})
        return edge

    # Helper for alt frame
    def add_alt_frame(fid, title, fx, fy, fw, fh):
        frame = ET.SubElement(root_cell, "mxCell", id=fid, value=title, 
                              style="shape=umlFrame;whiteSpace=wrap;html=1;strokeWidth=1.8;strokeColor=#000000;fillColor=none;fontFamily=Segoe UI;fontSize=11;fontStyle=1;align=left;verticalAlign=top;spacingLeft=10;spacingTop=6;", 
                              vertex="1", parent="1")
        ET.SubElement(frame, "mxGeometry", x=str(fx), y=str(fy), width=str(fw), height=str(fh), **{"as": "geometry"})
        return frame

    def add_label(lid, text, lx, ly, lw=250, lh=18):
        lbl = ET.SubElement(root_cell, "mxCell", id=lid, value=text, 
                            style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontFamily=Segoe UI;fontSize=10;fontStyle=1;", 
                            vertex="1", parent="1")
        ET.SubElement(lbl, "mxGeometry", x=str(lx), y=str(ly), width=str(lw), height=str(lh), **{"as": "geometry"})
        return lbl

    def add_divider(did, sx, sy, tx, ty):
        div = ET.SubElement(root_cell, "mxCell", id=did, value="", 
                            style="strokeWidth=1.5;dashed=1;strokeColor=#000000;html=1;", edge="1", parent="1")
        geo = ET.SubElement(div, "mxGeometry", relative="1", **{"as": "geometry"})
        ET.SubElement(geo, "mxPoint", x=str(sx), y=str(sy), **{"as": "sourcePoint"})
        ET.SubElement(geo, "mxPoint", x=str(tx), y=str(ty), **{"as": "targetPoint"})
        return div

    # Message 1: Connect
    add_msg("msg_1", "1: Kết nối TCP Socket (Port 6789)", 122, 135, 348, 135)
    
    # Message 2: Auth info
    add_msg("msg_2", "2: Gửi thông tin xác thực (USER &amp; PASS)", 122, 160, 348, 160)
    
    # Message 3: Server checks auth
    add_msg("msg_3", "3: Kiểm tra đăng nhập: authenticate(user, pass)", 362, 185, 588, 185)

    # Frame Alt 1: Auth
    add_alt_frame("frame_alt1", "alt [XÁC THỰC TÀI KHOẢN]", 55, 202, 600, 70)
    add_label("lbl_alt1_fail", "[Xác thực thất bại]", 135, 206, 150)
    add_msg("msg_4a", "4a: Phản hồi lỗi xác thực (530 Login incorrect)", 588, 222, 362, 222, dashed=True)
    add_msg("msg_4a_cli", "Thông báo lỗi xác thực &amp; đóng kết nối", 348, 222, 122, 222, dashed=True)
    add_divider("div_alt1", 55, 237, 655, 237)
    add_label("lbl_alt1_ok", "[Xác thực thành công]", 135, 240, 150)
    add_msg("msg_4b", "4b: Xác thực thành công (230 User logged in)", 588, 256, 362, 256, dashed=True)
    add_msg("msg_4b_cli", "Khởi tạo phiên làm việc thành công", 348, 256, 122, 256, dashed=True)

    # Message 5: RETR
    add_msg("msg_5", "5: Gửi yêu cầu tải tệp: RETR &lt;filename&gt;", 122, 287, 348, 287)

    # Message 6: Check Permission
    add_msg("msg_6", "6: Kiểm tra quyền truy xuất tệp: checkPermission(userId, filename, READ)", 362, 312, 588, 312)

    # Frame Alt 2: Permission
    add_alt_frame("frame_alt2", "alt [KIỂM TRA QUYỀN TRUY XUẤT TỆP]", 55, 330, 600, 70)
    add_label("lbl_alt2_denied", "[Không đủ quyền truy xuất]", 135, 334, 200)
    add_msg("msg_7a", "7a: Từ chối: 550 Permission denied (Không có quyền đọc)", 588, 350, 362, 350, dashed=True)
    add_msg("msg_7a_cli", "Thông báo lỗi: Quyền truy xuất tệp bị từ chối", 348, 350, 122, 350, dashed=True)
    add_divider("div_alt2", 55, 365, 655, 365)
    add_label("lbl_alt2_ok", "[Đủ quyền truy xuất]", 135, 368, 200)
    add_msg("msg_7b", "7b: Quyền hợp lệ: checkPermission = OK", 588, 384, 362, 384, dashed=True)

    # Message 8: Database metadata query
    add_msg("msg_8", "8: Truy vấn metadata tệp: SELECT file_id, file_size, checksum FROM files WHERE ...", 362, 415, 1048, 415)
    add_msg("msg_8_res", "Trả về thông tin metadata tệp tin (size, checksum, status)", 1048, 435, 362, 435, dashed=True)

    # Message 9: File storage query
    add_msg("msg_9", "9: Truy xuất tệp từ ổ đĩa: resolveSafe(userHome, filename)", 362, 458, 828, 458)

    # Frame Alt 3: File existence & transfer
    add_alt_frame("frame_alt3", "alt [KIỂM TRA TỒN TẠI VÀ TRUYỀN DỮ LIỆU TỆP TIN]", 55, 478, 1030, 287)
    add_label("lbl_alt3_notfound", "[Tệp không tồn tại / Không thể đọc]", 135, 482, 300)
    add_msg("msg_10a", "10a: File not found / Cannot read file (file.exists() == false)", 828, 498, 362, 498, dashed=True)
    add_msg("msg_10a_cli", "10: Phản hồi lỗi: 550 File not found hoặc Cannot read file", 348, 498, 122, 498, dashed=True)
    
    add_divider("div_alt3", 55, 514, 1085, 514)
    add_label("lbl_alt3_found", "[Tệp tồn tại &amp; Sẵn sàng truyền]", 135, 518, 250)
    add_msg("msg_10b", "10b: Tệp tồn tại, cung cấp File Handle &amp; Kích thước byte", 828, 532, 362, 532, dashed=True)
    add_msg("msg_11_ready", "11: Phản hồi sẵn sàng: 150 Opening binary mode (SIZE &lt;fileSize&gt;)", 348, 552, 122, 552, dashed=True)
    
    # Data stream: File Storage -> Server -> Client
    add_msg("msg_11_read", "Đọc luồng nhị phân từ đĩa (FileInputStream buffer 4096B)", 828, 576, 362, 576, dashed=False)
    add_msg("msg_11_send", "Truyền luồng dữ liệu nhị phân về Client (Binary Socket Stream)", 348, 576, 122, 576, dashed=False)
    
    # Client writes file
    add_msg("msg_12", "12: Nhận bytes &amp; ghi vào FileOutputStream tại downloads/ (Cập nhật Progress)", 122, 602, 220, 602, self_msg=True)

    # Frame Alt 4: Transfer result (Success / Failed)
    add_alt_frame("frame_alt4", "alt [KẾT QUẢ TẢI TỆP TIN]", 85, 625, 985, 130)
    add_label("lbl_alt4_ok", "[Tải tệp thành công &amp; Đầy đủ dữ liệu]", 155, 630, 260)
    add_msg("msg_13a", "13a: Phản hồi hoàn tất: 226 Transfer complete (kèm SHA256)", 348, 648, 122, 648, dashed=True)
    add_msg("msg_14a", "14a: Nhận kết quả thành công, đóng file &amp; thông báo hoàn tất", 122, 668, 230, 668, self_msg=True)
    add_msg("msg_log_ok", "Ghi nhật ký audit: INSERT transfer_logs (RETR, SUCCESS)", 362, 668, 1048, 668, dashed=False)

    add_divider("div_alt4", 85, 690, 1070, 690)
    add_label("lbl_alt4_fail", "[Lỗi mạng / Gián đoạn kết nối Socket]", 155, 695, 260)
    add_msg("msg_13b", "13b: Phản hồi lỗi: 426 Connection closed; transfer aborted", 348, 715, 122, 715, dashed=True)
    add_msg("msg_14b", "14b: Xóa tệp tải dở dang (.part) &amp; hiển thị cảnh báo lỗi", 122, 735, 230, 735, self_msg=True)
    add_msg("msg_log_fail", "Ghi nhật ký audit: INSERT transfer_logs (RETR, FAILED)", 362, 735, 1048, 735, dashed=False)

    # Format pretty XML
    xml_str = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    
    # Save files
    os.makedirs("docs", exist_ok=True)
    with open("docs/sequence_diagram_retr.drawio", "w", encoding="utf-8") as f:
        f.write(xml_str)
    with open("docs/sequence_diagram_retr.xml", "w", encoding="utf-8") as f:
        f.write(xml_str)
    print("Generated docs/sequence_diagram_retr.drawio & .xml successfully!")

if __name__ == "__main__":
    create_retr_sequence_xml()

