package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.domain.dto.english.ImportResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 英语单词管理服务接口。
 *
 * @author wisestar
 * @date 2026/8/30
 */
public interface EnglishWordManagerService {

	/**
	 * 单词列表（按版本/年级/单元筛选）。
	 */
	PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query);

	/**
	 * 单词详情。
	 */
	EnglishWordView getDetail(String id);

	/**
	 * 新增单词。
	 */
	void createWord(EnglishWordView word);

	/**
	 * 编辑单词。
	 */
	void updateWord(EnglishWordView word);

	/**
	 * 删除单词。
	 */
	void deleteWord(String id);

	/**
	 * 批量导入单词（Excel）。
	 */
	ImportResult importWords(MultipartFile file);

}
