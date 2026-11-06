import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def create_class_diagram_xml():
    root = ET.Element("mxfile", host="app.diagrams.net", modified="2026-11-02T15:45:00.000Z", agent="SecureFTPClientPro", version="24.0.0", type="device")
    diagram = ET.SubElement(root, "diagram", id="Class_Diagram_System", name="Sơ đồ Lớp (Class Diagram) - Hệ thống Secure FTP Client Pro")
    model = ET.SubElement(diagram, "mxGraphModel", dx="1200", dy="800", grid="1", gridSize="10", guides="1", tooltips="1", connect="1", arrows="1", fold="1", page="1", pageScale="1", pageWidth="1169", pageHeight="827", background="#FFFFFF", math="0", shadow="0")
    root_cell = ET.SubElement(model, "root")
    
    ET.SubElement(root_cell, "mxCell", id="0")
    ET.SubElement(root_cell, "mxCell", id="1", parent="0")

    # Package Server Box (Background Frame)
    pkg_server = ET.SubElement(root_cell, "mxCell", id="pkg_server", value="Package server (Máy chủ FTP)", 
                               style="shape=folder;fontStyle=1;spacingTop=10;tabWidth=190;tabHeight=26;tabPosition=left;html=1;whiteSpace=wrap;strokeWidth=1.5;strokeColor=#444444;fillColor=#FFFFFF;align=center;verticalAlign=top;fontFamily=Segoe UI;fontSize=12;", 
                               vertex="1", parent="1")
    ET.SubElement(pkg_server, "mxGeometry", x="30", y="50", width="670", height="730", **{"as": "geometry"})

    # Package Client Box (Background Frame)
    pkg_client = ET.SubElement(root_cell, "mxCell", id="pkg_client", value="Package client (Máy trạm FTP)", 
                               style="shape=folder;fontStyle=1;spacingTop=10;tabWidth=190;tabHeight=26;tabPosition=left;html=1;whiteSpace=wrap;strokeWidth=1.5;strokeColor=#444444;fillColor=#FFFFFF;align=center;verticalAlign=top;fontFamily=Segoe UI;fontSize=12;", 
                               vertex="1", parent="1")
    ET.SubElement(pkg_client, "mxGeometry", x="720", y="50", width="420", height="730", **{"as": "geometry"})

    # Helper function to create standard 3-compartment UML Class
    def add_uml_class(cid, title, attributes, methods, x, y, w, h):
        attr_str = "<hr size='1'/>".join(attributes)
        meth_str = "<hr size='1'/>".join(methods)
        
        # HTML label representation for UML class
        val = f"<b>{title}</b><hr size='1.5'/>"
        if attributes:
            val += "<p style='margin:0px;margin-left:4px;text-align:left;'>" + "<br/>".join(attributes) + "</p>"
        else:
            val += "<p style='margin:0px;margin-left:4px;text-align:left;color:#888888;'><i>(không có)</i></p>"
        val += "<hr size='1.5'/>"
        if methods:
            val += "<p style='margin:0px;margin-left:4px;text-align:left;'>" + "<br/>".join(methods) + "</p>"
        else:
            val += "<p style='margin:0px;margin-left:4px;text-align:left;color:#888888;'><i>(không có)</i></p>"
            
        cell = ET.SubElement(root_cell, "mxCell", id=cid, value=val, 
                             style="swimlane;fontStyle=0;childLayout=stackLayout;horizontal=1;startSize=26;fillColor=#FFFFFF;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;strokeColor=#000000;strokeWidth=1.5;align=center;fontFamily=Segoe UI;fontSize=10;html=1;whiteSpace=wrap;", 
                             vertex="1", parent="1")
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # Helper for simple UML class box
    def add_class_box(cid, name, attrs, meths, x, y, w, h, is_interface=False):
        header = f"&laquo;interface&raquo;<br/><b>{name}</b>" if is_interface else f"<b>{name}</b>"
        body_parts = [header]
        
        attr_text = "<br/>".join(attrs) if attrs else "<i>(none)</i>"
        meth_text = "<br/>".join(meths) if meths else "<i>(none)</i>"
        
        html_val = f"<table style='width:100%;height:100%;border-collapse:collapse;font-family:Segoe UI;font-size:10px;text-align:left;'>"
        html_val += f"<tr><td style='text-align:center;padding:4px;border-bottom:1.5px solid black;background:#F9F9F9;'>{header}</td></tr>"
        html_val += f"<tr><td style='padding:5px;border-bottom:1.5px solid black;vertical-align:top;'>{attr_text}</td></tr>"
        html_val += f"<tr><td style='padding:5px;vertical-align:top;'>{meth_text}</td></tr>"
        html_val += f"</table>"
        
        cell = ET.SubElement(root_cell, "mxCell", id=cid, value=html_val, 
                             style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=1.6;strokeColor=#000000;fillColor=#FFFFFF;verticalAlign=top;spacing=0;align=left;overflow=fill;", 
                             vertex="1", parent="1")
        ET.SubElement(cell, "mxGeometry", x=str(x), y=str(y), width=str(w), height=str(h), **{"as": "geometry"})
        return cell

    # --- 1. SERVER CLASSES ---
    # TCPServer
    add_class_box("c_tcpserver", "TCPServer", 
                  ["- PORT : int = 6789", 
                   "- clientCount : AtomicInteger", 
                   "- threadPool : ExecutorService"],
                  ["+ main(args : String[]) : void", 
                   "+ startServer() : void"],
                  50, 90, 190, 115)

    # ClientHandler
    add_class_box("c_clienthandler", "ClientHandler", 
                  ["- clientSocket : Socket", 
                   "- handler : CommandHandler", 
                   "- clientId : String",
                   "- lineReader : ProtocolInputStream"],
                  ["+ run() : void", 
                   "- closeConnection() : void"],
                  50, 260, 190, 140)

    # ProtocolInputStream
    add_class_box("c_protocolstream", "ProtocolInputStream", 
                  ["- in : InputStream"],
                  ["+ readLine() : String", 
                   "+ getRawInputStream() : InputStream"],
                  50, 460, 190, 105)

    # CommandHandler
    add_class_box("c_commandhandler", "CommandHandler", 
                  ["- session : Session", 
                   "- userDatabase : UserDatabase", 
                   "- storageManager : StorageManager"],
                  ["+ handle(rawInput : String) : String", 
                   "+ handleStor(f, in, out) : String", 
                   "+ handleRetr(f, out, rawOut) : String",
                   "- handleList() : String",
                   "- handleDele(filename : String) : String",
                   "- handleRnfr(oldName : String) : String",
                   "- handleRnto(newName : String) : String"],
                  270, 220, 205, 220)

    # Session
    add_class_box("c_session", "Session", 
                  ["- state : State", 
                   "- pendingUsername : String", 
                   "- loggedInUser : String",
                   "- currentDirectory : String",
                   "- renameFrom : String"],
                  ["+ isAuthenticated() : boolean", 
                   "+ loginSuccess(user : String) : void", 
                   "+ reset() : void",
                   "+ setRenameFrom(name : String) : void",
                   "+ clearRenameFrom() : void"],
                  500, 90, 185, 175)

    # UserDatabase
    add_class_box("c_userdb", "UserDatabase", 
                  ["- users : Map&lt;String, String&gt;"],
                  ["+ validate(u : String, p : String) : boolean", 
                   "+ userExists(u : String) : boolean"],
                  500, 310, 185, 115)

    # StorageManager
    add_class_box("c_storagemanager", "StorageManager", 
                  ["- storageRoot : Path", 
                   "- DATE_FORMAT : SimpleDateFormat"],
                  ["- resolveSafe(cDir, path) : Path", 
                   "+ listDirectory(home, cDir) : String", 
                   "+ receiveFile(home, cDir, ...) : String",
                   "+ sendFile(home, cDir, ...) : String",
                   "+ deleteFile(home, cDir, f) : String",
                   "+ renameFile(home, cDir, o, n) : String"],
                  500, 470, 185, 195)

    # --- 2. CLIENT CLASSES ---
    # LoginFrame
    add_class_box("c_loginframe", "LoginFrame", 
                  ["- tfServer, tfPort : JTextField", 
                   "- tfUsername : JTextField", 
                   "- pfPassword : JPasswordField",
                   "- ftpService : FtpClientService"],
                  ["- doConnect() : void", 
                   "- doLogin() : void", 
                   "- applyTheme(theme : String) : void"],
                  740, 90, 180, 155)

    # MainFrame
    add_class_box("c_mainframe", "MainFrame", 
                  ["- fileTable : JTable", 
                   "- progressBar : JProgressBar", 
                   "- logArea : JTextArea",
                   "- ftpService : FtpClientService"],
                  ["- doUpload() : void", 
                   "- doDownload() : void", 
                   "- refreshFiles() : void",
                   "+ setProgress(pct, msg) : void"],
                  740, 320, 180, 175)

    # FtpClientService
    add_class_box("c_ftpservice", "FtpClientService", 
                  ["- socket : Socket", 
                   "- socketIn : InputStream", 
                   "- writer : PrintWriter",
                   "- connected : boolean",
                   "- loggedIn : boolean"],
                  ["+ connect(host, port) : boolean", 
                   "+ login(u, p) : String", 
                   "+ list() : List&lt;FileEntry&gt;",
                   "+ stor(file, listener) : String",
                   "+ retr(file, dir, listener) : String",
                   "+ dele(filename) : String",
                   "+ rename(oldName, newName) : String"],
                  945, 230, 180, 215)

    # ProgressListener (Interface)
    add_class_box("c_listener", "ProgressListener", 
                  [],
                  ["+ onProgress(percent : int,<br/>  transferred : long,<br/>  totalBytes : long) : void"],
                  945, 90, 180, 105, is_interface=True)

    # TCPClient (Console Client)
    add_class_box("c_tcpclient", "TCPClient", 
                  ["- DEFAULT_HOST : String", 
                   "- DEFAULT_PORT : int = 6789"],
                  ["+ main(args : String[]) : void", 
                   "- handleDownload(f, ...) : void", 
                   "- handleUpload(f, ...) : void"],
                  740, 560, 180, 130)

    # Helper function for Relationships
    def add_rel(rid, sid, tid, rel_type, label="", mult_s="", mult_t="", exit_side=None, entry_side=None):
        style = "html=1;strokeColor=#000000;strokeWidth=1.5;fontFamily=Segoe UI;fontSize=10;"
        if rel_type == "composition": # Solid diamond at source
            style += "startArrow=diamond;startFill=1;endArrow=none;startSize=8;"
        elif rel_type == "aggregation": # Open diamond at source
            style += "startArrow=diamond;startFill=0;endArrow=none;startSize=8;"
        elif rel_type == "association": # Normal arrow
            style += "endArrow=open;endSize=6;"
        elif rel_type == "dependency": # Dashed arrow
            style += "dashed=1;endArrow=open;endSize=6;"
        elif rel_type == "realization": # Dashed with open triangle
            style += "dashed=1;endArrow=block;endFill=0;endSize=8;"
        elif rel_type == "generalization": # Solid with open triangle
            style += "endArrow=block;endFill=0;endSize=8;"

        edge = ET.SubElement(root_cell, "mxCell", id=rid, value=label, style=style, edge="1", parent="1", source=sid, target=tid)
        geo = ET.SubElement(edge, "mxGeometry", relative="1", **{"as": "geometry"})
        
        # Multiplicity labels
        if mult_s:
            lbl_s = ET.SubElement(root_cell, "mxCell", id=f"{rid}_ms", value=mult_s, 
                                  style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=10;fontStyle=1;", 
                                  vertex="1", connectable="0", parent=rid)
            ET.SubElement(lbl_s, "mxGeometry", x="-0.8", relative="1", **{"as": "geometry"})
        if mult_t:
            lbl_t = ET.SubElement(root_cell, "mxCell", id=f"{rid}_mt", value=mult_t, 
                                  style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontSize=10;fontStyle=1;", 
                                  vertex="1", connectable="0", parent=rid)
            ET.SubElement(lbl_t, "mxGeometry", x="0.8", relative="1", **{"as": "geometry"})
        return edge

    # Server relationships
    add_rel("rel_1", "c_tcpserver", "c_clienthandler", "composition", "", "1", "0..*")
    add_rel("rel_2", "c_clienthandler", "c_commandhandler", "composition", "", "1", "1")
    add_rel("rel_3", "c_clienthandler", "c_protocolstream", "composition", "", "1", "1")
    add_rel("rel_4", "c_commandhandler", "c_session", "composition", "", "1", "1")
    add_rel("rel_5", "c_commandhandler", "c_userdb", "aggregation", "", "*", "1")
    add_rel("rel_6", "c_commandhandler", "c_storagemanager", "aggregation", "", "*", "1")

    # Client relationships
    add_rel("rel_7", "c_loginframe", "c_ftpservice", "association", "", "1", "1")
    add_rel("rel_8", "c_loginframe", "c_mainframe", "dependency", "creates &amp; opens")
    add_rel("rel_9", "c_mainframe", "c_ftpservice", "association", "", "1", "1")
    add_rel("rel_10", "c_ftpservice", "c_listener", "aggregation", "", "1", "0..1")
    add_rel("rel_11", "c_mainframe", "c_listener", "dependency", "implements / passes")

    # Format pretty XML
    xml_str = minidom.parseString(ET.tostring(root, encoding="utf-8")).toprettyxml(indent="  ")
    
    os.makedirs("docs", exist_ok=True)
    with open("docs/class_diagram.drawio", "w", encoding="utf-8") as f:
        f.write(xml_str)
    with open("docs/class_diagram.xml", "w", encoding="utf-8") as f:
        f.write(xml_str)
    print("Generated docs/class_diagram.drawio & .xml successfully!")

if __name__ == "__main__":
    create_class_diagram_xml()

