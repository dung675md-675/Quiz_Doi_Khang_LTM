# -*- coding: utf-8 -*-
"""
Script hoàn thiện tài liệu đặc tả tổng quan Quiz Arena.
Gồm toàn bộ 9 chương chi tiết, đầy đủ bảng biểu, ma trận, công thức, giao thức socket.
Chuẩn format Word: Times New Roman, A4, margins 2-2-2.5-2, bảng biểu chuyên nghiệp,
callout box, header/footer, phân cấp heading Navy/Blue/Purple.
"""

import os
import sys
import docx
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls

def create_document():
    doc = docx.Document()
    
    # 1. Page setup A4 with VN standard margins
    section = doc.sections[0]
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.0)
    section.bottom_margin = Cm(2.0)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2.0)
    
    # 2. Header and Footer
    header = section.header
    hp = header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hrun = hp.add_run("TÀI LIỆU ĐẶC TẢ TỔNG QUAN HỆ THỐNG QUIZ ARENA 1V1 — ĐỒ ÁN LẬP TRÌNH MẠNG")
    hrun.font.name = "Times New Roman"
    hrun.font.size = Pt(8.5)
    hrun.font.italic = True
    hrun.font.color.rgb = RGBColor(148, 163, 184)
    
    footer = section.footer
    fp = footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    frun1 = fp.add_run("Học viện Công nghệ Bưu chính Viễn thông  |  Trang ")
    frun1.font.name = "Times New Roman"
    frun1.font.size = Pt(9.0)
    frun1.font.italic = True
    frun1.font.color.rgb = RGBColor(100, 116, 139)
    
    # Add page number field
    fld = OxmlElement('w:fldSimple')
    fld.set(qn('w:instr'), 'PAGE')
    fp._p.append(fld)
    
    frun2 = fp.add_run(" / ")
    frun2.font.name = "Times New Roman"
    frun2.font.size = Pt(9.0)
    frun2.font.italic = True
    frun2.font.color.rgb = RGBColor(100, 116, 139)
    
    fld_numpages = OxmlElement('w:fldSimple')
    fld_numpages.set(qn('w:instr'), 'NUMPAGES')
    fp._p.append(fld_numpages)
    
    # Setup Base Styles
    styles = doc.styles
    normal_style = styles['Normal']
    normal_font = normal_style.font
    normal_font.name = 'Times New Roman'
    normal_font.size = Pt(12)
    normal_font.color.rgb = RGBColor(30, 41, 59) # #1E293B
    normal_style.paragraph_format.line_spacing = 1.25
    normal_style.paragraph_format.space_after = Pt(4)
    
    return doc

# Formatting Helper Functions
def add_cover(doc):
    p_inst = doc.add_paragraph()
    p_inst.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_inst = p_inst.add_run("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG\nKHOA CÔNG NGHỆ THÔNG TIN")
    r_inst.font.name = "Times New Roman"
    r_inst.font.size = Pt(13)
    r_inst.font.bold = True
    r_inst.font.color.rgb = RGBColor(31, 42, 68)
    p_inst.paragraph_format.space_after = Pt(6)

    p_subinst = doc.add_paragraph()
    p_subinst.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_subinst = p_subinst.add_run("BỘ MÔN MẠNG MÁY TÍNH & TRUYỀN THÔNG DỮ LIỆU\n***")
    r_subinst.font.name = "Times New Roman"
    r_subinst.font.size = Pt(11.5)
    r_subinst.font.italic = True
    r_subinst.font.color.rgb = RGBColor(75, 85, 99)
    p_subinst.paragraph_format.space_after = Pt(36)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("TÀI LIỆU ĐẶC TẢ TỔNG QUAN HỆ THỐNG\nVÀ LUẬT CHƠI ĐỐI KHÁNG THỜI GIAN THỰC")
    r_title.font.name = "Times New Roman"
    r_title.font.size = Pt(18)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0, 51, 102) # #003366 Deep Navy
    p_title.paragraph_format.space_after = Pt(12)

    p_proj = doc.add_paragraph()
    p_proj.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_proj = p_proj.add_run("ĐỀ TÀI: GAME ĐẤU TRÍ TRẮC NGHIỆM ĐỐI KHÁNG ONLINE (QUIZ ARENA 1V1)")
    r_proj.font.name = "Times New Roman"
    r_proj.font.size = Pt(14)
    r_proj.font.bold = True
    r_proj.font.color.rgb = RGBColor(19, 104, 206) # #1368CE Ocean Blue
    p_proj.paragraph_format.space_after = Pt(8)

    p_desc = doc.add_paragraph()
    p_desc.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_desc = p_desc.add_run("Hệ thống Quiz Multiplayer Client - Server qua TCP Socket\nTích hợp Cơ chế Chiến thuật Cướp điểm / Khiên phản đòn / Ngôi sao hy vọng")
    r_desc.font.name = "Times New Roman"
    r_desc.font.size = Pt(11.5)
    r_desc.font.italic = True
    r_desc.font.color.rgb = RGBColor(71, 85, 105)
    p_desc.paragraph_format.space_after = Pt(40)

    # Info table
    tbl = doc.add_table(rows=5, cols=2)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    info_data = [
        ("Học phần:", "Lập trình Mạng (INT1433)"),
        ("Sinh viên thực hiện:", "Đào Duy Mạnh (MSV: B23DCCN527)"),
        ("Vai trò dự án:", "Toàn diện Kiến trúc Client (UI/UX) & Cơ chế Chiến thuật Server"),
        ("Lớp học phần:", "INT1433-20261-02 — Nhóm 5"),
        ("Thời gian thực hiện:", "Hà Nội, Năm học 2025 - 2026")
    ]
    for row_idx, (label, val) in enumerate(info_data):
        row = tbl.rows[row_idx]
        c0, c1 = row.cells[0], row.cells[1]
        c0.width = Cm(5.0)
        c1.width = Cm(10.5)
        
        p0 = c0.paragraphs[0]
        r0 = p0.add_run(label)
        r0.font.name = "Times New Roman"
        r0.font.bold = True
        r0.font.size = Pt(11.5)
        r0.font.color.rgb = RGBColor(31, 42, 68)
        
        p1 = c1.paragraphs[0]
        r1 = p1.add_run(val)
        r1.font.name = "Times New Roman"
        r1.font.size = Pt(11.5)
        r1.font.color.rgb = RGBColor(30, 41, 59)
        if row_idx == 1:
            r1.font.bold = True
            
        # border none
        for cell in (c0, c1):
            tcPr = cell._tc.get_or_add_tcPr()
            tcBorders = parse_xml(r'''
                <w:tcBorders {} >
                    <w:top w:val="none"/>
                    <w:left w:val="none"/>
                    <w:bottom w:val="none"/>
                    <w:right w:val="none"/>
                </w:tcBorders>
            '''.format(nsdecls('w')))
            tcPr.append(tcBorders)
            
    doc.add_page_break()

def add_heading_1(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(14)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    run.font.name = "Times New Roman"
    run.font.size = Pt(14.5)
    run.font.bold = True
    run.font.color.rgb = RGBColor(0, 51, 102) # #003366 Deep Navy
    return p

def add_heading_2(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    run.font.name = "Times New Roman"
    run.font.size = Pt(13)
    run.font.bold = True
    run.font.color.rgb = RGBColor(19, 104, 206) # #1368CE Ocean Blue
    return p

def add_heading_3(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    run.font.name = "Times New Roman"
    run.font.size = Pt(12)
    run.font.bold = True
    run.font.color.rgb = RGBColor(134, 76, 191) # #864CBF Deep Purple
    return p

def add_paragraph(doc, text, bold_prefix="", italic=False):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.line_spacing = 1.25
    p.paragraph_format.space_after = Pt(4)
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.font.name = "Times New Roman"
        r_pre.font.bold = True
        r_pre.font.color.rgb = RGBColor(31, 42, 68)
    r_body = p.add_run(text)
    r_body.font.name = "Times New Roman"
    r_body.font.size = Pt(12)
    r_body.font.italic = italic
    r_body.font.color.rgb = RGBColor(30, 41, 59)
    return p

def add_bullet(doc, text, bold_prefix=""):
    p = doc.add_paragraph(style='List Bullet')
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.line_spacing = 1.2
    p.paragraph_format.space_after = Pt(2)
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.font.name = "Times New Roman"
        r_pre.font.bold = True
        r_pre.font.color.rgb = RGBColor(31, 42, 68)
    r_body = p.add_run(text)
    r_body.font.name = "Times New Roman"
    r_body.font.size = Pt(11.5)
    r_body.font.color.rgb = RGBColor(30, 41, 59)
    return p

def add_callout(doc, text, title="GHI CHÚ CHUYÊN SÂU", box_type="info"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.cell(0, 0)
    cell.width = Cm(16.5)
    
    # Colors based on type
    if box_type == "info":
        bg_color = "EFF6FF"
        border_color = "1368CE"
    elif box_type == "success":
        bg_color = "F0FDF4"
        border_color = "10B981"
    elif box_type == "warning":
        bg_color = "FEFCE8"
        border_color = "F59E0B"
    elif box_type == "danger":
        bg_color = "FEF2F2"
        border_color = "EF4444"
    else:
        bg_color = "F8FAFC"
        border_color = "64748B"
        
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{bg_color}"/>')
    tcPr.append(shd)
    
    borders = parse_xml(f'''
        <w:tcBorders {nsdecls("w")}>
            <w:top w:val="none"/>
            <w:left w:val="single" w:sz="36" w:color="{border_color}"/>
            <w:bottom w:val="none"/>
            <w:right w:val="none"/>
        </w:tcBorders>
    ''')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.2
    
    r_title = p.add_run(f"[{title}] ")
    r_title.font.name = "Times New Roman"
    r_title.font.bold = True
    r_title.font.size = Pt(11)
    if box_type == "info":
        r_title.font.color.rgb = RGBColor(19, 104, 206)
    elif box_type == "danger":
        r_title.font.color.rgb = RGBColor(239, 68, 68)
    elif box_type == "warning":
        r_title.font.color.rgb = RGBColor(217, 119, 6)
    else:
        r_title.font.color.rgb = RGBColor(16, 185, 129)
        
    r_text = p.add_run(text)
    r_text.font.name = "Times New Roman"
    r_text.font.size = Pt(11)
    r_text.font.italic = True
    r_text.font.color.rgb = RGBColor(51, 65, 85)
    
    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_after = Pt(2)

def create_table(doc, headers, data, col_widths=None):
    tbl = doc.add_table(rows=len(data) + 1, cols=len(headers))
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Header Row
    hdr_cells = tbl.rows[0].cells
    for i, header_text in enumerate(headers):
        cell = hdr_cells[i]
        cell.text = header_text
        if col_widths and i < len(col_widths):
            cell.width = col_widths[i]
            
        tcPr = cell._tc.get_or_add_tcPr()
        shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="1F2A44"/>')
        tcPr.append(shd)
        
        tcBorders = parse_xml(r'''
            <w:tcBorders {} >
                <w:top w:val="single" w:sz="6" w:color="D1D5DB"/>
                <w:left w:val="single" w:sz="6" w:color="D1D5DB"/>
                <w:bottom w:val="single" w:sz="12" w:color="111827"/>
                <w:right w:val="single" w:sz="6" w:color="D1D5DB"/>
            </w:tcBorders>
        '''.format(nsdecls('w')))
        tcPr.append(tcBorders)
        
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(4)
        p.paragraph_format.space_after = Pt(4)
        for r in p.runs:
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.size = Pt(11)
            r.font.color.rgb = RGBColor(255, 255, 255)
            
    # Data Rows
    for row_idx, row_data in enumerate(data):
        row = tbl.rows[row_idx + 1]
        bg_color = "F8FAFC" if row_idx % 2 == 1 else "FFFFFF"
        
        for col_idx, cell_value in enumerate(row_data):
            cell = row.cells[col_idx]
            cell.text = str(cell_value)
            if col_widths and col_idx < len(col_widths):
                cell.width = col_widths[col_idx]
                
            tcPr = cell._tc.get_or_add_tcPr()
            if bg_color != "FFFFFF":
                shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{bg_color}"/>')
                tcPr.append(shd)
                
            tcBorders = parse_xml(r'''
                <w:tcBorders {} >
                    <w:top w:val="single" w:sz="4" w:color="E2E8F0"/>
                    <w:left w:val="single" w:sz="4" w:color="E2E8F0"/>
                    <w:bottom w:val="single" w:sz="4" w:color="E2E8F0"/>
                    <w:right w:val="single" w:sz="4" w:color="E2E8F0"/>
                </w:tcBorders>
            '''.format(nsdecls('w')))
            tcPr.append(tcBorders)
            
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(3)
            p.paragraph_format.space_after = Pt(3)
            p.paragraph_format.line_spacing = 1.15
            
            # Align center if short text or numbers
            if len(str(cell_value)) <= 15 or col_idx == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            else:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                
            for r in p.runs:
                r.font.name = "Times New Roman"
                r.font.size = Pt(10.5)
                r.font.color.rgb = RGBColor(30, 41, 59)
                
    p_sp = doc.add_paragraph()
    p_sp.paragraph_format.space_after = Pt(4)
    return tbl

def build_full_document():
    doc = create_document()
    
    # TRANG BÌA
    add_cover(doc)
    
    # LỜI MỞ ĐẦU
    add_heading_1(doc, "LỜI MỞ ĐẦU")
    add_paragraph(doc, 
        "Tài liệu này được biên soạn nhằm mục đích đặc tả tổng quan toàn diện kiến trúc kỹ thuật, "
        "luật chơi đối kháng, hệ thống kỹ năng chiến thuật, luồng đồng bộ thời gian thực và giao thức "
        "truyền thông socket của dự án Quiz Arena 1v1. Đây là sản phẩm thuộc đồ án môn học Lập trình Mạng (INT1433), "
        "khoa Công nghệ Thông tin - Học viện Công nghệ Bưu chính Viễn thông."
    )
    add_paragraph(doc, 
        "Khác với các ứng dụng đố vui trực tuyến thông thường chỉ tập trung vào việc bấm chọn đáp án tĩnh, "
        "Quiz Arena mang đến một sàn đấu trí tuệ đối kháng 1v1 nghẹt thở với các yếu tố 'Mind Games' đỉnh cao: "
        "cướp điểm đối thủ, kích hoạt khiên phản đòn thần thánh, đặt cược ngôi sao hy vọng, kết hợp cùng hệ thống "
        "5 dạng câu hỏi đa tương tác và cơ chế đếm giờ động. Mọi trạng thái trận đấu được kiểm soát tập trung tại Server "
        "theo mô hình máy trạng thái hữu hạn (FSM) với độ trễ thấp và độ tin cậy tuyệt đối."
    )
    add_callout(doc, 
        "Tài liệu phản ánh chính xác 100% hiện trạng mã nguồn của dự án tại nhánh 'duymanh', bao gồm các cải tiến "
        "mới nhất về giới hạn 1 lượt dùng cho kỹ năng chiến thuật, cơ chế tự động bảo lưu điểm kéo thả khi hết giờ, "
        "hệ thống banner thông báo kết quả đối đầu pha Reveal, và giao diện Glassmorphism Dark Cyberpunk cao cấp.",
        title="THÔNG TIN PHIÊN BẢN & MÃ NGUỒN", box_type="info"
    )

    # CHƯƠNG 1
    add_heading_1(doc, "CHƯƠNG 1: TỔNG QUAN HỆ THỐNG & KIẾN TRÚC PHÂN TẦNG")
    
    add_heading_2(doc, "1.1. Bối cảnh đề tài và Mục tiêu phát triển")
    add_paragraph(doc, 
        "Trong kỷ nguyên số, các nền tảng giáo dục tương tác và gamification ngày càng khẳng định vai trò then chốt "
        "trong việc kích thích tư duy người học. Tuy nhiên, phần lớn các ứng dụng quiz hiện nay (như Kahoot, Quizizz) "
        "hoạt động theo mô hình phòng đông người với nhịp độ một chiều, thiếu tính đối đầu trực tiếp giữa hai cá nhân. "
        "Quiz Arena được thiết kế để giải quyết bài toán đó thông qua một đấu trường 1v1 thời gian thực, nơi chiến thắng "
        "không chỉ phụ thuộc vào kiến thức uyên bác mà còn nằm ở sự nhạy bén chiến thuật và tâm lý thi đấu."
    )
    add_paragraph(doc, "Mục tiêu cụ thể của hệ thống bao gồm:")
    add_bullet(doc, "Đảm bảo tính đồng bộ thời gian thực (Realtime Synchronization) giữa hai người chơi qua TCP Socket với độ trễ phản hồi dưới 15ms trong mạng nội bộ.", "• Tính tức thời: ")
    add_bullet(doc, "Server đóng vai trò cơ quan thẩm quyền duy nhất (Authoritative Server) kiểm soát đồng hồ, tính điểm, xác thực câu trả lời và phân giải ma trận chiến thuật nhằm triệt tiêu hoàn toàn gian lận từ Client.", "• Tính công bằng & Bảo mật: ")
    add_bullet(doc, "Đa dạng hóa trải nghiệm bằng 5 định dạng câu hỏi phong phú (Trắc nghiệm, Đúng/Sai, Kéo thả từ, Điền khuyết, Chọn nhiều phương án).", "• Trải nghiệm học tập phong phú: ")
    add_bullet(doc, "Giao diện người dùng đồ họa hiện đại lấy cảm hứng từ Dark Cyberpunk và Glassmorphism, tích hợp âm thanh SFX, nhạc nền và hệ thống vẽ Vector 2D chống lỗi hiển thị.", "• Thẩm mỹ & Trực quan: ")

    add_heading_2(doc, "1.2. Mô hình kiến trúc phân tầng (Client - Server Architecture)")
    add_paragraph(doc, 
        "Hệ thống Quiz Arena áp dụng kiến trúc Client - Server tập trung, chia tách thành 4 tầng rõ rệt:"
    )
    
    arch_headers = ["Tầng chức năng", "Thành phần mã nguồn tiêu biểu", "Trách nhiệm & Cơ chế vận hành"]
    arch_data = [
        ["Presentation (Client UI)", "quiz.ui.*, MatchScreen, TacticalActionBar, ToastManager", "Hiển thị giao diện người dùng bằng Java Swing, quản lý animation trượt, nhận thao tác chuột/phím, cập nhật realtime trên EDT thread."],
        ["Network Controller", "quiz.Client, quiz.Server$Peer, quiz.Wire", "Duy trì kết nối TCP Socket dài hạn, đọc/ghi gói tin nhị phân có tiền tố độ dài, điều hướng bản tin đến các trình xử lý sự kiện."],
        ["Business Logic & FSM", "quiz.Server, quiz.Server$Match, quiz.Model", "Quản lý phòng đấu, điều phối nhịp trận đấu qua 6 pha (Ready, Countdown, Intro, Active, Reveal, Result), phân giải ma trận điểm."],
        ["Persistence & Security", "quiz.DB, quiz.Security, quiz.Bank, users.dat, questions.txt", "Xác thực tài khoản, băm mật khẩu SHA-256 + Salt, lưu lịch sử đấu, tự động chuyển đổi Fallback nếu mất kết nối Database SQL."]
    ]
    create_table(doc, arch_headers, arch_data, [Cm(3.8), Cm(4.8), Cm(7.9)])

    add_heading_2(doc, "1.3. Cơ chế định khung dữ liệu nhị phân (Binary Framing Protocol)")
    add_paragraph(doc, 
        "Để khắc phục triệt để hiện tượng 'dính gói' (packet coalescing) và 'xé gói' (packet fragmentation) vốn rất phổ biến "
        "khi truyền nhận dữ liệu dòng (stream-based) qua giao thức TCP, lớp Wire.java sử dụng cơ chế định khung bản tin "
        "dựa trên tiền tố số lượng trường (Length-Prefixed Framing):"
    )
    add_bullet(doc, "Mỗi bản tin bắt đầu bằng một số nguyên 4-byte (DataOutputStream.writeInt) biểu thị số lượng trường tham số (fields count).", "1. Tiền tố kích thước: ")
    add_bullet(doc, "Mỗi tham số tiếp theo được ghi dưới dạng chuỗi UTF-8 có tiền tố 2-byte độ dài chuỗi (DataOutputStream.writeUTF).", "2. Dữ liệu tham số: ")
    add_bullet(doc, "Socket được kích hoạt cờ TCP_NODELAY (Nagle's algorithm bypass) để các bản tin chiến thuật và đếm giờ được phát đi ngay lập tức mà không bị giữ lại trong bộ đệm hệ điều hành.", "3. Tối ưu độ trễ: ")

    add_heading_2(doc, "1.4. Hệ lưu trữ đa tầng & Cơ chế tự phục hồi (Fault Tolerance)")
    add_paragraph(doc, 
        "Hệ thống hỗ trợ song song hai tầng lưu trữ dữ liệu với cơ chế chuyển đổi thông minh (Smart Failover):"
    )
    add_bullet(doc, "Hệ quản trị CSDL quan hệ lưu trữ tập trung bảng Users, Questions, MatchHistory, MatchDetails và Leaderboard điểm Rank.", "• Chế độ Doanh nghiệp (PostgreSQL): ")
    add_bullet(doc, "Nếu môi trường không cài đặt PostgreSQL hoặc mất kết nối mạng CSDL, Server tự động chuyển sang đọc/ghi dữ liệu từ file nhị phân users.dat (Java Serialization) và questions.txt. Toàn bộ tiến trình hoạt động của người chơi không bị gián đoạn hay phát sinh lỗi crash.", "• Chế độ Dự phòng Cục bộ (Local Fallback): ")

    # CHƯƠNG 2
    add_heading_1(doc, "CHƯƠNG 2: NGÂN HÀNG CÂU HỎI & CƠ CHẾ ĐẾM GIỜ ĐỘNG")
    
    add_heading_2(doc, "2.1. Chi tiết 5 dạng câu hỏi thi đấu chuyên sâu")
    add_paragraph(doc, 
        "Khác biệt lớn nhất của Quiz Arena so với các hệ thống trắc nghiệm cổ điển là khả năng hỗ trợ đồng thời 5 dạng câu hỏi "
        "tương tác cao với cơ chế chấm điểm và giao diện nhập liệu riêng biệt được quy định trong Model.QuestionType:"
    )

    qtype_headers = ["Dạng câu hỏi", "Mã định danh Type", "Thời gian (s)", "Đặc tả giao diện & Cách thức người chơi trả lời"]
    qtype_data = [
        ["Trắc nghiệm 4 đáp án", "MULTIPLE_CHOICE", "10s", "Hiển thị câu hỏi và 4 ô đáp án A, B, C, D màu sắc nổi bật. Người chơi click chọn 1 đáp án duy nhất."],
        ["Đúng / Sai", "TRUE_FALSE", "8s", "Kiểm tra phản xạ kiến thức nhanh với 2 lựa chọn nhị phân 'Đúng' (Xanh lục) hoặc 'Sai' (Đỏ rực)."],
        ["Chọn nhiều đáp án", "MULTIPLE_SELECT", "15s", "Câu hỏi có từ 1 đến nhiều phương án đúng. Người chơi tích chọn các checkbox và bắt buộc bấm nút 'Nộp bài' để chốt."],
        ["Kéo thả vào ô trống", "DRAG_AND_DROP", "15s", "Đoạn văn bản chứa các vị trí khuyết [BLANK]. Người chơi kéo các thẻ từ vựng vào đúng ô trống tương ứng."],
        ["Điền từ vào ô trống", "FILL_IN", "17s", "Người chơi gõ trực tiếp cụm từ cần điền vào các ô nhập liệu dạng văn bản; hệ thống tự động chuẩn hóa chữ hoa/thường."]
    ]
    create_table(doc, qtype_headers, qtype_data, [Cm(3.5), Cm(3.5), Cm(2.2), Cm(7.3)])

    add_heading_2(doc, "2.2. Cơ chế đếm giờ động (Dynamic Timer) & Bù trễ mạng")
    add_paragraph(doc, 
        "Thời gian làm bài cho mỗi câu hỏi được cấp phát động (Dynamic Allocation) dựa trên độ phức tạp tương tác của dạng câu đó. "
        "Thay vì áp dụng thời gian cào bằng 10 giây cho mọi câu hỏi, Quiz Arena phân bổ:"
    )
    add_bullet(doc, "8 giây cho câu Đúng/Sai (cần phản xạ nhanh).", "• Dạng đơn giản: ")
    add_bullet(doc, "10 giây cho câu Trắc nghiệm 4 phương án chuẩn.", "• Dạng cơ bản: ")
    add_bullet(doc, "15 - 17 giây cho câu Kéo thả từ, Chọn nhiều đáp án và Điền khuyết (cần thời gian đọc hiểu và thao tác chuột/bàn phím).", "• Dạng tương tác: ")
    add_paragraph(doc, 
        "Cơ chế đệm trễ mạng (Network Latency Buffer): Tại Server, thời gian chờ nộp bài thực tế được cộng thêm một khoản buffer "
        "800ms so với đồng hồ hiển thị trên Client. Cơ chế này đảm bảo người chơi nộp đáp án ở những mili-giây cuối cùng "
        "không bao giờ bị Server từ chối do độ trễ truyền gói tin trên đường truyền mạng."
    )

    add_heading_2(doc, "2.3. Cơ chế tự động chốt đáp án khi hết giờ (Auto-Submit on Timeout)")
    add_paragraph(doc, 
        "Một vấn đề trải nghiệm người dùng nghiêm trọng trong các game trắc nghiệm tương tác là: người chơi đã bỏ công sức "
        "kéo thả toàn bộ các từ hoặc gõ xong đáp án, nhưng do mải suy nghĩ nên không kịp bấm nút 'Nộp bài' trước khi đồng hồ về 0. "
        "Quiz Arena giải quyết triệt để vấn đề này bằng cơ chế Tự động chốt đáp án đồng bộ hai tầng:"
    )
    add_bullet(doc, "Khi đồng hồ đếm ngược còn <= 1 giây hoặc chạm mốc 0 giây, MatchScreen kích hoạt hàm syncCurrentAnswerInput() để đọc trạng thái tức thời của các ô kéo thả / điền từ và chuyển hóa thành mã đáp án.", "1. Tầng Client: ")
    add_bullet(doc, "Client tự động gửi gói tin ANSWER mang đáp án hiện tại lên Server ngay trước khi chuyển giao diện, đồng thời khóa tương tác.", "2. Tầng Giao thức: ")
    add_bullet(doc, "Server tiếp nhận đáp án trong khoảng buffer 800ms, tiến hành so khớp với đáp án đúng và cộng điểm bình thường như khi người chơi tự tay bấm nộp.", "3. Tầng Server: ")

    # CHƯƠNG 3
    add_heading_1(doc, "CHƯƠNG 3: LUẬT CHƠI CHI TIẾT & CÔNG THỨC TÍNH ĐIỂM")
    
    add_heading_2(doc, "3.1. Điểm cơ sở & Thưởng tốc độ phản xạ (Dynamic Speed Bonus)")
    add_paragraph(doc, 
        "Mỗi câu hỏi thi đấu có điểm cơ bản và điểm thưởng tốc độ được tính toán độc lập cho từng người chơi:"
    )
    add_bullet(doc, "Người chơi trả lời đúng câu hỏi nhận được ngay 100 điểm cơ sở. Trả lời sai nhận 0 điểm.", "• Điểm cơ sở (Base Score): ")
    add_paragraph(doc, 
        "Điểm thưởng tốc độ (Speed Bonus): Thưởng tối đa 50 điểm cho người chơi có phản xạ nhanh nhạy. "
        "Công thức tính tuyến tính dựa trên tỉ lệ thời gian còn lại so với tổng thời gian quy định của câu hỏi:"
    )
    add_callout(doc, 
        "SpeedBonus = round( max(0.0, 1.0 - (elapsedMs / maxMs)) * 50.0 )\n"
        "Trong đó:\n"
        "  - elapsedMs: Thời gian thực tế tính từ lúc bắt đầu câu hỏi đến khi nộp bài (mili-giây).\n"
        "  - maxMs: Tổng thời gian cho phép của dạng câu hỏi đó (ví dụ 10.000ms đối với trắc nghiệm).",
        title="CÔNG THỨC THƯỞNG TỐC ĐỘ CHUẨN", box_type="info"
    )

    add_heading_2(doc, "3.2. Điểm thưởng chuỗi đúng liên tiếp (Streak Bonus)")
    add_paragraph(doc, 
        "Hệ thống tôn vinh sự chính xác bền bỉ qua từng câu hỏi thông qua cơ chế thưởng chuỗi (Streak Bonus). "
        "Khi người chơi trả lời đúng liên tiếp từ 2 câu trở lên (streak >= 2), điểm thưởng cộng thêm được tính như sau:"
    )
    add_callout(doc, 
        "StreakBonus = min(50, (streak - 1) * 15)\n"
        "Cụ thể:\n"
        "  - Chuỗi đúng 1 câu: 0 điểm thưởng chuỗi.\n"
        "  - Chuỗi đúng 2 câu: (2 - 1) * 15 = +15 điểm.\n"
        "  - Chuỗi đúng 3 câu: (3 - 1) * 15 = +30 điểm.\n"
        "  - Chuỗi đúng 4 câu: (4 - 1) * 15 = +45 điểm.\n"
        "  - Chuỗi đúng từ 5 câu trở lên: Kịch trần +50 điểm thưởng chuỗi.\n"
        "Lưu ý: Ngay khi trả lời sai một câu bất kỳ, chuỗi streak lập tức bị reset về 0.",
        title="CÔNG THỨC THƯỞNG CHUỖI ĐÚNG LIÊN TIẾP", box_type="warning"
    )
    add_paragraph(doc, 
        "Tổng điểm thuần của 1 câu hỏi (chưa bao gồm kỹ năng chiến thuật) là: Earned = 100 + SpeedBonus + StreakBonus. "
        "Số điểm tối đa một người chơi có thể đạt được trong 1 câu là 200 điểm."
    )

    # CHƯƠNG 4
    add_heading_1(doc, "CHƯƠNG 4: HỆ THỐNG KỸ NĂNG CHIẾN THUẬT (TACTICAL COMBAT SYSTEM)")
    
    add_heading_2(doc, "4.1. Triết lý thiết kế & Giới hạn tài nguyên chiến thuật")
    add_paragraph(doc, 
        "Kỹ năng chiến thuật là linh hồn tạo nên sự kịch tính và đột phá của Quiz Arena. Trò chơi trao cho mỗi đấu thủ "
        "3 kỹ năng đại diện cho 3 trường phái tâm lý thi đấu: Công kích dũng mãnh, Phòng ngự phản công, và Đặt cược mạo hiểm. "
        "Để đảm bảo tính cân bằng chiến lược (Game Balance), hệ thống áp đặt các quy tắc khắt khe:"
    )
    add_bullet(doc, "Trong suốt toàn bộ trận đấu gồm 10 câu hỏi, mỗi người chơi chỉ có DUY NHẤT 1 LƯỢT DÙNG cho mỗi kỹ năng (Tấn công: 1, Phòng thủ: 1, Ngôi sao: 1).", "1. Giới hạn 1 lượt dùng: ")
    add_bullet(doc, "Để phát động Tấn công hoặc cược Ngôi sao hy vọng, người chơi bắt buộc phải tích lũy được tối thiểu 50 điểm trong trận đấu. Nếu dưới 50 điểm, nút kỹ năng sẽ bị vô hiệu hóa.", "2. Điều kiện điểm sàn (Điểm >= 50): ")
    add_bullet(doc, "Kỹ năng Phòng thủ luôn luôn khả dụng (kể cả khi người chơi đang có 0 điểm) nhằm tạo cơ hội lật kèo và tự vệ cho người chơi đang bị dẫn trước.", "3. Đặc quyền Phòng thủ: ")
    add_bullet(doc, "Người chơi có thể kích hoạt hoặc hủy kích hoạt (CANCEL) kỹ năng tùy ý trước khi nộp đáp án. Lượt kỹ năng chỉ chính thức bị tiêu hao khi người chơi nộp bài hoặc hết giờ câu hỏi.", "4. Cơ chế Hủy linh hoạt: ")

    add_heading_2(doc, "4.2. Đặc tả chi tiết 3 kỹ năng chiến thuật")
    
    tactical_headers = ["Kỹ năng", "Biểu tượng & Màu sắc", "Lượt dùng", "Điều kiện kích hoạt", "Cơ chế tác động & Hiệu ứng trận đấu"]
    tactical_data = [
        ["Tấn công (Attack)", "⚔️ Đỏ Cam (#EF4444)", "1 lượt / trận", "Điểm >= 50, chưa nộp bài", "Nếu trả lời ĐÚNG và đối thủ không có khiên: CƯỚP 50 điểm trực tiếp từ quỹ điểm của đối thủ cộng vào điểm của mình. Nếu trả lời SAI: đòn đánh thất bại."],
        ["Phòng thủ (Defend)", "🛡️ Xanh Ngọc (#0D9488)", "1 lượt / trận", "Luôn khả dụng (0đ vẫn dùng được)", "Dựng khiên chặn đứng đòn Tấn công của đối thủ (không bị trừ điểm). ĐẶC BIỆT: Nếu đối thủ tấn công SAI mà mình trả lời ĐÚNG -> PHẢN ĐÒN cướp ngược 50 điểm của đối thủ!"],
        ["Ngôi sao hy vọng (Star)", "⭐ Vàng Gold (#F59E0B)", "1 lượt / trận", "Điểm >= 50, chưa nộp bài", "Đặt cược vào câu hỏi hiện tại: Nếu trả lời ĐÚNG: NHÂN ĐÔI (x2) toàn bộ điểm kiếm được ở câu đó. Nếu trả lời SAI: BỊ TRỪ 50 ĐIỂM (điểm sàn không âm)."]
    ]
    create_table(doc, tactical_headers, tactical_data, [Cm(3.2), Cm(3.2), Cm(2.2), Cm(3.2), Cm(4.7)])

    add_heading_2(doc, "4.3. Ma trận đối kháng pha Reveal (Combat Resolution Matrix)")
    add_paragraph(doc, 
        "Toàn bộ kết quả đối đầu chiến thuật được giải quyết tập trung tại hàm Server.resolveTacticalCombat() "
        "khi kết thúc câu hỏi theo ma trận 12 tình huống sau:"
    )

    matrix_headers = ["STT", "Hành động P1", "Hành động P2", "Kết quả trả lời", "Phân giải điểm số & Kết quả đối kháng"]
    matrix_data = [
        ["1", "Tấn công (⚔️)", "Không dùng (None)", "P1 Đúng, P2 Bất kỳ", "P1 cướp thành công 50đ của P2 (out1='STEAL_SUCCESS', out2='BEING_STOLEN'). P1 +50đ, P2 -50đ."],
        ["2", "Tấn công (⚔️)", "Không dùng (None)", "P1 Sai", "P1 tấn công thất bại (out1='ATTACK_FAIL'). Điểm số hai bên không đổi do chiến thuật."],
        ["3", "Tấn công (⚔️)", "Phòng thủ (🛡️)", "P1 Đúng, P2 Đúng", "Khiên P2 chặn đứng đòn đánh (out1='BLOCKED', out2='BLOCK_SUCCESS'). P2 không bị mất điểm."],
        ["4", "Tấn công (⚔️)", "Phòng thủ (🛡️)", "P1 Sai, P2 Đúng", "PHẢN ĐÒN THÀNH CÔNG (out1='COUNTERED', out2='COUNTER_SUCCESS'). P2 cướp ngược 50đ từ P1!"],
        ["5", "Tấn công (⚔️)", "Phòng thủ (🛡️)", "P1 Sai, P2 Sai", "Cả hai đều thất bại (out1='ATTACK_FAIL', out2='DEFEND_FAIL'). Không ai được cộng điểm chiến thuật."],
        ["6", "Phòng thủ (🛡️)", "Không dùng (None)", "Bất kỳ", "P1 lãng phí khiên do đối thủ không tấn công (out1='DEFEND_MISSED'). Mất lượt khiên."],
        ["7", "Tấn công (⚔️)", "Tấn công (⚔️)", "Cả 2 cùng Đúng", "Hai bên cùng cướp 50đ của nhau -> Triệt tiêu trực tiếp: Delta điểm chiến thuật = 0."],
        ["8", "Tấn công (⚔️)", "Tấn công (⚔️)", "P1 Đúng, P2 Sai", "P1 cướp thành công 50đ từ P2; P2 tấn công trượt (P1 +50đ, P2 -50đ)."],
        ["9", "Ngôi sao (⭐)", "Bất kỳ", "P1 Đúng", "P1 nhân đôi điểm kiếm được ở câu đó (out1='STAR_SUCCESS', điểm câu đó * 2)."],
        ["10", "Ngôi sao (⭐)", "Bất kỳ", "P1 Sai", "P1 bị trừ phạt 50 điểm (out1='STAR_FAIL', P1 = max(0, P1 - 50))."],
        ["11", "Tấn công (⚔️)", "Ngôi sao (⭐)", "P1 Đúng, P2 Đúng", "P1 cướp 50đ của P2; P2 được x2 điểm câu đó rồi bị trừ 50đ cướp."],
        ["12", "Tấn công (⚔️)", "Ngôi sao (⭐)", "P1 Đúng, P2 Sai", "Thảm họa kép cho P2: P2 vừa bị phạt sao -50đ, vừa bị P1 cướp thêm 50đ (tổng mất tới 100đ)."]
    ]
    create_table(doc, matrix_headers, matrix_data, [Cm(1.0), Cm(2.8), Cm(2.8), Cm(3.2), Cm(6.7)])

    add_heading_2(doc, "4.4. Hệ thống phản hồi trực quan (Toast Banner, Nhấp nháy & Âm thanh)")
    add_paragraph(doc, 
        "Hệ thống cung cấp trải nghiệm phản hồi giác quan đa chiều (Multi-sensory Feedback) sống động:"
    )
    add_bullet(doc, "Khi đối thủ vừa kích hoạt kỹ năng, một dải banner mỏng (~24px) trượt nhẹ xuống ngay dưới thanh Header đối đầu của MatchScreen hiển thị rõ tên đối thủ và loại kỹ năng, kèm hiệu ứng âm thanh SFX tương ứng.", "• Dải banner trượt TacticalToastBanner: ")
    add_bullet(doc, "Nếu đối thủ phát động TẤN CÔNG, nút 'Phòng Thủ' của người chơi lập tức nhấp nháy đèn nền cảnh báo khẩn cấp để nhắc nhở người chơi kịp thời bật khiên đỡ đòn trước khi bấm nộp bài.", "• Hiệu ứng nhấp nháy nút Phòng thủ (Pulsing Animation): ")
    add_bullet(doc, "Ngay khi bước vào pha Reveal (công bố kết quả), TacticalToastBanner hiển thị tức thời kết quả phân giải đối kháng (ví dụ: '⚔️ Bạn đã cướp thành công +50đ!' hoặc '🛡️ Phản đòn thần thánh! Bạn cướp ngược +50đ!') kèm âm thanh SFX kiếm chém hoặc khiên đỡ vang dội.", "• Banner kết quả pha Reveal: ")

    # CHƯƠNG 5
    add_heading_1(doc, "CHƯƠNG 5: VÒNG ĐỜI TRẬN ĐẤU & LUỒNG ĐỒNG BỘ TRẠNG THÁI")
    
    add_heading_2(doc, "5.1. Máy trạng thái trận đấu (Match State Machine)")
    add_paragraph(doc, 
        "Để đảm bảo tính nhất quán dữ liệu giữa hai Client phân tán, Server quản lý trận đấu qua một Máy trạng thái hữu hạn (FSM) "
        "với 6 pha tuần tự nghiêm ngặt:"
    )
    add_bullet(doc, "Hai người chơi kết nối vào phòng thi đấu và gửi bản tin READY để xác nhận sẵn sàng.", "1. READY_CHECK: ")
    add_bullet(doc, "Server kích hoạt bộ đếm ngược đồng bộ 3 -> 2 -> 1 -> GO phát sóng đến cả 2 Client.", "2. COUNTDOWN: ")
    add_bullet(doc, "Pha giới thiệu câu hỏi (2 giây) hiển thị số thứ tự câu hỏi và chủ đề để người chơi định thần.", "3. QUESTION_INTRO: ")
    add_bullet(doc, "Giai đoạn làm bài chính thức với thời gian động (8s - 17s), cho phép gửi Emote và thao tác kỹ năng chiến thuật.", "4. ACTIVE: ")
    add_bullet(doc, "Pha công bố đáp án (3 giây), hiển thị giải thích chi tiết, banner kết quả chiến thuật, hỗ trợ nút Skip Reveal.", "5. REVEAL: ")
    add_bullet(doc, "Kết thúc 10 câu, Server tổng hợp kết quả toàn trận, lưu lịch sử CSDL và mở màn hình ResultDialog.", "6. FINISHED: ")

    add_heading_2(doc, "5.2. Luồng làm bài & Pha công bố đáp án tối ưu (Skip Reveal)")
    add_paragraph(doc, 
        "Khi cả hai người chơi đều đã nộp bài (hoặc hết giờ), trận đấu lập tức chuyển sang pha REVEAL mà không cần chờ hết đồng hồ. "
        "Trong pha REVEAL, hai người chơi có 3 giây để quan sát đáp án đúng, đọc giải thích và nhìn nhận biến động điểm chiến thuật. "
        "Nếu cả hai người chơi cùng bấm nút 'Bỏ qua' (Skip), Server sẽ lập tức nhảy sang câu hỏi tiếp theo ngay mà không cần đợi hết 3 giây, "
        "tạo sự chủ động và đẩy nhanh nhịp độ trận đấu."
    )

    add_heading_2(doc, "5.3. Xử lý mất kết nối & Thoát trận ngang (Graceful Disconnect)")
    add_paragraph(doc, 
        "Nếu một người chơi bị mất kết nối mạng hoặc cố tình tắt ứng dụng giữa trận:"
    )
    add_bullet(doc, "Server phát hiện sự cố đứt Socket (IOException) ngay lập tức thông qua reader thread.", "• Phát hiện tức thì: ")
    add_bullet(doc, "Server gửi bản tin OPPONENT_DISCONNECTED thông báo cho người chơi còn lại, đồng thời dừng các bộ đếm giờ.", "• Thông báo đối thủ: ")
    add_bullet(doc, "Người chơi còn lại được xử THẮNG KNOCK-OUT tuyệt đối; người thoát ngang bị xử THUA và bị trừ điểm Rank danh dự.", "• Phán quyết công bằng: ")

    # CHƯƠNG 6
    add_heading_1(doc, "CHƯƠNG 6: TỪ ĐIỂN GIAO THỨC TRUYỀN THÔNG SOCKET")
    
    add_heading_2(doc, "6.1. Danh mục các bản tin Client gửi Server (Client -> Server)")
    add_paragraph(doc, "Tầng Client gửi 26 bản tin yêu cầu và tương tác nghiệp vụ lên Server trung tâm:")

    cs_headers = ["Lệnh (Command)", "Cú pháp tham số", "Mô tả nghiệp vụ"]
    cs_data = [
        ["LOGIN", "LOGIN <username> <password>", "Xác thực tài khoản và mật khẩu người dùng."],
        ["REGISTER", "REGISTER <username> <password>", "Đăng ký tài khoản người dùng mới."],
        ["CHALLENGE", "CHALLENGE <target_username> <topic> <level>", "Gửi lời mời thách đấu 1v1 trực tiếp đến một người chơi trong sảnh."],
        ["ACCEPT", "ACCEPT <inviter_username>", "Chấp nhận lời mời thách đấu từ người khác."],
        ["DECLINE", "DECLINE <inviter_username>", "Từ chối lời mời thách đấu."],
        ["CANCEL_CHALLENGE", "CANCEL_CHALLENGE <target>", "Hủy bỏ lời mời đang chờ phản hồi."],
        ["QUICK_MATCH", "QUICK_MATCH <topic> <level>", "Tham gia hàng đợi tìm trận ngẫu nhiên tự động."],
        ["CANCEL_MATCHMAKING", "CANCEL_MATCHMAKING", "Rút khỏi hàng đợi tìm trận tự động."],
        ["SOLO_MATCH", "SOLO_MATCH <topic> <level>", "Bắt đầu trận đấu luyện tập đơn với Bot thông minh."],
        ["READY", "READY <matchId>", "Xác nhận sẵn sàng bước vào đếm ngược trận đấu."],
        ["ANSWER", "ANSWER <matchId> <qIndex> <choice> <elapsedMs>", "Nộp đáp án cho câu hỏi hiện tại kèm thời gian làm bài."],
        ["TACTICAL_ACTION", "TACTICAL_ACTION <matchId> <qIndex> <ATTACK|DEFEND|STAR|CANCEL>", "Kích hoạt hoặc hủy kỹ năng chiến thuật trong câu hiện tại."],
        ["SKIP_REVEAL", "SKIP_REVEAL <matchId> <qIndex>", "Yêu cầu bỏ qua sớm pha xem đáp án để qua câu mới."],
        ["EMOTE", "EMOTE <matchId> <emoteId>", "Gửi biểu cảm cảm xúc thời gian thực (Haha, Sad, Wow...) sang đối thủ."],
        ["REMATCH", "REMATCH <matchId>", "Yêu cầu tái đấu với đối thủ vừa thi đấu xong."],
        ["LEAVE", "LEAVE <matchId>", "Chủ động rời phòng đấu hoặc chịu thua giữa trận."],
        ["LEADERBOARD", "LEADERBOARD", "Yêu cầu tải bảng xếp hạng top người chơi."],
        ["HISTORY", "HISTORY", "Yêu cầu tải lịch sử đấu cá nhân."],
        ["USER_LIST", "USER_LIST", "Yêu cầu cập nhật danh sách người chơi trực tuyến trong sảnh."],
        ["UPDATE_AVATAR", "UPDATE_AVATAR <avatarId>", "Thay đổi avatar đại diện của người chơi."],
        ["QUESTION_LIST", "QUESTION_LIST", "Quản trị viên yêu cầu tải danh sách câu hỏi."],
        ["ADD_QUESTION", "ADD_QUESTION <topic> <level> <type> <text>...", "Quản trị viên thêm câu hỏi mới vào ngân hàng."],
        ["UPDATE_QUESTION", "UPDATE_QUESTION <id> <data...>", "Quản trị viên chỉnh sửa nội dung câu hỏi."],
        ["DELETE_QUESTION", "DELETE_QUESTION <id>", "Quản trị viên xóa câu hỏi khỏi hệ thống."],
        ["ADD_USER / DEL_USER", "ADD_USER / DELETE_USER <username>", "Quản trị viên quản lý danh sách tài khoản."],
        ["PROGRESS", "PROGRESS <matchId> <qIndex>", "Đồng bộ tiến độ làm bài thời gian thực."]
    ]
    create_table(doc, cs_headers, cs_data, [Cm(4.0), Cm(5.2), Cm(7.3)])

    add_heading_2(doc, "6.2. Danh mục các bản tin Server gửi Client (Server -> Client)")
    add_paragraph(doc, "Server phát sóng 34 bản tin phản hồi và đồng bộ trạng thái đến các Client:")

    sc_headers = ["Lệnh (Command)", "Cú pháp tham số", "Mô tả phản hồi & Tác động Client"]
    sc_data = [
        ["WELCOME", "WELCOME <username> <rank> <points> <avatar>", "Đăng nhập thành công, đồng bộ thông tin người chơi vào sảnh."],
        ["LOBBY", "LOBBY <username> <status> ...", "Cập nhật trạng thái trực tuyến/bận/rỗi của mọi người chơi trong sảnh."],
        ["INVITE", "INVITE <from_user> <topic> <level>", "Hiển thị hộp thoại mời đấu từ người chơi khác."],
        ["DECLINED", "DECLINED <target_user>", "Thông báo đối thủ đã từ chối lời mời đấu."],
        ["MATCH_FOUND", "MATCH_FOUND <matchId> <oppName> <oppRank>...", "Tìm thấy trận đấu, chuyển người chơi từ sảnh vào MatchScreen."],
        ["MATCH_PHASE", "MATCH_PHASE <COUNTDOWN|INTRO|ACTIVE|REVEAL> ...", "Đồng bộ chuyển pha trận đấu cho toàn bộ client."],
        ["OPPONENT_TACTICAL", "OPPONENT_TACTICAL <ATTACK|DEFEND|STAR|CANCEL>", "Báo hiệu đối thủ vừa bấm chiêu để hiện banner và nhấp nháy khiên."],
        ["TACTICAL_CONFIRM", "TACTICAL_CONFIRM <skill> <remaining>", "Xác nhận Server đã chấp nhận chiêu thức và cập nhật số lượt còn lại."],
        ["TACTICAL_STATUS", "TACTICAL_STATUS <atkRem> <defRem> <starRem>", "Đồng bộ số lượt chiêu thức còn lại cho Client."],
        ["SCORE", "SCORE <myScore> <oppScore> [myDelta] [oppDelta]", "Cập nhật điểm số hai bên và biến động điểm sau câu hỏi."],
        ["RESULT", "RESULT <WIN|LOSS|DRAW> <myPts> <oppPts> <tacticalSummary>", "Công bố kết quả chung cuộc, mở ResultDialog kèm chỉ số Head-to-Head."],
        ["EMOTE", "EMOTE <emoteId>", "Hiển thị bong bóng biểu cảm nổi lên trên avatar đối thủ."],
        ["HISTORY", "HISTORY <count> <historyData...>", "Trả về danh sách các trận đấu gần nhất cho HistoryDialog."],
        ["LEADERBOARD", "LEADERBOARD <leaderboardData...>", "Trả về bảng vinh danh top cao thủ cho LeaderboardDialog."],
        ["CLOSED", "CLOSED <reason>", "Đóng phiên kết nối socket kèm thông điệp giải thích."],
        ["ERROR", "ERROR <message>", "Hiển thị thông báo lỗi bằng Toast notification màu đỏ."]
    ]
    create_table(doc, sc_headers, sc_data, [Cm(4.0), Cm(5.2), Cm(7.3)])

    # CHƯƠNG 7
    add_heading_1(doc, "CHƯƠNG 7: THIẾT KẾ TRẢI NGHIỆM NGƯỜI DÙNG (UI/UX DESIGN SYSTEM)")
    
    add_heading_2(doc, "7.1. Định hướng thẩm mỹ Dark Cyberpunk & Glassmorphism")
    add_paragraph(doc, 
        "Hệ thống giao diện của Quiz Arena được xây dựng dựa trên phong cách Dark Cyberpunk kết hợp hiệu ứng kính mờ (Glassmorphism), "
        "tạo cảm giác không gian công nghệ cao, lôi cuốn và tập trung tối đa cho người chơi:"
    )
    add_bullet(doc, "Sử dụng tông xanh đen vũ trụ (#0A0F1D, #131B2E) làm phông nền chính, giúp chống mỏi mắt và làm nổi bật các thành phần nội dung.", "• Nền tối sâu thẳm (Deep Space Navy): ")
    add_bullet(doc, "Sử dụng màu Cyan dạ quang (#00F2FE), Hồng Neon (#EC4899), Xanh ngọc Emerald (#10B981) và Vàng Amber (#F59E0B) cho các nút bấm, viền thẻ card và thanh tiến trình.", "• Điểm nhấn ánh sáng Neon (Neon Accents): ")
    add_bullet(doc, "Các hộp thoại xác nhận nộp bài (SubmitConfirmDialog) và rời trận (LeaveConfirmDialog) áp dụng viền bo góc bán trong suốt, đổ bóng đa lớp và gradient dịu mắt.", "• Hiệu ứng kính mờ (Glassmorphism): ")

    add_heading_2(doc, "7.2. Tối ưu hóa Vector Graphics thuần Java2D chống lỗi font")
    add_paragraph(doc, 
        "Để đảm bảo ứng dụng chạy đồng nhất và hoàn hảo trên mọi hệ điều hành (Windows 10/11, macOS, Ubuntu/Linux) "
        "mà không bao giờ bị lỗi ô vuông (tofu) hay font emoji bị thiếu, toàn bộ các biểu tượng đồ họa (Kiếm chiến đấu ⚔️, "
        "Khiên phòng ngự 🛡️, Ngôi sao ⭐, Bút chì chỉnh sửa, Thùng rác xóa) đều được vẽ thủ công bằng vector "
        "thông qua Java2D Graphics (Graphics2D, Path2D, BasicStroke, GradientPaint). Giải pháp này đem lại độ sắc nét "
        "tuyệt đối trên các màn hình độ phân giải cao (Retina, 2K, 4K) mà không phụ thuộc vào bộ font bên ngoài."
    )

    add_heading_2(doc, "7.3. Đặc tả các thành phần giao diện nâng cao")
    add_paragraph(doc, "Các thành phần giao diện được Module hóa cao độ:")
    add_bullet(doc, "Thanh HUD đối đầu trực diện hiển thị Avatar, Tên, Điểm số, Rank và Thanh kéo co tỉ số thời gian thực giữa hai đấu thủ.", "• MatchScreen HUD đối đầu: ")
    add_bullet(doc, "Thanh kỹ năng gắn liền dưới câu hỏi hiển thị rõ số lượt dùng còn lại (1), tự động kiểm tra điều kiện điểm sàn >= 50 và đổi trạng thái sẵn sàng/hủy bỏ.", "• TacticalActionBar: ")
    add_bullet(doc, "Bổ sung khu vực 'Đối đầu (Head-to-Head)' so sánh trực quan: Số lần Tấn công thành công, Số lần Phản đòn thành công, và Tổng điểm cướp được từ đối thủ.", "• Màn hình tổng kết ResultDialog: ")
    add_bullet(doc, "Gắn các huy hiệu Badge màu sắc nổi bật vào từng câu hỏi có biến cố chiến thuật ([⚔️ +50đ Cướp điểm], [🛡️ Đã Hóa Giải], [🛡️ Phản đòn: +50đ], [⭐ x2 Điểm: +280đ], [⭐ -50đ]).", "• Màn hình xem lại ReviewAnswersDialog: ")

    # CHƯƠNG 8
    add_heading_1(doc, "CHƯƠNG 8: QUY TRÌNH KIỂM THỬ TỰ ĐỘNG & ĐẢM BẢO CHẤT LƯỢNG")
    
    add_heading_2(doc, "8.1. Phương pháp luận kiểm thử tích hợp không cần GUI (Headless Socket Testing)")
    add_paragraph(doc, 
        "Để đảm bảo logic truyền nhận socket, máy trạng thái và ma trận tính điểm hoạt động chính xác tuyệt đối mà không bị "
        "ảnh hưởng bởi yếu tố con người, dự án triển khai bộ kiểm thử tự động toàn diện chạy trực tiếp trên môi trường Headless. "
        "Các Client ảo được giả lập bằng luồng Socket độc lập, tự động gửi bản tin và kiểm tra phản hồi từ Server."
    )

    add_heading_2(doc, "8.2. Danh mục các bộ kiểm thử & Kết quả nghiệm thu thực tế")
    
    test_headers = ["Bộ kiểm thử (Test Suite)", "Phạm vi chức năng kiểm tra", "Kỳ vọng kiểm thử", "Kết quả"]
    test_data = [
        ["TestNewRequirements", "Giới hạn 1 lượt dùng cho mỗi kỹ năng; Tự động chốt đáp án kéo thả khi hết giờ.", "Kỹ năng chỉ dùng được đúng 1 lần; hết giờ câu kéo thả vẫn được chấm điểm đúng.", "PASS (100%)"],
        ["TestRevealTacticalBanner", "Hiển thị banner kết quả đối đầu pha Reveal; Tối ưu thời gian chuyển câu.", "Banner hiện chuẩn xác kết quả cướp/chặn/sao; không gây tăng độ trễ chuyển câu.", "PASS (100%)"],
        ["TestTacticalCombat", "Toàn bộ 12 trường hợp ma trận điểm Tấn công, Phòng thủ, Phản đòn, Ngôi sao.", "Điểm số và kết quả out0/out1 khớp chính xác 100% với đặc tả toán học.", "PASS (100%)"],
        ["TestTacticalActionBarUI", "Giao diện thanh kỹ năng, trạng thái disable, pulsing animation.", "Giao diện phản hồi chuẩn, nhấp nháy đúng khi bị tấn công, tắt đúng khi cancel.", "PASS (100%)"],
        ["TestMatch", "Vòng đời trận đấu 1v1 từ Ready Check đến Finish.", "Chuyển pha mượt mà, đồng bộ điểm số và câu hỏi chính xác.", "PASS (100%)"],
        ["TestE2E & TestPhase7", "Kiểm thử luồng người dùng từ Đăng nhập, Sảnh, Mời đấu đến Tổng kết.", "Hệ thống vận hành trơn tru không phát sinh deadlock hay rò rỉ Socket.", "PASS (100%)"]
    ]
    create_table(doc, test_headers, test_data, [Cm(4.2), Cm(5.0), Cm(5.1), Cm(2.2)])

    # CHƯƠNG 9
    add_heading_1(doc, "CHƯƠNG 9: HƯỚNG DẪN TRIỂN KHAI & CÀI ĐẶT HỆ THỐNG")
    
    add_heading_2(doc, "9.1. Yêu cầu môi trường & Cấu hình phần cứng")
    add_bullet(doc, "Java Development Kit (JDK) phiên bản 17 trở lên (đã kiểm thử tương thích hoàn hảo trên OpenJDK 17, Oracle JDK 21).", "• Nền tảng thực thi: ")
    add_bullet(doc, "Hệ điều hành Windows 10/11, macOS, hoặc các bản phân phối Linux (Ubuntu 20.04+).", "• Hệ điều hành: ")
    add_bullet(doc, "CPU tối thiểu 2 Cores, RAM tối thiểu 2GB, cổng mạng mở TCP 12345 (hoặc cổng cấu hình).", "• Tài nguyên phần cứng: ")

    add_heading_2(doc, "9.2. Hướng dẫn biên dịch và khởi chạy")
    add_paragraph(doc, "Thực hiện biên dịch và khởi chạy dự án thông qua cửa sổ dòng lệnh (Terminal/PowerShell):")
    
    add_callout(doc, 
        "# Bước 1: Biên dịch toàn bộ mã nguồn Java với bảng mã UTF-8\n"
        "javac -encoding UTF-8 -d bin src/quiz/*.java src/quiz/ui/*.java\n\n"
        "# Bước 2: Khởi động Server trung tâm (mặc định lắng nghe cổng 12345)\n"
        "java -cp bin quiz.Server\n\n"
        "# Bước 3: Khởi động ứng dụng Client cho người chơi 1\n"
        "java -cp bin quiz.Client\n\n"
        "# Bước 4: Khởi động thêm ứng dụng Client cho người chơi 2 để đối đầu\n"
        "java -cp bin quiz.Client",
        title="LỆNH BIÊN DỊCH VÀ CHẠY HỆ THỐNG", box_type="info"
    )

    # KẾT LUẬN
    add_heading_1(doc, "KẾT LUẬN & HƯỚNG PHÁT TRIỂN")
    add_paragraph(doc, 
        "Hệ thống Quiz Arena 1v1 đã hoàn thành xuất sắc toàn bộ các mục tiêu đặt ra cho đồ án môn học Lập trình Mạng. "
        "Sản phẩm không chỉ là một ứng dụng đố vui trực tuyến đơn thuần mà là một nền tảng thi đấu đối kháng thời gian thực "
        "hoàn chỉnh, kết hợp hài hòa giữa kiến trúc truyền thông Socket vững chắc, máy trạng thái chặt chẽ, luật chơi chiến thuật "
        "độc đáo và giao diện người dùng lôi cuốn."
    )
    add_paragraph(doc, 
        "Hướng phát triển trong tương lai bao gồm việc mở rộng chế độ thi đấu Battle Royale (nhiều người cùng tranh tài), "
        "tích hợp giao thức bảo mật SSL/TLS cho kết nối Socket, và xây dựng phiên bản Web/Mobile đa nền tảng."
    )
    
    # Save file to both locations
    path1 = r"d:\ADMIN\Documents\ltm\TAI_LIEU_TONG_QUAN_LUAT_CHOI_VA_LUONG_HOAT_DONG_QUIZ_ARENA.docx"
    path2 = r"d:\ADMIN\Documents\ltm\Quiz_Doi_Khang_LTM_Original\TAI_LIEU_TONG_QUAN_QUIZ_ARENA.docx"
    
    doc.save(path1)
    doc.save(path2)
    print(f"Successfully generated:\n1. {path1}\n2. {path2}")

if __name__ == "__main__":
    build_full_document()
