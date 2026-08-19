package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 章节批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：学科名 / 章节名 / 图标(选填) / 排序(选填，默认 1)。
 * 归属学科由「学科名」列按 t_subject.name 匹配定位。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class ChapterImportRequest {

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
