package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识点批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：知识点名 / 排序(选填，默认 1)。
 * 归属小节由 sectionId 指定（页面当前选中的小节）。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class KnowledgePointImportRequest {

	/** 目标小节 ID（t_section.id） */
	private String sectionId;

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
