package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小节批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：学科名 / 章节名 / 小节名 / 排序(选填，默认 1)。
 * 归属由「学科名 + 章节名」按 t_subject.name + t_chapter.name 匹配定位。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class SectionImportRequest {

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
