package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识点批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：
 * 学科名 / 章节名 / 小节名 / 知识点名 / 排序(选填，默认 1) / 内容设置(选填，仅文本)。
 * 归属由「学科名 + 章节名 + 小节名」按 t_subject.name + t_chapter.name + t_section.name 匹配定位；
 * 内容设置不支持图片，整格文本作为一条讲解要点（{@code {"points":[文本]}}）。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class KnowledgePointImportRequest {

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
