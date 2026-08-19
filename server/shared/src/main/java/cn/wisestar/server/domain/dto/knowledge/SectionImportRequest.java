package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 小节批量导入请求（multipart 表单绑定）。
 *
 * <p>Excel 列格式（首行为表头自动跳过）：小节名 / 排序(选填，默认 1)。
 * 归属章节由 chapterId 指定（页面当前选中的章节）。</p>
 *
 * @author wisestar
 * @date 2026/8/17
 */
@Data
public class SectionImportRequest {

	/** 目标章节 ID（t_chapter.id） */
	private String chapterId;

	/** 待导入 Excel 文件 */
	private MultipartFile file;

}
