from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas

from generate_merged_resume import (
    ROOT, PORTRAIT, MARGIN_X, CONTENT_W, BLUE, INK, MUTED, LINE, WHITE,
    register_fonts, extract_portrait, draw_section_header, draw_wrapped,
    draw_rich_bullet,
)


OUTPUT_PDF = ROOT / "output" / "pdf" / "陈研研_Java后端开发_业务能力版.pdf"
PAGE_W, PAGE_H = A4


def build_pdf():
    OUTPUT_PDF.parent.mkdir(parents=True, exist_ok=True)
    extract_portrait()
    register_fonts()

    c = canvas.Canvas(str(OUTPUT_PDF), pagesize=A4)
    c.setTitle("陈研研 - Java后端开发 - 业务能力版")
    c.setAuthor("陈研研")

    # Header
    c.setFillColor(BLUE)
    c.rect(0, PAGE_H - 16, PAGE_W, 16, fill=1, stroke=0)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 24)
    c.drawString(MARGIN_X, PAGE_H - 55, "陈研研")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 12.5)
    c.drawString(MARGIN_X, PAGE_H - 78, "Java 后端开发  |  电商业务与 AI 应用")

    info_x = 300
    info_y = PAGE_H - 45
    c.setFillColor(MUTED)
    c.setFont("CN", 9.1)
    for line in [
        "电话：19565608221",
        "邮箱：3217810383@qq.com",
    ]:
        c.drawString(info_x, info_y, line)
        info_y -= 19

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

    y = PAGE_H - 122

    # Business profile
    y = draw_section_header(c, "能力概览", y)
    profile = (
        "具备完整电商业务链路理解，能够围绕商品、购物车、营销、订单、支付、库存、售后和会员场景，"
        "从业务问题出发完成流程拆解、接口设计、异常兜底与技术落地；具备将 AI 能力接入真实业务系统的实践经验。"
    )
    y = draw_wrapped(c, profile, MARGIN_X + 3, y, CONTENT_W - 6,
                     size=10.0, leading=15.5, color=INK)
    y -= 6

    tags = [
        ("业务分析", "交易流程梳理、角色权限拆分、异常场景识别与业务规则落地。"),
        ("系统设计", "微服务拆分、异步解耦、缓存应用、统一鉴权和服务治理。"),
        ("AI 业务落地", "意图规划、工具调用、知识库问答、多轮记忆和结构化结果展示。"),
    ]
    for title, body in tags:
        y = draw_rich_bullet(c, title, body, MARGIN_X + 3, y, CONTENT_W - 6)

    y -= 3

    # Project
    y = draw_section_header(c, "项目经验", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 14.0)
    c.drawString(MARGIN_X, y, "mall 微服务电商与 AI 智能客服系统")
    c.setFillColor(BLUE)
    c.setFont("CN-Bold", 9.4)
    c.drawRightString(PAGE_W - MARGIN_X, y + 1, "后端开发  |  2026.01 - 2026.07")
    y -= 20

    c.setFillColor(MUTED)
    c.setFont("CN", 9.1)
    c.drawString(
        MARGIN_X, y,
        "技术栈：Java 17、Spring Boot 3、Spring Cloud Alibaba、MySQL、Redis、RabbitMQ、Spring AI"
    )
    y -= 18

    bullets = [
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
    for title, body in bullets:
        y = draw_rich_bullet(c, title, body, MARGIN_X + 3, y, CONTENT_W - 6)

    y -= 7

    # Skills
    y = draw_section_header(c, "专业技能", y)
    skills = [
        "Java、集合、多线程、JVM；Spring Boot、Spring MVC、MyBatis",
        "Spring Cloud Alibaba、Nacos、Gateway、Sa-Token；MySQL、Redis、RabbitMQ、Elasticsearch",
        "Spring AI、Function Calling、RAG、Chat Memory；Maven、Docker、Nginx、Linux",
    ]
    for item in skills:
        c.setFillColor(BLUE)
        c.circle(MARGIN_X + 6, y + 3, 2.3, fill=1, stroke=0)
        y = draw_wrapped(c, item, MARGIN_X + 18, y, CONTENT_W - 18,
                         size=9.2, leading=13.5, color=INK)
        y -= 2

    y -= 11

    # Education
    y = draw_section_header(c, "教育背景", y)
    c.setFillColor(INK)
    c.setFont("CN-Bold", 10.4)
    c.drawString(MARGIN_X, y, "2023.09 - 2027.07")
    c.drawString(MARGIN_X + 144, y, "邢台学院")
    c.drawRightString(PAGE_W - MARGIN_X, y, "网络工程专业  |  本科")
    y -= 17
    draw_wrapped(
        c,
        "核心课程：数据结构、计算机网络、数据库系统、操作系统、计算机组成原理、协议分析与网络编程。",
        MARGIN_X, y, CONTENT_W, size=8.8, leading=13.0, color=MUTED,
    )

    c.setStrokeColor(LINE)
    c.line(MARGIN_X, 25, PAGE_W - MARGIN_X, 25)
    c.setFillColor(MUTED)
    c.setFont("CN", 7.5)
    c.drawCentredString(PAGE_W / 2, 13, "求职方向：Java 后端开发")

    c.showPage()
    c.save()
    print(OUTPUT_PDF)


if __name__ == "__main__":
    build_pdf()
