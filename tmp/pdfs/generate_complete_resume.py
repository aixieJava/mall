from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfgen import canvas

from generate_merged_resume import (
    ROOT, PORTRAIT, MARGIN_X, CONTENT_W, BLUE, INK, MUTED, LINE,
    register_fonts, extract_portrait, draw_section_header, draw_wrapped,
    draw_rich_bullet, wrap_text,
)


OUTPUT_PDF = ROOT / "output" / "pdf" / "陈研研_Java后端开发_完整简历.pdf"
PAGE_W, PAGE_H = A4

PROJECT_BULLETS = [
    (
        "搭建核心交易闭环",
        "围绕商品浏览、购物车促销、订单确认与提交、支付、取消和售后等场景组织服务能力，贯通商品、会员、营销、订单与库存模块，形成完整商城业务链路。"
    ),
    (
        "解决超时订单占用库存",
        "针对未支付订单长期占用库存的问题，设计“下单-延迟消息-状态校验-超时取消-库存回滚”流程，通过 RabbitMQ TTL 与死信队列异步触发关单，避免定时轮询并及时释放可售库存。"
    ),
    (
        "实现多角色访问控制",
        "区分商城会员端与运营管理端身份，在 Gateway 统一处理登录校验、白名单和接口权限；结合 Sa-Token + JWT 降低各业务服务重复鉴权成本，保障后台操作与用户数据访问边界。"
    ),
    (
        "将 AI 从问答升级为业务办理",
        "把商品搜索、商品详情、订单列表和订单详情封装为 ToolCallback，使客服能够识别用户意图后查询真实业务数据；将调用结果转换为商品卡片和订单卡片，提升结果可读性与交互效率。"
    ),
    (
        "提升客服回答准确性与连续性",
        "建设配送、支付、会员、退换货和常见问题知识库，通过查询改写、向量检索与上下文增强降低业务幻觉；使用 Redis 保存最近 10 轮有效问答，支持用户追问和上下文关联。"
    ),
    (
        "完善复杂问题处理机制",
        "设计 Plan-Act-Observe-Reflect-Synthesis 闭环，支持目标拆解、工具执行、结果观察、反思重规划与答复合成；设置最大迭代次数和降级路径，避免流程失控。"
    ),
]


def draw_expanded_bullet(c, title, body, x, y, max_width):
    bullet_x = x
    text_x = x + 15
    c.setFillColor(BLUE)
    c.circle(bullet_x + 3.5, y + 3.1, 2.4, fill=1, stroke=0)

    title_text = f"{title}："
    c.setFillColor(INK)
    c.setFont("CN-Bold", 10.8)
    c.drawString(text_x, y, title_text)
    title_w = pdfmetrics.stringWidth(title_text, "CN-Bold", 10.8)
    first_width = max_width - 15 - title_w

    first = ""
    for i in range(1, len(body) + 1):
        if pdfmetrics.stringWidth(body[:i], "CN", 10.7) <= first_width:
            first = body[:i]
        else:
            break
    rest = body[len(first):]

    c.setFont("CN", 10.7)
    c.drawString(text_x + title_w, y, first)
    y -= 17.2
    for line in wrap_text(rest, "CN", 10.7, max_width - 15):
        c.drawString(text_x, y, line)
        y -= 17.2
    return y - 5


def draw_header(c, full=True):
    c.setFillColor(BLUE)
    c.rect(0, PAGE_H - 16, PAGE_W, 16, fill=1, stroke=0)

    if full:
        c.setFillColor(INK)
        c.setFont("CN-Bold", 24)
        c.drawString(MARGIN_X, PAGE_H - 55, "陈研研")
        c.setFillColor(BLUE)
        c.setFont("CN-Bold", 12.3)
        c.drawString(MARGIN_X, PAGE_H - 78, "Java 后端开发  |  制造业业务、电商与 AI 应用")

        c.setFillColor(MUTED)
        c.setFont("CN", 9.1)
        c.drawString(315, PAGE_H - 45, "电话：19565608221")
        c.drawString(315, PAGE_H - 64, "邮箱：3217810383@qq.com")

        portrait_w, portrait_h = 58, 78
        c.setStrokeColor(LINE)
        c.setLineWidth(0.8)
        c.rect(PAGE_W - MARGIN_X - portrait_w, PAGE_H - 101,
               portrait_w, portrait_h, fill=0, stroke=1)
        c.drawImage(
            ImageReader(str(PORTRAIT)),
            PAGE_W - MARGIN_X - portrait_w + 1,
            PAGE_H - 100,
            portrait_w - 2,
            portrait_h - 2,
            preserveAspectRatio=True,
            anchor="c",
        )
        return PAGE_H - 122

    c.setFillColor(INK)
    c.setFont("CN-Bold", 15)
    c.drawString(MARGIN_X, PAGE_H - 46, "陈研研")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 10)
    c.drawString(MARGIN_X + 62, PAGE_H - 45, "Java 后端开发")
    c.setFillColor(MUTED)
    c.setFont("CN", 8.5)
    c.drawRightString(
        PAGE_W - MARGIN_X, PAGE_H - 45,
        "19565608221  |  3217810383@qq.com"
    )
    return PAGE_H - 72


def draw_footer(c, page_num):
    c.setStrokeColor(LINE)
    c.line(MARGIN_X, 25, PAGE_W - MARGIN_X, 25)
    c.setFillColor(MUTED)
    c.setFont("CN", 7.5)
    c.drawCentredString(
        PAGE_W / 2, 13,
        f"求职方向：Java 后端开发  |  {page_num}/2"
    )


def draw_page_one(c):
    y = draw_header(c, full=True)

    y = draw_section_header(c, "能力概览", y)
    summary = (
        "能够从业务角色、单据流、实物流与状态流拆解需求，识别正常流程、边界条件和异常分支，"
        "并将业务规则落实为状态校验、幂等控制、事务回滚与可追溯数据；具备制造业 MES/WMS、"
        "微服务电商及 AI 业务接入实践。"
    )
    y = draw_wrapped(c, summary, MARGIN_X + 3, y, CONTENT_W - 6,
                     size=10.6, leading=17.0, color=INK)
    y -= 11

    y = draw_section_header(c, "实习经历", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 14.2)
    c.drawString(MARGIN_X, y, "制造业 MES/WMS 一体化管理平台")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 9.4)
    c.drawRightString(
        PAGE_W - MARGIN_X, y + 1,
        "Java 后端开发实习生  |  2026.06 - 2026.07"
    )
    y -= 20

    y = draw_wrapped(
        c,
        "业务范围：ERP 对接、生产计划、工单执行、供应商协同、仓储管理、质量检验及设备数据采集。",
        MARGIN_X, y, CONTENT_W, size=9.2, leading=14, color=MUTED
    )
    y = draw_wrapped(
        c,
        "技术栈：Spring Boot、Spring Cloud Alibaba、Nacos、OpenFeign、Seata、MyBatis-Plus、MySQL、Redis",
        MARGIN_X, y, CONTENT_W, size=9.2, leading=14, color=MUTED
    )
    y -= 5

    internship_bullets = [
        (
            "梳理制造业务闭环",
            "明确 ERP 管订单与经营凭证、MES 管计划和生产执行、WMS 管实物库存、QMS 管质量门禁；以订单号、工单号、箱码、批次和库位串联“订单-排产-领料-生产-质检-入库-报工”。"
        ),
        (
            "贯通计划、生产与质量",
            "结合 BOM、工厂日历、产线能力和物料齐套情况拆分生产任务，通过工单状态流控制下发、生产、报工和完工；在到货、领料、完工及出货节点联动 IQC、IPQC、FQC、OQC 检验。"
        ),
        (
            "解决余料流转失真",
            "针对部分下料实现自动拆箱与新箱码生成，分别记录消耗量和剩余量；下料完成后扣减线边仓库存并生成退料单，退料入库同步生成源仓出库和目标仓入库记录，保证物料可追溯。"
        ),
        (
            "保障跨模块数据一致",
            "在 PDA 退料等场景校验单据状态、箱码归属、物料、数量及源/目标库位；使用 OpenFeign + Seata 协同生产、仓储和供应商服务，并结合重复扫码拦截、终态校验与异常回滚避免重复入库。"
        ),
        (
            "理解供应与设备协同",
            "梳理“采购订单-供应商发货-到货质检-仓储入库”链路；结合 Quartz 生成设备点检、保养和校准任务，并通过 MQTT、OPC UA 采集设备数据，使生产执行结果能够追溯并回传 ERP。"
        ),
    ]
    for title, body in internship_bullets:
        y = draw_expanded_bullet(c, title, body, MARGIN_X + 3, y,
                                 CONTENT_W - 6)

    y -= 5
    y = draw_section_header(c, "项目经验", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 13.5)
    c.drawString(MARGIN_X, y, "mall 微服务电商与 AI 智能客服系统")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 9.2)
    c.drawRightString(
        PAGE_W - MARGIN_X, y + 1,
        "后端开发  |  2026.01 - 2026.07"
    )
    y -= 19
    y = draw_wrapped(
        c,
        "技术栈：Java 17、Spring Boot 3、Spring Cloud Alibaba、MySQL、Redis、RabbitMQ、Spring AI",
        MARGIN_X, y, CONTENT_W, size=9.0, leading=13.5, color=MUTED
    )
    y -= 4
    for title, body in PROJECT_BULLETS[:2]:
        y = draw_rich_bullet(c, title, body, MARGIN_X + 3, y,
                             CONTENT_W - 6)

    draw_footer(c, 1)
    c.showPage()


def draw_page_two(c):
    y = draw_header(c, full=False)

    y = draw_section_header(c, "项目经验（续）", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 14.2)
    c.drawString(MARGIN_X, y, "mall 微服务电商与 AI 智能客服系统")
    y -= 23
    for title, body in PROJECT_BULLETS[2:]:
        y = draw_expanded_bullet(c, title, body, MARGIN_X + 3, y,
                                 CONTENT_W - 6)

    y -= 5
    y = draw_section_header(c, "专业技能", y)
    skills = [
        "Java、集合、多线程、JVM；Spring Boot、Spring MVC、MyBatis-Plus",
        "MySQL、Redis、RabbitMQ、Elasticsearch；Spring Cloud Alibaba、Nacos、Gateway、OpenFeign、Seata",
        "Spring AI、Function Calling、RAG、Chat Memory；Maven、Docker、Nginx、Linux",
    ]
    for item in skills:
        c.setFillColor(BLUE)
        c.circle(MARGIN_X + 6, y + 3, 2.3, fill=1, stroke=0)
        y = draw_wrapped(c, item, MARGIN_X + 18, y, CONTENT_W - 18,
                         size=9.2, leading=13.5, color=INK)
        y -= 2

    y -= 5
    y = draw_section_header(c, "教育背景", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 10.5)
    c.drawString(MARGIN_X, y, "2023.09 - 2027.07")
    c.drawString(MARGIN_X + 144, y, "邢台学院")
    c.drawRightString(PAGE_W - MARGIN_X, y, "网络工程专业  |  本科")
    y -= 18
    draw_wrapped(
        c,
        "核心课程：数据结构、计算机网络、数据库系统、操作系统、计算机组成原理、协议分析与网络编程。",
        MARGIN_X, y, CONTENT_W, size=8.9, leading=13.5, color=MUTED
    )

    draw_footer(c, 2)
    c.showPage()


def build_pdf():
    OUTPUT_PDF.parent.mkdir(parents=True, exist_ok=True)
    extract_portrait()
    register_fonts()
    c = canvas.Canvas(str(OUTPUT_PDF), pagesize=A4)
    c.setTitle("陈研研 - Java后端开发 - 完整简历")
    c.setAuthor("陈研研")
    draw_page_one(c)
    draw_page_two(c)
    c.save()
    print(OUTPUT_PDF)


if __name__ == "__main__":
    build_pdf()
