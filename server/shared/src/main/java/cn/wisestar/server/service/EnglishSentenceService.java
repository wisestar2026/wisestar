package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSentenceQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.ImportResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 英语句库服务（管理与学员共用）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
public interface EnglishSentenceService {

	/**
	 * 句子分页列表。
	 *
	 * @param query 查询条件
	 * @return 分页句子列表
	 */
	PaginationResponse<EnglishSentenceView> list(EnglishSentenceQuery query);

	/**
	 * 新增/更新句子（有 id 为更新，否则新增）。
	 *
	 * @param view 句子
	 */
	void saveOrUpdate(EnglishSentenceView view);

	/**
	 * 删除句子。
	 *
	 * @param id 句子 ID
	 */
	void delete(String id);

	/**
	 * 批量导入句子（Excel）。
	 *
	 * <p>列顺序：版本 / 年级 / 册别 / 单元 / 英文 / 中文 / 音频。
	 * 按「版本 + 年级 + 册别 + 单元 + 英文」去重更新。</p>
	 *
	 * @param file Excel 文件
	 * @return 导入结果
	 */
	ImportResult importSentences(MultipartFile file);

}
