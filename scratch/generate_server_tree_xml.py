import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def create_server_tree_xml():
    root = ET.Element("mxfile", host="app.diagrams.net", modified="2026-11-02T16:00:00.000Z", agent="SecureFTPClientPro", version="24.0.0", type="device")
    diagram = ET.SubElement(root, "diagram", id="Server_Directory_Tree", name="Sơ đồ Cây Thư mục Server (Server Directory Tree)")
    model = ET.SubElement(diagram, "mxGraphModel", dx="1200", dy="800", grid="1", gridSize="10", guides="1", tooltips="1", connect="1", arrows="1", fold="1", page="1", pageScale="1", pageWidth="1169", pageHeight="827", background="#FFFFFF", math="0", shadow="0")
    root_cell = ET.SubElement(model, "root")
    
    ET.SubElement(root_cell, "mxCell", id="0")
    ET.SubElement(root_cell, "mxCell", id="1", parent="0")

    # Helper: Folder node
    def add_folder(fid, name, desc, x, y, w, h=40, parent_id="1"):
        label = f"<b>{name}</b>"
        if desc:
            label += f"<br/><font style='font-size:9px;color:#555555;'>{desc}</font>"
        cell = ET.SubElement(root_cell, "mxCell", id=fid, value=label, 
                             style="shape=folder;fontStyle=0;tabWidth=60;tabHeight=14;tabPosition=left;html=1;whiteSpace=wrap;strokeWidth=1.5;strokeColor=#000000;fillColor=#F5F5F5;align=center;verticalAlign=middle;fontFamily=Segoe UI;fontSize=10;spacingTop=4;", 
                             vertex="1", parent=parent_id)
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # Helper: File node
    def add_file(fid, name, desc, x, y, w, h=30, parent_id="1"):
        label = f"<b>{name}</b>"
        if desc:
            label += f" &nbsp;<font style='font-size:9px;color:#555555;'>({desc})</font>"
        cell = ET.SubElement(root_cell, "mxCell", id=fid, value=label, 
                             style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.4;strokeColor=#000000;fillColor=#FFFFFF;align=left;spacingLeft=10;verticalAlign=middle;fontFamily=Segoe UI;fontSize=10;", 
                             vertex="1", parent=parent_id)
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # Helper: Group file container box
    def add_file_box(bid, title, files, x, y, w, h, parent_id="1"):
        content = f"<table style='width:100%;border-collapse:collapse;font-family:Segoe UI;font-size:10px;text-align:left;'>"
        content += f"<tr><td style='padding:3px 6px;border-bottom:1.5px solid #000000;background:#EAEAEA;'><b>{title}</b></td></tr>"
        for f, d in files:
            content += f"<tr><td style='padding:2px 6px;border-bottom:0.5px solid #DDDDDD;'>📄 <b>{f}</b> <font color='#666666' style='font-size:9px;'>— {d}</font></td></tr>"
        content += "</table>"
        cell = ET.SubElement(root_cell, "mxCell", id=bid, value=content, 
                             style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.5;strokeColor=#000000;fillColor=#FFFFFF;verticalAlign=top;spacing=0;align=left;overflow=fill;", 
                             vertex="1", parent=parent_id)
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # Helper: Tree connector
    def add_tree_edge(eid, sid, tid):
        style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#000000;strokeWidth=1.4;endArrow=none;"
        edge = ET.SubElement(root_cell, "mxCell", id=eid, value="", style=style, edge="1", parent="1", source=sid, target=tid)
        ET.SubElement(edge, "mxGeometry", relative="1", **{"as": "geometry"})
        return edge

    # 1. Project Root
    add_folder("root_dir", "giuaky_Lap_Trinh_Mang/", "Thư mục gốc hệ thống Backend/Server", 30, 360, 200, 50)

    # 2. Main Level 1 Folders
    add_folder("dir_src_server", "src/server/", "Mã nguồn xử lý Backend Server", 280, 180, 180, 44)
    add_folder("dir_config", "config/", "Tệp cấu hình kết nối CSDL", 280, 500, 180, 40)
    add_folder("dir_sql", "sql/", "Kịch bản DDL Cơ sở dữ liệu", 280, 570, 180, 40)
    add_folder("dir_storage", "storage/", "Thư mục lưu trữ vật lý (Sandbox)", 280, 640, 180, 40)
    add_folder("dir_test", "test/", "Bộ kiểm thử tự động 7 Phase", 280, 710, 180, 40)

    # Connect Root to Level 1
    add_tree_edge("e_r_server", "root_dir", "dir_src_server")
    add_tree_edge("e_r_config", "root_dir", "dir_config")
    add_tree_edge("e_r_sql", "root_dir", "dir_sql")
    add_tree_edge("e_r_storage", "root_dir", "dir_storage")
    add_tree_edge("e_r_test", "root_dir", "dir_test")

    # 3. Sub-packages in src/server/
    # 3.1 Core Server Engine Files
    core_files = [
        ("TCPServer.java", "Entry point, mở ServerSocket Port 6789, cấp phát ThreadPool"),
        ("ClientHandler.java", "Thực thi Runnable, quản lý socket kết nối từng Client"),
        ("CommandHandler.java", "Router định tuyến xử lý lệnh FTP (USER, PASS, STOR, RETR...)"),
        ("ProtocolInputStream.java", "Đọc luồng byte-by-byte, giải quyết triệt để lỗi Buffer-Ahead"),
        ("Session.java", "Quản lý phiên: trạng thái xác thực, thư mục hiện tại"),
        ("StorageManager.java", "Xử lý file vật lý, chuẩn hóa đường dẫn chống Path Traversal"),
        ("UserDatabase.java", "Xác thực tài khoản người dùng In-Memory / CSDL")
    ]
    add_file_box("box_core", "src/server/ (Core FTP Engine)", core_files, 510, 40, 340, 175)
    add_tree_edge("e_server_core", "dir_src_server", "box_core")

    # 3.2 Sub-package: dao/
    dao_files = [
        ("UserDAO.java", "Truy vấn & cập nhật bảng users"),
        ("FileDAO.java", "Quản lý metadata bảng files & Checksum SHA-256"),
        ("LogDAO.java", "Ghi nhật ký lịch sử truyền tệp transfer_logs"),
        ("ShareDAO.java", "Quản lý liên kết chia sẻ tệp shared_files"),
        ("PlanDAO.java", "Quản lý gói cước & hạn mức Quota plans"),
        ("OrderDAO.java", "Xử lý đơn hàng nâng cấp gói orders"),
        ("PaymentDAO.java", "Xử lý giao dịch thanh toán payments"),
        ("SubscriptionDAO.java", "Quản lý gói dịch vụ người dùng subscriptions")
    ]
    add_file_box("box_dao", "src/server/dao/ (Data Access Objects)", dao_files, 510, 230, 340, 195)
    add_tree_edge("e_server_dao", "dir_src_server", "box_dao")

    # 3.3 Sub-package: db/
    db_files = [
        ("DBConnection.java", "Quản lý kết nối JDBC Connection Pool HikariCP"),
        ("DatabaseConfig.java", "Đọc thông số cấu hình từ config/db.properties"),
        ("DataReconciler.java", "Đối soát & đồng bộ dữ liệu đĩa với CSDL MySQL")
    ]
    add_file_box("box_db", "src/server/db/ (Database Management)", db_files, 880, 40, 260, 95)
    add_tree_edge("e_server_db", "dir_src_server", "box_db")

    # 3.4 Sub-package: model/
    model_files = [
        ("User.java", "Entity người dùng & hạn mức lưu trữ"),
        ("FileMetadata.java", "Entity tệp tin, kích thước, mã băm"),
        ("TransferLog.java", "Entity nhật ký truyền tải tệp tin"),
        ("SharedFile.java", "Entity liên kết chia sẻ tệp công khai"),
        ("Plan.java", "Entity gói lưu trữ dung lượng"),
        ("Order.java", "Entity đơn đặt hàng nâng cấp"),
        ("Payment.java", "Entity biên lai thanh toán"),
        ("Subscription.java", "Entity thời hạn gói lưu trữ")
    ]
    add_file_box("box_model", "src/server/model/ (Domain Entities)", model_files, 880, 150, 260, 195)
    add_tree_edge("e_server_model", "dir_src_server", "box_model")

    # 3.5 Sub-package: util/
    util_files = [
        ("SecurityUtil.java", "Băm SHA-256, BCrypt password, sinh token")
    ]
    add_file_box("box_util", "src/server/util/ (Security & Utils)", util_files, 880, 360, 260, 55)
    add_tree_edge("e_server_util", "dir_src_server", "box_util")

    # 4. Details of config/, sql/, storage/, test/
    # 4.1 config/
    cfg_files = [
        ("db.properties", "Thông số kết nối MySQL: host, port, user, pass"),
        ("db.properties.example", "Tệp mẫu tham chiếu môi trường")
    ]
    add_file_box("box_config_files", "config/ (Configuration)", cfg_files, 510, 485, 340, 68)
    add_tree_edge("e_cfg_files", "dir_config", "box_config_files")

    # 4.2 sql/
    sql_files = [
        ("schema.sql", "DDL tạo 8 bảng CSDL: users, files, transfer_logs...")
    ]
    add_file_box("box_sql_files", "sql/ (Database DDL Scripts)", sql_files, 510, 565, 340, 50)
    add_tree_edge("e_sql_files", "dir_sql", "box_sql_files")

    # 4.3 storage/
    storage_files = [
        ("storage/<username>/", "Thư mục Sandbox phân quyền riêng cho từng user"),
        (".upload.tmp", "Tệp tạm trong quá trình ghi dữ liệu luồng STOR")
    ]
    add_file_box("box_storage_files", "storage/ (Storage Sandbox)", storage_files, 510, 630, 340, 68)
    add_tree_edge("e_storage_files", "dir_storage", "box_storage_files")

    # 4.4 test/
    test_files = [
        ("TestPhase1Database.java", "Kiểm thử kết nối CSDL MySQL"),
        ("TestPhase2AuthAndSandbox.java", "Kiểm thử đăng nhập & Sandbox"),
        ("TestPhase3StorageAndQuota.java", "Kiểm thử hạn mức Quota lưu trữ"),
        ("TestPhase4SharingAndHistory.java", "Kiểm thử chia sẻ link & log"),
        ("TestPhase5PlansAndPayment.java", "Kiểm thử gói cước & thanh toán"),
        ("TestPhase6AdminDashboard.java", "Kiểm thử quyền quản trị Admin"),
        ("TestPhase7SystemReliabilitySecurity.java", "Kiểm thử an toàn & tin cậy")
    ]
    add_file_box("box_test_files", "test/ (Automated Test Suite)", test_files, 510, 710, 340, 105)
    add_tree_edge("e_test_files", "dir_test", "box_test_files")

    # Format pretty XML
    xml_str = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    
    os.makedirs("docs", exist_ok=True)
    with open("docs/server_directory_tree.drawio", "w", encoding="utf-8") as f:
        f.write(xml_str)
    with open("docs/server_directory_tree.xml", "w", encoding="utf-8") as f:
        f.write(xml_str)
    print("Generated docs/server_directory_tree.drawio & .xml successfully!")

if __name__ == "__main__":
    create_server_tree_xml()

