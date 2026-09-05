package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小节批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：学科名 / 章节名 / 小节名 / 年级(选填) / 学期(选填)。
 * 归属由「学科名 + 章节名」按 t_subject.name + t_chapter.name 匹配定位；
 * 排序不参与导入，由系统按所属章节自动追加。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class SectionImportRequest {

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
